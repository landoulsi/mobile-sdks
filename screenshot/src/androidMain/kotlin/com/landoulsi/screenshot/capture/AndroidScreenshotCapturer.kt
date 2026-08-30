package com.landoulsi.screenshot.capture

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.Window
import com.landoulsi.screenshot.currentTimeMillis
import com.landoulsi.screenshot.config.CaptureConfig
import com.landoulsi.screenshot.model.ImageFormat
import com.landoulsi.screenshot.model.ScreenshotImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

/**
 * Android implementation of [ScreenshotCapturer] capturing screenshot from the current [Activity] or [Window].
 */
class AndroidScreenshotCapturer(
    private val activityProvider: () -> Activity?
) : ScreenshotCapturer {

    override fun isAvailable(): Boolean {
        return activityProvider() != null
    }

    override suspend fun capture(config: CaptureConfig): Result<ScreenshotImage> = withContext(Dispatchers.Main.immediate) {
        runCatching {
            val activity = activityProvider() ?: throw IllegalStateException("No active Activity available to capture screenshot")
            val window = activity.window ?: throw IllegalStateException("Activity window is not available")
            val rootView = window.decorView.rootView ?: throw IllegalStateException("Decor view root is null")

            val width = rootView.width
            val height = rootView.height

            if (width <= 0 || height <= 0) {
                throw IllegalStateException("View dimensions are invalid: ${width}x${height}")
            }

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                captureViaPixelCopy(window, rootView)
            } else {
                captureViaCanvas(rootView, width, height)
            }

            // Downscale or resize if configured
            val processedBitmap = processBitmap(bitmap, config)

            // Compress to byte array
            val bytes = compressBitmap(processedBitmap, config)
            val finalWidth = processedBitmap.width
            val finalHeight = processedBitmap.height

            if (processedBitmap != bitmap) {
                processedBitmap.recycle()
            }
            bitmap.recycle()

            ScreenshotImage(
                bytes = bytes,
                format = config.format,
                width = finalWidth,
                height = finalHeight,
                timestamp = currentTimeMillis()
            )
        }
    }

    private suspend fun captureViaPixelCopy(window: Window, view: View): Bitmap = suspendCancellableCoroutine { continuation ->
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val location = IntArray(2)
        view.getLocationInWindow(location)

        val rect = Rect(
            location[0],
            location[1],
            location[0] + view.width,
            location[1] + view.height
        )

        val handler = Handler(Looper.getMainLooper())

        try {
            PixelCopy.request(
                window,
                rect,
                bitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        continuation.resume(bitmap)
                    } else {
                        bitmap.recycle()
                        continuation.resumeWith(Result.failure(IllegalStateException("PixelCopy failed with error code: $copyResult")))
                    }
                },
                handler
            )
        } catch (e: Throwable) {
            bitmap.recycle()
            continuation.resumeWith(Result.failure(e))
        }
    }

    private fun captureViaCanvas(view: View, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun processBitmap(bitmap: Bitmap, config: CaptureConfig): Bitmap {
        var scaled = bitmap

        // 1. Scale factor
        if (config.scaleFactor > 0f && config.scaleFactor != 1.0f) {
            val targetW = (bitmap.width * config.scaleFactor).toInt().coerceAtLeast(1)
            val targetH = (bitmap.height * config.scaleFactor).toInt().coerceAtLeast(1)
            scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        }

        // 2. Max dimension constraint
        config.maxDimension?.let { maxDim ->
            val maxCurrent = maxOf(scaled.width, scaled.height)
            if (maxCurrent > maxDim) {
                val ratio = maxDim.toFloat() / maxCurrent.toFloat()
                val targetW = (scaled.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (scaled.height * ratio).toInt().coerceAtLeast(1)
                val resized = Bitmap.createScaledBitmap(scaled, targetW, targetH, true)
                if (scaled != bitmap && scaled != resized) {
                    scaled.recycle()
                }
                scaled = resized
            }
        }

        return scaled
    }

    private fun compressBitmap(bitmap: Bitmap, config: CaptureConfig): ByteArray {
        val stream = ByteArrayOutputStream()
        val compressFormat = when (config.format) {
            ImageFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageFormat.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
        }
        val quality = config.quality.coerceIn(1, 100)
        bitmap.compress(compressFormat, quality, stream)
        return stream.toByteArray()
    }
}
