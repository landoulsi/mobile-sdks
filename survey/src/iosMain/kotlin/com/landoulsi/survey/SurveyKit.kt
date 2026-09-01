package com.landoulsi.survey

import com.landoulsi.schemaui.SchemaUIEngine
import com.landoulsi.schemaui.ir.UINode
import kotlin.experimental.ExperimentalObjCName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Swift-callable facade over [SurveyController].
 *
 * The `:schemaui` Swift renderer consumes [engine] plus the current [node]; call [observe]
 * to be told when [node] / [state] change so SwiftUI can re-read them.
 *
 * ## Swift usage
 * ```swift
 * let kit = SurveyKit()
 * kit.observe { state in /* setNeedsRender() */ }
 * kit.load(json: surveyJson)
 * // render: SchemaUIView(node: kit.node!, engine: kit.engine)
 * // the schema's submit button calls back into the controller automatically
 * ```
 */
@OptIn(ExperimentalObjCName::class)
@ObjCName("SurveyKit", swiftName = "SurveyKit")
class SurveyKit {

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val controller = SurveyController(scope = CoroutineScope(SupervisorJob() + Dispatchers.Main))
    private var observeJob: Job? = null

    /** The `:schemaui` engine to hand to `SchemaUIView`. */
    val engine: SchemaUIEngine get() = controller.engine

    /** Latest controller state. Read fresh from your [observe] callback. */
    var state: SurveyState = controller.state.value
        private set

    /** The current tree to render, or null unless [state] is [SurveyState.Ready]. */
    val node: UINode? get() = (state as? SurveyState.Ready)?.node

    /** Message of the last load/parse failure, or null. */
    val loadError: String? get() = (state as? SurveyState.LoadError)?.message

    /** True once the response has been accepted by the server. */
    val isSubmitted: Boolean get() = state is SurveyState.Submitted

    /** Registers a [listener] invoked on the main thread for every state change. */
    fun observe(listener: (SurveyState) -> Unit) {
        observeJob?.cancel()
        observeJob = controller.state
            .onEach { next ->
                state = next
                listener(next)
            }
            .launchIn(mainScope)
    }

    /** Parses [json] and makes the survey interactive. */
    fun load(json: String) = controller.loadFromJson(json)

    /** Fetches the survey from [url], then behaves like [load]. */
    fun loadFromServer(url: String) = controller.loadFromServer(url)

    /** Submits the collected response. [url] overrides the definition's `submitUrl`. */
    fun submit(url: String? = null) = controller.submit(url)

    /** Releases coroutines and the owned HTTP client. */
    fun dispose() {
        observeJob?.cancel()
        controller.close()
    }
}
