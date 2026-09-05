package com.landoulsi.integrity.network

import com.landoulsi.integrity.SignalEvaluator
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.logger.Logger

/**
 * [SignalEvaluator] implementation for network anomaly and debugging detection.
 *
 * Evaluates network configuration threats including active VPN tunnels, HTTP/HTTPS proxies,
 * and enabled Android Debug Bridge (ADB) debugging.
 *
 * @property context Platform-specific abstraction for network and developer settings queries.
 */
class NetworkDetectionEvaluator(
    private val context: NetworkCheckContext,
) : SignalEvaluator {

    override val category: IntegrityCategory = IntegrityCategory.NETWORK_ANOMALY

    override val knownSignalIds: Set<String> = NetworkSignal.all

    override suspend fun evaluate(): List<IntegritySignal> {
        val signals = mutableListOf<IntegritySignal>()

        runCatching { checkVpnActive(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "VPN active check failed", it) }

        runCatching { checkSystemProxy(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "System proxy check failed", it) }

        runCatching { checkDeveloperAdbStatus(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Developer ADB status check failed", it) }

        return signals
    }

    companion object {
        private const val TAG = "NetworkDetectionEvaluator"
    }
}
