package com.landoulsi.diagnostic.location

import com.landoulsi.diagnostic.DiagnosticState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LocationDiagnosticsTest {

    private class FakeLocationDiagnosticsProvider(
        var snapshot: LocationStatusSnapshot = LocationStatusSnapshot()
    ) : LocationDiagnosticsProvider {
        override suspend fun getLocationSnapshot(): LocationStatusSnapshot = snapshot
    }

    @Test
    fun gpsStatusCheck_whenPermissionNotGranted_returnsError() = runTest {
        val provider = FakeLocationDiagnosticsProvider(
            LocationStatusSnapshot(
                isLocationServicesEnabled = true,
                isPermissionGranted = false,
                providerType = LocationProviderType.NONE
            )
        )
        val check = GpsStatusDiagnosticCheck(provider)
        val result = check.run()

        assertEquals(DiagnosticState.ERROR, result.state)
        assertEquals("Location permission not granted", result.cause)
        assertEquals("false", result.metadata["permissionGranted"])
    }

    @Test
    fun gpsStatusCheck_whenLocationServicesDisabled_returnsError() = runTest {
        val provider = FakeLocationDiagnosticsProvider(
            LocationStatusSnapshot(
                isLocationServicesEnabled = false,
                isPermissionGranted = true,
                providerType = LocationProviderType.NONE
            )
        )
        val check = GpsStatusDiagnosticCheck(provider)
        val result = check.run()

        assertEquals(DiagnosticState.ERROR, result.state)
        assertEquals("Location services / GPS disabled", result.cause)
        assertEquals("false", result.metadata["servicesEnabled"])
    }

    @Test
    fun gpsStatusCheck_whenEnabledAndPermitted_returnsPass() = runTest {
        val provider = FakeLocationDiagnosticsProvider(
            LocationStatusSnapshot(
                isLocationServicesEnabled = true,
                isPermissionGranted = true,
                providerType = LocationProviderType.GPS
            )
        )
        val check = GpsStatusDiagnosticCheck(provider)
        val result = check.run()

        assertEquals(DiagnosticState.PASS, result.state)
        assertNull(result.cause)
        assertEquals("true", result.metadata["servicesEnabled"])
        assertEquals("true", result.metadata["permissionGranted"])
    }

    @Test
    fun locationAccuracyCheck_whenNoFix_returnsWarning() = runTest {
        val provider = FakeLocationDiagnosticsProvider(
            LocationStatusSnapshot(
                isLocationServicesEnabled = true,
                isPermissionGranted = true,
                accuracyMeters = null,
                providerType = LocationProviderType.GPS
            )
        )
        val check = LocationAccuracyDiagnosticCheck(provider)
        val result = check.run()

        assertEquals(DiagnosticState.WARNING, result.state)
        assertEquals("Location accuracy unavailable (no fix)", result.cause)
    }

    @Test
    fun locationAccuracyCheck_whenLowAccuracy_returnsWarning() = runTest {
        val provider = FakeLocationDiagnosticsProvider(
            LocationStatusSnapshot(
                isLocationServicesEnabled = true,
                isPermissionGranted = true,
                accuracyMeters = 350f,
                providerType = LocationProviderType.NETWORK
            )
        )
        val check = LocationAccuracyDiagnosticCheck(provider, accuracyThresholdMeters = 100f)
        val result = check.run()

        assertEquals(DiagnosticState.WARNING, result.state)
        assertNotNull(result.cause)
        assertEquals("350.0", result.metadata["accuracyMeters"])
    }

    @Test
    fun locationAccuracyCheck_whenHighAccuracy_returnsPass() = runTest {
        val provider = FakeLocationDiagnosticsProvider(
            LocationStatusSnapshot(
                isLocationServicesEnabled = true,
                isPermissionGranted = true,
                accuracyMeters = 15f,
                providerType = LocationProviderType.GPS
            )
        )
        val check = LocationAccuracyDiagnosticCheck(provider, accuracyThresholdMeters = 100f)
        val result = check.run()

        assertEquals(DiagnosticState.PASS, result.state)
        assertNull(result.cause)
        assertEquals("15.0", result.metadata["accuracyMeters"])
    }
}
