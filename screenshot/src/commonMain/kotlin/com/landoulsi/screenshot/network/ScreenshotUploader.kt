package com.landoulsi.screenshot.network

import com.landoulsi.screenshot.config.ServerConfig
import com.landoulsi.screenshot.model.ScreenshotPayload
import com.landoulsi.screenshot.model.UploadResponse

/**
 * Contract for uploading screenshot payloads to a remote server.
 */
interface ScreenshotUploader {

    /**
     * Uploads the given [payload] to the remote destination specified in [serverConfig].
     *
     * @param payload The screenshot image data and metadata bundle.
     * @param serverConfig Configuration for endpoint URL, headers, timeouts, and retry behavior.
     * @return [Result] containing [UploadResponse] or failure error.
     */
    suspend fun upload(
        payload: ScreenshotPayload,
        serverConfig: ServerConfig
    ): Result<UploadResponse>
}
