package com.landoulsi.integrity.virtualos

/**
 * Abstraction for platform-specific operations used by "virtual OS" container
 * detection checks — apps such as V Android, VMOS, or Parallel Space that host a
 * nested, virtualized Android instance and run other apps *inside* it as guests
 * (as opposed to [com.landoulsi.integrity.emulator.EmulatorCheckContext], which detects
 * that the device itself is a desktop-hosted virtual device).
 *
 * Decouples detection heuristics from Android framework types ([android.content.Context],
 * [android.content.pm.PackageManager], [android.os.Process]) to enable JVM host testing
 * with fakes.
 */
interface VirtualOsCheckContext {
    fun isPackageInstalled(packageName: String): Boolean

    /** Whether the real, system-level [android.content.pm.PackageManager] knows this app's own package. */
    fun isOwnPackageKnownToPackageManager(): Boolean

    /** UID of the current process, from [android.os.Process.myUid]. */
    fun getSelfReportedUid(): Int

    /**
     * UID the system [android.content.pm.PackageManager] reports for this app's own
     * package, or `null` when unresolvable.
     */
    fun getPackageManagerUid(): Int?

    /** Absolute path of this app's private data directory. */
    fun getDataDirPath(): String

    /** This app's own declared package name. */
    fun getOwnPackageName(): String
}
