package com.landoulsi.diagnostic.location

import com.landoulsi.diagnostic.DiagnosticCheck
import com.landoulsi.diagnostic.DiagnosticResult
import com.landoulsi.diagnostic.DiagnosticState

/**
 * Type of location provider used for acquiring geographic fixes.
 */
enum class LocationProviderType {
    GPS,
    NETWORK,
    PASSIVE,
    FUSED,
    CELLULAR,
    WIFI,
    UNKNOWN,
    NONE
}

/**
 * Snapshot of current location service availability and accuracy.
 *
 * @property isLocationServicesEnabled Whether system location services / GPS are enabled.
 * @property isPermissionGranted Whether the app has permission to access location.
 * @property accuracyMeters Estimated horizontal accuracy in meters (lower is better), or null if no fix.
 * @property providerType Active or last known location provider type.
 * @property details Additional diagnostics metadata.
 */
data class LocationStatusSnapshot(
    val isLocationServicesEnabled: Boolean = true,
    val isPermissionGranted: Boolean = true,
    val accuracyMeters: Float? = null,
    val providerType: LocationProviderType = LocationProviderType.UNKNOWN,
    val details: Map<String, String> = emptyMap()
)

/**
 * Platform reader contract providing [LocationStatusSnapshot].
 */
interface LocationDiagnosticsProvider {
    suspend fun getLocationSnapshot(): LocationStatusSnapshot
}

/**
 * Diagnostic check that evaluates whether location services and GPS are enabled and permitted.
 */
class GpsStatusDiagnosticCheck(
    private val provider: LocationDiagnosticsProvider
) : DiagnosticCheck {
    override val id: String = CHECK_ID
    override val name: String = "GPS & Location Services Status"

    override suspend fun run(): DiagnosticResult {
        val snapshot = provider.getLocationSnapshot()

        val (state, cause) = when {
            !snapshot.isPermissionGranted -> {
                DiagnosticState.ERROR to "Location permission not granted"
            }
            !snapshot.isLocationServicesEnabled -> {
                DiagnosticState.ERROR to "Location services / GPS disabled"
            }
            else -> {
                DiagnosticState.PASS to null
            }
        }

        val metadata = buildMap {
            put("permissionGranted", snapshot.isPermissionGranted.toString())
            put("servicesEnabled", snapshot.isLocationServicesEnabled.toString())
            put("provider", snapshot.providerType.name)
            putAll(snapshot.details)
        }

        return DiagnosticResult(
            id = id,
            title = name,
            state = state,
            cause = cause,
            metadata = metadata
        )
    }

    companion object {
        const val CHECK_ID = "gps_status"
    }
}

/**
 * Diagnostic check that evaluates location fix accuracy against an acceptable threshold.
 */
class LocationAccuracyDiagnosticCheck(
    private val provider: LocationDiagnosticsProvider,
    private val accuracyThresholdMeters: Float = DEFAULT_ACCURACY_THRESHOLD_METERS
) : DiagnosticCheck {
    override val id: String = CHECK_ID
    override val name: String = "Location Accuracy"

    override suspend fun run(): DiagnosticResult {
        val snapshot = provider.getLocationSnapshot()

        val (state, cause) = when {
            !snapshot.isPermissionGranted -> {
                DiagnosticState.ERROR to "Location permission not granted"
            }
            !snapshot.isLocationServicesEnabled -> {
                DiagnosticState.ERROR to "Location services disabled"
            }
            snapshot.accuracyMeters == null -> {
                DiagnosticState.WARNING to "Location accuracy unavailable (no fix)"
            }
            snapshot.accuracyMeters > accuracyThresholdMeters -> {
                DiagnosticState.WARNING to "Low location accuracy (${snapshot.accuracyMeters}m > ${accuracyThresholdMeters.toInt()}m threshold)"
            }
            else -> {
                DiagnosticState.PASS to null
            }
        }

        val metadata = buildMap {
            put("permissionGranted", snapshot.isPermissionGranted.toString())
            put("servicesEnabled", snapshot.isLocationServicesEnabled.toString())
            put("provider", snapshot.providerType.name)
            snapshot.accuracyMeters?.let { put("accuracyMeters", it.toString()) }
            putAll(snapshot.details)
        }

        return DiagnosticResult(
            id = id,
            title = name,
            state = state,
            cause = cause,
            metadata = metadata
        )
    }

    companion object {
        const val CHECK_ID = "location_accuracy"
        const val DEFAULT_ACCURACY_THRESHOLD_METERS = 100f
    }
}
