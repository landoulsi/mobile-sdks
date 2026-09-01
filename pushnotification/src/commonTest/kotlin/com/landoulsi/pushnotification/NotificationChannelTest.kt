package com.landoulsi.pushnotification

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NotificationChannelTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testSerializationRoundTrip() {
        val channel = NotificationChannel(
            id = "test-channel",
            name = "Test Channel",
            description = "A test notification channel",
            importance = NotificationChannel.Importance.HIGH,
            sound = "notification_sound",
            vibrationPattern = listOf(0L, 250L, 250L, 250L),
            lockScreenVisibility = NotificationChannel.LockScreenVisibility.PUBLIC,
        )

        val serialized = json.encodeToString(NotificationChannel.serializer(), channel)
        val deserialized = json.decodeFromString(NotificationChannel.serializer(), serialized)

        assertEquals(channel, deserialized)
    }

    @Test
    fun testImportanceWireFormat() {
        val channel = NotificationChannel(
            id = "test",
            name = "Test",
            importance = NotificationChannel.Importance.HIGH,
        )

        val serialized = json.encodeToString(NotificationChannel.serializer(), channel)
        assertTrue(serialized.contains("\"importance\":\"high\""), "Expected lowercase 'high', got: $serialized")
    }

    @Test
    fun testLockScreenVisibilityWireFormat() {
        val channel = NotificationChannel(
            id = "test",
            name = "Test",
            lockScreenVisibility = NotificationChannel.LockScreenVisibility.PUBLIC,
        )

        val serialized = json.encodeToString(NotificationChannel.serializer(), channel)
        assertTrue(serialized.contains("\"lockScreenVisibility\":\"public\""), "Expected 'public', got: $serialized")
    }

    @Test
    fun testDefaultValues() {
        val channel = NotificationChannel(
            id = "minimal",
            name = "Minimal",
        )

        assertEquals("minimal", channel.id)
        assertEquals("Minimal", channel.name)
        assertEquals("", channel.description)
        assertEquals(NotificationChannel.Importance.DEFAULT, channel.importance)
        assertNull(channel.sound)
        assertTrue(channel.vibrationPattern.isEmpty())
        assertEquals(NotificationChannel.LockScreenVisibility.PRIVATE, channel.lockScreenVisibility)
    }
}
