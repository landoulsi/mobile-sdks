package com.landoulsi.screenshot.capture

import com.landoulsi.screenshot.config.CaptureConfig
import com.landoulsi.screenshot.model.ImageFormat
import com.landoulsi.screenshot.model.ScreenshotImage
import com.landoulsi.timeprovider.SystemTimeProvider
import com.landoulsi.timeprovider.TimeProvider
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.UIGraphicsImageRenderer
import platform.posix.memcpy

/**
 * iOS implementation of [ScreenshotCapturer] capturing screenshot from the active key window or supplied view.
 */
class IosScreenshotCapturer(
    private val windowProvider: () -> UIWindow? = { findKeyWindow() },
    private val timeProvider: TimeProvider = SystemTimeProvider(),
) : ScreenshotCapturer {

    override fun isAvailable(): Boolean {
        return windowProvider() != null
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun capture(config: CaptureConfig): Result<ScreenshotImage> = runCatching {
        val window = windowProvider() ?: throw IllegalStateException("No active UIWindow available to capture screenshot")
        val bounds = window.bounds

        val renderer = UIGraphicsImageRenderer(bounds = bounds)
        val image: UIImage = renderer.imageWithActions { _ ->
            window.drawViewHierarchyInRect(bounds, afterScreenUpdates = true)
        }

        val processedImage = processImage(image, config)

        val nsData: NSData = when (config.format) {
            ImageFormat.PNG -> UIImagePNGRepresentation(processedImage)
                ?: throw IllegalStateException("Failed to encode screenshot as PNG")
            ImageFormat.JPEG, ImageFormat.WEBP -> {
                val quality = (config.quality.coerceIn(1, 100) / 100.0)
                UIImageJPEGRepresentation(processedImage, quality)
                    ?: throw IllegalStateException("Failed to encode screenshot as JPEG")
            }
        }

        val bytes = nsData.toByteArray()
        val width = processedImage.size.useContents { width.toInt() }
        val height = processedImage.size.useContents { height.toInt() }

        ScreenshotImage(
            bytes = bytes,
            format = if (config.format == ImageFormat.WEBP) ImageFormat.JPEG else config.format,
            width = width,
            height = height,
            timestamp = timeProvider.currentTimeMillis()
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun processImage(image: UIImage, config: CaptureConfig): UIImage {
        var currentW: Double = image.size.useContents { width }
        var currentH: Double = image.size.useContents { height }

        var targetW = currentW
        var targetH = currentH

        if (config.scaleFactor > 0f && config.scaleFactor != 1.0f) {
            targetW *= config.scaleFactor.toDouble()
            targetH *= config.scaleFactor.toDouble()
        }

        config.maxDimension?.let { maxDim ->
            val maxCurrent = maxOf(targetW, targetH)
            if (maxCurrent > maxDim.toDouble()) {
                val ratio = maxDim.toDouble() / maxCurrent
                targetW *= ratio
                targetH *= ratio
            }
        }

        if (targetW != currentW || targetH != currentH) {
            val targetSize = CGSizeMake(targetW, targetH)
            val renderer = UIGraphicsImageRenderer(size = targetSize)
            return renderer.imageWithActions { _ ->
                image.drawInRect(CGRectMake(0.0, 0.0, targetW, targetH))
            }
        }

        return image
    }

    companion object {
        fun findKeyWindow(): UIWindow? {
            val scenes = UIApplication.sharedApplication.connectedScenes
            for (scene in scenes) {
                if (scene is UIWindowScene) {
                    for (window in scene.windows) {
                        if (window is UIWindow && window.isKeyWindow()) {
                            return window
                        }
                    }
                }
            }
            @Suppress("DEPRECATION")
            return UIApplication.sharedApplication.keyWindow
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val byteArray = ByteArray(size)
    if (size > 0) {
        byteArray.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
    return byteArray
}
