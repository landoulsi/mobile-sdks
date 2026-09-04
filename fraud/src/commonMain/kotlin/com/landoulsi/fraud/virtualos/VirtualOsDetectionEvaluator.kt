package com.landoulsi.fraud.virtualos

import com.landoulsi.fraud.SignalEvaluator
import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudSignal
import com.landoulsi.logger.Logger

/**
 * [SignalEvaluator] implementation for "virtual OS" container detection — apps such as
 * V Android, VMOS, or Parallel Space that host a nested, virtualized Android instance
 * and run this app *inside* it as a guest, rather than as a genuine standalone install.
 *
 * Orchestrates process-identity heuristics (package resolvability, UID consistency,
 * data directory shape) alongside a curated list of known container apps.
 *
 * @property context Platform-specific abstraction for package manager and process access.
 */
class VirtualOsDetectionEvaluator(
    private val context: VirtualOsCheckContext,
) : SignalEvaluator {

    override val category: FraudCategory = FraudCategory.VIRTUAL_OS_OR_EMULATOR

    override val knownSignalIds: Set<String> = VirtualOsSignal.all

    override suspend fun evaluate(): List<FraudSignal> {
        val signals = mutableListOf<FraudSignal>()

        runCatching { checkOwnPackageResolvable(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Package resolvability check failed", it) }

        runCatching { checkUidMismatch(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "UID mismatch check failed", it) }

        runCatching { checkDataDirAnomaly(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Data directory check failed", it) }

        runCatching { signals.addAll(checkKnownContainerApps(context)) }
            .onFailure { Logger.e(TAG, "Known container app check failed", it) }

        return signals
    }

    private companion object {
        const val TAG = "VirtualOsDetectionEvaluator"
    }
}
