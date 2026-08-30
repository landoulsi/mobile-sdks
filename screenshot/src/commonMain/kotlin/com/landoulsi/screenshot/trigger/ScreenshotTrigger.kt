package com.landoulsi.screenshot.trigger

import com.landoulsi.screenshot.config.EventTriggerConfig
import com.landoulsi.screenshot.config.PushTriggerConfig
import com.landoulsi.screenshot.model.ScreenshotTriggerType

/**
 * Encapsulates the signal details that prompted a capture.
 */
data class TriggerSignal(
    val type: ScreenshotTriggerType,
    val customParameters: Map<String, String> = emptyMap()
)

/**
 * Base interface for all screenshot trigger listeners and handlers.
 */
interface ScreenshotTrigger {
    val triggerType: ScreenshotTriggerType
    val isEnabled: Boolean

    fun startListening(onTrigger: (TriggerSignal) -> Unit)
    fun stopListening()
}

/**
 * Trigger handler for evaluating remote push notification payloads.
 */
class PushTriggerHandler(
    private val config: PushTriggerConfig
) : ScreenshotTrigger {

    override val triggerType: ScreenshotTriggerType = ScreenshotTriggerType.PUSH_NOTIFICATION
    override val isEnabled: Boolean get() = config.isEnabled

    private var listener: ((TriggerSignal) -> Unit)? = null

    override fun startListening(onTrigger: (TriggerSignal) -> Unit) {
        this.listener = onTrigger
    }

    override fun stopListening() {
        this.listener = null
    }

    /**
     * Checks if the push payload contains the expected action key and value to trigger a capture.
     *
     * @param payload Key-value map from the push notification data payload.
     * @return True if the push payload matched the trigger condition and dispatched the signal.
     */
    fun handlePushPayload(payload: Map<String, String>): Boolean {
        if (!config.isEnabled) return false

        val action = payload[config.payloadActionKey]
        if (action.equals(config.triggerActionValue, ignoreCase = true)) {
            val customParams = buildMap {
                payload[config.payloadMetadataKey]?.let { put("push_metadata", it) }
                // Also propagate any other relevant push keys if needed
                payload.forEach { (k, v) ->
                    if (k != config.payloadActionKey && k != config.payloadMetadataKey) {
                        put("push_$k", v)
                    }
                }
            }
            listener?.invoke(TriggerSignal(type = ScreenshotTriggerType.PUSH_NOTIFICATION, customParameters = customParams))
            return true
        }
        return false
    }
}

/**
 * Handler for programmatic manual trigger calls.
 */
class ManualTriggerHandler(
    override val isEnabled: Boolean = true
) : ScreenshotTrigger {

    override val triggerType: ScreenshotTriggerType = ScreenshotTriggerType.MANUAL
    private var listener: ((TriggerSignal) -> Unit)? = null

    override fun startListening(onTrigger: (TriggerSignal) -> Unit) {
        this.listener = onTrigger
    }

    override fun stopListening() {
        this.listener = null
    }

    /**
     * Programmatically fires the trigger signal.
     */
    fun trigger(customParameters: Map<String, String> = emptyMap()) {
        if (isEnabled) {
            listener?.invoke(TriggerSignal(type = ScreenshotTriggerType.MANUAL, customParameters = customParameters))
        }
    }
}

/**
 * Trigger handler that matches analytics or custom app events against a configured trigger list.
 */
class EventTriggerHandler(
    private val config: EventTriggerConfig
) : ScreenshotTrigger {

    override val triggerType: ScreenshotTriggerType = ScreenshotTriggerType.CUSTOM_EVENT
    override val isEnabled: Boolean get() = config.isEnabled

    private var listener: ((TriggerSignal) -> Unit)? = null

    override fun startListening(onTrigger: (TriggerSignal) -> Unit) {
        this.listener = onTrigger
    }

    override fun stopListening() {
        this.listener = null
    }

    /**
     * Evaluates an app event name and parameters. If the event is in the configured trigger set, fires signal.
     */
    fun onEvent(eventName: String, params: Map<String, String> = emptyMap()): Boolean {
        if (!config.isEnabled) return false

        if (config.triggerEvents.contains(eventName)) {
            val customParams = buildMap {
                put("triggered_by_event", eventName)
                putAll(params)
            }
            listener?.invoke(TriggerSignal(type = ScreenshotTriggerType.CUSTOM_EVENT, customParameters = customParams))
            return true
        }
        return false
    }
}
