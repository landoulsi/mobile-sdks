package com.landoulsi.integrity.network

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.SignalSeverity
import kotlin.time.Clock

internal fun checkVpnActive(context: NetworkCheckContext): IntegritySignal? {
    val isVpnActive = try {
        context.isVpnActive()
    } catch (_: Exception) {
        false
    }

    if (!isVpnActive) return null

    return IntegritySignal(
        id = NetworkSignal.VPN_ACTIVE,
        name = "Active VPN Connection Detected",
        category = IntegrityCategory.NETWORK_ANOMALY,
        severity = SignalSeverity.MEDIUM,
        confidence = 0.95,
        details = "Active VPN tunnel or virtual network interface detected",
        detectedAt = Clock.System.now().toEpochMilliseconds(),
        metadata = mapOf("check" to NetworkSignal.Check.VPN_ACTIVE),
    )
}

internal fun checkSystemProxy(context: NetworkCheckContext): IntegritySignal? {
    val isProxyConfigured = try {
        context.isSystemProxyConfigured()
    } catch (_: Exception) {
        false
    }

    if (!isProxyConfigured) return null

    return IntegritySignal(
        id = NetworkSignal.SYSTEM_PROXY_ACTIVE,
        name = "System Proxy Configured",
        category = IntegrityCategory.NETWORK_ANOMALY,
        severity = SignalSeverity.HIGH,
        confidence = 0.9,
        details = "HTTP/HTTPS proxy configured to intercept or redirect network traffic",
        detectedAt = Clock.System.now().toEpochMilliseconds(),
        metadata = mapOf("check" to NetworkSignal.Check.SYSTEM_PROXY),
    )
}

internal fun checkDeveloperAdbStatus(context: NetworkCheckContext): IntegritySignal? {
    val isAdbEnabled = try {
        context.isAdbEnabled()
    } catch (_: Exception) {
        false
    }

    if (!isAdbEnabled) return null

    return IntegritySignal(
        id = NetworkSignal.DEVELOPER_ADB_ENABLED,
        name = "Developer ADB Debugging Enabled",
        category = IntegrityCategory.NETWORK_ANOMALY,
        severity = SignalSeverity.MEDIUM,
        confidence = 1.0,
        details = "Android Debug Bridge (ADB) / USB debugging is enabled on the device",
        detectedAt = Clock.System.now().toEpochMilliseconds(),
        metadata = mapOf("check" to NetworkSignal.Check.DEVELOPER_ADB),
    )
}
