package com.landoulsi.fraud

import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudSignal
import com.landoulsi.logger.Logger

/**
 * [SignalEvaluator] implementation for root/jailbreak detection.
 *
 * Orchestrates multiple root detection heuristics including su binary probing,
 * Magisk/KernelSU identification, build tag inspection, writable system mounts,
 * and superuser application presence.
 *
 * @property context Platform-specific abstraction for file system and system property access.
 */
class RootDetectionEvaluator(
    private val context: RootCheckContext,
) : SignalEvaluator {

    override val category: FraudCategory = FraudCategory.ROOT_OR_JAILBREAK

    override val knownSignalIds: Set<String> = RootSignal.all

    override suspend fun evaluate(): List<FraudSignal> {
        val signals = mutableListOf<FraudSignal>()

        runCatching { signals.addAll(checkSuBinaries(context)) }
            .onFailure { Logger.e(TAG, "SU binary check failed", it) }

        runCatching { signals.addAll(checkMagisk(context)) }
            .onFailure { Logger.e(TAG, "Magisk check failed", it) }

        runCatching { checkTestBuildKeys(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Build tag check failed", it) }

        runCatching { checkWritableSystem(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Writable system check failed", it) }

        runCatching { signals.addAll(checkSuperuserApps(context)) }
            .onFailure { Logger.e(TAG, "Superuser app check failed", it) }

        return signals
    }

    private companion object {
        const val TAG = "RootDetectionEvaluator"
    }
}

