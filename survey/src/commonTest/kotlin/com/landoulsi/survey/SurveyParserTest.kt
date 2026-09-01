package com.landoulsi.survey

import com.landoulsi.survey.model.BooleanQuestion
import com.landoulsi.survey.model.LongTextQuestion
import com.landoulsi.survey.model.MultiChoiceQuestion
import com.landoulsi.survey.model.RatingQuestion
import com.landoulsi.survey.model.ShortTextQuestion
import com.landoulsi.survey.model.SingleChoiceQuestion
import com.landoulsi.survey.model.UnknownQuestion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SurveyParserTest {

    private val parser = SurveyParser()

    @Test
    fun parses_every_question_type() {
        val def = parser.parseOrThrow(SurveyFixtures.FULL_SURVEY_JSON)

        assertEquals("demo", def.id)
        assertEquals("Product feedback", def.title)
        assertEquals("Send it", def.submitLabel)
        assertEquals(SurveyFixtures.SUBMIT_URL, def.submitUrl)
        assertEquals(6, def.questions.size)

        assertIs<RatingQuestion>(def.questions[0]).also {
            assertEquals(10, it.max)
            assertTrue(it.required)
        }
        assertIs<SingleChoiceQuestion>(def.questions[1]).also {
            assertEquals(3, it.options.size)
            assertEquals("Developer", it.options.first().label)
        }
        assertIs<MultiChoiceQuestion>(def.questions[2]).also {
            // label defaults to value when omitted
            assertEquals("ios", it.options.first().label)
        }
        assertIs<BooleanQuestion>(def.questions[3])
        assertIs<ShortTextQuestion>(def.questions[4]).also { assertEquals("Optional", it.placeholder) }
        assertIs<LongTextQuestion>(def.questions[5]).also { assertTrue(it.required) }
    }

    @Test
    fun unknown_question_type_falls_back_without_failing_the_parse() {
        val def = parser.parseOrThrow(SurveyFixtures.UNKNOWN_TYPE_JSON)

        assertEquals(2, def.questions.size)
        assertIs<UnknownQuestion>(def.questions[0]).also {
            assertEquals("hologram", it.originalType)
            assertEquals("q1", it.id)
            assertEquals("Rate the hologram", it.title)
        }
        assertIs<ShortTextQuestion>(def.questions[1])
    }

    @Test
    fun minimal_survey_parses() {
        val def = parser.parseOrThrow(SurveyFixtures.MINIMAL_JSON)
        assertEquals("m", def.id)
        assertTrue(def.questions.isEmpty())
        assertEquals("Submit", def.submitLabel)
        assertNull(def.submitUrl)
    }

    @Test
    fun malformed_json_returns_failure() {
        val result = parser.parse("{ not json")
        assertTrue(result.isFailure)
        assertIs<SurveyParseException>(result.exceptionOrNull())
    }

    @Test
    fun missing_required_field_returns_failure() {
        // no "id"
        val result = parser.parse("""{ "title": "x", "questions": [] }""")
        assertTrue(result.isFailure)
        assertIs<SurveyParseException>(result.exceptionOrNull())
    }

    @Test
    fun unknown_top_level_keys_are_ignored() {
        val json = """{ "id": "a", "title": "b", "questions": [], "theme": "dark", "v": 3 }"""
        val def = parser.parseOrThrow(json)
        assertEquals("a", def.id)
    }
}
