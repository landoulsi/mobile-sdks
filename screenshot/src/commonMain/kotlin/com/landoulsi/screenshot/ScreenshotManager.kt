package com.landoulsi.screenshot

import com.landoulsi.screenshot.capture.ScreenshotCapturer
import com.landoulsi.screenshot.config.ScreenshotConfig
import com.landoulsi.screenshot.metadata.DefaultMetadataCollector
import com.landoulsi.screenshot.metadata.MetadataCollector
import com.landoulsi.screenshot.model.ScreenshotPayload
import com.landoulsi.screenshot.model.ScreenshotTriggerType
import com.landoulsi.screenshot.model.UploadResponse
import com.landoulsi.screenshot.network.KtorScreenshotUploader
import com.landoulsi.screenshot.network.ScreenshotUploader
import com.landoulsi.screenshot.trigger.EventTriggerHandler
import com.landoulsi.screenshot.trigger.ManualTriggerHandler
import com.landoulsi.screenshot.trigger.PushTriggerHandler
import com.landoulsi.screenshot.trigger.ScreenshotTrigger
import com.landoulsi.screenshot.trigger.TriggerSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main coordinator and entry point for the Screenshot SDK.
 *
 * Coordinates triggers, screen capture, metadata gathering, and remote upload.
 */
class ScreenshotManager(
    private var config: ScreenshotConfig,
    private val capturer: ScreenshotCapturer,
    private val uploader: ScreenshotUploader = KtorScreenshotUploader(),
    private val metadataCollector: MetadataCollector = DefaultMetadataCollector(config.metadata),
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    private val triggers = mutableListOf<ScreenshotTrigger>()
    private var pushTriggerHandler: PushTriggerHandler? = null
    private var manualTriggerHandler: ManualTriggerHandler? = null
    private var eventTriggerHandler: EventTriggerHandler? = null

    private var onUploadResultListener: ((Result<UploadResponse>) -> Unit)? = null

    init {
        setupTriggers()
    }

    /**
     * Updates the active configuration and reinitializes triggers.
     */
    fun updateConfig(newConfig: ScreenshotConfig) {
        stopTriggers()
        this.config = newConfig
        setupTriggers()
    }

    /**
     * Retrieves the current configuration.
     */
    fun getConfig(): ScreenshotConfig = config

    /**
     * Attaches a listener invoked on completion of any screenshot capture & upload cycle.
     */
    fun setOnUploadResultListener(listener: ((Result<UploadResponse>) -> Unit)?) {
        this.onUploadResultListener = listener
    }

    /**
     * Primary entry point for executing an immediate capture and upload flow.
     *
     * @param triggerType Reason or signal initiating this capture.
     * @param customParams Extra key-value pairs to include in the metadata payload.
     * @return [Result] containing [UploadResponse] or capture/upload error.
     */
    suspend fun captureAndUpload(
        triggerType: ScreenshotTriggerType = ScreenshotTriggerType.MANUAL,
        customParams: Map<String, String> = emptyMap()
    ): Result<UploadResponse> {
        if (!config.isEnabled) {
            return Result.failure(IllegalStateException("Screenshot SDK is currently disabled in configuration"))
        }

        if (!capturer.isAvailable()) {
            return Result.failure(IllegalStateException("ScreenshotCapturer is not available in the current context"))
        }

        // Apply capture delay if configured
        if (config.capture.captureDelayMillis > 0) {
            delay(config.capture.captureDelayMillis)
        }

        // 1. Capture screen
        val captureResult = capturer.capture(config.capture)
        val image = captureResult.getOrElse { error ->
            val res = Result.failure<UploadResponse>(error)
            onUploadResultListener?.invoke(res)
            return res
        }

        // 2. Collect metadata
        val metadata = metadataCollector.collectMetadata(triggerType, customParams)

        // 3. Assemble payload
        val payload = ScreenshotPayload(image = image, metadata = metadata)

        // 4. Upload to configured backend
        val uploadResult = uploader.upload(payload, config.server)
        onUploadResultListener?.invoke(uploadResult)
        return uploadResult
    }

    /**
     * Helper to process incoming Push Notification payloads (e.g. from Firebase / APNs).
     *
     * @param payload Push notification key-value data map.
     * @return True if the payload matched the screenshot trigger configuration and launched capture.
     */
    fun handlePushNotification(payload: Map<String, String>): Boolean {
        val handler = pushTriggerHandler ?: return false
        return handler.handlePushPayload(payload)
    }

    /**
     * Helper to process an analytics or custom application event.
     *
     * @param eventName Name of the event.
     * @param params Additional event parameters.
     * @return True if the event was in the trigger set and launched capture.
     */
    fun onAppEvent(eventName: String, params: Map<String, String> = emptyMap()): Boolean {
        val handler = eventTriggerHandler ?: return false
        return handler.onEvent(eventName, params)
    }

    /**
     * Convenience method to manually initiate capture and upload asynchronously.
     */
    fun triggerManualCapture(customParams: Map<String, String> = emptyMap()) {
        manualTriggerHandler?.trigger(customParams)
    }

    /**
     * Registers an additional custom trigger handler (e.g. custom hardware button or socket event).
     */
    fun registerTrigger(trigger: ScreenshotTrigger) {
        triggers.add(trigger)
        if (trigger.isEnabled) {
            trigger.startListening { signal ->
                onSignalReceived(signal)
            }
        }
    }

    private fun setupTriggers() {
        triggers.clear()

        // Push trigger
        val push = PushTriggerHandler(config.triggers.push)
        this.pushTriggerHandler = push
        triggers.add(push)

        // Manual trigger
        val manual = ManualTriggerHandler(config.triggers.manual.isEnabled)
        this.manualTriggerHandler = manual
        triggers.add(manual)

        // Event trigger
        val event = EventTriggerHandler(config.triggers.events)
        this.eventTriggerHandler = event
        triggers.add(event)

        // Start listening on enabled triggers
        triggers.forEach { trigger ->
            if (trigger.isEnabled) {
                trigger.startListening { signal ->
                    onSignalReceived(signal)
                }
            }
        }
    }

    private fun stopTriggers() {
        triggers.forEach { it.stopListening() }
        triggers.clear()
    }

    private fun onSignalReceived(signal: TriggerSignal) {
        coroutineScope.launch {
            captureAndUpload(
                triggerType = signal.type,
                customParams = signal.customParameters
            )
        }
    }
}
