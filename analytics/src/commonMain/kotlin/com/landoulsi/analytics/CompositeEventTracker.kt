package com.landoulsi.analytics

/**
 * A composite [EventTracker] that broadcasts every event to every registered backend.
 *
 * Implements the Composite pattern (SOLID: Open/Closed) — new backends can be added
 * without modifying call sites. Each delegate call is wrapped in try/catch so a single
 * failing backend does not prevent other backends from receiving the event (fire-and-forget).
 *
 * Thread-safe by construction: the delegate list is immutable after construction.
 */
class CompositeEventTracker(
    private val trackers: List<EventTracker>,
) : EventTracker {

    override fun trackEvent(event: Event) {
        for (tracker in trackers) {
            try {
                tracker.trackEvent(event)
            } catch (_: Exception) {
                // Swallowed per the EventTracker contract — fire-and-forget.
            }
        }
    }

    override fun identifyUser(userId: String) {
        for (tracker in trackers) {
            try {
                tracker.identifyUser(userId)
            } catch (_: Exception) {
                // Swallowed per the EventTracker contract.
            }
        }
    }

    override fun setUserProperty(name: String, value: String?) {
        for (tracker in trackers) {
            try {
                tracker.setUserProperty(name, value)
            } catch (_: Exception) {
                // Swallowed per the EventTracker contract.
            }
        }
    }
}