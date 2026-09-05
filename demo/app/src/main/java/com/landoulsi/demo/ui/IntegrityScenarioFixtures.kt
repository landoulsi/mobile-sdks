package com.landoulsi.demo.ui

import android.content.Context
import com.landoulsi.integrity.SignalEvaluator
import com.landoulsi.integrity.emulator.AndroidEmulatorCheckContext
import com.landoulsi.integrity.emulator.EmulatorCheckContext
import com.landoulsi.integrity.emulator.EmulatorDetectionEvaluator
import com.landoulsi.integrity.hooking.frida.AndroidFridaCheckContext
import com.landoulsi.integrity.hooking.frida.FridaCheckContext
import com.landoulsi.integrity.hooking.frida.FridaDetectionEvaluator
import com.landoulsi.integrity.hooking.substrate.AndroidSubstrateCheckContext
import com.landoulsi.integrity.hooking.substrate.SubstrateCheckContext
import com.landoulsi.integrity.hooking.substrate.SubstrateDetectionEvaluator
import com.landoulsi.integrity.hooking.xposed.AndroidXposedCheckContext
import com.landoulsi.integrity.hooking.xposed.XposedCheckContext
import com.landoulsi.integrity.hooking.xposed.XposedDetectionEvaluator
import com.landoulsi.integrity.mocklocation.AndroidMockLocationCheckContext
import com.landoulsi.integrity.mocklocation.LocationSample
import com.landoulsi.integrity.mocklocation.MockLocationCheckContext
import com.landoulsi.integrity.mocklocation.MockLocationDetectionEvaluator
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.network.AndroidNetworkCheckContext
import com.landoulsi.integrity.network.NetworkCheckContext
import com.landoulsi.integrity.network.NetworkDetectionEvaluator
import com.landoulsi.integrity.root.AndroidRootCheckContext
import com.landoulsi.integrity.root.RootCheckContext
import com.landoulsi.integrity.root.RootDetectionEvaluator
import com.landoulsi.integrity.virtualos.AndroidVirtualOsCheckContext
import com.landoulsi.integrity.virtualos.VirtualOsCheckContext
import com.landoulsi.integrity.virtualos.VirtualOsDetectionEvaluator
import kotlin.time.Clock

/**
 * Factory and fixture provider for creating mock and live evaluators across all demonstration scenarios.
 */
object IntegrityScenarioFixtures {

    /**
     * Categories that currently have concrete detection evaluators implemented in the SDK.
     */
    val IMPLEMENTED_CATEGORIES: Set<IntegrityCategory> = setOf(
        IntegrityCategory.ROOT_OR_JAILBREAK,
        IntegrityCategory.HOOKING_OR_TAMPERING,
        IntegrityCategory.MOCK_LOCATION,
        IntegrityCategory.NETWORK_ANOMALY,
        IntegrityCategory.VIRTUAL_OS_OR_EMULATOR,
    )

    /**
     * Returns true if a concrete evaluator is registered and implemented for the given [category].
     */
    fun isCategoryImplemented(category: IntegrityCategory): Boolean = category in IMPLEMENTED_CATEGORIES

