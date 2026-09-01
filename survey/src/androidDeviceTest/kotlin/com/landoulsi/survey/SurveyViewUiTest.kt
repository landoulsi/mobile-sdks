package com.landoulsi.survey

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.landoulsi.survey.compose.Survey
import com.landoulsi.survey.testing.FakeSurveyClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented UI test for the `Survey()` composable end to end: render from JSON, block on a
 * missing required answer, then collect input and submit through a [FakeSurveyClient].
 */
@RunWith(AndroidJUnit4::class)
class SurveyViewUiTest {

    @get:Rule
    val compose = createComposeRule()

    private val surveyJson = """
        {
          "id": "quick",
          "title": "Quick check",
          "submitLabel": "Submit",
          "submitUrl": "https://api.example.com/quick/responses",
          "questions": [
            {
              "type": "singleChoice",
              "id": "enjoy",
              "title": "Enjoying the app?",
              "required": true,
              "options": [ { "value": "yes", "label": "Yes" }, { "value": "no", "label": "No" } ]
            },
            { "type": "shortText", "id": "handle", "title": "Your handle" }
          ]
        }
    """.trimIndent()

    private fun start(fake: FakeSurveyClient): SurveyController {
        val controller = SurveyController(fake, ownsClient = false)
        compose.setContent {
            MaterialTheme { Survey(controller) }
        }
        controller.loadFromJson(surveyJson)
        compose.waitUntil(5_000) { controller.state.value is SurveyState.Ready }
        compose.waitForIdle()
        return controller
    }

    @Test
    fun renders_the_survey_from_json() {
        start(FakeSurveyClient())

        compose.onNodeWithText("Quick check").assertIsDisplayed()
        compose.onNodeWithText("Enjoying the app? *").assertIsDisplayed()
        compose.onNodeWithText("Yes").assertIsDisplayed()
        compose.onNodeWithText("Submit").assertIsDisplayed()
    }

    @Test
    fun blocks_submit_until_required_question_is_answered() {
        val fake = FakeSurveyClient()
        start(fake)

        compose.onNodeWithText("Submit").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("This question is required.").assertIsDisplayed()
        assertTrue(fake.submissions.isEmpty())
    }

    @Test
    fun collects_answers_and_submits_to_the_server() {
        val fake = FakeSurveyClient()
        val controller = start(fake)

        compose.onNodeWithText("Yes").performClick()
        // The only editable field in the fixture — its label/placeholder are empty, and
        // "Your handle" is a separate (non-editable) heading node.
        compose.onNode(hasSetTextAction()).performTextInput("ahmed")
        compose.waitForIdle()

        compose.onNodeWithText("Submit").performClick()
        compose.waitUntil(5_000) { controller.state.value is SurveyState.Submitted }

        val submission = fake.submissions.single()
        val answers = submission.response.answers.associate { it.questionId to it.values }
        assertEquals(listOf("yes"), answers["enjoy"])
        assertEquals(listOf("ahmed"), answers["handle"])

        compose.onNodeWithText("Thanks!").assertIsDisplayed()
    }
}
