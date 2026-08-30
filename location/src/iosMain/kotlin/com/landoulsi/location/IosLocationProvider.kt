package com.landoulsi.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.CoreLocation.CLLocation
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class IosLocationProvider(
    private val timeProvider: TimeProvider
) : LocationProvider {
    private val locationManager = CLLocationManager()
    private var isTracking = false
    private var locationCallback: ((Location) -> Unit)? = null

    private val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(
            manager: CLLocationManager,
            didUpdateLocations: List<*>
        ) {
            if (!isTracking) return
            val clLoc = didUpdateLocations.lastOrNull() as? CLLocation ?: return
            clLoc.toLocationOrNull(timeProvider)?.let { locationCallback?.invoke(it) }
        }
    }

    init {
        locationManager.delegate = delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.requestWhenInUseAuthorization()
    }

    // CLLocationManager must be touched on the main thread; a suspend fn may resume elsewhere.
    override suspend fun lastKnownLocation(): Location? = withContext(Dispatchers.Main) {
        locationManager.location?.toLocationOrNull(timeProvider)
    }

    override fun startTracking() {
        isTracking = true
        locationManager.startUpdatingLocation()
    }

    override fun stopTracking() {
        isTracking = false
        locationManager.stopUpdatingLocation()
    }

    override fun locationUpdates(): Flow<Location> = callbackFlow {
        locationCallback = { trySend(it) }
        awaitClose { locationCallback = null }
    }
}

/**
 * Converts a [CLLocation] to a [Location], or `null` when the fix is unusable.
 *
 * A negative `horizontalAccuracy` means CoreLocation could not determine the coordinate and its
 * latitude/longitude must not be used (per Apple's `CLLocation` docs).
 */
@OptIn(ExperimentalForeignApi::class)
internal fun CLLocation.toLocationOrNull(timeProvider: TimeProvider): Location? {
    if (horizontalAccuracy < 0.0) return null
    return Location(
        latitude = coordinate.useContents { latitude },
        longitude = coordinate.useContents { longitude },
        accuracy = horizontalAccuracy,
        speed = speed.takeIf { it >= 0 },
        bearing = course.takeIf { it >= 0 },
        // Per the TimeProvider contract every Location.timestamp is the SDK's own RFC-3339 clock,
        // not the platform fix time (CLLocation.timestamp). A cached fix from lastKnownLocation()
        // is therefore stamped "now", not when CoreLocation acquired it.
        timestamp = timeProvider.currentTimestamp(),
    )
}