    /**
     * Builds the evaluator suite for the specified [scenario].
     * If [context] is null for [IntegrityScenario.LIVE_DEVICE], a clean baseline mock suite is used.
     */
    fun buildEvaluators(
        scenario: IntegrityScenario,
        context: Context? = null,
    ): List<SignalEvaluator> {
        return when (scenario) {
            IntegrityScenario.LIVE_DEVICE -> {
                if (context != null) {
                    listOf(
                        FridaDetectionEvaluator(AndroidFridaCheckContext(context)),
                        XposedDetectionEvaluator(AndroidXposedCheckContext(context)),
                        SubstrateDetectionEvaluator(AndroidSubstrateCheckContext(context)),
                        MockLocationDetectionEvaluator(AndroidMockLocationCheckContext(context)),
                        RootDetectionEvaluator(AndroidRootCheckContext(context)),
                        EmulatorDetectionEvaluator(AndroidEmulatorCheckContext(context)),
                        VirtualOsDetectionEvaluator(AndroidVirtualOsCheckContext(context)),
                        NetworkDetectionEvaluator(AndroidNetworkCheckContext(context)),
                    )
                } else {
                    buildCleanBaselineEvaluators()
                }
            }

            IntegrityScenario.ROOT_BREACH -> {
                val mockRootContext = object : RootCheckContext {
                    override fun fileExists(path: String): Boolean =
                        path == "/system/xbin/su" || path == "/system/app/Superuser.apk"

                    override fun readFileLines(path: String): List<String> =
                        if (path == "/proc/mounts") listOf("magisk /sbin/.magisk/mirror ext4 rw 0 0", "rootfs / rootfs rw 0 0") else emptyList()

                    override fun isPackageInstalled(packageName: String): Boolean =
                        packageName == "com.topjohnwu.magisk"

                    override fun getBuildTag(): String = "test-keys"
                }
                listOf(
                    RootDetectionEvaluator(mockRootContext),
                    FridaDetectionEvaluator(createCleanFridaContext()),
                    XposedDetectionEvaluator(createCleanXposedContext()),
                    SubstrateDetectionEvaluator(createCleanSubstrateContext()),
                    MockLocationDetectionEvaluator(createCleanMockLocationContext()),
                    EmulatorDetectionEvaluator(createCleanEmulatorContext()),
                    VirtualOsDetectionEvaluator(createCleanVirtualOsContext()),
                    NetworkDetectionEvaluator(createCleanNetworkContext()),
                )
            }

            IntegrityScenario.DYNAMIC_HOOKING -> {
                val mockFridaContext = object : FridaCheckContext {
                    override fun fileExists(path: String): Boolean = path == "/data/local/tmp/frida-server"
                    override fun readFileLines(path: String): List<String> =
                        if (path == "/proc/self/maps") listOf("7f8a0000-7f8b0000 r-xp 00000000 08:01 12345 /data/local/tmp/libfrida-gadget.so") else emptyList()

                    override fun isPortOpen(port: Int): Boolean = port == 27042
                    override fun isProcessRunning(processName: String): Boolean = processName == "frida-server"
                    override fun isPackageInstalled(packageName: String): Boolean = false
                }
                val mockXposedContext = object : XposedCheckContext {
                    override fun fileExists(path: String): Boolean = path == "/system/framework/XposedBridge.jar"
                    override fun readFileLines(path: String): List<String> = emptyList()
                    override fun isPackageInstalled(packageName: String): Boolean = packageName == "org.lsposed.manager"
                    override fun isClassLoadable(className: String): Boolean = className == "de.robv.android.xposed.XposedBridge"
                }
                val mockSubstrateContext = object : SubstrateCheckContext {
                    override fun fileExists(path: String): Boolean = path == "/Library/MobileSubstrate/MobileSubstrate.dylib"
                    override fun directoryContents(path: String): List<String> = listOf("Tweak1.dylib")
                }
                listOf(
                    FridaDetectionEvaluator(mockFridaContext),
                    XposedDetectionEvaluator(mockXposedContext),
                    SubstrateDetectionEvaluator(mockSubstrateContext),
                    RootDetectionEvaluator(createCleanRootContext()),
                    MockLocationDetectionEvaluator(createCleanMockLocationContext()),
                    EmulatorDetectionEvaluator(createCleanEmulatorContext()),
                    VirtualOsDetectionEvaluator(createCleanVirtualOsContext()),
                    NetworkDetectionEvaluator(createCleanNetworkContext()),
                )
            }

            IntegrityScenario.GPS_SPOOFING -> {
                val now = Clock.System.now().toEpochMilliseconds()
                val mockLocationContext = object : MockLocationCheckContext {
                    override fun isMockLocationAppSet(): Boolean = true
                    override fun isMockProviderActive(): Boolean = true
                    override fun isDeveloperMockSettingEnabled(): Boolean = true
                    override fun isPackageInstalled(packageName: String): Boolean = packageName == "com.lexa.fakegps"
                    override fun getRecentLocations(): List<LocationSample> = listOf(
                        LocationSample(latitude = 37.7749, longitude = -122.4194, timestampMs = now - 2000L, isMock = true),
                        LocationSample(latitude = 40.7128, longitude = -74.0060, timestampMs = now, isMock = true),
                    )
                }
                listOf(
                    MockLocationDetectionEvaluator(mockLocationContext),
                    RootDetectionEvaluator(createCleanRootContext()),
                    FridaDetectionEvaluator(createCleanFridaContext()),
                    XposedDetectionEvaluator(createCleanXposedContext()),
                    SubstrateDetectionEvaluator(createCleanSubstrateContext()),
                    EmulatorDetectionEvaluator(createCleanEmulatorContext()),
                    VirtualOsDetectionEvaluator(createCleanVirtualOsContext()),
                    NetworkDetectionEvaluator(createCleanNetworkContext()),
                )
            }

            IntegrityScenario.NETWORK_ANOMALY -> {
                val mockNetworkContext = object : NetworkCheckContext {
                    override fun isVpnActive(): Boolean = true
                    override fun isSystemProxyConfigured(): Boolean = true
                    override fun isAdbEnabled(): Boolean = true
                }
                listOf(
                    NetworkDetectionEvaluator(mockNetworkContext),
                    RootDetectionEvaluator(createCleanRootContext()),
                    FridaDetectionEvaluator(createCleanFridaContext()),
                    XposedDetectionEvaluator(createCleanXposedContext()),
                    SubstrateDetectionEvaluator(createCleanSubstrateContext()),
                    MockLocationDetectionEvaluator(createCleanMockLocationContext()),
                    EmulatorDetectionEvaluator(createCleanEmulatorContext()),
                    VirtualOsDetectionEvaluator(createCleanVirtualOsContext()),
                )
            }

            IntegrityScenario.CRITICAL_ATTACK -> {
                val mockRootContext = object : RootCheckContext {
                    override fun fileExists(path: String): Boolean = path == "/system/bin/su"
                    override fun readFileLines(path: String): List<String> =
                        if (path == "/proc/mounts") listOf("magisk /sbin/.magisk/mirror ext4 rw 0 0") else emptyList()

                    override fun isPackageInstalled(packageName: String): Boolean = packageName == "me.weishu.kernelsu"
                    override fun getBuildTag(): String = "test-keys"
                }
                val mockFridaContext = object : FridaCheckContext {
                    override fun fileExists(path: String): Boolean = true
                    override fun readFileLines(path: String): List<String> = emptyList()
                    override fun isPortOpen(port: Int): Boolean = port == 27042
                    override fun isProcessRunning(processName: String): Boolean = processName == "frida-server"
                    override fun isPackageInstalled(packageName: String): Boolean = false
                }
                val mockMockLocationContext = object : MockLocationCheckContext {
                    override fun isMockLocationAppSet(): Boolean = true
                    override fun isMockProviderActive(): Boolean = true
                    override fun isDeveloperMockSettingEnabled(): Boolean = true
                    override fun isPackageInstalled(packageName: String): Boolean = true
                    override fun getRecentLocations(): List<LocationSample> = listOf(
                        LocationSample(latitude = 37.7749, longitude = -122.4194, isMock = true),
                    )
                }
                val mockNetworkContext = object : NetworkCheckContext {
                    override fun isVpnActive(): Boolean = true
                    override fun isSystemProxyConfigured(): Boolean = true
                    override fun isAdbEnabled(): Boolean = true
                }
                val mockVirtualOsContext = object : VirtualOsCheckContext {
                    override fun isPackageInstalled(packageName: String): Boolean = packageName == "com.lbe.parallel.intl"
                    override fun isOwnPackageKnownToPackageManager(): Boolean = false
                    override fun getSelfReportedUid(): Int = 10099
                    override fun getPackageManagerUid(): Int? = 10050
                    override fun getDataDirPath(): String = "/data/data/com.lbe.parallel.intl/virtual/data/user/0/com.landoulsi.demo"
                    override fun getOwnPackageName(): String = "com.landoulsi.demo"
                }
                val mockEmulatorContext = object : EmulatorCheckContext {
                    override fun fileExists(path: String): Boolean = path == "/dev/qemu_pipe"
                    override fun isPackageInstalled(packageName: String): Boolean = false
                    override fun getBuildFingerprint(): String = "generic/google/generic:14"
                    override fun getBuildModel(): String = "Android SDK built for x86"
                    override fun getBuildManufacturer(): String = "Google"
                    override fun getBuildBrand(): String = "generic"
                    override fun getBuildDevice(): String = "generic"
                    override fun getBuildProduct(): String = "sdk_gphone64_arm64"
                    override fun getBuildHardware(): String = "goldfish"
                    override fun getSensorCount(): Int? = 0
                }
                listOf(
                    RootDetectionEvaluator(mockRootContext),
                    FridaDetectionEvaluator(mockFridaContext),
                    MockLocationDetectionEvaluator(mockMockLocationContext),
                    NetworkDetectionEvaluator(mockNetworkContext),
                    VirtualOsDetectionEvaluator(mockVirtualOsContext),
                    EmulatorDetectionEvaluator(mockEmulatorContext),
                    XposedDetectionEvaluator(createCleanXposedContext()),
                    SubstrateDetectionEvaluator(createCleanSubstrateContext()),
                )
            }

            IntegrityScenario.CLEAN_BASELINE -> buildCleanBaselineEvaluators()
        }
    }

