package com.landoulsi.fraud

import com.landoulsi.fraud.emulator.EmulatorSignal
import com.landoulsi.fraud.jailbreak.JailbreakSignal
import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudMitigationAction
import com.landoulsi.fraud.model.FraudSignal
import com.landoulsi.fraud.model.RiskLevel
import com.landoulsi.fraud.root.RootSignal
import com.landoulsi.fraud.simulator.SimulatorSignal
import com.landoulsi.fraud.virtualos.VirtualOsSignal
import kotlinx.serialization.Serializable

/**
 * Flat, aggregated verdict produced by [FraudManager].
 *
 * Every check known to the registered evaluators is reported in [signals] as an
 * explicit `true`/`false` (a missing key means the check does not exist, never
 * "not evaluated"), alongside the composite [fraudScore] from the scoring engine.
 *
 * The named `is*` / `has*` accessors are non-breaking conveniences layered over
 * [signals] and [categories]; adding a new check never changes this type's shape.
 *
 * @property fraudScore Composite risk score in the range 0..100 (rounded).
 * @property riskLevel Qualitative risk band from the scoring engine.
 * @property action Recommended mitigation action from the scoring engine.
 * @property signals Every known check id mapped to whether it fired this sweep.
 * @property categories Every [FraudCategory] mapped to whether any of its signals fired.
 * @property fired Full [FraudSignal] objects for the checks that fired (evidence, metadata).
 * @property evaluatedAt Epoch milliseconds of the sweep, taken from the scoring engine.
 */
@Serializable
data class FraudResult(
    val fraudScore: Int,
    val riskLevel: RiskLevel,
    val action: FraudMitigationAction,
    val signals: Map<String, Boolean>,
    val categories: Map<FraudCategory, Boolean>,
    val fired: List<FraudSignal> = emptyList(),
    val evaluatedAt: Long = 0L,
) {

    /** True when at least one signal fired. */
    val hasAnySignal: Boolean get() = fired.isNotEmpty()

    // --- Category-level rollups (forward-compatible with not-yet-implemented vectors) ---

    /** Any root (Android) or jailbreak (iOS) indicator fired. */
    val isRootedOrJailbroken: Boolean get() = categories[FraudCategory.ROOT_OR_JAILBREAK] == true

    /** A specifically Android-root signal fired. */
    val isRooted: Boolean get() = RootSignal.all.any { signals[it] == true }

    /** A specifically iOS-jailbreak signal fired. */
    val isJailbroken: Boolean get() = JailbreakSignal.all.any { signals[it] == true }

    val isEmulator: Boolean get() = categories[FraudCategory.VIRTUAL_OS_OR_EMULATOR] == true
    val hasMockLocation: Boolean get() = categories[FraudCategory.MOCK_LOCATION] == true
    val isHooked: Boolean get() = categories[FraudCategory.HOOKING_OR_TAMPERING] == true
    val isDebuggerAttached: Boolean get() = categories[FraudCategory.DEBUGGER_ATTACHED] == true
    val isCloned: Boolean get() = categories[FraudCategory.APP_CLONING] == true
    val hasNetworkAnomaly: Boolean get() = categories[FraudCategory.NETWORK_ANOMALY] == true
    val hasUntrustedInstaller: Boolean get() = categories[FraudCategory.UNTRUSTED_INSTALLER] == true

    // --- Signal-level shortcuts for the currently implemented checks ---

    val hasSuBinary: Boolean get() = signals[RootSignal.SU_BINARY] == true
    val hasMagiskMount: Boolean get() = signals[RootSignal.MAGISK_MOUNT] == true
    val hasMagisk: Boolean get() = signals[RootSignal.MAGISK_PACKAGE] == true
    val hasKernelSu: Boolean get() = signals[RootSignal.KERNELSU_PACKAGE] == true
    val hasTestKeysBuild: Boolean get() = signals[RootSignal.TEST_KEYS] == true
    val hasWritableSystem: Boolean get() = signals[RootSignal.WRITABLE_SYSTEM] == true
    val hasSuperuserApp: Boolean get() = signals[RootSignal.SUPERUSER_APP] == true

    val hasJailbreakApp: Boolean get() = signals[JailbreakSignal.APP_BUNDLE] == true
    val hasJailbreakSystemBinary: Boolean get() = signals[JailbreakSignal.SYSTEM_BINARY] == true
    val canForkProcess: Boolean get() = signals[JailbreakSignal.FORK_CAPABILITY] == true
    val canEscapeSandbox: Boolean get() = signals[JailbreakSignal.SANDBOX_ESCAPE] == true
    val hasDylibInjection: Boolean get() = signals[JailbreakSignal.DYLIB_INJECTION] == true

    val hasGenericEmulatorBuild: Boolean get() = signals[EmulatorSignal.BUILD_GENERIC] == true
    val hasEmulatorHardware: Boolean get() = signals[EmulatorSignal.BUILD_HARDWARE] == true
    val hasQemuArtifact: Boolean get() = signals[EmulatorSignal.QEMU_FILE] == true
    val hasEmulatorManagementApp: Boolean get() = signals[EmulatorSignal.MANAGEMENT_APP] == true
    val hasSensorDeficit: Boolean get() = signals[EmulatorSignal.SENSOR_DEFICIT] == true

    val isSimulatorEnvironment: Boolean get() = signals[SimulatorSignal.ENVIRONMENT] == true
    val isSimulatorBundlePath: Boolean get() = signals[SimulatorSignal.BUNDLE_PATH] == true

    val hasUnresolvableOwnPackage: Boolean get() = signals[VirtualOsSignal.PACKAGE_UNRESOLVABLE] == true
    val hasUidMismatch: Boolean get() = signals[VirtualOsSignal.UID_MISMATCH] == true
    val hasDataDirAnomaly: Boolean get() = signals[VirtualOsSignal.DATA_DIR_ANOMALY] == true
    val hasVirtualOsContainerApp: Boolean get() = signals[VirtualOsSignal.KNOWN_CONTAINER_APP] == true

    companion object {
        /**
         * A clean verdict: zero score, [FraudMitigationAction.ALLOW], and every id in
         * [knownSignalIds] plus every [FraudCategory] mapped to `false`.
         */
        fun clean(knownSignalIds: Set<String>, evaluatedAt: Long = 0L): FraudResult = FraudResult(
            fraudScore = 0,
            riskLevel = RiskLevel.LOW,
            action = FraudMitigationAction.ALLOW,
            signals = knownSignalIds.associateWith { false },
            categories = FraudCategory.entries.associateWith { false },
            fired = emptyList(),
            evaluatedAt = evaluatedAt,
        )
    }
}
