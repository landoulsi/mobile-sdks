package com.landoulsi.analytics

import cocoapods.FirebaseAnalytics.FIRAnalytics
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class FirebaseEventTracker : EventTracker {

    private var userId: String? = null

    override fun trackEvent(event: Event) {
        try {
            val params = buildParams(EventMapper.toFlatMap(event))
            FIRAnalytics.logEventWithName(event.eventName, parameters = params)
        } catch (_: Exception) {
        }
    }

    override fun identifyUser(userId: String) {
        try {
            this.userId = userId
            FIRAnalytics.setUserID(userId)
        } catch (_: Exception) {
        }
    }

    override fun setUserProperty(name: String, value: String?) {
        try {
            FIRAnalytics.setUserPropertyString(value, forName = name)
        } catch (_: Exception) {
        }
    }

    private fun buildParams(flatMap: Map<String, Any>): Map<Any?, *> {
        @Suppress("UNCHECKED_CAST")
        return flatMap as Map<Any?, *>
    }
}
