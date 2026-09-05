package com.landoulsi.integrity

import com.landoulsi.integrity.emulator.EmulatorSignal
import com.landoulsi.integrity.hooking.frida.FridaSignal
import com.landoulsi.integrity.hooking.xposed.XposedSignal
import com.landoulsi.integrity.hooking.substrate.SubstrateSignal
import com.landoulsi.integrity.jailbreak.JailbreakSignal
import com.landoulsi.integrity.mocklocation.MockLocationSignal
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegrityMitigationAction
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.RiskLevel
import com.landoulsi.integrity.root.RootSignal
import com.landoulsi.integrity.network.NetworkSignal
import com.landoulsi.integrity.simulator.SimulatorSignal
import com.landoulsi.integrity.virtualos.VirtualOsSignal
import kotlinx.serialization.Serializable

/**
 * Flat, aggregated verdict produced by [IntegrityManager].
 *
 * Every check known to the registered evaluators is reported in [signals] as an
 * explicit `true`/`false` (a missing key means the check does not exist, never
 * "not evaluated"), alongside the composite [integrityScore] from the scoring engine.
 *
 * The named `is*` / `has*` accessors are non-breaking conveniences layered over
 * [signals] and [categories]; adding a new check never changes this type's shape.
 *
 * @property integrityScore Composite risk score in the range 0..100 (rounded).
 * @property riskLevel Qualitative risk band from the scoring engine.
 * @property action Recommended mitigation action from the scoring engine.
 * @property signals Every known check id mapped to whether it fired this sweep.
 * @property categories Every [IntegrityCategory] mapped to whether any of its signals fired.
 * @property fired Full [IntegritySignal] objects for the checks that fired (evidence, metadata).
 * @property evaluatedAt Epoch milliseconds of the sweep, taken from the scoring engine.
 */
@Serializable
data class IntegrityResult(
    val integrityScore: Int,
    val riskLevel: RiskLevel,
    val action: IntegrityMitigationAction,
    val signals: Map<String, Boolean>,
    val categories: Map<IntegrityCategory, Boolean>,
    val fired: List<IntegritySignal> = emptyList(),
    val evaluatedAt: Long = 0L,
) {

    /** True when at least one signal fired. */
    val hasAnySignal: Boolean get() = fired.isNotEmpty()

    // --- Category-level rollups (forward-compatible with not-yet-implemented vectors) ---

    /** Any root (Android) or jailbreak (iOS) indicator fired. */
    val isRootedOrJailbroken: Boolean get() = categories[IntegrityCategory.ROOT_OR_JAILBREAK] == true

    /** A specifically Android-root signal fired. */
    val isRooted: Boolean get() = RootSignal.all.any { signals[it] == true }

    /** A specifically iOS-jailbreak signal fired. */
    val isJailbroken: Boolean get() = JailbreakSignal.all.any { signals[it] == true }

    val isEmulator: Boolean get() = categories[IntegrityCategory.VIRTUAL_OS_OR_EMULATOR] == true
    val hasMockLocation: Boolean get() = categories[IntegrityCategory.MOCK_LOCATION] == true
    val isHooked: Boolean get() = categories[IntegrityCategory.HOOKING_OR_TAMPERING] == true
    /** True when any Frida signal fired (server, port, gadget maps, or gadget file). */
    val hasFrida: Boolean get() =
        listOf(
            FridaSignal.FRIDA_SERVER_PROCESS,
            FridaSignal.FRIDA_PORT_OPEN,
            FridaSignal.FRIDA_GADGET_MAPS,
            FridaSignal.FRIDA_GADGET_FILE,
        ).any { signals[it] == true }
    /** True when any Xposed signal fired (framework, bridge class, module, or installer). */
    val hasXposed: Boolean get() =
        listOf(
            XposedSignal.XPOSED_FRAMEWORK_INSTALLED,
            XposedSignal.XPOSED_BRIDGE_CLASS,
            XposedSignal.XPOSED_MODULE_INSTALLED,
            XposedSignal.XPOSED_INSTALLER_APP,
        ).any { signals[it] == true }
    /** True when any Substrate signal fired (dylib injection, framework, tweak inject, or loaded). */
    val hasSubstrate: Boolean get() =
        listOf(
            SubstrateSignal.SUBSTRATE_DYLIB_INJECTION,
            SubstrateSignal.SUBSTRATE_FRAMEWORK,
            SubstrateSignal.SUBSTRATE_TWEAK_INJECT,
            SubstrateSignal.SUBSTRATE_LOADED,
        ).any { signals[it] == true }
    val isDebuggerAttached: Boolean get() = categories[IntegrityCategory.DEBUGGER_ATTACHED] == true
    val isCloned: Boolean get() = categories[IntegrityCategory.APP_CLONING] == true
    val hasNetworkAnomaly: Boolean get() = categories[IntegrityCategory.NETWORK_ANOMALY] == true
    val hasUntrustedInstaller: Boolean get() = categories[IntegrityCategory.UNTRUSTED_INSTALLER] == true

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

    val hasMockLocationFlag: Boolean get() = signals[MockLocationSignal.MOCK_FLAG_ACTIVE] == true
    val hasMockProviderActive: Boolean get() = signals[MockLocationSignal.MOCK_PROVIDER_ACTIVE] == true
    val hasDeveloperMockSetting: Boolean get() = signals[MockLocationSignal.DEVELOPER_MOCK_SETTING] == true
    val hasMockAppInstalled: Boolean get() = signals[MockLocationSignal.MOCK_APP_INSTALLED] == true
    val hasLocationVelocityAnomaly: Boolean get() = signals[MockLocationSignal.LOCATION_ANOMALY_VELOCITY] == true
    val hasLocationJumpAnomaly: Boolean get() = signals[MockLocationSignal.LOCATION_ANOMALY_JUMP] == true
    val hasLocationFrozenAnomaly: Boolean get() = signals[MockLocationSignal.LOCATION_ANOMALY_FROZEN] == true

    val hasActiveVpn: Boolean get() = signals[NetworkSignal.VPN_ACTIVE] == true
    val hasSystemProxy: Boolean get() = signals[NetworkSignal.SYSTEM_PROXY_ACTIVE] == true
    val hasAdbEnabled: Boolean get() = signals[NetworkSignal.DEVELOPER_ADB_ENABLED] == true

    companion object {
        /**
         * A clean verdict: zero score, [IntegrityMitigationAction.ALLOW], and every id in
         * [knownSignalIds] plus every [IntegrityCategory] mapped to `false`.
         */
        fun clean(knownSignalIds: Set<String>, evaluatedAt: Long = 0L): IntegrityResult = IntegrityResult(
            integrityScore = 0,
            riskLevel = RiskLevel.LOW,
            action = IntegrityMitigationAction.ALLOW,
            signals = knownSignalIds.associateWith { false },
            categories = IntegrityCategory.entries.associateWith { false },
            fired = emptyList(),
            evaluatedAt = evaluatedAt,
        )
    }
}
