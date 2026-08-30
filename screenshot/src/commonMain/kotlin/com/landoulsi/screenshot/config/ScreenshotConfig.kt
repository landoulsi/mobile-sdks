package com.landoulsi.screenshot.config

import com.landoulsi.screenshot.model.ImageFormat
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Root configuration object for the Screenshot SDK.
 *
 * Can be loaded from a JSON configuration file, remote config service, or constructed programmatically.
 */
@Serializable
data class ScreenshotConfig(
    val server: ServerConfig,
    val capture: CaptureConfig = CaptureConfig(),
    val triggers: TriggerConfig = TriggerConfig(),
    val metadata: MetadataConfig = MetadataConfig(),
    val isEnabled: Boolean = true
) {
    companion object {
        private val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = true
        }

        /**
         * Parses a [ScreenshotConfig] from a JSON string.
         */
        fun fromJson(json: String): ScreenshotConfig {
            return jsonParser.decodeFromString(serializer(), json)
        }

        /**
         * Serializes a [ScreenshotConfig] to a formatted JSON string.
         */
        fun toJson(config: ScreenshotConfig): String {
            return jsonParser.encodeToString(serializer(), config)
        }
    }
}

/**
 * Configuration for the remote server upload destination.
 */
@Serializable
data class ServerConfig(
    /** Full URL endpoint where the screenshot and metadata should be sent. */
    val endpointUrl: String,

    /** HTTP method to use (e.g. "POST" or "PUT"). */
    val method: String = "POST",

    /** Static custom HTTP headers attached to every upload request. */
    val headers: Map<String, String> = emptyMap(),

    /** Optional bearer token or API key to attach in Authorization header. */
    val authToken: String? = null,

    /** Connection and socket timeout in milliseconds. */
    val timeoutMillis: Long = 30_000L,

    /** Upload retry policy in case of network or transient server errors. */
    val retryPolicy: RetryPolicy = RetryPolicy(),

    /** Multipart form field name for the image binary data. */
    val fileFieldName: String = "screenshot",

    /** Multipart form field name for the serialized JSON metadata. */
    val metadataFieldName: String = "metadata",

    /** Additional static key-value fields to include in multipart form payload. */
    val additionalFields: Map<String, String> = emptyMap()
)

/**
 * Configuration for screenshot capture rendering, formatting, and compression.
 */
@Serializable
data class CaptureConfig(
    /** Desired image compression format (JPEG, PNG, WEBP). */
    val format: ImageFormat = ImageFormat.JPEG,

    /** Compression quality (1-100), applied when format supports lossy compression (JPEG, WEBP). */
    val quality: Int = 80,

    /** Optional maximum width or height dimension (in pixels) for downscaling large displays. */
    val maxDimension: Int? = null,

    /** Scale factor applied during capture (e.g. 0.5 for half resolution). */
    val scaleFactor: Float = 1.0f,

    /** If true, sensitive input views (e.g. passwords, payment fields) should be masked or omitted. */
    val maskSensitiveViews: Boolean = true,

    /** Optional delay in milliseconds before taking the screenshot to allow UI transitions to settle. */
    val captureDelayMillis: Long = 0L
)

/**
 * Configuration for signals and triggers that initiate screenshot captures.
 */
@Serializable
data class TriggerConfig(
    val push: PushTriggerConfig = PushTriggerConfig(),
    val shake: ShakeTriggerConfig = ShakeTriggerConfig(),
    val manual: ManualTriggerConfig = ManualTriggerConfig(),
    val events: EventTriggerConfig = EventTriggerConfig(),
    val periodic: PeriodicTriggerConfig = PeriodicTriggerConfig()
)

/**
 * Configuration for remote push notification signals.
 */
@Serializable
data class PushTriggerConfig(
    val isEnabled: Boolean = true,
    /** The key in the push data payload containing the action signal. */
    val payloadActionKey: String = "action",
    /** The expected value for [payloadActionKey] that triggers screenshot capture. */
    val triggerActionValue: String = "CAPTURE_SCREENSHOT",
    /** Optional key in push payload to pass extra tracking identifiers (e.g. campaignId, issueId). */
    val payloadMetadataKey: String = "screenshot_metadata"
)

/**
 * Configuration for physical shake or motion gesture triggers.
 */
@Serializable
data class ShakeTriggerConfig(
    val isEnabled: Boolean = false,
    /** Minimum acceleration threshold to detect a shake gesture (in G-force / m/s^2). */
    val sensitivityThreshold: Float = 2.5f
)

/**
 * Configuration for manual programmatic API triggers.
 */
@Serializable
data class ManualTriggerConfig(
    val isEnabled: Boolean = true
)

/**
 * Configuration for triggering screenshot capture based on custom app events or tags.
 */
@Serializable
data class EventTriggerConfig(
    val isEnabled: Boolean = false,
    /** Set of event names that should trigger a capture when logged. */
    val triggerEvents: Set<String> = emptySet()
)

/**
 * Configuration for scheduled periodic screenshot captures.
 */
@Serializable
data class PeriodicTriggerConfig(
    val isEnabled: Boolean = false,
    /** Interval between automatic captures in milliseconds. */
    val intervalMillis: Long = 60_000L
)

/**
 * Metadata collection settings.
 */
@Serializable
data class MetadataConfig(
    val includeDeviceInfo: Boolean = true,
    val includeAppVersion: Boolean = true,
    val includeTimestamp: Boolean = true,
    val defaultCustomParameters: Map<String, String> = emptyMap()
)

/**
 * Configuration for network retry policies.
 */
@Serializable
data class RetryPolicy(
    val maxRetries: Int = 3,
    val initialBackoffMillis: Long = 1000L,
    val backoffMultiplier: Double = 2.0
)
