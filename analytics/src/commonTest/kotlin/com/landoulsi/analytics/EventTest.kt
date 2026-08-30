package com.landoulsi.analytics

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EventTest {

    @Test
    fun validSnakeCaseNames() {
        Event(eventName = "checkout_started")
        Event(eventName = "card_validated")
        Event(eventName = "screen_view")
        Event(eventName = "app")
        Event(eventName = "a1")
    }

    @Test
    fun rejectsEmptyName() {
        assertFailsWith<IllegalArgumentException> { Event(eventName = "") }
    }

    @Test
    fun rejectsUpperCase() {
        assertFailsWith<IllegalArgumentException> { Event(eventName = "CheckoutStarted") }
    }

    @Test
    fun rejectsSpaces() {
        assertFailsWith<IllegalArgumentException> { Event(eventName = "checkout started") }
    }

    @Test
    fun rejectsHyphens() {
        assertFailsWith<IllegalArgumentException> { Event(eventName = "checkout-started") }
    }

    @Test
    fun rejectsLeadingDigit() {
        assertFailsWith<IllegalArgumentException> { Event(eventName = "1_checkout") }
    }

    @Test
    fun rejectsLeadingUnderscore() {
        assertFailsWith<IllegalArgumentException> { Event(eventName = "_checkout") }
    }

    @Test
    fun serializationRoundTrip() {
        val event = Event(
            eventName = "payment_completed",
            timestamp = 1700000000000L,
            properties = mapOf(
                "amount" to AnalyticsValue.Long(9900),
                "currency" to AnalyticsValue.String("USD"),
                "success" to AnalyticsValue.Boolean(true),
                "fee_pct" to AnalyticsValue.Double(2.9),
            ),
        )

        val json = Json.encodeToString(event)
        val decoded = Json.decodeFromString<Event>(json)

        assertEquals(event.eventName, decoded.eventName)
        assertEquals(event.timestamp, decoded.timestamp)
        assertEquals(event.properties, decoded.properties)
    }

    @Test
    fun serializationHandlesEmptyProperties() {
        val event = Event(eventName = "app_foregrounded", timestamp = 0L)
        val json = Json.encodeToString(event)
        val decoded = Json.decodeFromString<Event>(json)

        assertEquals("app_foregrounded", decoded.eventName)
        assertEquals(0L, decoded.timestamp)
        assertTrue(decoded.properties.isEmpty())
    }

    @Test
    fun loggingTrackerAttachesUserId() {
        val logs = mutableListOf<String>()
        val tracker = LoggingEventTracker(log = { logs.add(it) })

        tracker.identifyUser("user-123")
        tracker.trackEvent(Event(eventName = "test_event", timestamp = 0L))

        assertTrue(logs.any { it.contains("user=user-123") })
        assertTrue(logs.any { it.contains("event=test_event") })
    }

    @Test
    fun loggingTrackerRedactsSensitiveKeys() {
        val logs = mutableListOf<String>()
        val tracker = LoggingEventTracker(log = { logs.add(it) })

        tracker.trackEvent(
            Event(
                eventName = "card_added",
                timestamp = 0L,
                properties = mapOf(
                    "card_last4" to AnalyticsValue.String("4242"),
                    "email" to AnalyticsValue.String("user@example.com"),
                    "safe_key" to AnalyticsValue.String("visible"),
                ),
            )
        )

        val output = logs.first()
        assertTrue(output.contains("card_last4=[REDACTED]"))
        assertTrue(output.contains("email=[REDACTED]"))
        assertTrue(output.contains("safe_key=visible"))
    }
}
