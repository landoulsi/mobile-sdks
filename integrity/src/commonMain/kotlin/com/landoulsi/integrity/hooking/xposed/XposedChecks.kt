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
)

internal val XPOSED_INSTALLER_PACKAGES = listOf(
    "de.robv.android.xposed.installer",
    "org.lsposed.manager",
)

internal val XPOSED_MODULE_PACKAGES = listOf(
    "com.ceco.marshmallow.gravitybox",
    "com.pyler.xposed.flatstylecoloredbars",
    "de.robv.android.xposed.mods.appsettings",
)

internal fun checkXposedFrameworkInstalled(context: XposedCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    for (path in XPOSED_BRIDGE_JAR_PATHS) {
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
                    metadata = mapOf("path" to path, "check" to XposedSignal.Check.XPOSED_FRAMEWORK_INSTALLED),
                ),
            )
        }
    }

    return signals
}

internal fun checkXposedBridgeClass(context: XposedCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    // Attempt to reflectively load the XposedBridge class;
    // on a non-Xposed device this will fail with ClassNotFoundException.
    if (context.isClassLoadable("de.robv.android.xposed.XposedBridge")) {
        signals.add(
            IntegritySignal(
                id = XposedSignal.XPOSED_BRIDGE_CLASS,
                name = "Xposed Bridge Class Loadable",
                category = IntegrityCategory.HOOKING_OR_TAMPERING,
                severity = SignalSeverity.HIGH,
                confidence = 0.95,
                details = "Xposed Bridge class de.robv.android.xposed.XposedBridge is loadable",
                detectedAt = currentTimestampMs,
                metadata = mapOf("class" to "de.robv.android.xposed.XposedBridge", "check" to XposedSignal.Check.XPOSED_BRIDGE_CLASS),
            ),
        )
    }

    return signals
}

internal fun checkXposedInstallerApp(context: XposedCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    for (pkg in XPOSED_INSTALLER_PACKAGES) {
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
                    metadata = mapOf("package" to pkg, "check" to XposedSignal.Check.XPOSED_INSTALLER_APP),
                ),
            )
        }
    }

    return signals
}

internal fun checkXposedModuleInstalled(context: XposedCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    for (pkg in XPOSED_MODULE_PACKAGES) {
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
                    metadata = mapOf("package" to pkg, "check" to XposedSignal.Check.XPOSED_MODULE_INSTALLED),
                ),
            )
        }
    }

    return signals
}