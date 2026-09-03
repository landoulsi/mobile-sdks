package com.landoulsi.diagnostic.network

import com.landoulsi.diagnostic.DiagnosticState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NetworkDiagnosticsTest {

    private class FakeNetworkDiagnosticsProvider(
        var snapshot: NetworkStatusSnapshot = NetworkStatusSnapshot()
    ) : NetworkDiagnosticsProvider {
        override suspend fun getNetworkSnapshot(): NetworkStatusSnapshot = snapshot
    }

    @Test
    fun vpnCheck_whenNoVpnOrProxy_returnsPass() = runTest {
        val provider = FakeNetworkDiagnosticsProvider(
            NetworkStatusSnapshot(
                isConnected = true,
                isVpnActive = false,
                isProxyActive = false,
                transportType = NetworkTransportType.WIFI
            )
        )
        val check = VpnDiagnosticCheck(provider)
        val result = check.run()

        assertEquals(DiagnosticState.PASS, result.state)
        assertNull(result.cause)
        assertEquals("false", result.metadata["vpnActive"])
        assertEquals("false", result.metadata["proxyActive"])
    }

    @Test
    fun vpnCheck_whenVpnActive_returnsWarning() = runTest {
        val provider = FakeNetworkDiagnosticsProvider(
            NetworkStatusSnapshot(
                isConnected = true,
                isVpnActive = true,
                isProxyActive = false,
                transportType = NetworkTransportType.VPN
            )
        )
        val check = VpnDiagnosticCheck(provider)
        val result = check.run()

        assertEquals(DiagnosticState.WARNING, result.state)
        assertEquals("Active VPN detected", result.cause)
        assertEquals("true", result.metadata["vpnActive"])
    }

    @Test
    fun vpnCheck_whenProxyActive_returnsWarning() = runTest {
        val provider = FakeNetworkDiagnosticsProvider(
            NetworkStatusSnapshot(
                isConnected = true,
                isVpnActive = false,
                isProxyActive = true,
                transportType = NetworkTransportType.WIFI
            )
        )
        val check = VpnDiagnosticCheck(provider)
        val result = check.run()

        assertEquals(DiagnosticState.WARNING, result.state)
        assertEquals("Active proxy detected", result.cause)
        assertEquals("true", result.metadata["proxyActive"])
    }

    @Test
    fun vpnCheck_whenBothVpnAndProxyActive_returnsWarning() = runTest {
        val provider = FakeNetworkDiagnosticsProvider(
            NetworkStatusSnapshot(
                isConnected = true,
                isVpnActive = true,
                isProxyActive = true,
                transportType = NetworkTransportType.VPN
            )
        )
        val check = VpnDiagnosticCheck(provider)
        val result = check.run()

        assertEquals(DiagnosticState.WARNING, result.state)
        assertEquals("Active VPN and proxy detected", result.cause)
    }

    @Test
    fun networkSignalCheck_whenOffline_returnsError() = runTest {
        val provider = FakeNetworkDiagnosticsProvider(
            NetworkStatusSnapshot(
                isConnected = false,
                transportType = NetworkTransportType.NONE
            )
        )
        val check = NetworkSignalDiagnosticCheck(provider)
        val result = check.run()

        assertEquals(DiagnosticState.ERROR, result.state)
        assertEquals("No active network connection", result.cause)
        assertEquals("false", result.metadata["connected"])
    }

    @Test
    fun networkSignalCheck_whenLowSignal_returnsWarning() = runTest {
        val provider = FakeNetworkDiagnosticsProvider(
            NetworkStatusSnapshot(
                isConnected = true,
                signalStrengthPercent = 15,
                transportType = NetworkTransportType.CELLULAR
            )
        )
        val check = NetworkSignalDiagnosticCheck(provider, lowSignalThresholdPercent = 25)
        val result = check.run()

        assertEquals(DiagnosticState.WARNING, result.state)
        assertNotNull(result.cause)
        assertEquals("15", result.metadata["signalPercent"])
    }

    @Test
    fun networkSignalCheck_whenStrongSignal_returnsPass() = runTest {
        val provider = FakeNetworkDiagnosticsProvider(
            NetworkStatusSnapshot(
                isConnected = true,
                signalStrengthPercent = 85,
                transportType = NetworkTransportType.WIFI
            )
        )
        val check = NetworkSignalDiagnosticCheck(provider)
        val result = check.run()

        assertEquals(DiagnosticState.PASS, result.state)
        assertNull(result.cause)
        assertEquals("85", result.metadata["signalPercent"])
    }
}
