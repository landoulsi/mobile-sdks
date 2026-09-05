package com.landoulsi.integrity.network

/**
 * Signal identifiers and check tags emitted by the network anomaly detection vector.
 */
object NetworkSignal {
    const val VPN_ACTIVE = "network_vpn_active"
    const val SYSTEM_PROXY_ACTIVE = "network_proxy_active"
    const val DEVELOPER_ADB_ENABLED = "network_adb_enabled"

    /** Every signal id this vector can emit; used to seed the [com.landoulsi.integrity.IntegrityResult] catalog. */
    val all: Set<String> = setOf(
        VPN_ACTIVE,
        SYSTEM_PROXY_ACTIVE,
        DEVELOPER_ADB_ENABLED,
    )

    object Check {
        const val VPN_ACTIVE = "vpn_active"
        const val SYSTEM_PROXY = "system_proxy"
        const val DEVELOPER_ADB = "developer_adb"
    }
}

/**
 * Abstraction for platform-specific operations used by network integrity checks.
 *
 * Decouples detection heuristics from platform framework types (e.g. Android's ConnectivityManager,
 * NetworkInterface, Settings.Global) to enable deterministic JVM host testing with fakes.
 */
interface NetworkCheckContext {
    /**
     * Checks whether an active VPN connection or virtual tunnel interface is present on the device.
     */
    fun isVpnActive(): Boolean

    /**
     * Checks whether a system-wide HTTP or HTTPS proxy has been configured to intercept network traffic.
     */
    fun isSystemProxyConfigured(): Boolean

    /**
     * Checks whether Android Debug Bridge (ADB) / USB debugging is currently enabled.
     */
    fun isAdbEnabled(): Boolean
}
