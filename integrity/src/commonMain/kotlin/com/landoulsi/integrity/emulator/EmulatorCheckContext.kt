package com.landoulsi.integrity.emulator

/**
 * Abstraction for platform-specific operations used by Android emulator / virtual-OS
 * detection checks.
 *
 * Decouples detection heuristics from Android framework types ([android.os.Build],
 * [android.hardware.SensorManager], [android.content.Context]) to enable JVM host
 * testing with fakes.
 */
interface EmulatorCheckContext {
    fun fileExists(path: String): Boolean
    fun isPackageInstalled(packageName: String): Boolean
    fun getBuildFingerprint(): String
    fun getBuildModel(): String
    fun getBuildManufacturer(): String
    fun getBuildBrand(): String
    fun getBuildDevice(): String
    fun getBuildProduct(): String
    fun getBuildHardware(): String

    /**
     * Total hardware sensor count reported by the sensor service, or `null` when the
     * service is unavailable. `null` is never coerced to zero — an unavailable service
     * is "unknown", not "no sensors".
     */
    fun getSensorCount(): Int?
}
