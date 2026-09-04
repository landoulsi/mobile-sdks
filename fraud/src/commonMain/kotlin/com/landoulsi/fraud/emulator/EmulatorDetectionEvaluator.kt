package com.landoulsi.fraud.emulator

import com.landoulsi.fraud.SignalEvaluator
import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudSignal
import com.landoulsi.logger.Logger

/**
 * [SignalEvaluator] implementation for Android emulator detection (AVD, Genymotion,
 * BlueStacks, Nox, MEmu, and similar desktop-hosted virtual devices).
 *
 * Orchestrates multiple heuristics including generic AVD build property matching,
 * known emulator hardware backend identification, QEMU/Genymotion filesystem
 * artifacts, installed emulator management apps, and hardware sensor availability.
 *
 * @property context Platform-specific abstraction for build properties, file system,
 * package manager, and sensor access.
 */
class EmulatorDetectionEvaluator(
    private val context: EmulatorCheckContext,
) : SignalEvaluator {

    override val category: FraudCategory = FraudCategory.VIRTUAL_OS_OR_EMULATOR

    override val knownSignalIds: Set<String> = EmulatorSignal.all

    override suspend fun evaluate(): List<FraudSignal> {
        val signals = mutableListOf<FraudSignal>()

        runCatching { checkGenericBuildProperties(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Generic build property check failed", it) }

        runCatching { checkEmulatorHardware(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Emulator hardware check failed", it) }

        runCatching { signals.addAll(checkQemuFiles(context)) }
            .onFailure { Logger.e(TAG, "QEMU file check failed", it) }

        runCatching { signals.addAll(checkEmulatorManagementApps(context)) }
            .onFailure { Logger.e(TAG, "Emulator management app check failed", it) }

        runCatching { checkSensorDeficit(context)?.let { signals.add(it) } }
            .onFailure { Logger.e(TAG, "Sensor deficit check failed", it) }

        return signals
    }

    private companion object {
        const val TAG = "EmulatorDetectionEvaluator"
    }
}
