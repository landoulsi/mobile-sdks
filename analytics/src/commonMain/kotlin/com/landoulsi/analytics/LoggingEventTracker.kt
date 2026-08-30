package com.landoulsi.analytics

/**
 * A trivial [EventTracker] implementation that logs events to standard output.
 *
 * Useful for development, demos, and testing without any backend dependency.
 * Real backends (Firebase, Ktor, etc.) should be implemented in separate modules.
 *
 * Delegates sensitive-key redaction to [EventMapper] so all trackers share
 * the same redaction logic.
 *
 * @property log Function that receives formatted event strings. Defaults to `println`.
 */
class LoggingEventTracker(
    private val log: (String) -> Unit = ::println,
) : EventTracker {

    private var userId: String? = null

    override fun trackEvent(event: Event) {
        val id = userId ?: "anonymous"
        val flatMap = EventMapper.toFlatMap(event)
        val redactedProps = flatMap.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
            "$key=$value"
        }
        log("[Analytics] user=$id event=${event.eventName} timestamp=${event.timestamp} properties=$redactedProps")
    }

    override fun identifyUser(userId: String) {
        this.userId = userId
        log("[Analytics] identifyUser: $userId")
    }

    override fun setUserProperty(name: String, value: String?) {
        log("[Analytics] setUserProperty: $name=$value")
    }
}
