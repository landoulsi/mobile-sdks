package com.landoulsi.analytics

/**
 * Pure, stateless mapper that converts an [Event] into a flat, platform-agnostic
 * parameter map suitable for delivery to any analytics backend.
 *
 * This keeps vendor-agnostic logic (type flattening, sensitive-key redaction) in
 * [commonMain] so platform adapters don't duplicate mapping code. It is fully
 * unit-testable without any native SDK dependency.
 */
object EventMapper {

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

    /**
     * Convert [event] properties to a flat `Map<String, Any>` with sensitive keys redacted.
     *
     * - [AnalyticsValue.String] → `String`
     * - [AnalyticsValue.Long] → `Long`
     * - [AnalyticsValue.Double] → `Double`
     * - [AnalyticsValue.Boolean] → `Boolean`
     * - Sensitive keys (matching any prefix in [SENSITIVE_KEYS]) → `"[REDACTED]"`
     *
     * @return Unmodifiable map of event name to flattened parameters.
     */
    fun toFlatMap(event: Event): Map<String, Any> {
        return event.properties.mapValues { (key, value) ->
            if (isSensitiveKey(key)) {
                REDACTED_SENTINEL
            } else {
                when (value) {
                    is AnalyticsValue.String -> value.value
                    is AnalyticsValue.Long -> value.value
                    is AnalyticsValue.Double -> value.value
                    is AnalyticsValue.Boolean -> value.value
                }
            }
        }
    }

    private fun isSensitiveKey(key: String): Boolean {
        return SENSITIVE_KEYS.any { pattern -> key.startsWith(pattern, ignoreCase = true) }
    }

    internal const val REDACTED_SENTINEL = "[REDACTED]"
}
