package com.landoulsi.integrity.installer

/**
 * Platform-reported provenance of the package's installation.
 *
 * @property installingPackageName Package currently responsible for the app (e.g. "com.android.vending"
 * for Play Store), or `null` if the platform has no installer of record (common for adb/shell installs).
 * @property initiatingPackageName Package that actually performed the installation (e.g. "com.android.shell"
 * for `adb install`), or `null` if unavailable. Only populated on API 30+; always `null` below that.
 */
data class InstallSourceInfo(
    val installingPackageName: String?,
    val initiatingPackageName: String?,
)

/**
 * Abstraction for platform-specific operations used by untrusted installer source detection checks.
 *
 * Decouples detection heuristics from Android framework types ([android.content.pm.PackageManager])
 * to enable JVM host testing with fakes. Android-only vector: there is no iOS installer provenance
 * API equivalent, matching the precedent set by [com.landoulsi.integrity.emulator.EmulatorCheckContext]
 * and [com.landoulsi.integrity.network.NetworkCheckContext].
 */
interface InstallerCheckContext {
    /**
     * @throws Exception if the platform query fails (e.g. [android.os.RemoteException]); implementations
     * must not swallow failures into a synthetic result, since that would misreport a platform error as
     * an unknown/untrusted install source. Callers are expected to catch at the evaluator boundary.
     */
    fun getInstallSourceInfo(): InstallSourceInfo
}
