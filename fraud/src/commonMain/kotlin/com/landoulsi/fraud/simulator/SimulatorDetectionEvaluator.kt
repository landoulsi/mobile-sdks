package com.landoulsi.fraud.simulator

import com.landoulsi.fraud.SignalEvaluator
import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudSignal
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

    override val category: FraudCategory = FraudCategory.VIRTUAL_OS_OR_EMULATOR

    override val knownSignalIds: Set<String> = SimulatorSignal.all

    override suspend fun evaluate(): List<FraudSignal> {
        val signals = mutableListOf<FraudSignal>()

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
