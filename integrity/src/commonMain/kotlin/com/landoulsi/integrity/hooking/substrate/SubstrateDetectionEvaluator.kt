package com.landoulsi.integrity.hooking.substrate

import com.landoulsi.integrity.SignalEvaluator
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.logger.Logger

/**
 * [SignalEvaluator] implementation for Substrate (MobileSubstrate / Cydia
 * Substrate) hooking framework detection.
 *
 * Orchestrates multiple Substrate detection heuristics including dynamic
 * library injection directory probing, framework dylib presence, and tweak
 * inject path checks.
 *
 * @property context Platform-specific abstraction for file system operations.
 */
class SubstrateDetectionEvaluator(
    private val context: SubstrateCheckContext,
) : SignalEvaluator {

    override val category: IntegrityCategory = IntegrityCategory.HOOKING_OR_TAMPERING

    override val knownSignalIds: Set<String> = SubstrateSignal.all

    override suspend fun evaluate(): List<IntegritySignal> {
        val signals = mutableListOf<IntegritySignal>()

        runCatching { signals.addAll(checkSubstrateDylibInjection(context)) }
            .onFailure { Logger.e(TAG, "Substrate dylib injection check failed", it) }

        runCatching { signals.addAll(checkSubstrateFramework(context)) }
            .onFailure { Logger.e(TAG, "Substrate framework check failed", it) }

        runCatching { signals.addAll(checkSubstrateTweakInject(context)) }
            .onFailure { Logger.e(TAG, "Substrate tweak inject check failed", it) }

        runCatching { signals.addAll(checkSubstrateLoaded(context)) }
            .onFailure { Logger.e(TAG, "Substrate loaded check failed", it) }

        return signals
    }

    private companion object {
        const val TAG = "SubstrateDetectionEvaluator"
    }
}