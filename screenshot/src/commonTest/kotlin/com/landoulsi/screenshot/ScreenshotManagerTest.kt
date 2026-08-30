package com.landoulsi.screenshot

import com.landoulsi.screenshot.capture.ScreenshotCapturer
import com.landoulsi.screenshot.config.CaptureConfig
import com.landoulsi.screenshot.config.EventTriggerConfig
import com.landoulsi.screenshot.config.PushTriggerConfig
import com.landoulsi.screenshot.config.ScreenshotConfig
import com.landoulsi.screenshot.config.ServerConfig
import com.landoulsi.screenshot.model.ImageFormat
import com.landoulsi.screenshot.model.ScreenshotImage
import com.landoulsi.screenshot.model.ScreenshotTriggerType
import com.landoulsi.screenshot.network.KtorScreenshotUploader
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScreenshotManagerTest {

    private class FakeScreenshotCapturer(
        var available: Boolean = true,
        var captureResult: Result<ScreenshotImage> = Result.success(
            ScreenshotImage(
                bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47),
                format = ImageFormat.PNG,
                width = 1080,
                height = 1920,
                timestamp = 1700000000000L
            )
        )
    ) : ScreenshotCapturer {
        var captureCallCount = 0
        override fun isAvailable(): Boolean = available
        override suspend fun capture(config: CaptureConfig): Result<ScreenshotImage> {
            captureCallCount++
            return captureResult
        }
    }

    @Test
    fun testCaptureAndUploadSuccess() = runTest {
        var capturedRequestUrl = ""
        var capturedAuthHeader: String? = null

        val mockEngine = MockEngine { request ->
            capturedRequestUrl = request.url.toString()
            capturedAuthHeader = request.headers[HttpHeaders.Authorization]

            respond(
                content = """{"status":"uploaded","id":"img_123"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val config = ScreenshotConfig(
            server = ServerConfig(
                endpointUrl = "https://backend.test/upload",
                authToken = "secret-token-xyz"
            )
        )

        val capturer = FakeScreenshotCapturer()
        val uploader = KtorScreenshotUploader(HttpClient(mockEngine))
        val manager = ScreenshotManager(
            config = config,
            capturer = capturer,
            uploader = uploader,
            coroutineScope = this
        )

        val result = manager.captureAndUpload(
            triggerType = ScreenshotTriggerType.MANUAL,
            customParams = mapOf("screen" to "HomeScreen")
        )

        assertTrue(result.isSuccess)
        val uploadResponse = result.getOrNull()
        assertEquals(200, uploadResponse?.statusCode)
        assertTrue(uploadResponse?.isSuccess == true)
        assertEquals("https://backend.test/upload", capturedRequestUrl)
        assertEquals("Bearer secret-token-xyz", capturedAuthHeader)
        assertEquals(1, capturer.captureCallCount)
    }

    @Test
    fun testPushNotificationTriggerHandling() = runTest {
        val capturer = FakeScreenshotCapturer()
        var uploadCalls = 0

        val mockEngine = MockEngine {
            uploadCalls++
            respond(content = "OK", status = HttpStatusCode.OK)
        }

        val config = ScreenshotConfig(
            server = ServerConfig(endpointUrl = "https://backend.test/upload"),
            triggers = com.landoulsi.screenshot.config.TriggerConfig(
                push = PushTriggerConfig(
                    isEnabled = true,
                    payloadActionKey = "action",
                    triggerActionValue = "CAPTURE_SCREENSHOT"
                )
            )
        )

        val manager = ScreenshotManager(
            config = config,
            capturer = capturer,
            uploader = KtorScreenshotUploader(HttpClient(mockEngine)),
            coroutineScope = this
        )

        // Non-matching push payload
        val nonMatching = manager.handlePushNotification(mapOf("action" to "SHOW_MESSAGE"))
        assertFalse(nonMatching)

        // Matching push payload
        val matching = manager.handlePushNotification(mapOf("action" to "CAPTURE_SCREENSHOT", "campaign" to "test_campaign"))
        assertTrue(matching)
    }

    @Test
    fun testAppEventTriggerHandling() = runTest {
        val capturer = FakeScreenshotCapturer()
        val mockEngine = MockEngine {
            respond(content = "OK", status = HttpStatusCode.OK)
        }

        val config = ScreenshotConfig(
            server = ServerConfig(endpointUrl = "https://backend.test/upload"),
            triggers = com.landoulsi.screenshot.config.TriggerConfig(
                events = EventTriggerConfig(
                    isEnabled = true,
                    triggerEvents = setOf("CRASH_DETECTED", "BUG_REPORT_CLICKED")
                )
            )
        )

        val manager = ScreenshotManager(
            config = config,
            capturer = capturer,
            uploader = KtorScreenshotUploader(HttpClient(mockEngine)),
            coroutineScope = this
        )

        assertFalse(manager.onAppEvent("UNRELATED_EVENT"))
        assertTrue(manager.onAppEvent("BUG_REPORT_CLICKED", mapOf("user_id" to "123")))
    }

    @Test
    fun testSdkDisabledReturnsFailure() = runTest {
        val capturer = FakeScreenshotCapturer()
        val config = ScreenshotConfig(
            server = ServerConfig(endpointUrl = "https://backend.test/upload"),
            isEnabled = false
        )

        val manager = ScreenshotManager(
            config = config,
            capturer = capturer,
            uploader = KtorScreenshotUploader(HttpClient(MockEngine { respond("OK") })),
            coroutineScope = this
        )

        val result = manager.captureAndUpload()
        assertTrue(result.isFailure)
        assertEquals("Screenshot SDK is currently disabled in configuration", result.exceptionOrNull()?.message)
        assertEquals(0, capturer.captureCallCount)
    }

    @Test
    fun testCapturerUnavailableReturnsFailure() = runTest {
        val capturer = FakeScreenshotCapturer(available = false)
        val config = ScreenshotConfig(
            server = ServerConfig(endpointUrl = "https://backend.test/upload")
        )

        val manager = ScreenshotManager(
            config = config,
            capturer = capturer,
            uploader = KtorScreenshotUploader(HttpClient(MockEngine { respond("OK") })),
            coroutineScope = this
        )

        val result = manager.captureAndUpload()
        assertTrue(result.isFailure)
        assertEquals("ScreenshotCapturer is not available in the current context", result.exceptionOrNull()?.message)
        assertEquals(0, capturer.captureCallCount)
    }
}
