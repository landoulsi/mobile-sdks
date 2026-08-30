package com.landoulsi.analytics

import kotlin.test.Test
import kotlin.test.assertTrue

class LoggingEventTrackerTest {

    @Test
    fun track_event_without_identify_user_uses_anonymous() {
        val logs = mutableListOf<String>()
        val tracker = LoggingEventTracker(log = { logs.add(it) })

        tracker.trackEvent(Event(eventName = "test_event", timestamp = 0L))

        assertTrue(logs.any { it.contains("user=anonymous") })
        assertTrue(logs.any { it.contains("event=test_event") })
    }

    @Test
    fun set_user_property_logs_assignment() {
        val logs = mutableListOf<String>()
        val tracker = LoggingEventTracker(log = { logs.add(it) })

        tracker.setUserProperty("segment", "premium")

        assertTrue(logs.any { it.contains("setUserProperty") })
    }

    @Test
    fun multiple_events_produce_multiple_log_lines() {
        val logs = mutableListOf<String>()
        val tracker = LoggingEventTracker(log = { logs.add(it) })

        tracker.identifyUser("user-42")
        tracker.trackEvent(Event(eventName = "event_1", timestamp = 1000L))
        tracker.trackEvent(Event(eventName = "event_2", timestamp = 2000L))

        assertTrue(logs.size >= 2)
        assertTrue(logs.any { it.contains("user=user-42") })
        assertTrue(logs.any { it.contains("event=event_1") })
        assertTrue(logs.any { it.contains("event=event_2") })
    }
}