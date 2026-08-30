package com.landoulsi.analytics

/**
 * A trivial [EventTracker] implementation that logs events to standard output.
 *
 * Useful for development, demos, and testing without any backend dependency.
 * Real backends (Firebase, Ktor, etc.) should be implemented in separate modules.
 *
 * @property log Function that receives formatted event strings. Defaults to `println`.
 */
class LoggingEventTracker(
    private val log: (String) -> Unit = ::println,
) : EventTracker {

    private var userId: String? = null

    override fun trackEvent(event: Event) {
        val id = userId ?: "anonymous"
        val redactedProps = event.properties.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            val formattedValue = if (SENSITIVE_KEYS.any { pattern -> key.startsWith(pattern, ignoreCase = true) }) {
                "[REDACTED]"
            } else {
                when (value) {
                    is AnalyticsValue.String -> value.value
                    is AnalyticsValue.Long -> value.value.toString()
                    is AnalyticsValue.Double -> value.value.toString()
                    is AnalyticsValue.Boolean -> value.value.toString()
                }
            }
            "$key=$formattedValue"
        }
        log("[Analytics] user=$id event=${event.eventName} timestamp=${event.timestamp} properties=$redactedProps")
    }

    override fun identifyUser(userId: String) {
        this.userId = userId
        log("[Analytics] identifyUser: $userId")
    }

    companion object {
        private val SENSITIVE_KEYS = listOf(
            "email",
            "card_",
            "token",
            "password",
            "secret",
            "phone",
            "address",
            "ssn",
            "cvv",
            "pan",
        )
    }
}
