package com.landoulsi.analytics

/**
 * Contract for tracking analytics events across the application.
 *
 * Implementations own the stable user identifier (set via [identifyUser]) and attach it
 * to every event at delivery time. Call sites never supply a user id per event — this
 * prevents accidental PII leakage and ensures a consistent identifier across sessions.
 *
 * PII contract:
 * - [EventTracker.identifyUser] accepts a stable, opaque identifier (e.g. UUID, opaque user id).
 *   Callers MUST NOT pass email addresses, phone numbers, or other PII as [userId].
 * - Implementations MUST NOT log or transmit PII embedded in [Event.properties].
 *   Sensitive keys (e.g. `email`, `card_*`, `token`) should be redacted before storage.
 *
 * Threading: [trackEvent] is fire-and-forget and non-suspend — implementations should
 * handle queuing/batching internally if needed.
 */
interface EventTracker {

    /**
     * Track an analytics event.
     *
     * Implementations should attach the identified user id and dispatch asynchronously.
     * Failures MUST be swallowed — callers rely on fire-and-forget semantics.
     */
    fun trackEvent(event: Event)

    /**
     * Set the stable user identifier for subsequent events.
     *
     * @param userId An opaque, stable identifier. MUST NOT be PII (email, phone, etc.).
     */
    fun identifyUser(userId: String)

    /**
     * Set a user property for subsequent events.
     *
     * User properties are key-value pairs that describe segments of your user base.
     * Implementations should silently drop invalid keys/values.
     *
     * @param name The property name. Must be a non-empty snake_case string.
     * @param value The property value, or null to clear the property.
     */
    fun setUserProperty(name: String, value: String?) {
        // Default no-op so existing implementations don't break.
    }
}
