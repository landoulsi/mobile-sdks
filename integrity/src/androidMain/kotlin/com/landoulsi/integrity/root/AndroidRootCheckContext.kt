package com.landoulsi.integrity.root

import android.content.Context
import android.os.Build
import java.io.File

/**
 * Android implementation of [RootCheckContext] wiring platform framework types
 * ([Context], [File], [Build]) into the root detection heuristics.
 *
 * Ensures [Context.getApplicationContext] is retained to prevent leaking short-lived [android.app.Activity]
 * contexts across background detection sweeps.
 *
 * @param context Context used for package manager queries; automatically coerced to application context.
 */
class AndroidRootCheckContext(
    context: Context,
) : RootCheckContext {

    private val applicationContext: Context = context.applicationContext ?: context

    override fun fileExists(path: String): Boolean = try {
        File(path).exists()
    } catch (_: Exception) {
        false
    }

    override fun readFileLines(path: String): List<String> = try {
        File(path).takeIf { it.exists() && it.canRead() }?.readLines() ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    override fun getBuildTag(): String = Build.TAGS ?: ""

    @Suppress("DEPRECATION")
    override fun isPackageInstalled(packageName: String): Boolean = try {
        applicationContext.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }
}