package com.landoulsi.survey

import com.landoulsi.schemaui.ir.UIBox
import com.landoulsi.schemaui.ir.UIButton
import com.landoulsi.schemaui.ir.UIButtonStyle
import com.landoulsi.schemaui.ir.UIColumn
import com.landoulsi.schemaui.ir.UINode
import com.landoulsi.schemaui.ir.UIRow
import com.landoulsi.schemaui.ir.UIText
import com.landoulsi.schemaui.ir.UITextField
import com.landoulsi.survey.internal.encodeValueList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SurveySchemaBuilderTest {

    private val builder = SurveySchemaBuilder()
    private val definition = SurveyParser().parseOrThrow(SurveyFixtures.FULL_SURVEY_JSON)

    private fun build(answers: Map<String, String> = emptyMap(), errors: Map<String, String> = emptyMap()) =
        builder.build(definition, answers, errors).flatten()

    @Test
    fun root_is_a_column() {
        assertIs<UIColumn>(builder.build(definition, emptyMap()))
    }

    @Test
    fun renders_title_and_description() {
        val texts = build().filterIsInstance<UIText>().map { it.text }
        assertTrue("Product feedback" in texts)
        assertTrue("Two minutes, tops." in texts)
    }

    @Test
    fun required_questions_get_an_asterisk() {
        val texts = build().filterIsInstance<UIText>().map { it.text }
        assertTrue(texts.any { it == "How likely are you to recommend us? *" })
        assertTrue(texts.any { it == "Anything else? *" })
        assertTrue(texts.any { it == "Your name" }) // not required — no asterisk
    }

    @Test
    fun submit_button_carries_the_submit_action_and_label() {
        val submit = build().filterIsInstance<UIButton>().single { it.action == SurveyActions.SUBMIT }
        assertEquals("Send it", submit.label)
    }

    @Test
    fun single_choice_emits_one_button_per_option_with_a_stable_action() {
        val actions = build().filterIsInstance<UIButton>().map { it.action }
        assertTrue(SurveyActions.option("role", "dev") in actions)
        assertTrue(SurveyActions.option("role", "pm") in actions)
        assertTrue(SurveyActions.option("role", "design") in actions)
    }

    @Test
    fun selected_single_choice_option_is_filled_the_rest_outlined() {
        val buttons = build(answers = mapOf("role" to "pm"))
            .filterIsInstance<UIButton>()
            .associateBy { it.action }

        assertEquals(UIButtonStyle.Filled, buttons[SurveyActions.option("role", "pm")]?.style)
        assertEquals(UIButtonStyle.Outlined, buttons[SurveyActions.option("role", "dev")]?.style)
    }

    @Test
    fun rating_emits_a_button_for_every_step() {
        val ratingActions = build().filterIsInstance<UIButton>()
            .map { it.action }
            .filter { it.startsWith(SurveyActions.option("nps", "")) }
        assertEquals((1..10).map { SurveyActions.option("nps", it.toString()) }.toSet(), ratingActions.toSet())
    }

    @Test
    fun multi_choice_selected_options_are_ticked_and_filled() {
        val buttons = build(answers = mapOf("channels" to encodeValueList(listOf("ios", "web"))))
            .filterIsInstance<UIButton>()
            .associateBy { it.action }

        val ios = buttons[SurveyActions.option("channels", "ios")]
        val android = buttons[SurveyActions.option("channels", "android")]
        assertNotNull(ios)
        assertEquals(UIButtonStyle.Filled, ios.style)
        assertTrue(ios.label.startsWith("✓"))
        assertEquals(UIButtonStyle.Outlined, android?.style)
    }

    @Test
    fun free_text_questions_bind_a_textfield_to_the_question_id() {
        val keys = build().filterIsInstance<UITextField>().map { it.stateKey }
        assertTrue("name" in keys)
        assertTrue("why" in keys)
    }

    @Test
    fun validation_error_text_is_rendered_under_the_question() {
        val withError = build(errors = mapOf("why" to "This question is required."))
            .filterIsInstance<UIText>()
            .map { it.text }
        assertTrue("This question is required." in withError)

        val without = build().filterIsInstance<UIText>().map { it.text }
        assertNull(without.firstOrNull { it == "This question is required." })
    }
}

/** Depth-first flatten of a `:schemaui` IR tree into a list of every node. */
internal fun UINode.flatten(): List<UINode> = buildList {
    add(this@flatten)
    when (val n = this@flatten) {
        is UIColumn -> n.children.forEach { addAll(it.flatten()) }
        is UIRow -> n.children.forEach { addAll(it.flatten()) }
        is UIBox -> n.children.forEach { addAll(it.flatten()) }
        else -> Unit
    }
}
