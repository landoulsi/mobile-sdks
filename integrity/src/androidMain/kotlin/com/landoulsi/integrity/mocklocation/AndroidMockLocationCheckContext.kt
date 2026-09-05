package com.landoulsi.integrity.mocklocation

import android.app.AppOpsManager
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Converts an Android [Location] instance into a pure domain [LocationSample].
 */
fun Location.toLocationSample(): LocationSample {
    val mock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        isMock
    } else {
        @Suppress("DEPRECATION")
        isFromMockProvider
    }
    return LocationSample(
        latitude = latitude,
        longitude = longitude,
        altitude = if (hasAltitude()) altitude else null,
        accuracy = if (hasAccuracy()) accuracy else null,
        speed = if (hasSpeed()) speed else null,
        timestampMs = time,
        isMock = mock,
    )
}

/**
 * Android implementation of [MockLocationCheckContext] wiring platform framework types
 * ([Context], [LocationManager], [AppOpsManager], [Settings]) into mock location detection heuristics.
 *
 * Ensures [Context.getApplicationContext] is retained to prevent leaking short-lived
 * [android.app.Activity] contexts across background detection sweeps.
 *
 * @param context Android context used for system services; automatically coerced to application context.
 * @param recentLocationSupplier Optional provider lambda supplying recently observed [LocationSample] fixes.
 */
class AndroidMockLocationCheckContext(
    context: Context,
    private val recentLocationSupplier: () -> List<LocationSample> = { emptyList() },
) : MockLocationCheckContext {

    private val applicationContext: Context = context.applicationContext ?: context

    @Suppress("DEPRECATION")
    override fun isMockLocationAppSet(): Boolean = try {
        val appOps = applicationContext.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        if (appOps != null) {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    applicationContext.packageName,
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_MOCK_LOCATION,
                    Process.myUid(),
                    applicationContext.packageName,
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }

    override fun isMockProviderActive(): Boolean = try {
        val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (locationManager != null) {
            val providers = locationManager.getProviders(false)
            providers.any { provider ->
                provider.contains("mock", ignoreCase = true) || provider.contains("test", ignoreCase = true)
            }
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }

    override fun isDeveloperMockSettingEnabled(): Boolean = try {
        @Suppress("DEPRECATION")
        val setting = Settings.Secure.getString(
            applicationContext.contentResolver,
            Settings.Secure.ALLOW_MOCK_LOCATION,
        )
        setting != null && setting != "0"
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

    override fun getRecentLocations(): List<LocationSample> = try {
        recentLocationSupplier()
    } catch (_: Exception) {
        emptyList()
    }
}
