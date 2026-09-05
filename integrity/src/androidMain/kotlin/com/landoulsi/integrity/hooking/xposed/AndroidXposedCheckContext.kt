package com.landoulsi.integrity.hooking.xposed

import android.content.Context
import java.io.File

/**
 * Android implementation of [XposedCheckContext] wiring platform framework types
 * ([Context], [File]) into Xposed hooking framework detection heuristics.
 */
class AndroidXposedCheckContext(
    private val androidContext: Context,
) : XposedCheckContext {

    private val applicationContext: Context = androidContext.applicationContext

    override fun fileExists(path: String): Boolean = try {
        File(path).exists()
    } catch (_: Exception) {
        false
    }

    override fun readFileLines(path: String): List<String> = try {
        File(path).readLines()
    } catch (_: Exception) {
        emptyList()
    }

    override fun isPackageInstalled(packageName: String): Boolean = try {
        applicationContext.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }

    override fun isClassLoadable(className: String): Boolean = try {
        Class.forName(className)
        true
    } catch (_: Exception) {
        false
    }
}