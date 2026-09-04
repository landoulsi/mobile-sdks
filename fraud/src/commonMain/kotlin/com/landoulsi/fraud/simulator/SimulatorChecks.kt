package com.landoulsi.fraud.simulator

import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudSignal
import com.landoulsi.fraud.model.SignalSeverity
import kotlin.time.Clock

object SimulatorSignal {
    const val ENVIRONMENT = "simulator_environment"
    const val BUNDLE_PATH = "simulator_bundle_path"

    /** Every signal id this vector can emit; used to seed the [FraudResult] catalog. */
    val all: Set<String> = setOf(
        ENVIRONMENT,
        BUNDLE_PATH,
    )

    object Check {
        const val ENVIRONMENT = "environment"
        const val BUNDLE_PATH = "bundle_path"
    }
}

/** Environment variables Apple's Simulator runtime sets that never appear on a real device. */
internal val SIMULATOR_ENVIRONMENT_KEYS = listOf(
    "SIMULATOR_DEVICE_NAME",
    "SIMULATOR_UDID",
    "SIMULATOR_ROOT",
    "SIMULATOR_MODEL_IDENTIFIER",
    "SIMULATOR_HOST_HOME",
)

internal fun checkSimulatorEnvironment(context: SimulatorCheckContext): FraudSignal? {
    val matchedKeys = SIMULATOR_ENVIRONMENT_KEYS.filter { key ->
        try {
            !context.getEnvironmentVariable(key).isNullOrEmpty()
        } catch (_: Exception) {
            false
        }
    }

    if (matchedKeys.isEmpty()) return null

    return FraudSignal(
        id = SimulatorSignal.ENVIRONMENT,
        name = "iOS Simulator Environment Detected",
        category = FraudCategory.VIRTUAL_OS_OR_EMULATOR,
        severity = SignalSeverity.CRITICAL,
        confidence = 1.0,
        details = "Simulator-only environment variable(s) present: ${matchedKeys.joinToString(", ")}",
        detectedAt = Clock.System.now().toEpochMilliseconds(),
        metadata = mapOf("keys" to matchedKeys.joinToString(","), "check" to SimulatorSignal.Check.ENVIRONMENT),
    )
}

internal fun checkSimulatorBundlePath(context: SimulatorCheckContext): FraudSignal? {
    val isSimulatorPath = try {
        context.isBundlePathWithinCoreSimulator()
    } catch (_: Exception) {
        false
    }

    if (!isSimulatorPath) return null

    return FraudSignal(
        id = SimulatorSignal.BUNDLE_PATH,
        name = "CoreSimulator Bundle Path Detected",
        category = FraudCategory.VIRTUAL_OS_OR_EMULATOR,
        severity = SignalSeverity.HIGH,
        confidence = 0.9,
        details = "App bundle resolves within a CoreSimulator device sandbox",
        detectedAt = Clock.System.now().toEpochMilliseconds(),
        metadata = mapOf("check" to SimulatorSignal.Check.BUNDLE_PATH),
    )
}
