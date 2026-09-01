package com.landoulsi.survey

import com.landoulsi.schemaui.ir.UIButton
import com.landoulsi.schemaui.ir.UIButtonStyle
import com.landoulsi.schemaui.ir.UIColumn
import com.landoulsi.schemaui.ir.UIFontWeight
import com.landoulsi.schemaui.ir.UIModifiers
import com.landoulsi.schemaui.ir.UINode
import com.landoulsi.schemaui.ir.UIRow
import com.landoulsi.schemaui.ir.UISpacer
import com.landoulsi.schemaui.ir.UIText
import com.landoulsi.schemaui.ir.UITextField
import com.landoulsi.schemaui.ir.UITextStyle
import com.landoulsi.survey.internal.decodeValueList
import com.landoulsi.survey.model.BooleanQuestion
import com.landoulsi.survey.model.LongTextQuestion
import com.landoulsi.survey.model.MultiChoiceQuestion
import com.landoulsi.survey.model.RatingQuestion
import com.landoulsi.survey.model.ShortTextQuestion
import com.landoulsi.survey.model.SingleChoiceQuestion
import com.landoulsi.survey.model.SurveyDefinition
import com.landoulsi.survey.model.SurveyQuestion
import com.landoulsi.survey.model.UnknownQuestion

/**
 * Action identifiers dispatched by the rendered `:schemaui` tree back to the
 * [SurveyController]. The controller registers a matching handler for each one.
 */
internal object SurveyActions {
    const val SUBMIT = "survey.submit"

    private const val OPTION_PREFIX = "survey.opt::"
    private const val SEP = "::"

    /** Action fired when the respondent taps option [value] of question [questionId]. */
    fun option(questionId: String, value: String): String = "$OPTION_PREFIX$questionId$SEP$value"
}

/**
 * Turns a [SurveyDefinition] plus the current answer state into a `:schemaui` [UINode] tree.
 *
 * The build is pure and cheap — [SurveyController] re-runs it on every answer change so that
 * choice selections re-style (a picked option renders [UIButtonStyle.Filled], the rest
 * [UIButtonStyle.Outlined]) and validation messages appear/clear.
 *
 * @param answers the `:schemaui` state snapshot, keyed by question id. Multi-choice values
 *   are a JSON string array (see [com.landoulsi.survey.internal.encodeValueList]).
 * @param errors per-question validation messages to render under the offending question.
 */
internal class SurveySchemaBuilder {

    fun build(
        definition: SurveyDefinition,
        answers: Map<String, String>,
        errors: Map<String, String> = emptyMap(),
    ): UINode {
        val children = buildList {
            add(heading(definition.title, size = 22f, weight = UIFontWeight.Bold))
            definition.description?.takeIf { it.isNotBlank() }?.let {
                add(spacer(4f))
                add(body(it, color = MUTED))
            }
            add(spacer(20f))

            definition.questions.forEach { question ->
                add(questionBlock(question, answers, errors[question.id]))
                add(spacer(20f))
            }

            add(
                UIButton(
                    label = definition.submitLabel,
                    action = SurveyActions.SUBMIT,
                    style = UIButtonStyle.Filled,
                    modifiers = FILL_WIDTH,
                ),
            )
        }
        return UIColumn(children = children, modifiers = ROOT)
    }

    // ─── Question blocks ─────────────────────────────────────────────────────

    private fun questionBlock(
        question: SurveyQuestion,
        answers: Map<String, String>,
        error: String?,
    ): UINode {
        val parts = buildList {
            val titleText = if (question.required) "${question.title} *" else question.title
            add(heading(titleText, size = 16f, weight = UIFontWeight.SemiBold))
            question.description?.takeIf { it.isNotBlank() }?.let {
                add(spacer(2f))
                add(body(it, size = 13f, color = MUTED))
            }
            add(spacer(8f))
            add(input(question, answers))
            if (error != null) {
                add(spacer(4f))
                add(body(error, size = 12f, color = ERROR))
            }
        }
        return UIColumn(children = parts, modifiers = FILL_WIDTH)
    }

