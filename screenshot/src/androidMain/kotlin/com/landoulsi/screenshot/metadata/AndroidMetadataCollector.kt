package com.landoulsi.screenshot.metadata

import android.content.Context
import android.os.Build
import com.landoulsi.screenshot.config.MetadataConfig
import com.landoulsi.timeprovider.SystemTimeProvider
import com.landoulsi.timeprovider.TimeProvider

/**
 * Factory for creating [DefaultMetadataCollector] pre-populated with Android system and package metadata.
 */
object AndroidMetadataCollector {

    fun create(
        context: Context,
        config: MetadataConfig = MetadataConfig(),
        timeProvider: TimeProvider = SystemTimeProvider(),
        screenNameProvider: () -> String? = { null },
        userIdProvider: () -> String? = { null },
        sessionIdProvider: () -> String? = { null },
    ): MetadataCollector {
        val packageInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()

        return DefaultMetadataCollector(
            config = config,
            timeProvider = timeProvider,
            deviceModelProvider = { "${Build.MANUFACTURER} ${Build.MODEL}" },
            osNameProvider = { "Android" },
            osVersionProvider = { "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})" },
            appVersionProvider = { packageInfo?.versionName },
            appBuildNumberProvider = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo?.longVersionCode?.toString()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo?.versionCode?.toString()
                }
            },
            screenNameProvider = screenNameProvider,
            userIdProvider = userIdProvider,
            sessionIdProvider = sessionIdProvider
        )
    }
}
