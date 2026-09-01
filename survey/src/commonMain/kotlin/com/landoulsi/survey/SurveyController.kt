package com.landoulsi.survey

import com.landoulsi.logger.Logger
import com.landoulsi.schemaui.SchemaUIEngine
import com.landoulsi.survey.internal.decodeValueList
import com.landoulsi.survey.internal.encodeValueList
import com.landoulsi.survey.model.BooleanQuestion
import com.landoulsi.survey.model.LongTextQuestion
import com.landoulsi.survey.model.MultiChoiceQuestion
import com.landoulsi.survey.model.RatingQuestion
import com.landoulsi.survey.model.ShortTextQuestion
import com.landoulsi.survey.model.SingleChoiceQuestion
import com.landoulsi.survey.model.SurveyAnswer
import com.landoulsi.survey.model.SurveyDefinition
import com.landoulsi.survey.model.SurveyQuestion
import com.landoulsi.survey.model.SurveyResponse
import com.landoulsi.survey.model.UnknownQuestion
import com.landoulsi.timeprovider.SystemTimeProvider
import com.landoulsi.timeprovider.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives one survey end to end:
 *
 * 1. **Load** a [SurveyDefinition] — from a JSON string ([loadFromJson]) or a server URL
 *    ([loadFromServer]).
 * 2. **Render** — [state] emits [SurveyState.Ready] carrying a `:schemaui` [com.landoulsi.schemaui.ir.UINode]
 *    tree. Hand that plus [engine] to the `Survey()` composable (Android) or `SurveyKit` (iOS).
 * 3. **Collect** — respondent input flows into [engine]'s `StateStore`; the tree is rebuilt on
 *    every change so choice selections re-style live.
 * 4. **Submit** — [submit] validates required questions, builds a [SurveyResponse] and POSTs it.
 *
 * Not thread-confined, but intended to be called from the main thread. Call [close] when done.
 *
 * @param client transport for fetch/submit. Defaults to a platform client this controller owns.
 * @param timeProvider clock stamped onto [SurveyResponse.submittedAtMillis].
 * @param scope coroutine scope for the reactive rebuild and network calls. When null the
 *   controller creates (and, in [close], cancels) its own.
 */
