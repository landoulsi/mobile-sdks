package com.landoulsi.survey.model

import kotlinx.serialization.Serializable

/**
 * The payload POSTed to the server when a respondent submits.
 *
 * ```json
 * {
 *   "surveyId": "nps-2026-q1",
 *   "submittedAtMillis": 1756704000000,
 *   "answers": [
 *     { "questionId": "nps", "values": ["9"] },
 *     { "questionId": "role", "values": ["dev"] },
 *     { "questionId": "why", "values": ["Faster cold start please"] }
 *   ]
 * }
 * ```
 *
 * Every answer is a list of strings: single-value questions carry one element, [multi-choice]
 * questions carry zero or more. Questions the respondent left untouched are omitted entirely.
 */
@Serializable
data class SurveyResponse(
    val surveyId: String,
    val answers: List<SurveyAnswer>,
    /** Epoch milliseconds from the SDK's [com.landoulsi.timeprovider.TimeProvider]. */
    val submittedAtMillis: Long,
)

/** One respondent answer. [values] is empty only for an unanswered non-required question. */
@Serializable
data class SurveyAnswer(
    val questionId: String,
    val values: List<String>,
)