    fun buildCleanBaselineEvaluators(): List<SignalEvaluator> = listOf(
        RootDetectionEvaluator(createCleanRootContext()),
        FridaDetectionEvaluator(createCleanFridaContext()),
        XposedDetectionEvaluator(createCleanXposedContext()),
        SubstrateDetectionEvaluator(createCleanSubstrateContext()),
        MockLocationDetectionEvaluator(createCleanMockLocationContext()),
        EmulatorDetectionEvaluator(createCleanEmulatorContext()),
        VirtualOsDetectionEvaluator(createCleanVirtualOsContext()),
        NetworkDetectionEvaluator(createCleanNetworkContext()),
    )

    fun createCleanRootContext(): RootCheckContext = object : RootCheckContext {
        override fun fileExists(path: String): Boolean = false
        override fun readFileLines(path: String): List<String> = emptyList()
        override fun isPackageInstalled(packageName: String): Boolean = false
        override fun getBuildTag(): String = "release-keys"
    }

    fun createCleanFridaContext(): FridaCheckContext = object : FridaCheckContext {
        override fun fileExists(path: String): Boolean = false
        override fun readFileLines(path: String): List<String> = emptyList()
        override fun isPortOpen(port: Int): Boolean = false
        override fun isProcessRunning(processName: String): Boolean = false
        override fun isPackageInstalled(packageName: String): Boolean = false
    }

