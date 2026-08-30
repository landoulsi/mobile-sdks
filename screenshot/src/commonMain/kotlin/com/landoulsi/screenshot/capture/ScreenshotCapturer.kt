package com.landoulsi.screenshot.capture

import com.landoulsi.screenshot.config.CaptureConfig
import com.landoulsi.screenshot.model.ScreenshotImage

/**
 * Platform-agnostic contract for capturing the device screen or view hierarchy.
 */
interface ScreenshotCapturer {

    /**
     * Checks whether screen capture is available and permissions/contexts are valid.
     */
    fun isAvailable(): Boolean = true

    /**
     * Captures the current visible screen content and encodes it according to [config].
     *
     * @param config The capture settings (format, quality, downscaling, sensitivity masking).
     * @return [Result] containing [ScreenshotImage] or an error description.
     */
    suspend fun capture(config: CaptureConfig = CaptureConfig()): Result<ScreenshotImage>
}
