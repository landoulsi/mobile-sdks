package com.landoulsi.integrity.hooking.frida

import com.landoulsi.integrity.SignalEvaluator
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.logger.Logger

/**
 * [SignalEvaluator] implementation for Frida hooking framework detection.
 *
 * Orchestrates multiple Frida detection heuristics including server process
 * probing, protocol port scanning, gadget library mapping inspection, and
 * gadget binary file presence checks.
 *
 * @property context Platform-specific abstraction for file system, process,
 *   and network operations.
 */
class FridaDetectionEvaluator(
    private val context: FridaCheckContext,
) : SignalEvaluator {

    override val category: IntegrityCategory = IntegrityCategory.HOOKING_OR_TAMPERING

    override val knownSignalIds: Set<String> = FridaSignal.all

    override suspend fun evaluate(): List<IntegritySignal> {
        val signals = mutableListOf<IntegritySignal>()

        runCatching { signals.addAll(checkFridaServerProcess(context)) }
            .onFailure { Logger.e(TAG, "Frida server process check failed", it) }

        runCatching { signals.addAll(checkFridaPortOpen(context)) }
            .onFailure { Logger.e(TAG, "Frida port check failed", it) }

        runCatching { signals.addAll(checkFridaGadgetMaps(context)) }
            .onFailure { Logger.e(TAG, "Frida gadget maps check failed", it) }

        runCatching { signals.addAll(checkFridaGadgetFile(context)) }
            .onFailure { Logger.e(TAG, "Frida gadget file check failed", it) }

        return signals
    }

    private companion object {
        const val TAG = "FridaDetectionEvaluator"
    }
}