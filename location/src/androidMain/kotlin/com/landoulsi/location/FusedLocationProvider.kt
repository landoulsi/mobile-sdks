package com.landoulsi.location

import android.annotation.SuppressLint
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.landoulsi.logger.Logger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Inject
class FusedLocationProvider(
    private val fusedLocationClient: FusedLocationProviderClient,
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
    override suspend fun lastKnownLocation(): Location? {
        val androidLocation = try {
            fusedLocationClient.lastLocation.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // SecurityException (permission revoked), ApiException (Play Services), etc.
            Logger.w(TAG, "lastLocation unavailable: ${e.message}")
            null
        }
        return androidLocation?.let { convertToLocation(it, timeProvider) }
    }

    @SuppressLint("MissingPermission")
    override fun locationUpdates(): Flow<Location> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_UPDATE_INTERVAL_MS)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (!isTracking) return

                result.lastLocation?.let { androidLocation ->
                    trySend(convertToLocation(androidLocation, timeProvider))
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private companion object {
        const val TAG = "FusedLocationProvider"
        const val UPDATE_INTERVAL_MS = 5_000L
        const val MIN_UPDATE_INTERVAL_MS = 2_000L
    }
}
