package com.landoulsi.location

import com.landoulsi.location.network.IpGeolocationResponse
import com.landoulsi.location.network.createLocationHttpClient
import com.landoulsi.logger.Logger
import dev.zacsweers.metro.Inject
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default reported [Location.accuracy] (metres) for an IP-derived fix.
 *
 * IP geolocation resolves, at best, to a city — commonly tens of kilometres off, and much worse
 * on mobile carrier networks. `50 km` is a deliberately conservative radius so that consumers
 * filtering on `accuracy` can tell an approximate fix from a real GPS/fused one. Same unit as
 * Android `Location.getAccuracy()` and iOS `CLLocation.horizontalAccuracy`. Override per instance
 * via [IpLocationConfig.approximateAccuracyMeters].
 */
const val IP_LOCATION_ACCURACY_METERS: Double = 50_000.0

/**
 * Keyless public IP-geolocation service used as the zero-config default. Fine for development
 * and low volume; production apps should override [IpLocationConfig.endpointUrl] with a
 * first-party proxy (see that field's docs). Using this default logs a one-time warning.
 */
const val DEFAULT_IP_ENDPOINT: String = "https://ipwho.is/"

/** Tuning for [IpLocationProvider]. Defaults target the keyless `ipwho.is` service. */
data class IpLocationConfig(
    /**
     * Endpoint returning JSON with top-level `latitude` / `longitude`. Must be HTTPS.
     *
     * Defaults to [DEFAULT_IP_ENDPOINT], the keyless public `ipwho.is` service — fine for
     * development and low volume. Production apps should point this at a first-party backend
     * proxy: the public default sends the client IP (PII under GDPR/CCPA) to an unauthenticated
     * third party and gives no rate-limit or uptime guarantee across carrier CGNAT ranges.
     */
    val endpointUrl: String = DEFAULT_IP_ENDPOINT,
    /** Value reported as [Location.accuracy] for fixes from this provider. */
    val approximateAccuracyMeters: Double = IP_LOCATION_ACCURACY_METERS,
) {
    init {
        val parsed = try {
            Url(endpointUrl)
        } catch (e: Exception) {
            throw IllegalArgumentException("endpointUrl is not a valid URL: $endpointUrl", e)
        }
        require(parsed.protocol == URLProtocol.HTTPS && parsed.host.isNotBlank()) {
            // HTTPS only: the client IP is PII and must not travel in the clear.
            "endpointUrl must be a valid https:// URL: $endpointUrl"
        }
        require(approximateAccuracyMeters.isFinite() && approximateAccuracyMeters > 0.0) {
            "approximateAccuracyMeters must be a positive, finite number: $approximateAccuracyMeters"
        }
    }
}

/**
 * [LocationProvider] that estimates the device's position from its public IP address.
 *
 * This is the fallback used *before* the OS location permission is granted: it needs no
 * permission and no GPS, only network access, and yields a coarse, city-level fix. Once a real
 * permission-backed provider ([FusedLocationProvider] / [GpsLocationProvider] / iOS
 * `IosLocationProvider`) starts emitting, callers should prefer it.
 *
 * Primary entry point is [lastKnownLocation]: it returns the IP fix this instance has already
 * cached, or fetches one once if the cache is empty (concurrent callers share that single
 * request; a later call retries if the fetch failed). Use it to render an approximate starting
 * position immediately, with no permission and no stream to spin up.
 *
 * An IP fix only moves when the network egress does (Wi-Fi/cellular switch, VPN, travel), so
 * there is deliberately **no periodic refresh**. [locationUpdates] exists to fit the common
 * interface: once tracking is enabled it emits the cached-or-once-fetched fix (if any) and then
 * **completes** — unlike the streaming providers, whose flows stay open. It never stalls: a
 * failed fetch just yields an empty, completed flow. Re-collect, or call [lastKnownLocation],
 * to retry.
 *
 * Construct via DI with `(httpClient, timeProvider)` — a graph-managed [httpClient] is used
 * as-is and never closed here. Callers without a graph use the top-level
 * `IpLocationProvider(timeProvider, config)` factory, which owns its [HttpClient]; [close] it
 * (or treat the instance as process-lived) to release the engine's pools.
 */
