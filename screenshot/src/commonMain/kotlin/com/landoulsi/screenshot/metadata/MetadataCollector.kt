package com.landoulsi.screenshot.metadata

import com.landoulsi.screenshot.config.MetadataConfig
import com.landoulsi.screenshot.model.ScreenshotMetadata
import com.landoulsi.screenshot.model.ScreenshotTriggerType
import com.landoulsi.timeprovider.SystemTimeProvider
import com.landoulsi.timeprovider.TimeProvider

/**
 * Interface responsible for gathering device, operating system, and application metadata.
 */
interface MetadataCollector {

    /**
     * Collects all contextual metadata to accompany a screenshot upload.
     *
     * @param triggerType The mechanism that initiated the capture.
     * @param customParams Extra key-value parameters provided by caller or trigger payload.
     * @return Fully populated [ScreenshotMetadata].
     */
    suspend fun collectMetadata(
        triggerType: ScreenshotTriggerType,
        customParams: Map<String, String> = emptyMap()
    ): ScreenshotMetadata
}

/**
 * Platform-independent metadata collector that can be customized or enriched by platform-specific providers.
 */
class DefaultMetadataCollector(
    private val config: MetadataConfig = MetadataConfig(),
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val deviceModelProvider: () -> String? = { null },
    private val osNameProvider: () -> String? = { null },
    private val osVersionProvider: () -> String? = { null },
    private val appVersionProvider: () -> String? = { null },
    private val appBuildNumberProvider: () -> String? = { null },
    private val screenNameProvider: () -> String? = { null },
    private val userIdProvider: () -> String? = { null },
    private val sessionIdProvider: () -> String? = { null }
) : MetadataCollector {

    override suspend fun collectMetadata(
        triggerType: ScreenshotTriggerType,
        customParams: Map<String, String>
    ): ScreenshotMetadata {
        val mergedParams = buildMap {
            putAll(config.defaultCustomParameters)
            putAll(customParams)
        }

        return ScreenshotMetadata(
            triggerType = triggerType,
            timestamp = if (config.includeTimestamp) timeProvider.currentTimeMillis() else 0L,
            deviceModel = if (config.includeDeviceInfo) deviceModelProvider() else null,
            osName = if (config.includeDeviceInfo) osNameProvider() else null,
            osVersion = if (config.includeDeviceInfo) osVersionProvider() else null,
            appVersion = if (config.includeAppVersion) appVersionProvider() else null,
            appBuildNumber = if (config.includeAppVersion) appBuildNumberProvider() else null,
            currentScreen = screenNameProvider(),
            userId = userIdProvider(),
            sessionId = sessionIdProvider(),
            customParameters = mergedParams
        )
    }
}
