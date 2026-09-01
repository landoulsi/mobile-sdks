package com.landoulsi.survey.internal

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Shared JSON configuration for the module. Lenient and tolerant of unknown keys because
 * survey schemas evolve server-side and may carry fields this SDK version does not model.
 */
internal val surveyJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}

private val stringListSerializer = ListSerializer(String.serializer())

/**
 * Encodes the selected values of a multi-choice question for storage in the
 * `:schemaui` [com.landoulsi.schemaui.state.StateStore] (which holds only strings).
 */
internal fun encodeValueList(values: List<String>): String =
    surveyJson.encodeToString(stringListSerializer, values)

/**
 * Inverse of [encodeValueList]. Tolerates `null`, blank, a bare scalar (treated as a
 * one-element list) and malformed input (treated as empty).
 */
internal fun decodeValueList(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching { surveyJson.decodeFromString(stringListSerializer, raw) }
        .getOrElse { listOf(raw) }
}
