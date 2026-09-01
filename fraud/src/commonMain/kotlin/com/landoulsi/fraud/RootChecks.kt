package com.landoulsi.fraud

import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudSignal
import com.landoulsi.fraud.model.SignalSeverity
import kotlin.time.Clock

object RootSignal {
    const val SU_BINARY = "root_su_binary"
    const val MAGISK_MOUNT = "root_magisk_mount"
    const val MAGISK_PACKAGE = "root_magisk_package"
    const val KERNELSU_PACKAGE = "root_kernelsu_package"
    const val TEST_KEYS = "root_test_keys"
    const val WRITABLE_SYSTEM = "root_writable_system"
    const val SUPERUSER_APP = "root_superuser_app"

    /** Every signal id this vector can emit; used to seed the [FraudResult] catalog. */
    val all: Set<String> = setOf(
        SU_BINARY,
        MAGISK_MOUNT,
        MAGISK_PACKAGE,
        KERNELSU_PACKAGE,
        TEST_KEYS,
        WRITABLE_SYSTEM,
        SUPERUSER_APP,
    )

    object Check {
        const val SU_BINARY = "su_binary"
        const val MAGISK_MOUNT = "magisk_mount"
        const val MAGISK_PACKAGE = "magisk_package"
        const val KERNELSU_PACKAGE = "kernelsu_package"
        const val BUILD_TAG = "build_tag"
        const val WRITABLE_SYSTEM = "writable_system"
        const val SUPERUSER_APP = "superuser_app"
    }
}

internal val SU_BINARY_PATHS = listOf(
    "/system/bin/su",
    "/system/xbin/su",
    "/sbin/su",
    "/vendor/bin/su",
    "/data/local/xbin/su",
    "/data/local/bin/su",
    "/system/sd/xbin/su",
    "/system/usr/weNeedRoot/xbin/su",
)

internal val MAGISK_PACKAGE_NAMES = listOf(
    "com.topjohnwu.magisk",
    "com.topjohnwu.magisk_",
)

internal val KERNELSU_PACKAGE_NAMES = listOf(
    "me.weishu.kernelsu",
    "me.weishu.kernelsu.manager",
)

internal val SUPERUSER_APK_PATHS = listOf(
    "/system/app/Superuser.apk",
    "/system/app/SuperSU.apk",
    "/system/priv-app/Superuser.apk",
    "/system/priv-app/SuperSU.apk",
)

private val WHITESPACE_REGEX = Regex("\\s+")
private const val MOUNT_POINT_COLUMN_INDEX = 1
private const val MOUNT_OPTIONS_COLUMN_INDEX = 3
private const val MIN_MOUNT_COLUMNS = 4

internal fun checkSuBinaries(context: RootCheckContext): List<FraudSignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    return SU_BINARY_PATHS.filter { path ->
        try {
            context.fileExists(path)
        } catch (_: Exception) {
            false
        }
    }.map { path ->
        FraudSignal(
            id = RootSignal.SU_BINARY,
            name = "SU Binary Detected",
            category = FraudCategory.ROOT_OR_JAILBREAK,
            severity = SignalSeverity.HIGH,
            confidence = 1.0,
            details = "Root management binary found at: $path",
            detectedAt = currentTimestampMs,
            metadata = mapOf("path" to path, "check" to RootSignal.Check.SU_BINARY),
        )
    }
}