    private fun input(question: SurveyQuestion, answers: Map<String, String>): UINode = when (question) {
        is ShortTextQuestion -> UITextField(
            label = "",
            placeholder = question.placeholder,
            stateKey = question.id,
            action = null,
            modifiers = FILL_WIDTH,
        )

        is LongTextQuestion -> UITextField(
            label = "",
            placeholder = question.placeholder,
            stateKey = question.id,
            action = null,
            modifiers = UIModifiers(fillMaxWidth = true, minHeight = 96f),
        )

        is SingleChoiceQuestion -> {
            val selected = answers[question.id]
            UIColumn(
                children = question.options.map { opt ->
                    optionButton(
                        label = opt.label,
                        action = SurveyActions.option(question.id, opt.value),
                        selected = selected == opt.value,
                    )
                },
                modifiers = FILL_WIDTH,
            )
        }

        is MultiChoiceQuestion -> {
            val selected = decodeValueList(answers[question.id]).toSet()
            UIColumn(
                children = question.options.map { opt ->
                    val isOn = opt.value in selected
                    optionButton(
                        label = if (isOn) "✓ ${opt.label}" else opt.label,
                        action = SurveyActions.option(question.id, opt.value),
                        selected = isOn,
                    )
                },
                modifiers = FILL_WIDTH,
            )
        }

        is RatingQuestion -> {
            val selected = answers[question.id]
            val max = question.max.coerceIn(1, 10)
            // :schemaui has no wrapping row, so chunk into rows of 5 — keeps a 1..10 scale
            // on screen without horizontal overflow.
            UIColumn(
                children = (1..max).chunked(5).map { chunk ->
                    UIRow(
                        children = chunk.map { n ->
                            val v = n.toString()
                            UIButton(
                                label = v,
                                action = SurveyActions.option(question.id, v),
                                style = if (selected == v) UIButtonStyle.Filled else UIButtonStyle.Outlined,
                                modifiers = UIModifiers(paddingEnd = 6f, paddingBottom = 6f),
                            )
                        },
                    )
                },
                modifiers = FILL_WIDTH,
            )
        }

        is BooleanQuestion -> {
            val selected = answers[question.id]
            UIColumn(
                children = listOf(
                    optionButton(question.trueLabel, SurveyActions.option(question.id, "true"), selected == "true"),
                    optionButton(question.falseLabel, SurveyActions.option(question.id, "false"), selected == "false"),
                ),
                modifiers = FILL_WIDTH,
            )
        }

        is UnknownQuestion -> body(
            "This question type (\"${question.originalType}\") isn't supported by this app version.",
            size = 13f,
            color = MUTED,
        )
    }

    private fun optionButton(label: String, action: String, selected: Boolean): UIButton = UIButton(
        label = label,
        action = action,
        style = if (selected) UIButtonStyle.Filled else UIButtonStyle.Outlined,
        modifiers = UIModifiers(fillMaxWidth = true, paddingBottom = 6f),
    )

    // ─── Text helpers ───────────────────────────────────────────────────────

    private fun heading(text: String, size: Float, weight: UIFontWeight) = UIText(
        text = text,
        style = UITextStyle(fontSize = size, fontWeight = weight),
        modifiers = FILL_WIDTH,
    )

    private fun body(text: String, size: Float = 14f, color: String? = null) = UIText(
        text = text,
        style = UITextStyle(fontSize = size, color = color),
        modifiers = FILL_WIDTH,
    )

    private fun spacer(height: Float) = UISpacer(width = null, height = height)

    private companion object {
        val ROOT = UIModifiers(
            fillMaxWidth = true,
            paddingStart = 16f,
            paddingEnd = 16f,
            paddingTop = 16f,
            paddingBottom = 16f,
        )
        val FILL_WIDTH = UIModifiers(fillMaxWidth = true)
        const val MUTED = "#666666"
        const val ERROR = "#B00020"
    }
}