    fun createCleanXposedContext(): XposedCheckContext = object : XposedCheckContext {
        override fun fileExists(path: String): Boolean = false
        override fun readFileLines(path: String): List<String> = emptyList()
        override fun isPackageInstalled(packageName: String): Boolean = false
        override fun isClassLoadable(className: String): Boolean = false
    }

    fun createCleanSubstrateContext(): SubstrateCheckContext = object : SubstrateCheckContext {
        override fun fileExists(path: String): Boolean = false
        override fun directoryContents(path: String): List<String> = emptyList()
    }

    fun createCleanMockLocationContext(): MockLocationCheckContext = object : MockLocationCheckContext {
        override fun isMockLocationAppSet(): Boolean = false
        override fun isMockProviderActive(): Boolean = false
        override fun isDeveloperMockSettingEnabled(): Boolean = false
        override fun isPackageInstalled(packageName: String): Boolean = false
        override fun getRecentLocations(): List<LocationSample> = emptyList()
    }

    fun createCleanEmulatorContext(): EmulatorCheckContext = object : EmulatorCheckContext {
        override fun fileExists(path: String): Boolean = false
        override fun isPackageInstalled(packageName: String): Boolean = false
        override fun getBuildFingerprint(): String = "google/cheetah/cheetah:14/UQ1A.240205.004/11266203:user/release-keys"
        override fun getBuildModel(): String = "Pixel 7 Pro"
        override fun getBuildManufacturer(): String = "Google"
        override fun getBuildBrand(): String = "google"
        override fun getBuildDevice(): String = "cheetah"
        override fun getBuildProduct(): String = "cheetah"
        override fun getBuildHardware(): String = "tensor"
        override fun getSensorCount(): Int? = 16
    }

    fun createCleanVirtualOsContext(): VirtualOsCheckContext = object : VirtualOsCheckContext {
        override fun isPackageInstalled(packageName: String): Boolean = false
        override fun isOwnPackageKnownToPackageManager(): Boolean = true
        override fun getSelfReportedUid(): Int = 10100
        override fun getPackageManagerUid(): Int? = 10100
        override fun getDataDirPath(): String = "/data/user/0/com.landoulsi.demo"
        override fun getOwnPackageName(): String = "com.landoulsi.demo"
    }

    fun createCleanNetworkContext(): NetworkCheckContext = object : NetworkCheckContext {
        override fun isVpnActive(): Boolean = false
        override fun isSystemProxyConfigured(): Boolean = false
        override fun isAdbEnabled(): Boolean = false
    }
}