internal fun checkMagisk(context: RootCheckContext): List<FraudSignal> {
    val signals = mutableListOf<FraudSignal>()
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()

    val mounts = try {
        context.readFileLines("/proc/mounts")
    } catch (_: Exception) {
        emptyList()
    }

    val hasMagiskMount = mounts.any { mountLine ->
        val lowercaseLine = mountLine.lowercase()
        lowercaseLine.contains("magisk") || lowercaseLine.contains("kernelsu")
    }

    if (hasMagiskMount) {
        signals.add(
            FraudSignal(
                id = RootSignal.MAGISK_MOUNT,
                name = "Magisk Mount Detected",
                category = FraudCategory.ROOT_OR_JAILBREAK,
                severity = SignalSeverity.HIGH,
                confidence = 1.0,
                details = "Magisk or KernelSU mount entry found in /proc/mounts",
                detectedAt = currentTimestampMs,
                metadata = mapOf("check" to RootSignal.Check.MAGISK_MOUNT),
            ),
        )
    }

    val magiskInstalled = MAGISK_PACKAGE_NAMES.any { packageName ->
        try {
            context.isPackageInstalled(packageName)
        } catch (_: Exception) {
            false
        }
    }
    if (magiskInstalled) {
        signals.add(
            FraudSignal(
                id = RootSignal.MAGISK_PACKAGE,
                name = "Magisk App Detected",
                category = FraudCategory.ROOT_OR_JAILBREAK,
                severity = SignalSeverity.HIGH,
                confidence = 1.0,
                details = "Magisk package is installed on this device",
                detectedAt = currentTimestampMs,
                metadata = mapOf("check" to RootSignal.Check.MAGISK_PACKAGE),
            ),
        )
    }

    val kernelSuInstalled = KERNELSU_PACKAGE_NAMES.any { packageName ->
        try {
            context.isPackageInstalled(packageName)
        } catch (_: Exception) {
            false
        }
    }
    if (kernelSuInstalled) {
        signals.add(
            FraudSignal(
                id = RootSignal.KERNELSU_PACKAGE,
                name = "KernelSU Detected",
                category = FraudCategory.ROOT_OR_JAILBREAK,
                severity = SignalSeverity.CRITICAL,
                confidence = 1.0,
                details = "KernelSU package is installed on this device",
                detectedAt = currentTimestampMs,
                metadata = mapOf("check" to RootSignal.Check.KERNELSU_PACKAGE),
            ),
        )
    }

    return signals
}

internal fun checkTestBuildKeys(context: RootCheckContext): FraudSignal? {
    val buildTag = try {
        context.getBuildTag()
    } catch (_: Exception) {
        return null
    }

    if (buildTag.contains("test-keys", ignoreCase = true)) {
        return FraudSignal(
            id = RootSignal.TEST_KEYS,
            name = "Test-Keys Build Tag",
            category = FraudCategory.ROOT_OR_JAILBREAK,
            severity = SignalSeverity.MEDIUM,
            confidence = 0.8,
            details = "Build tag is 'test-keys', indicating a userdebug/eng or custom ROM build",
            detectedAt = Clock.System.now().toEpochMilliseconds(),
            metadata = mapOf("tag" to buildTag, "check" to RootSignal.Check.BUILD_TAG),
        )
    }

    return null
}

internal fun checkWritableSystem(context: RootCheckContext): FraudSignal? {
    val mounts = try {
        context.readFileLines("/proc/mounts")
    } catch (_: Exception) {
        return null
    }

    val hasWritableSystem = mounts.any { mountLine ->
        val columns = mountLine.trim().split(WHITESPACE_REGEX)
        columns.size >= MIN_MOUNT_COLUMNS &&
            (columns[MOUNT_POINT_COLUMN_INDEX] == "/system" || columns[MOUNT_POINT_COLUMN_INDEX] == "/") &&
            columns[MOUNT_OPTIONS_COLUMN_INDEX].split(",").contains("rw")
    }

    if (hasWritableSystem) {
        return FraudSignal(
            id = RootSignal.WRITABLE_SYSTEM,
            name = "Writable System Partition",
            category = FraudCategory.ROOT_OR_JAILBREAK,
            severity = SignalSeverity.MEDIUM,
            confidence = 0.7,
            details = "/system partition is mounted read-write",
            detectedAt = Clock.System.now().toEpochMilliseconds(),
            metadata = mapOf("check" to RootSignal.Check.WRITABLE_SYSTEM),
        )
    }

    return null
}

internal fun checkSuperuserApps(context: RootCheckContext): List<FraudSignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    return SUPERUSER_APK_PATHS.filter { path ->
        try {
            context.fileExists(path)
        } catch (_: Exception) {
            false
        }
    }.map { path ->
        FraudSignal(
            id = RootSignal.SUPERUSER_APP,
            name = "Superuser Application Detected",
            category = FraudCategory.ROOT_OR_JAILBREAK,
            severity = SignalSeverity.LOW,
            confidence = 0.9,
            details = "Superuser/SuperSU application found at: $path",
            detectedAt = currentTimestampMs,
            metadata = mapOf("path" to path, "check" to RootSignal.Check.SUPERUSER_APP),
        )
    }
}

