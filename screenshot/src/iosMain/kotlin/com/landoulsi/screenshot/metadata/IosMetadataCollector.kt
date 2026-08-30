package com.landoulsi.screenshot.metadata

import com.landoulsi.screenshot.config.MetadataConfig
import com.landoulsi.timeprovider.SystemTimeProvider
import com.landoulsi.timeprovider.TimeProvider
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

/**
 * Factory for creating [DefaultMetadataCollector] pre-populated with iOS device and bundle info.
 */
object IosMetadataCollector {

    fun create(
        config: MetadataConfig = MetadataConfig(),
        timeProvider: TimeProvider = SystemTimeProvider(),
        screenNameProvider: () -> String? = { null },
        userIdProvider: () -> String? = { null },
        sessionIdProvider: () -> String? = { null },
    ): MetadataCollector {
        val mainBundle = NSBundle.mainBundle
        val infoDict = mainBundle.infoDictionary

        val appVersion = infoDict?.get("CFBundleShortVersionString") as? String
        val appBuild = infoDict?.get("CFBundleVersion") as? String

        return DefaultMetadataCollector(
            config = config,
            timeProvider = timeProvider,
            deviceModelProvider = { UIDevice.currentDevice.model },
            osNameProvider = { UIDevice.currentDevice.systemName },
            osVersionProvider = { "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}" },
            appVersionProvider = { appVersion },
            appBuildNumberProvider = { appBuild },
            screenNameProvider = screenNameProvider,
            userIdProvider = userIdProvider,
            sessionIdProvider = sessionIdProvider
        )
    }
}
