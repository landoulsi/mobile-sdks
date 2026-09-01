package com.landoulsi.survey

import com.landoulsi.survey.internal.surveyJson
import com.landoulsi.survey.model.SurveyDefinition
import kotlinx.serialization.SerializationException

/**
 * Parses a survey JSON document into a [SurveyDefinition].
 *
 * Lenient: unknown keys are ignored and unknown question `type` values become
 * [com.landoulsi.survey.model.UnknownQuestion]. It still fails on structurally invalid
 * JSON or a missing required field (`id`, `title`, per-question `id`/`title`).
 */
class SurveyParser {

    /** @return [Result.success] with the [SurveyDefinition], or [Result.failure] with a [SurveyParseException]. */
    fun parse(json: String): Result<SurveyDefinition> = runCatching { parseOrThrow(json) }

    /** @throws SurveyParseException if [json] cannot be turned into a [SurveyDefinition]. */
    fun parseOrThrow(json: String): SurveyDefinition = try {
        surveyJson.decodeFromString(SurveyDefinition.serializer(), json)
    } catch (e: SerializationException) {
        throw SurveyParseException("Survey JSON could not be parsed: ${e.message}", e)
    } catch (e: IllegalArgumentException) {
        throw SurveyParseException("Survey JSON is not valid: ${e.message}", e)
    }
}

/** Thrown when a survey document cannot be parsed into a [SurveyDefinition]. */
class SurveyParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
