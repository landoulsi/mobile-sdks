package com.landoulsi.integrity.hooking.xposed

import com.landoulsi.integrity.SignalEvaluator
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.logger.Logger

/**
 * [SignalEvaluator] implementation for Xposed hooking framework detection.
 *
 * Orchestrates multiple Xposed detection heuristics including framework JAR
 * probing, bridge class loadability, installer app presence, and known module
 * package detection.
 *
 * @property context Platform-specific abstraction for file system, package
 *   manager, and class loader operations.
 */
class XposedDetectionEvaluator(
    private val context: XposedCheckContext,
) : SignalEvaluator {

    override val category: IntegrityCategory = IntegrityCategory.HOOKING_OR_TAMPERING

    override val knownSignalIds: Set<String> = XposedSignal.all

    override suspend fun evaluate(): List<IntegritySignal> {
        val signals = mutableListOf<IntegritySignal>()

        runCatching { signals.addAll(checkXposedFrameworkInstalled(context)) }
            .onFailure { Logger.e(TAG, "Xposed framework check failed", it) }

        runCatching { signals.addAll(checkXposedBridgeClass(context)) }
            .onFailure { Logger.e(TAG, "Xposed bridge class check failed", it) }

        runCatching { signals.addAll(checkXposedInstallerApp(context)) }
            .onFailure { Logger.e(TAG, "Xposed installer app check failed", it) }

        runCatching { signals.addAll(checkXposedModuleInstalled(context)) }
            .onFailure { Logger.e(TAG, "Xposed module check failed", it) }

        return signals
    }

    private companion object {
        const val TAG = "XposedDetectionEvaluator"
    }
}