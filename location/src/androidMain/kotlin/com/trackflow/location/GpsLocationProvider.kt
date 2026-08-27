package com.trackflow.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Inject
class GpsLocationProvider constructor(
    private val context: Context,
    private val timeProvider: TimeProvider
) : LocationProvider {

    private var isTracking = false

    override fun startTracking() {
        isTracking = true
    }

    override fun stopTracking() {
        isTracking = false
    }

    @SuppressLint("MissingPermission")
    override fun locationUpdates(): Flow<Location> = callbackFlow {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener = LocationListener { androidLoc ->
            if (!isTracking) return@LocationListener

            trySend(convertToLocation(androidLoc, timeProvider))
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
}
