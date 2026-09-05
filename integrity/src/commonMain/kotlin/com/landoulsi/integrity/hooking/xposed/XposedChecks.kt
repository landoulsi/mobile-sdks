package com.landoulsi.integrity.hooking.xposed

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.SignalSeverity
import kotlin.time.Clock

object XposedSignal {
    const val XPOSED_FRAMEWORK_INSTALLED = "xposed_framework_installed"
    const val XPOSED_BRIDGE_CLASS = "xposed_bridge_class"
    const val XPOSED_MODULE_INSTALLED = "xposed_module_installed"
    const val XPOSED_INSTALLER_APP = "xposed_installer_app"

    /** Every signal id this vector can emit; used to seed the [IntegrityResult] catalog. */
    val all: Set<String> = setOf(
        XPOSED_FRAMEWORK_INSTALLED,
        XPOSED_BRIDGE_CLASS,
        XPOSED_MODULE_INSTALLED,
        XPOSED_INSTALLER_APP,
    )

    object Check {
        const val XPOSED_FRAMEWORK_INSTALLED = "xposed_framework_installed"
        const val XPOSED_BRIDGE_CLASS = "xposed_bridge_class"
        const val XPOSED_MODULE_INSTALLED = "xposed_module_installed"
        const val XPOSED_INSTALLER_APP = "xposed_installer_app"
    }
}

internal val XPOSED_BRIDGE_JAR_PATHS = listOf(
    "/system/framework/XposedBridge.jar",
    "/system/xposed/XposedBridge.jar",
    "/data/data/de.robv.android.xposed.installer/app_asar/XposedBridge.jar",
    "/data/data/org.lsposed.manager/files/bridge.jar",
)

internal val XPOSED_CLASSES = listOf(
    "de.robv.android.xposed.XposedBridge",
    "de.robv.android.xposed.XposedHelpers",
    "org.lsposed.lspd.nativebridge.NativeAPI",
)

internal val XPOSED_INSTALLER_PACKAGES = listOf(
    "de.robv.android.xposed.installer",
    "org.lsposed.manager",
    "org.meowcat.edxposed.manager",
)

internal val XPOSED_MODULE_PACKAGES = listOf(
    "com.ceco.marshmallow.gravitybox",
    "com.pyler.xposed.flatstylecoloredbars",
    "de.robv.android.xposed.mods.appsettings",
    "tw.fatminmin.xposed.minminguard",
)

internal fun checkXposedFrameworkInstalled(context: XposedCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    for (path in XPOSED_BRIDGE_JAR_PATHS) {
        try {
            if (context.fileExists(path)) {
                signals.add(
                    IntegritySignal(
                        id = XposedSignal.XPOSED_FRAMEWORK_INSTALLED,
                        name = "Xposed Framework Installed",
                        category = IntegrityCategory.HOOKING_OR_TAMPERING,
                        severity = SignalSeverity.HIGH,
                        confidence = 1.0,
                        details = "Xposed Bridge JAR found at: $path",
                        detectedAt = currentTimestampMs,
                        metadata = mapOf(
                            "path" to path,
                            "check" to XposedSignal.Check.XPOSED_FRAMEWORK_INSTALLED,
                        ),
                    ),
                )
            }
        } catch (_: Exception) {
            // File not accessible — best-effort only
        }
    }

    return signals
}

internal fun checkXposedBridgeClass(context: XposedCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    for (className in XPOSED_CLASSES) {
        try {
            if (context.isClassLoadable(className)) {
                signals.add(
                    IntegritySignal(
                        id = XposedSignal.XPOSED_BRIDGE_CLASS,
                        name = "Xposed Bridge Class Loadable",
                        category = IntegrityCategory.HOOKING_OR_TAMPERING,
                        severity = SignalSeverity.HIGH,
                        confidence = 0.95,
                        details = "Xposed Bridge class $className is loadable",
                        detectedAt = currentTimestampMs,
                        metadata = mapOf(
                            "class" to className,
                            "check" to XposedSignal.Check.XPOSED_BRIDGE_CLASS,
                        ),
                    ),
                )
            }
        } catch (_: Exception) {
            // Class reflection failed — best-effort only
        }
    }

    return signals
}

internal fun checkXposedInstallerApp(context: XposedCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    for (pkg in XPOSED_INSTALLER_PACKAGES) {
        try {
            if (context.isPackageInstalled(pkg)) {
                signals.add(
                    IntegritySignal(
                        id = XposedSignal.XPOSED_INSTALLER_APP,
                        name = "Xposed Installer App Detected",
                        category = IntegrityCategory.HOOKING_OR_TAMPERING,
                        severity = SignalSeverity.HIGH,
                        confidence = 0.9,
                        details = "Xposed/LSPosed installer package is installed: $pkg",
                        detectedAt = currentTimestampMs,
                        metadata = mapOf(
                            "package" to pkg,
                            "check" to XposedSignal.Check.XPOSED_INSTALLER_APP,
                        ),
                    ),
                )
            }
        } catch (_: Exception) {
            // Package lookup failed — best-effort only
        }
    }

    return signals
}

internal fun checkXposedModuleInstalled(context: XposedCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    for (pkg in XPOSED_MODULE_PACKAGES) {
        try {
            if (context.isPackageInstalled(pkg)) {
                signals.add(
                    IntegritySignal(
                        id = XposedSignal.XPOSED_MODULE_INSTALLED,
                        name = "Xposed Module Package Installed",
                        category = IntegrityCategory.HOOKING_OR_TAMPERING,
                        severity = SignalSeverity.MEDIUM,
                        confidence = 0.8,
                        details = "Known Xposed module package is installed: $pkg",
                        detectedAt = currentTimestampMs,
                        metadata = mapOf(
                            "package" to pkg,
                            "check" to XposedSignal.Check.XPOSED_MODULE_INSTALLED,
                        ),
                    ),
                )
            }
        } catch (_: Exception) {
            // Package lookup failed — best-effort only
        }
    }

    return signals
}