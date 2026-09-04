package com.landoulsi.fraud.virtualos

import android.content.Context
import android.content.pm.PackageManager
import android.os.Process

/**
 * Android implementation of [VirtualOsCheckContext] wiring platform framework types
 * ([Context], [PackageManager], [Process]) into "virtual OS" container detection heuristics.
 *
 * Ensures [Context.getApplicationContext] is retained to prevent leaking short-lived
 * [android.app.Activity] contexts across background detection sweeps.
 *
 * @param context Context used for package manager queries; automatically coerced to
 * application context.
 */
class AndroidVirtualOsCheckContext(
    context: Context,
) : VirtualOsCheckContext {

    private val applicationContext: Context = context.applicationContext ?: context
    private val ownPackageName: String = applicationContext.packageName

    @Suppress("DEPRECATION")
    private val ownApplicationInfo = try {
        applicationContext.packageManager.getApplicationInfo(ownPackageName, 0)
    } catch (_: Exception) {
        null
    }

    @Suppress("DEPRECATION")
    override fun isPackageInstalled(packageName: String): Boolean = try {
        applicationContext.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }

    override fun isOwnPackageKnownToPackageManager(): Boolean = ownApplicationInfo != null

    override fun getSelfReportedUid(): Int = Process.myUid()

    override fun getPackageManagerUid(): Int? = ownApplicationInfo?.uid

    override fun getDataDirPath(): String =
        applicationContext.applicationInfo?.dataDir ?: applicationContext.filesDir.absolutePath

    override fun getOwnPackageName(): String = ownPackageName
}
