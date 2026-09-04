package com.landoulsi.fraud.emulator

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import java.io.File

/**
 * Android implementation of [EmulatorCheckContext] wiring platform framework types
 * ([Build], [SensorManager], [Context]) into emulator / virtual-OS detection heuristics.
 *
 * Ensures [Context.getApplicationContext] is retained to prevent leaking short-lived
 * [android.app.Activity] contexts across background detection sweeps.
 *
 * @param context Context used for package manager and sensor service queries;
 * automatically coerced to application context.
 */
class AndroidEmulatorCheckContext(
    context: Context,
) : EmulatorCheckContext {

    private val applicationContext: Context = context.applicationContext ?: context

    override fun fileExists(path: String): Boolean = try {
        File(path).exists()
    } catch (_: Exception) {
        false
    }

    @Suppress("DEPRECATION")
    override fun isPackageInstalled(packageName: String): Boolean = try {
        applicationContext.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }

    override fun getBuildFingerprint(): String = Build.FINGERPRINT ?: ""

    override fun getBuildModel(): String = Build.MODEL ?: ""

    override fun getBuildManufacturer(): String = Build.MANUFACTURER ?: ""

    override fun getBuildBrand(): String = Build.BRAND ?: ""

    override fun getBuildDevice(): String = Build.DEVICE ?: ""

    override fun getBuildProduct(): String = Build.PRODUCT ?: ""

    override fun getBuildHardware(): String = Build.HARDWARE ?: ""

    override fun getSensorCount(): Int? {
        val sensorManager = applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return null
        return try {
            sensorManager.getSensorList(Sensor.TYPE_ALL).size
        } catch (_: Exception) {
            null
        }
    }
}
