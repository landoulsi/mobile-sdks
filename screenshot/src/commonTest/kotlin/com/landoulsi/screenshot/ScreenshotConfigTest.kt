package com.landoulsi.screenshot

import com.landoulsi.screenshot.config.CaptureConfig
import com.landoulsi.screenshot.config.EventTriggerConfig
import com.landoulsi.screenshot.config.PushTriggerConfig
import com.landoulsi.screenshot.config.ScreenshotConfig
import com.landoulsi.screenshot.config.ServerConfig
import com.landoulsi.screenshot.model.ImageFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScreenshotConfigTest {

    @Test
    fun testParseFullJsonConfig() {
        val json = """
        {
            "server": {
                "endpointUrl": "https://api.example.com/v1/screenshots",
                "method": "POST",
                "headers": {
                    "X-Api-Key": "test-key-123"
                },
                "authToken": "secret_token",
                "timeoutMillis": 15000,
                "fileFieldName": "image_file",
                "metadataFieldName": "meta_info",
                "additionalFields": {
                    "app_id": "com.example.app"
                }
            },
            "capture": {
                "format": "PNG",
                "quality": 90,
                "maxDimension": 1920,
                "scaleFactor": 0.75,
                "maskSensitiveViews": true,
                "captureDelayMillis": 200
            },
            "triggers": {
                "push": {
                    "isEnabled": true,
                    "payloadActionKey": "cmd",
                    "triggerActionValue": "TAKE_SCREENSHOT",
                    "payloadMetadataKey": "custom_meta"
                },
                "manual": {
                    "isEnabled": true
                },
                "events": {
                    "isEnabled": true,
                    "triggerEvents": ["USER_FEEDBACK", "CRASH_REPORTED"]
                }
            },
            "metadata": {
                "includeDeviceInfo": true,
                "includeAppVersion": true,
                "includeTimestamp": true,
                "defaultCustomParameters": {
                    "environment": "staging"
                }
            },
            "isEnabled": true
        }
        """.trimIndent()

        val config = ScreenshotConfig.fromJson(json)

        // Verify Server config
        assertEquals("https://api.example.com/v1/screenshots", config.server.endpointUrl)
        assertEquals("POST", config.server.method)
        assertEquals("test-key-123", config.server.headers["X-Api-Key"])
        assertEquals("secret_token", config.server.authToken)
        assertEquals(15000L, config.server.timeoutMillis)
        assertEquals("image_file", config.server.fileFieldName)
        assertEquals("meta_info", config.server.metadataFieldName)
        assertEquals("com.example.app", config.server.additionalFields["app_id"])

        // Verify Capture config
        assertEquals(ImageFormat.PNG, config.capture.format)
        assertEquals(90, config.capture.quality)
        assertEquals(1920, config.capture.maxDimension)
        assertEquals(0.75f, config.capture.scaleFactor)
        assertTrue(config.capture.maskSensitiveViews)
        assertEquals(200L, config.capture.captureDelayMillis)

        // Verify Triggers config
        assertTrue(config.triggers.push.isEnabled)
        assertEquals("cmd", config.triggers.push.payloadActionKey)
        assertEquals("TAKE_SCREENSHOT", config.triggers.push.triggerActionValue)
        assertEquals("custom_meta", config.triggers.push.payloadMetadataKey)
        assertTrue(config.triggers.manual.isEnabled)
        assertTrue(config.triggers.events.isEnabled)
        assertTrue(config.triggers.events.triggerEvents.contains("USER_FEEDBACK"))

        // Verify Metadata config
        assertTrue(config.metadata.includeDeviceInfo)
        assertEquals("staging", config.metadata.defaultCustomParameters["environment"])
        assertTrue(config.isEnabled)
    }

    @Test
    fun testParseMinimalJsonConfigWithDefaults() {
        val json = """
        {
            "server": {
                "endpointUrl": "https://api.example.com/upload"
            }
        }
        """.trimIndent()

        val config = ScreenshotConfig.fromJson(json)

        assertEquals("https://api.example.com/upload", config.server.endpointUrl)
        assertEquals("POST", config.server.method)
        assertEquals("screenshot", config.server.fileFieldName)
        assertEquals("metadata", config.server.metadataFieldName)
        assertEquals(ImageFormat.JPEG, config.capture.format)
        assertEquals(80, config.capture.quality)
        assertTrue(config.triggers.push.isEnabled)
        assertEquals("action", config.triggers.push.payloadActionKey)
        assertEquals("CAPTURE_SCREENSHOT", config.triggers.push.triggerActionValue)
        assertTrue(config.isEnabled)
    }

    @Test
    fun testSerializationRoundTrip() {
        val original = ScreenshotConfig(
            server = ServerConfig(
                endpointUrl = "https://example.com/api",
                authToken = "bearer_abc",
                headers = mapOf("X-Custom" to "Value")
            ),
            capture = CaptureConfig(
                format = ImageFormat.WEBP,
                quality = 70
            ),
            triggers = com.landoulsi.screenshot.config.TriggerConfig(
                push = PushTriggerConfig(
                    isEnabled = true,
                    payloadActionKey = "action_signal",
                    triggerActionValue = "SNAP"
                ),
                events = EventTriggerConfig(
                    isEnabled = true,
                    triggerEvents = setOf("ERROR_OCCURRED")
                )
            )
        )

        val json = ScreenshotConfig.toJson(original)
        val deserialized = ScreenshotConfig.fromJson(json)

        assertEquals(original.server.endpointUrl, deserialized.server.endpointUrl)
        assertEquals(original.server.authToken, deserialized.server.authToken)
        assertEquals(original.server.headers, deserialized.server.headers)
        assertEquals(original.capture.format, deserialized.capture.format)
        assertEquals(original.capture.quality, deserialized.capture.quality)
        assertEquals(original.triggers.push.payloadActionKey, deserialized.triggers.push.payloadActionKey)
        assertEquals(original.triggers.push.triggerActionValue, deserialized.triggers.push.triggerActionValue)
        assertEquals(original.triggers.events.triggerEvents, deserialized.triggers.events.triggerEvents)
    }
}
