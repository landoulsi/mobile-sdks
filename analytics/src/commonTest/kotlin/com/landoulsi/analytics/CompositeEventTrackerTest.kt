package com.landoulsi.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompositeEventTrackerTest {

    @Test
    fun track_event_broadcasts_to_all_delegates() {
        val tracker1 = object : EventTracker {
            override fun trackEvent(event: Event) {}
            override fun identifyUser(userId: String) {}
            override fun setUserProperty(name: String, value: String?) {}
        }
        val tracker2 = object : EventTracker {
            override fun trackEvent(event: Event) {}
            override fun identifyUser(userId: String) {}
            override fun setUserProperty(name: String, value: String?) {}
        }

        val composite = CompositeEventTracker(listOf(tracker1, tracker2))
        composite.trackEvent(Event(eventName = "test"))

        // Both delegates receive the call; no crash.
    }

    @Test
    fun identify_user_broadcasts_to_all_delegates() {
        val called1 = mutableListOf<String>()
        val called2 = mutableListOf<String>()

        val tracker1 = object : EventTracker {
            override fun trackEvent(event: Event) {}
            override fun identifyUser(userId: String) { called1.add(userId) }
            override fun setUserProperty(name: String, value: String?) {}
        }
        val tracker2 = object : EventTracker {
            override fun trackEvent(event: Event) {}
            override fun identifyUser(userId: String) { called2.add(userId) }
            override fun setUserProperty(name: String, value: String?) {}
        }

        val composite = CompositeEventTracker(listOf(tracker1, tracker2))
        composite.identifyUser("user-42")

        assertEquals("user-42", called1.first())
        assertEquals("user-42", called2.first())
    }

    @Test
    fun set_user_property_broadcasts_to_all_delegates() {
        val called1 = mutableListOf<String>()
        val called2 = mutableListOf<String>()

        val tracker1 = object : EventTracker {
            override fun trackEvent(event: Event) {}
            override fun identifyUser(userId: String) {}
            override fun setUserProperty(name: String, value: String?) { called1.add("$name=$value") }
        }
        val tracker2 = object : EventTracker {
            override fun trackEvent(event: Event) {}
            override fun identifyUser(userId: String) {}
            override fun setUserProperty(name: String, value: String?) { called2.add("$name=$value") }
        }

        val composite = CompositeEventTracker(listOf(tracker1, tracker2))
        composite.setUserProperty("segment", "premium")

        assertTrue(called1.any { it.contains("segment=premium") })
        assertTrue(called2.any { it.contains("segment=premium") })
    }

    @Test
    fun exception_in_one_delegate_does_not_block_others() {
        val tracker1 = object : EventTracker {
            override fun trackEvent(event: Event) { throw RuntimeException("boom") }
            override fun identifyUser(userId: String) {}
            override fun setUserProperty(name: String, value: String?) {}
        }
        val tracker2 = object : EventTracker {
            override fun trackEvent(event: Event) {}
            override fun identifyUser(userId: String) {}
            override fun setUserProperty(name: String, value: String?) {}
        }

        val composite = CompositeEventTracker(listOf(tracker1, tracker2))
        composite.trackEvent(Event(eventName = "test")) // should not throw

        // No exception — fire-and-forget swallows the failure.
    }

    @Test
    fun empty_delegate_list_no_op() {
        val composite = CompositeEventTracker(emptyList())

        composite.trackEvent(Event(eventName = "test"))
        composite.identifyUser("user-1")
        composite.setUserProperty("key", "value")

        // No crash with empty delegates.
    }
}