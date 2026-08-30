package com.landoulsi.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Android [EventTracker] implementation backed by Firebase Analytics.
 *
 * Translates the vendor-agnostic [Event] model into Firebase's `Bundle`-based
 * parameter API. Sensitive keys are redacted via [EventMapper] before delivery.
 * Failures are swallowed per the [EventTracker] contract (fire-and-forget).
 *
 * @property analytics The Firebase Analytics instance. Obtain via
 *   `FirebaseAnalytics.getInstance(context)` in the consuming app and inject here.
 */
class FirebaseEventTracker(
    private val analytics: FirebaseAnalytics,
) : EventTracker {

    private var userId: String? = null

    override fun trackEvent(event: Event) {
        try {
            val params = buildBundle(EventMapper.toFlatMap(event))
            analytics.logEvent(event.eventName, params)
        } catch (_: Exception) {
            // Swallowed per the EventTracker contract.
        }
    }

    override fun identifyUser(userId: String) {
        try {
            this.userId = userId
            analytics.setUserId(userId)
        } catch (_: Exception) {
            // Swallowed per the EventTracker contract.
        }
    }

    override fun setUserProperty(name: String, value: String?) {
        try {
            analytics.setUserProperty(name, value)
        } catch (_: Exception) {
            // Swallowed per the EventTracker contract.
        }
    }

    private fun buildBundle(flatMap: Map<String, Any>): Bundle {
        val bundle = Bundle()
        for ((key, value) in flatMap) {
            when (value) {
                is String -> bundle.putString(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        return bundle
    }
}