class SurveyController(
    private val client: SurveyClient = SurveyClientFactory.create(),
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    scope: CoroutineScope? = null,
    private val ownsClient: Boolean = true,
) {
    private val ownsScope: Boolean = scope == null
    private val scope: CoroutineScope = scope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The `:schemaui` engine that owns the answer `StateStore` and the action registry. */
    val engine: SchemaUIEngine = SchemaUIEngine()

    private val parser = SurveyParser()
    private val builder = SurveySchemaBuilder()

    private val _state = MutableStateFlow<SurveyState>(SurveyState.Idle)
    val state: StateFlow<SurveyState> = _state.asStateFlow()

    private var definition: SurveyDefinition? = null
    private var validationErrors: Map<String, String> = emptyMap()
    private var rebuildJob: Job? = null

    // ─── Loading ────────────────────────────────────────────────────────────

    /** Parses [json] and, on success, makes the survey interactive. */
    fun loadFromJson(json: String) {
        parser.parse(json).fold(
            onSuccess = ::prepare,
            onFailure = { e ->
                Logger.w(TAG, "Survey JSON parse failed: ${e.message}")
                _state.value = SurveyState.LoadError(e.message ?: "Could not parse survey")
            },
        )
    }

    /** Fetches the survey from [url], then behaves like [loadFromJson]. */
    fun loadFromServer(url: String) {
        _state.value = SurveyState.Loading
        scope.launch {
            runCatching { client.fetchDefinition(url) }.fold(
                onSuccess = ::prepare,
                onFailure = { e ->
                    Logger.w(TAG, "Survey fetch failed: ${e.message}")
                    _state.value = SurveyState.LoadError(e.message ?: "Could not load survey")
                },
            )
        }
    }

    private fun prepare(def: SurveyDefinition) {
        definition = def
        validationErrors = emptyMap()
        engine.clearActions()
        engine.stateStore.clear()
        registerActions(def)

        rebuildJob?.cancel()
        // stateStore.state is a StateFlow — collect emits the (empty) current value immediately,
        // producing the first Ready. Every later answer change rebuilds the tree.
        rebuildJob = scope.launch {
            engine.stateStore.state.collect { answers -> rebuild(def, answers) }
        }
    }

    // ─── Action wiring ──────────────────────────────────────────────────────

    private fun registerActions(def: SurveyDefinition) {
        def.questions.forEach { q ->
            when (q) {
                is SingleChoiceQuestion -> q.options.forEach { opt ->
                    engine.registerAction(SurveyActions.option(q.id, opt.value)) {
                        setAnswer(q.id, opt.value)
                    }
                }

                is MultiChoiceQuestion -> q.options.forEach { opt ->
                    engine.registerAction(SurveyActions.option(q.id, opt.value)) {
                        val current = decodeValueList(engine.stateStore.get(q.id)).toMutableList()
                        if (!current.remove(opt.value)) current.add(opt.value)
                        setAnswer(q.id, encodeValueList(current))
                    }
                }

                is RatingQuestion -> (1..q.max.coerceIn(1, 10)).forEach { n ->
                    engine.registerAction(SurveyActions.option(q.id, n.toString())) {
                        setAnswer(q.id, n.toString())
                    }
                }

                is BooleanQuestion -> listOf("true", "false").forEach { v ->
                    engine.registerAction(SurveyActions.option(q.id, v)) { setAnswer(q.id, v) }
                }

                // Free-text fields write straight to the StateStore via :schemaui.
                is ShortTextQuestion, is LongTextQuestion, is UnknownQuestion -> Unit
            }
        }
        engine.registerAction(SurveyActions.SUBMIT) { submit() }
    }

    private fun setAnswer(questionId: String, value: String) {
        if (validationErrors.containsKey(questionId)) {
            validationErrors = validationErrors - questionId
        }
        // Triggers the rebuild collector, which re-renders with the cleared error.
        engine.stateStore.set(questionId, value)
    }

    private fun rebuild(def: SurveyDefinition, answers: Map<String, String>) {
        val node = builder.build(def, answers, validationErrors)
        _state.update { prev ->
            val ready = prev as? SurveyState.Ready
            SurveyState.Ready(
                definition = def,
                node = node,
                validationErrors = validationErrors,
                submitting = ready?.submitting ?: false,
                submitError = ready?.submitError,
            )
        }
    }

    // ─── Submission ─────────────────────────────────────────────────────────

    /**
     * Validates required questions and, if they pass, POSTs a [SurveyResponse].
     *
     * @param url overrides [SurveyDefinition.submitUrl] for this call. If both are null the
     *   attempt fails with a [SurveyState.Ready.submitError].
     */
    fun submit(url: String? = null) {
        val def = definition ?: return
        if ((_state.value as? SurveyState.Ready)?.submitting == true) return
        val answers = engine.stateStore.snapshot()

        val errors = validate(def, answers)
        if (errors.isNotEmpty()) {
            validationErrors = errors
            rebuild(def, answers)
            return
        }

        val target = url ?: def.submitUrl
        if (target.isNullOrBlank()) {
            _state.update {
                (it as? SurveyState.Ready)?.copy(submitError = "No submit URL configured") ?: it
            }
            return
        }

        val response = buildResponse(def, answers)
        _state.update { (it as? SurveyState.Ready)?.copy(submitting = true, submitError = null) ?: it }

        scope.launch {
            runCatching { client.submit(target, response) }.fold(
                onSuccess = {
                    rebuildJob?.cancel()
                    _state.value = SurveyState.Submitted(def, response)
                },
                onFailure = { e ->
                    Logger.w(TAG, "Survey submit failed: ${e.message}")
                    _state.update {
                        (it as? SurveyState.Ready)?.copy(
                            submitting = false,
                            submitError = e.message ?: "Submit failed",
                        ) ?: it
                    }
                },
            )
        }
    }

    /** @return question id → message for every required question left unanswered. */
    fun validate(def: SurveyDefinition, answers: Map<String, String>): Map<String, String> = buildMap {
        def.questions.forEach { q ->
            if (!q.required) return@forEach
            val answered = when (q) {
                is MultiChoiceQuestion -> decodeValueList(answers[q.id]).isNotEmpty()
                is UnknownQuestion -> true // never blocks submission
                else -> !answers[q.id].isNullOrBlank()
            }
            if (!answered) put(q.id, REQUIRED_MESSAGE)
        }
    }

    private fun buildResponse(def: SurveyDefinition, answers: Map<String, String>): SurveyResponse {
        val collected = def.questions.mapNotNull { q: SurveyQuestion ->
            when (q) {
                is MultiChoiceQuestion -> decodeValueList(answers[q.id])
                    .takeIf { it.isNotEmpty() }
                    ?.let { SurveyAnswer(q.id, it) }

                is UnknownQuestion -> null
                else -> answers[q.id]
                    ?.takeIf { it.isNotBlank() }
                    ?.let { SurveyAnswer(q.id, listOf(it)) }
            }
        }
        return SurveyResponse(
            surveyId = def.id,
            answers = collected,
            submittedAtMillis = timeProvider.currentTimeMillis(),
        )
    }

    /** Cancels in-flight work and releases the HTTP client / scope this controller owns. */
    fun close() {
        rebuildJob?.cancel()
        if (ownsScope) scope.cancel()
        if (ownsClient) client.close()
    }

    private companion object {
        const val TAG = "SurveyController"
        const val REQUIRED_MESSAGE = "This question is required."
    }
}
