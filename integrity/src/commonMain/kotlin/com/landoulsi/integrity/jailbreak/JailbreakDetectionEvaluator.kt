package com.landoulsi.integrity.jailbreak

import com.landoulsi.integrity.SignalEvaluator
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.logger.Logger

/**
 * [SignalEvaluator] implementation for jailbreak detection.
 *
 * Orchestrates multiple jailbreak detection heuristics including jailbreak app bundle
 * probing, system binary presence, fork() capability testing, sandbox integrity
 * verification, and dynamic library injection detection.
 *
 * @property context Platform-specific abstraction for file system and process operations.
 */
class JailbreakDetectionEvaluator(
    private val context: JailbreakCheckContext,
) : SignalEvaluator {

    override val category: IntegrityCategory = IntegrityCategory.ROOT_OR_JAILBREAK

    override val knownSignalIds: Set<String> = JailbreakSignal.all

    override suspend fun evaluate(): List<IntegritySignal> {
        val signals = mutableListOf<IntegritySignal>()

        runCatching { signals.addAll(checkJailbreakApps(context)) }
            .onFailure { Logger.e(TAG, "Jailbreak app check failed", it) }

        runCatching { signals.addAll(checkSystemBinaries(context)) }
            .onFailure { Logger.e(TAG, "System binary check failed", it) }

        runCatching { checkForkCapability(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Fork capability check failed", it) }

        runCatching { checkSandboxIntegrity(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Sandbox integrity check failed", it) }

        runCatching { signals.addAll(checkDylibInjection(context)) }
            .onFailure { Logger.e(TAG, "Dylib injection check failed", it) }

        return signals
    }

    private companion object {
        const val TAG = "JailbreakDetectionEvaluator"
    }
}

