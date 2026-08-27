package com.trackflow.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

@Inject
class GpsLocationProvider(
    private val context: Context,
    private val timeProvider: TimeProvider
) : LocationProvider {

    private var isTracking = false

    // applicationContext so a caller-supplied Activity context can't be leaked.
    // getSystemService(Class) needs API 23; this module's minSdk is 24.
    private val locationManager: LocationManager =
        requireNotNull(context.applicationContext.getSystemService(LocationManager::class.java)) {
            "LocationManager unavailable"
        }

    override fun startTracking() {
        isTracking = true
    }

    override fun stopTracking() {
        isTracking = false
    }

    @SuppressLint("MissingPermission")
    // getLastKnownLocation() is blocking binder IPC — keep it off the caller's (often main) thread.
    override suspend fun lastKnownLocation(): Location? = withContext(Dispatchers.IO) {
        // GPS often has no cached fix indoors; NETWORK/PASSIVE frequently do. Pick the freshest.
        LAST_KNOWN_PROVIDERS
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            // elapsedRealtimeNanos is monotonic; wall-clock `time` can jump on clock changes.
            .maxByOrNull { it.elapsedRealtimeNanos }
            ?.let { convertToLocation(it, timeProvider) }
    }

    @SuppressLint("MissingPermission")
    override fun locationUpdates(): Flow<Location> = callbackFlow {
        val locationListener = LocationListener { androidLocation ->
            if (!isTracking) return@LocationListener

            trySend(convertToLocation(androidLocation, timeProvider))
        }

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            2000L,
            0f,
            locationListener,
            Looper.getMainLooper()
        )

        awaitClose {
            locationManager.removeUpdates(locationListener)
        }
    }

    private companion object {
        private val LAST_KNOWN_PROVIDERS = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
    }
}
