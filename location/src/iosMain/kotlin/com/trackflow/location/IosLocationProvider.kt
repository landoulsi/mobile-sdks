package com.trackflow.location

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
            val loc = Location(
                latitude = clLoc.coordinate.useContents { latitude },
                longitude = clLoc.coordinate.useContents { longitude },
                accuracy = clLoc.horizontalAccuracy.takeIf { it >= 0 },
                speed = clLoc.speed.takeIf { it >= 0 },
                bearing = clLoc.course.takeIf { it >= 0 },
                timestamp = timeProvider.currentTimestamp()
            )
            locationCallback?.invoke(loc)
        }
    }

    init {
        locationManager.delegate = delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.requestWhenInUseAuthorization()
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
