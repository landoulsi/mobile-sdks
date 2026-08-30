package com.landoulsi.analytics

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock

/**
 * A bounded value type for analytics event properties.
 *
 * Restricting to primitive types ensures serializability, reliable equality/hashing,
 * and makes it impossible to accidentally embed PII or secrets through complex objects.
 */
@Serializable
sealed interface AnalyticsValue {

    @Serializable
    @SerialName("string")
    data class String(val value: kotlin.String) : AnalyticsValue

    @Serializable
    @SerialName("long")
    data class Long(val value: kotlin.Long) : AnalyticsValue

    @Serializable
    @SerialName("double")
    data class Double(val value: kotlin.Double) : AnalyticsValue

    @Serializable
    @SerialName("boolean")
    data class Boolean(val value: kotlin.Boolean) : AnalyticsValue
}

/**
 * A standardized analytics event.
 *
 * Event names MUST be snake_case (validated at construction).
 * Properties use [AnalyticsValue] to guarantee serializability and prevent PII/secrets.
 *
 * The stable user identifier is **not** on this class — it is owned by the
 * [EventTracker] via [EventTracker.identifyUser] and attached at delivery time.
 *
 * @property eventName The unique, snake_case event identifier (e.g. `checkout_started`, `card_validated`).
 * @property timestamp Epoch milliseconds when the event occurred.
 * @property properties Flexible key-value pairs for contextual data. Keys should be snake_case.
 *   Values must be [AnalyticsValue] — no arbitrary objects.
 */
@Serializable
data class Event(
    val eventName: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val properties: Map<String, AnalyticsValue> = emptyMap(),
) {
    init {
        require(eventName.matches(SNAKE_CASE_REGEX)) {
            "Event name must be snake_case: '$eventName'. Use lowercase letters, digits, and underscores."
        }
        require(eventName.isNotEmpty()) { "Event name must not be empty." }
    }

    companion object {
        private val SNAKE_CASE_REGEX = "^[a-z][a-z0-9_]*$".toRegex()
    }
}
