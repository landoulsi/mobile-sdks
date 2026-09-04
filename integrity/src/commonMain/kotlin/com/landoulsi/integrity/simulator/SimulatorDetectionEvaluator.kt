package com.landoulsi.integrity.simulator

import com.landoulsi.integrity.SignalEvaluator
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.logger.Logger

/**
 * [SignalEvaluator] implementation for iOS simulator detection.
 *
 * Orchestrates simulator-only environment variable probing and CoreSimulator
 * bundle sandbox detection.
 *
 * @property context Platform-specific abstraction for process environment and bundle access.
 */
class SimulatorDetectionEvaluator(
    private val context: SimulatorCheckContext,
) : SignalEvaluator {

    override val category: IntegrityCategory = IntegrityCategory.VIRTUAL_OS_OR_EMULATOR

    override val knownSignalIds: Set<String> = SimulatorSignal.all

    override suspend fun evaluate(): List<IntegritySignal> {
        val signals = mutableListOf<IntegritySignal>()

        runCatching { checkSimulatorEnvironment(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Simulator environment check failed", it) }

        runCatching { checkSimulatorBundlePath(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Simulator bundle path check failed", it) }

        return signals
    }

    private companion object {
        const val TAG = "SimulatorDetectionEvaluator"
    }
}