@Inject
class IpLocationProvider(
    private val httpClient: HttpClient,
    private val timeProvider: TimeProvider,
    private val config: IpLocationConfig = IpLocationConfig(),
    /** True only when this instance created [httpClient] itself (via the no-client factory). */
    internal val ownsHttpClient: Boolean = false,
) : LocationProvider, AutoCloseable {

    private val tracking = MutableStateFlow(false)
    private val cachedLocation = MutableStateFlow<Location?>(null)
    private val fetchMutex = Mutex()
    private val closed = MutableStateFlow(false)

    init {
        if (config.endpointUrl == DEFAULT_IP_ENDPOINT) {
            Logger.w(
                TAG,
                "Using the keyless public endpoint $DEFAULT_IP_ENDPOINT. Set " +
                    "IpLocationConfig.endpointUrl to a first-party proxy for production: it sends " +
                    "the client IP to an unauthenticated third party with no rate-limit/SLA.",
            )
        }
    }

    override fun startTracking() {
        tracking.value = true
    }

    override fun stopTracking() {
        tracking.value = false
    }

    /**
     * The IP fix this instance has cached, or one fetched on the spot if the cache is empty.
     * Returns `null` only when nothing is cached and the lookup fails. Concurrent first-time
     * callers coalesce onto a single request.
     */
    override suspend fun lastKnownLocation(): Location? {
        cachedLocation.value?.let { return it }
        return fetchMutex.withLock {
            // Re-check under the lock: the cache may have filled while we waited, or close()
            // may have run (a coroutine already queued on the mutex must not fetch afterwards).
            cachedLocation.value?.let { return@withLock it }
            if (closed.value) return@withLock null
            try {
                fetchAndCache()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(TAG, "IP geolocation lookup failed: ${e.message}")
                null
            }
        }
    }

    override fun locationUpdates(): Flow<Location> = flow {
        tracking.first { it }              // park until tracking is enabled
        lastKnownLocation()?.let { emit(it) } // one coarse fix at most, then the flow completes
    }

    /**
     * Drops the cached fix so the next [lastKnownLocation] / [locationUpdates] re-queries. Call
     * this when the network egress may have changed (Wi-Fi/cellular switch, VPN, travel) — the
     * provider does not refresh on its own.
     */
    fun invalidateCache() {
        cachedLocation.value = null
    }

    /**
     * Releases the [HttpClient] only if this instance created it; a DI-supplied client is left
     * alone. After this, [lastKnownLocation] returns the cached fix if present, else `null`.
     */
    override fun close() {
        closed.value = true
        if (ownsHttpClient) httpClient.close()
    }

    private suspend fun fetchAndCache(): Location? =
        fetchApproximateLocation()?.also { cachedLocation.value = it }

    private suspend fun fetchApproximateLocation(): Location? {
        val httpResponse = httpClient.get(config.endpointUrl) {
            header(HttpHeaders.Accept, "application/json")
        }
        if (!httpResponse.status.isSuccess()) {
            // Error pages are commonly HTML; don't even try to decode them as the DTO.
            Logger.w(TAG, "IP geolocation HTTP ${httpResponse.status.value}")
            return null
        }

        val response: IpGeolocationResponse = httpResponse.body()

        if (response.isFailure) {
            Logger.w(TAG, "IP geolocation endpoint reported failure: ${response.failureText ?: "unknown"}")
            return null
        }

        val latitude = response.latitude
        val longitude = response.longitude
        if (latitude == null || longitude == null) {
            Logger.w(TAG, "IP geolocation response missing coordinates")
            return null
        }
        if (latitude !in MIN_LATITUDE..MAX_LATITUDE || longitude !in MIN_LONGITUDE..MAX_LONGITUDE) {
            Logger.w(TAG, "IP geolocation response out of range: ($latitude, $longitude)")
            return null
        }
        if (latitude == 0.0 && longitude == 0.0) {
            // "Null Island" — the canonical sentinel for a failed geocode.
            Logger.w(TAG, "IP geolocation response is null-island (0, 0)")
            return null
        }

        return Location(
            latitude = latitude,
            longitude = longitude,
            accuracy = config.approximateAccuracyMeters,
            speed = null,
            bearing = null,
            timestamp = timeProvider.currentTimestamp(),
        )
    }

    private companion object {
        const val TAG = "IpLocationProvider"
        const val MIN_LATITUDE = -90.0
        const val MAX_LATITUDE = 90.0
        const val MIN_LONGITUDE = -180.0
        const val MAX_LONGITUDE = 180.0
    }
}

/**
 * Convenience factory for callers without a DI graph (e.g. the iOS entry point): pairs
 * [IpLocationProvider] with a platform [HttpClient] that the returned instance owns. Call
 * [IpLocationProvider.close] when done, or keep the instance for the process lifetime.
 */
fun IpLocationProvider(
    timeProvider: TimeProvider,
    config: IpLocationConfig = IpLocationConfig(),
): IpLocationProvider = IpLocationProvider(
    httpClient = createLocationHttpClient(),
    timeProvider = timeProvider,
    config = config,
    ownsHttpClient = true,
)
