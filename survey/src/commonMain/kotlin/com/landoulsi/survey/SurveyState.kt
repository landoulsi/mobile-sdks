package com.landoulsi.survey

import com.landoulsi.schemaui.ir.UINode
import com.landoulsi.survey.model.SurveyDefinition
import com.landoulsi.survey.model.SurveyResponse

/**
 * Observable state of a [SurveyController]. Collect [SurveyController.state] and render
 * whichever case is current.
 */
sealed interface SurveyState {

    /** Nothing loaded yet. */
    data object Idle : SurveyState

    /** A server fetch is in flight. */
    data object Loading : SurveyState

    /**
     * The survey is loaded and interactive.
     *
     * @param node the `:schemaui` tree to render — rebuilt on every answer change.
     * @param validationErrors per-question messages from the last failed [SurveyController.submit].
     * @param submitting a submit request is in flight.
     * @param submitError the last submit attempt failed with this message (transport/server).
     */
    data class Ready(
        val definition: SurveyDefinition,
        val node: UINode,
        val validationErrors: Map<String, String> = emptyMap(),
        val submitting: Boolean = false,
        val submitError: String? = null,
    ) : SurveyState

    /** Loading or parsing the survey failed. Terminal until the next `load…` call. */
    data class LoadError(val message: String) : SurveyState

    /** The response was accepted by the server. Terminal. */
    data class Submitted(
        val definition: SurveyDefinition,
        val response: SurveyResponse,
    ) : SurveyState
}
