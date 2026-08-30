package com.landoulsi.screenshot.model

import kotlinx.serialization.Serializable

/**
 * Supported image compression and encoding formats.
 */
@Serializable
enum class ImageFormat(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    WEBP("webp", "image/webp")
}

/**
 * Identifies the signal or mechanism that triggered the screenshot capture.
 */
@Serializable
enum class ScreenshotTriggerType {
    /** Triggered automatically via a remote push notification signal. */
    PUSH_NOTIFICATION,

    /** Triggered explicitly by application code via the SDK API. */
    MANUAL,

    /** Triggered by physical shake or motion gesture. */
    SHAKE_GESTURE,

    /** Triggered when a matching custom event or analytics tag occurs. */
    CUSTOM_EVENT,

    /** Triggered on a periodic or scheduled timer. */
    SCHEDULED,

    /** Triggered upon an uncaught error, crash, or app diagnostic signal. */
    DIAGNOSTIC
}

/**
 * Represents raw captured screenshot image data and metadata.
 */
data class ScreenshotImage(
    val bytes: ByteArray,
    val format: ImageFormat = ImageFormat.JPEG,
    val width: Int = 0,
    val height: Int = 0,
    val timestamp: Long = 0L,
    val fileName: String = "screenshot_${timestamp}.${format.extension}"
) {
    val sizeInBytes: Int get() = bytes.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ScreenshotImage
        return bytes.contentEquals(other.bytes) &&
                format == other.format &&
                width == other.width &&
                height == other.height &&
                timestamp == other.timestamp &&
                fileName == other.fileName
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }

    override fun toString(): String {
        return "ScreenshotImage(fileName='$fileName', format=$format, sizeBytes=$sizeInBytes, dimensions=${width}x${height}, timestamp=$timestamp)"
    }
}

/**
 * Device, application, and contextual metadata attached to a screenshot upload.
 */
@Serializable
data class ScreenshotMetadata(
    val triggerType: ScreenshotTriggerType,
    val timestamp: Long,
    val deviceModel: String? = null,
    val osName: String? = null,
    val osVersion: String? = null,
    val appVersion: String? = null,
    val appBuildNumber: String? = null,
    val currentScreen: String? = null,
    val userId: String? = null,
    val sessionId: String? = null,
    val customParameters: Map<String, String> = emptyMap()
)

/**
 * Encapsulates the complete screenshot bundle ready for processing and upload.
 */
data class ScreenshotPayload(
    val image: ScreenshotImage,
    val metadata: ScreenshotMetadata
)

/**
 * Result returned following an upload attempt to the backend server.
 */
@Serializable
data class UploadResponse(
    val isSuccess: Boolean,
    val statusCode: Int,
    val responseBody: String? = null,
    val uploadId: String? = null,
    val errorMessage: String? = null
)
