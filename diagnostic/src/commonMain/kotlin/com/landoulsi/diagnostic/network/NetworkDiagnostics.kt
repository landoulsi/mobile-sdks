package com.landoulsi.diagnostic.network

import com.landoulsi.diagnostic.DiagnosticCheck
import com.landoulsi.diagnostic.DiagnosticResult
import com.landoulsi.diagnostic.DiagnosticState

/**
 * Underlying network transport type.
 */
enum class NetworkTransportType {
    WIFI,
    CELLULAR,
    ETHERNET,
    BLUETOOTH,
    VPN,
    OTHER,
    UNKNOWN,
    NONE
}

/**
 * Snapshot of current network telemetry and connection status.
 *
 * @property isConnected Whether the device has an active network connection.
 * @property isVpnActive Whether an active VPN tunnel was detected.
 * @property isProxyActive Whether an active HTTP/HTTPS proxy configuration was detected.
 * @property signalStrengthPercent Signal strength scaled 0-100%, or null if unavailable.
 * @property transportType Active transport mechanism.
 * @property details Additional diagnostics metadata.
 */
data class NetworkStatusSnapshot(
    val isConnected: Boolean = true,
    val isVpnActive: Boolean = false,
    val isProxyActive: Boolean = false,
    val signalStrengthPercent: Int? = null,
    val transportType: NetworkTransportType = NetworkTransportType.UNKNOWN,
    val details: Map<String, String> = emptyMap()
)

/**
 * Platform reader contract providing [NetworkStatusSnapshot].
 */
interface NetworkDiagnosticsProvider {
    suspend fun getNetworkSnapshot(): NetworkStatusSnapshot
}

/**
 * Diagnostic check that evaluates whether a VPN or proxy is actively routing network traffic.
 */
class VpnDiagnosticCheck(
    private val provider: NetworkDiagnosticsProvider
) : DiagnosticCheck {
    override val id: String = CHECK_ID
    override val name: String = "VPN & Proxy Detection"

    override suspend fun run(): DiagnosticResult {
        val snapshot = provider.getNetworkSnapshot()
        val isVpn = snapshot.isVpnActive
        val isProxy = snapshot.isProxyActive

        val (state, cause) = when {
            isVpn && isProxy -> DiagnosticState.WARNING to "Active VPN and proxy detected"
            isVpn -> DiagnosticState.WARNING to "Active VPN detected"
            isProxy -> DiagnosticState.WARNING to "Active proxy detected"
            else -> DiagnosticState.PASS to null
        }

        val metadata = buildMap {
            put("vpnActive", isVpn.toString())
            put("proxyActive", isProxy.toString())
            put("transport", snapshot.transportType.name)
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
        const val CHECK_ID = "network_vpn"
    }
}

/**
 * Diagnostic check that evaluates network connectivity and signal strength degradation.
 */
class NetworkSignalDiagnosticCheck(
    private val provider: NetworkDiagnosticsProvider,
    private val lowSignalThresholdPercent: Int = DEFAULT_LOW_SIGNAL_THRESHOLD
) : DiagnosticCheck {
    override val id: String = CHECK_ID
    override val name: String = "Network Signal Strength"

    override suspend fun run(): DiagnosticResult {
        val snapshot = provider.getNetworkSnapshot()

        val (state, cause) = when {
            !snapshot.isConnected || snapshot.transportType == NetworkTransportType.NONE -> {
                DiagnosticState.ERROR to "No active network connection"
            }
            snapshot.signalStrengthPercent != null && snapshot.signalStrengthPercent < lowSignalThresholdPercent -> {
                DiagnosticState.WARNING to "Low network signal strength (${snapshot.signalStrengthPercent}%)"
            }
            else -> {
                DiagnosticState.PASS to null
            }
        }

        val metadata = buildMap {
            put("connected", snapshot.isConnected.toString())
            put("transport", snapshot.transportType.name)
            snapshot.signalStrengthPercent?.let { put("signalPercent", it.toString()) }
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
        const val CHECK_ID = "network_signal"
        const val DEFAULT_LOW_SIGNAL_THRESHOLD = 25
    }
}
