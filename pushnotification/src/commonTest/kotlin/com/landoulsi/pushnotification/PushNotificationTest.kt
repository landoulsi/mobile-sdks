package com.landoulsi.pushnotification

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PushNotificationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testSerializationRoundTrip() {
        val notification = PushNotification(
            id = "notif-123",
            title = "Test Title",
            body = "Test body content",
            payload = mapOf("action" to "open", "screen" to "home"),
            deepLink = "app://home",
            priority = PushNotification.Priority.HIGH,
            sentAt = 1000000L,
            createdAt = 2000000L,
        )

        val serialized = json.encodeToString(PushNotification.serializer(), notification)
        val deserialized = json.decodeFromString(PushNotification.serializer(), serialized)

        assertEquals(notification, deserialized)
    }

    @Test
    fun testPriorityWireFormat() {
        val notification = PushNotification(
            id = "test",
            title = "Title",
            body = "Body",
            priority = PushNotification.Priority.HIGH,
        )

        val serialized = json.encodeToString(PushNotification.serializer(), notification)
        assertTrue(serialized.contains("\"priority\":\"high\""), "Expected lowercase 'high' in JSON, got: $serialized")
    }

    @Test
    fun testDefaultValues() {
        val notification = PushNotification(
            id = "minimal",
            title = "Title",
            body = "Body",
        )

        assertEquals("minimal", notification.id)
        assertEquals("Title", notification.title)
        assertEquals("Body", notification.body)
        assertTrue(notification.payload.isEmpty())
        assertNull(notification.deepLink)
        assertEquals(PushNotification.Priority.DEFAULT, notification.priority)
        assertNull(notification.sentAt)
        assertNull(notification.createdAt)
    }

    @Test
    fun testToMapAndFromMapRoundTrip() {
        val notification = PushNotification(
            id = "test-id",
            title = "Test",
            body = "Body",
            payload = mapOf("key" to "value"),
            deepLink = "app://test",
            priority = PushNotification.Priority.HIGH,
            sentAt = 1000L,
            createdAt = 2000L,
        )

        val map = notification.toMap()
        val reconstructed = PushNotification.fromMap(map)

        assertEquals(notification, reconstructed)
    }

    @Test
    fun testToMapNamespacesPayloadKeys() {
        val notification = PushNotification(
            id = "test-id",
            title = "Test",
            body = "Body",
            payload = mapOf("id" to "payload-id", "title" to "payload-title"),
        )

        val map = notification.toMap()

        // Reserved fields must not be overwritten by payload keys
        assertEquals("test-id", map["id"])
        assertEquals("Test", map["title"])
        // Payload keys are namespaced
        assertEquals("payload-id", map["data.id"])
        assertEquals("payload-title", map["data.title"])
    }

    @Test
    fun testFromMapPreservesPayloadWithReservedNames() {
        val map = mapOf(
            "id" to "real-id",
            "title" to "Real Title",
            "body" to "Real Body",
            "data.id" to "payload-id",
            "data.title" to "payload-title",
        )

        val notification = PushNotification.fromMap(map)

        assertEquals("real-id", notification.id)
        assertEquals("Real Title", notification.title)
        assertEquals(mapOf("id" to "payload-id", "title" to "payload-title"), notification.payload)
    }

    @Test
    fun testFromMapWithMissingOptionalFields() {
        val map = mapOf(
            "id" to "test",
            "title" to "Title",
            "body" to "Body",
        )

        val notification = PushNotification.fromMap(map)

        assertEquals("test", notification.id)
        assertEquals("Title", notification.title)
        assertEquals("Body", notification.body)
        assertNull(notification.deepLink)
        assertEquals(PushNotification.Priority.DEFAULT, notification.priority)
        assertNull(notification.sentAt)
        assertNull(notification.createdAt)
    }

    @Test
    fun testFromMapUnknownPriorityFallsBackToDefault() {
        val map = mapOf(
            "id" to "test",
            "title" to "Title",
            "body" to "Body",
            "priority" to "urgent",
        )

        val notification = PushNotification.fromMap(map)

        assertEquals(PushNotification.Priority.DEFAULT, notification.priority)
    }

    @Test
    fun testFromMapThrowsOnMissingId() {
        val map = mapOf("title" to "Title", "body" to "Body")

        try {
            PushNotification.fromMap(map)
            assertTrue(false, "Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Missing 'id'") == true)
        }
    }

    @Test
    fun testFromMapThrowsOnMissingTitle() {
        val map = mapOf("id" to "test", "body" to "Body")

        try {
            PushNotification.fromMap(map)
            assertTrue(false, "Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Missing 'title'") == true)
        }
    }

    @Test
    fun testFromMapThrowsOnMissingBody() {
        val map = mapOf("id" to "test", "title" to "Title")

        try {
            PushNotification.fromMap(map)
            assertTrue(false, "Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Missing 'body'") == true)
        }
    }
}
