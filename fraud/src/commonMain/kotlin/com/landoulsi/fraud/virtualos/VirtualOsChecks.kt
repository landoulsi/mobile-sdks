package com.landoulsi.fraud.virtualos

import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudSignal
import com.landoulsi.fraud.model.SignalSeverity
import kotlin.time.Clock

object VirtualOsSignal {
    const val PACKAGE_UNRESOLVABLE = "virtual_os_package_unresolvable"
    const val UID_MISMATCH = "virtual_os_uid_mismatch"
    const val DATA_DIR_ANOMALY = "virtual_os_data_dir_anomaly"
    const val KNOWN_CONTAINER_APP = "virtual_os_known_container_app"

    /** Every signal id this vector can emit; used to seed the [FraudResult] catalog. */
    val all: Set<String> = setOf(
        PACKAGE_UNRESOLVABLE,
        UID_MISMATCH,
        DATA_DIR_ANOMALY,
        KNOWN_CONTAINER_APP,
    )

    object Check {
        const val PACKAGE_UNRESOLVABLE = "package_unresolvable"
        const val UID_MISMATCH = "uid_mismatch"
        const val DATA_DIR_ANOMALY = "data_dir_anomaly"
        const val KNOWN_CONTAINER_APP = "known_container_app"
    }
}

/**
 * Must stay 1:1 with the `<queries>` package visibility declarations in AndroidManifest.xml.
 *
 * Well-known "run another app inside me" container apps: V Android, Parallel Space,
 * Dual Space (the Excelliance engine also embedded in many rebranded clone apps), and Magic.
 *
 * A container's guest `PackageManager` is typically a proxy that only reveals itself and the
 * apps it hosts, so this check does not detect that this app is *currently running as a guest*
 * — that is what [checkOwnPackageResolvable], [checkUidMismatch] and [checkDataDirAnomaly] are
 * for. This one instead detects that a container is installed alongside this app on a device
 * where it is running natively, which is itself a meaningful multi-account/cloning risk signal.
 */
internal val VIRTUAL_OS_CONTAINER_PACKAGES = listOf(
    "com.pspace.vandroid",
    "com.lbe.parallel",
    "com.excelliance.dualaid",
    "com.qihoo.magic",
)

internal fun checkOwnPackageResolvable(context: VirtualOsCheckContext): FraudSignal? {
    val isKnown = try {
        context.isOwnPackageKnownToPackageManager()
    } catch (_: Exception) {
        return null
    }

    if (isKnown) return null

    return FraudSignal(
        id = VirtualOsSignal.PACKAGE_UNRESOLVABLE,
        name = "App Package Unknown To System",
        category = FraudCategory.VIRTUAL_OS_OR_EMULATOR,
        severity = SignalSeverity.CRITICAL,
        confidence = 0.85,
        details = "The system PackageManager has no record of this app's own package, " +
            "indicating it is running inside a virtualized container rather than as a real install",
        detectedAt = Clock.System.now().toEpochMilliseconds(),
        metadata = mapOf("check" to VirtualOsSignal.Check.PACKAGE_UNRESOLVABLE),
    )
}

internal fun checkUidMismatch(context: VirtualOsCheckContext): FraudSignal? {
    val expectedUid = (
        try {
            context.getPackageManagerUid()
        } catch (_: Exception) {
            null
        }
        ) ?: return null

    val actualUid = try {
        context.getSelfReportedUid()
    } catch (_: Exception) {
        return null
    }

    if (expectedUid == actualUid) return null

    return FraudSignal(
        id = VirtualOsSignal.UID_MISMATCH,
        name = "Process UID Mismatch",
        category = FraudCategory.VIRTUAL_OS_OR_EMULATOR,
        severity = SignalSeverity.HIGH,
        confidence = 0.85,
        details = "The running process UID does not match the UID the system PackageManager " +
            "assigned to this app, indicating the process identity was substituted by a host container",
        detectedAt = Clock.System.now().toEpochMilliseconds(),
        metadata = mapOf("check" to VirtualOsSignal.Check.UID_MISMATCH),
    )
}

internal fun checkDataDirAnomaly(context: VirtualOsCheckContext): FraudSignal? {
    val dataDir = try { context.getDataDirPath() } catch (_: Exception) { "" }
    val ownPackage = try { context.getOwnPackageName() } catch (_: Exception) { "" }

    if (dataDir.isEmpty() || ownPackage.isEmpty()) return null

    // A real install's data dir is exactly one of these two shapes — nothing more, nothing
    // less. A container gives its guest a data dir *nested* under its own sandbox (e.g.
    // "/data/data/<container_pkg>/virtual/data/user/0/<our_pkg>"), which still ends with our
    // package name, so a substring/suffix check would miss it. Matching the legitimate shapes
    // positively instead of pattern-matching the anomaly is what actually catches that case.
    val legitimateDataDir = Regex("^/data/(?:data|user/\\d+)/${Regex.escape(ownPackage)}/?$")
    if (legitimateDataDir.matches(dataDir)) return null

    return FraudSignal(
        id = VirtualOsSignal.DATA_DIR_ANOMALY,
        name = "Data Directory Path Anomaly",
        category = FraudCategory.VIRTUAL_OS_OR_EMULATOR,
        severity = SignalSeverity.HIGH,
        confidence = 0.75,
        // Never include the raw path: it reveals the container's private sandbox layout.
        details = "This app's private data directory does not resolve under its own package name, " +
            "consistent with a nested/virtualized storage sandbox",
        detectedAt = Clock.System.now().toEpochMilliseconds(),
        metadata = mapOf("check" to VirtualOsSignal.Check.DATA_DIR_ANOMALY),
    )
}

internal fun checkKnownContainerApps(context: VirtualOsCheckContext): List<FraudSignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    return VIRTUAL_OS_CONTAINER_PACKAGES.filter { packageName ->
        try {
            context.isPackageInstalled(packageName)
        } catch (_: Exception) {
            false
        }
    }.map { packageName ->
        FraudSignal(
            id = VirtualOsSignal.KNOWN_CONTAINER_APP,
            name = "Virtual OS Container App Detected",
            category = FraudCategory.VIRTUAL_OS_OR_EMULATOR,
            severity = SignalSeverity.HIGH,
            confidence = 0.8,
            details = "A known app-virtualization container is installed: $packageName",
            detectedAt = currentTimestampMs,
            metadata = mapOf("package" to packageName, "check" to VirtualOsSignal.Check.KNOWN_CONTAINER_APP),
        )
    }
}
