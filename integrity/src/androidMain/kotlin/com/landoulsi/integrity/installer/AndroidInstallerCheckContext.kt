package com.landoulsi.integrity.installer

import android.content.Context
import android.os.Build

/**
 * Android implementation of [InstallerCheckContext] wiring [android.content.pm.PackageManager]
 * install provenance APIs into the untrusted installer source detection heuristics.
 *
 * Ensures [Context.getApplicationContext] is retained to prevent leaking short-lived
 * [android.app.Activity] contexts across background detection sweeps.
 *
 * @param context Context used for package manager queries; automatically coerced to application context.
 */
class AndroidInstallerCheckContext(
    context: Context,
) : InstallerCheckContext {

    private val applicationContext: Context = context.applicationContext ?: context

    override fun getInstallSourceInfo(): InstallSourceInfo {
        val packageManager = applicationContext.packageManager
        val packageName = applicationContext.packageName

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val sourceInfo = packageManager.getInstallSourceInfo(packageName)
            InstallSourceInfo(
                installingPackageName = sourceInfo.installingPackageName,
                initiatingPackageName = sourceInfo.initiatingPackageName,
            )
        } else {
            @Suppress("DEPRECATION")
            InstallSourceInfo(
                installingPackageName = packageManager.getInstallerPackageName(packageName),
                initiatingPackageName = null,
            )
        }
    }
}
