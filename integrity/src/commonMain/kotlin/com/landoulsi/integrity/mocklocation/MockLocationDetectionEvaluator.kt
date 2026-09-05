package com.landoulsi.integrity.mocklocation

import com.landoulsi.integrity.SignalEvaluator
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.logger.Logger

/**
 * [SignalEvaluator] implementation for mock location and GPS spoofing detection.
 *
 * Orchestrates multiple heuristics including mock location provider checks, developer
 * settings inspection, known GPS spoofing packages, and kinematic anomalies
 * (velocity violations, teleportation jumps, frozen coordinates).
 *
 * @property context Platform-specific abstraction for location, settings, and package manager.
 */
class MockLocationDetectionEvaluator(
    private val context: MockLocationCheckContext,
) : SignalEvaluator {

    override val category: IntegrityCategory = IntegrityCategory.MOCK_LOCATION

    override val knownSignalIds: Set<String> = MockLocationSignal.all

    override suspend fun evaluate(): List<IntegritySignal> {
        val signals = mutableListOf<IntegritySignal>()

        runCatching { checkMockLocationFlag(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Mock location flag check failed", it) }

        runCatching { checkMockProviderActive(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Mock provider active check failed", it) }

        runCatching { checkDeveloperMockSettings(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Developer mock settings check failed", it) }

        runCatching { signals.addAll(checkKnownMockApps(context)) }
            .onFailure { Logger.e(TAG, "Known mock apps check failed", it) }

        runCatching { signals.addAll(checkVelocityAndJumpAnomalies(context)) }
            .onFailure { Logger.e(TAG, "Velocity and jump anomaly check failed", it) }

        runCatching { checkFrozenLocationAnomaly(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Frozen location anomaly check failed", it) }

        return signals
    }

    private companion object {
        const val TAG = "MockLocationDetectionEvaluator"
    }
}
