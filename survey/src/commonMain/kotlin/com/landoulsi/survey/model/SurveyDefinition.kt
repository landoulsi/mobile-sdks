package com.landoulsi.survey.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A complete survey, as delivered by a server (or handed in directly as JSON).
 *
 * The wire format is a plain JSON object:
 * ```json
 * {
 *   "id": "nps-2026-q1",
 *   "title": "How are we doing?",
 *   "description": "Takes about a minute.",
 *   "submitUrl": "https://api.example.com/surveys/nps-2026-q1/responses",
 *   "questions": [
 *     { "type": "rating", "id": "nps", "title": "How likely are you to recommend us?", "max": 10, "required": true },
 *     { "type": "singleChoice", "id": "role", "title": "What best describes you?",
 *       "options": [ { "value": "dev", "label": "Developer" }, { "value": "pm", "label": "Product" } ] },
 *     { "type": "longText", "id": "why", "title": "Anything else?" }
 *   ]
 * }
 * ```
 *
 * Unknown [questions] `type` values deserialize to [UnknownQuestion] rather than failing the
 * whole parse, so a newer server schema degrades gracefully on an older SDK.
 */
@Serializable
data class SurveyDefinition(
    val id: String,
    val title: String,
    val description: String? = null,
    val questions: List<SurveyQuestion> = emptyList(),
    /** Button label for the final submit action. */
    val submitLabel: String = "Submit",
    /**
     * Endpoint the SDK POSTs the [SurveyResponse] to when the host does not pass a URL
     * explicitly to [com.landoulsi.survey.SurveyController.submit]. Optional.
     */
    val submitUrl: String? = null,
)

/** A selectable answer for choice-style questions. */
@Serializable
data class SurveyOption(
    val value: String,
    val label: String = value,
)

/**
 * One question in a [SurveyDefinition]. Polymorphic on the `"type"` field.
 *
 * Supported types: `shortText`, `longText`, `singleChoice`, `multiChoice`, `rating`, `boolean`.
 */
@Serializable(with = SurveyQuestionSerializer::class)
sealed class SurveyQuestion {
    abstract val id: String
    abstract val title: String
    abstract val description: String?
    abstract val required: Boolean
}

/** Single-line free text. */
@Serializable
@SerialName("shortText")
data class ShortTextQuestion(
    override val id: String,
    override val title: String,
    override val description: String? = null,
    override val required: Boolean = false,
    val placeholder: String = "",
) : SurveyQuestion()

/** Multi-line free text. */
@Serializable
@SerialName("longText")
data class LongTextQuestion(
    override val id: String,
    override val title: String,
    override val description: String? = null,
    override val required: Boolean = false,
    val placeholder: String = "",
) : SurveyQuestion()

/** Pick exactly one of [options]. */
@Serializable
@SerialName("singleChoice")
data class SingleChoiceQuestion(
    override val id: String,
    override val title: String,
    override val description: String? = null,
    override val required: Boolean = false,
    val options: List<SurveyOption> = emptyList(),
) : SurveyQuestion()

/** Pick any number of [options]. */
@Serializable
@SerialName("multiChoice")
data class MultiChoiceQuestion(
    override val id: String,
    override val title: String,
    override val description: String? = null,
    override val required: Boolean = false,
    val options: List<SurveyOption> = emptyList(),
) : SurveyQuestion()

/** An integer scale from `1` to [max]. */
@Serializable
@SerialName("rating")
data class RatingQuestion(
    override val id: String,
    override val title: String,
    override val description: String? = null,
    override val required: Boolean = false,
    val max: Int = 5,
) : SurveyQuestion()

/** A yes / no choice. Stored as the string `"true"` or `"false"`. */
@Serializable
@SerialName("boolean")
data class BooleanQuestion(
    override val id: String,
    override val title: String,
    override val description: String? = null,
    override val required: Boolean = false,
    val trueLabel: String = "Yes",
    val falseLabel: String = "No",
) : SurveyQuestion()

/**
 * Fallback for a `type` this SDK version does not recognize. Rendered as its [title]
 * plus a short notice; never contributes an answer.
 */
@Serializable
@SerialName("unknown")
data class UnknownQuestion(
    override val id: String,
    override val title: String = "",
    override val description: String? = null,
    override val required: Boolean = false,
    val originalType: String = "unknown",
) : SurveyQuestion()

/**
 * Dispatches on `"type"` and falls back to [UnknownQuestion] for unrecognized values so a
 * forward-incompatible server schema does not break the whole survey.
 */
internal object SurveyQuestionSerializer :
    JsonContentPolymorphicSerializer<SurveyQuestion>(SurveyQuestion::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<SurveyQuestion> {
        val obj = element as? JsonObject
            ?: throw SerializationException("Expected a JSON object for a survey question, found: $element")
        val type = obj["type"]?.jsonPrimitive?.contentOrNull
            ?: throw SerializationException("Survey question is missing the 'type' discriminator: $obj")
        return when (type) {
            "shortText" -> ShortTextQuestion.serializer()
            "longText" -> LongTextQuestion.serializer()
            "singleChoice" -> SingleChoiceQuestion.serializer()
            "multiChoice" -> MultiChoiceQuestion.serializer()
            "rating" -> RatingQuestion.serializer()
            "boolean" -> BooleanQuestion.serializer()
            "unknown" -> UnknownQuestion.serializer()
            else -> UnknownQuestionShim(type)
        }
    }
}

/**
 * Deserializes a question whose `type` this SDK version does not know into an
 * [UnknownQuestion], preserving the original `type` string in [UnknownQuestion.originalType].
 */
private class UnknownQuestionShim(private val originalType: String) : KSerializer<UnknownQuestion> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UnknownQuestionShim")

    override fun deserialize(decoder: Decoder): UnknownQuestion {
        val obj = (decoder as? JsonDecoder)?.decodeJsonElement()?.jsonObject
            ?: return UnknownQuestion(id = originalType, originalType = originalType)
        return UnknownQuestion(
            id = obj["id"]?.jsonPrimitive?.contentOrNull ?: originalType,
            title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "",
            description = obj["description"]?.jsonPrimitive?.contentOrNull,
            required = obj["required"]?.jsonPrimitive?.booleanOrNull ?: false,
            originalType = originalType,
        )
    }

    override fun serialize(encoder: Encoder, value: UnknownQuestion) {
        UnknownQuestion.serializer().serialize(encoder, value)
    }
}
