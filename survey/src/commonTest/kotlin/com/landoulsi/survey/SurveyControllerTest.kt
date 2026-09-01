package com.landoulsi.survey

import com.landoulsi.schemaui.ir.UIButton
import com.landoulsi.schemaui.ir.UIButtonStyle
import com.landoulsi.survey.internal.decodeValueList
import com.landoulsi.survey.testing.FakeSurveyClient
import com.landoulsi.timeprovider.FakeTimeProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * All coroutine work runs on an [UnconfinedTestDispatcher], so the controller's
 * fire-and-forget `launch`es (server fetch, submit, reactive rebuild) execute eagerly and
 * these tests can assert straight after the call with no scheduler pumping.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SurveyControllerTest {

    private val time = FakeTimeProvider(initialMillis = 1_756_704_000_000L)
    private val scope = CoroutineScope(UnconfinedTestDispatcher())

    private fun controller(fake: FakeSurveyClient = FakeSurveyClient()) =
        SurveyController(fake, time, scope, ownsClient = false)

    @AfterTest
    fun tearDown() = scope.cancel()

    private fun SurveyController.ready() = assertIs<SurveyState.Ready>(state.value)

    // ─── Loading ────────────────────────────────────────────────────────────

    @Test
    fun loadFromJson_makes_state_ready_with_a_rendered_tree() {
        val controller = controller()

        controller.loadFromJson(SurveyFixtures.FULL_SURVEY_JSON)

        val ready = controller.ready()
        assertEquals("demo", ready.definition.id)
        assertTrue(ready.node.flatten().isNotEmpty())
    }

    @Test
    fun parse_failure_sets_load_error() {
        val controller = controller()

        controller.loadFromJson("{ broken")

        assertIs<SurveyState.LoadError>(controller.state.value)
    }

    @Test
    fun loadFromServer_fetches_via_client_then_becomes_ready() {
        val fake = FakeSurveyClient(definition = SurveyParser().parseOrThrow(SurveyFixtures.FULL_SURVEY_JSON))
        val controller = controller(fake)

        controller.loadFromServer("https://api.example.com/surveys/demo")

        assertEquals(listOf("https://api.example.com/surveys/demo"), fake.fetchedUrls)
        assertIs<SurveyState.Ready>(controller.state.value)
    }

    @Test
    fun loadFromServer_failure_sets_load_error() {
        val fake = FakeSurveyClient(fetchError = SurveyNetworkException("offline"))
        val controller = controller(fake)

        controller.loadFromServer("https://x")

        assertEquals("offline", assertIs<SurveyState.LoadError>(controller.state.value).message)
    }

    // ─── Answering ──────────────────────────────────────────────────────────

    @Test
    fun choosing_an_option_writes_to_the_state_store_and_restyles_the_tree() {
        val controller = controller()
        controller.loadFromJson(SurveyFixtures.FULL_SURVEY_JSON)

        controller.engine.triggerAction(SurveyActions.option("role", "dev"))

        assertEquals("dev", controller.engine.stateStore.get("role"))
        val devButton = controller.ready().node.flatten()
            .filterIsInstance<UIButton>()
            .single { it.action == SurveyActions.option("role", "dev") }
        assertEquals(UIButtonStyle.Filled, devButton.style)
    }

    @Test
    fun multi_choice_action_toggles_membership() {
        val controller = controller()
        controller.loadFromJson(SurveyFixtures.FULL_SURVEY_JSON)

        val ios = SurveyActions.option("channels", "ios")
        controller.engine.triggerAction(ios)
        controller.engine.triggerAction(SurveyActions.option("channels", "web"))
        assertEquals(setOf("ios", "web"), decodeValueList(controller.engine.stateStore.get("channels")).toSet())

        controller.engine.triggerAction(ios) // toggle off
        assertEquals(listOf("web"), decodeValueList(controller.engine.stateStore.get("channels")))
    }

    // ─── Submitting ─────────────────────────────────────────────────────────

    @Test
    fun submit_without_required_answers_reports_validation_errors_and_does_not_call_client() {
        val fake = FakeSurveyClient()
        val controller = controller(fake)
        controller.loadFromJson(SurveyFixtures.FULL_SURVEY_JSON)

        controller.submit()

        assertEquals(setOf("nps", "role", "why"), controller.ready().validationErrors.keys)
        assertTrue(fake.submissions.isEmpty())
    }

    @Test
    fun submit_with_all_required_answers_posts_the_expected_response() {
        val fake = FakeSurveyClient()
        val controller = controller(fake)
        controller.loadFromJson(SurveyFixtures.FULL_SURVEY_JSON)

        controller.engine.triggerAction(SurveyActions.option("nps", "9"))
        controller.engine.triggerAction(SurveyActions.option("role", "dev"))
        controller.engine.triggerAction(SurveyActions.option("channels", "ios"))
        controller.engine.stateStore.set("why", "Great, faster cold start please")

        controller.submit()

        val submitted = assertIs<SurveyState.Submitted>(controller.state.value)
        val submission = fake.submissions.single()
        assertEquals(SurveyFixtures.SUBMIT_URL, submission.url)

        val r = submission.response
        assertEquals("demo", r.surveyId)
        assertEquals(1_756_704_000_000L, r.submittedAtMillis)
        assertEquals(
            mapOf(
                "nps" to listOf("9"),
                "role" to listOf("dev"),
                "channels" to listOf("ios"),
                "why" to listOf("Great, faster cold start please"),
            ),
            r.answers.associate { it.questionId to it.values },
        )
        assertEquals(submitted.response, r)
    }

    @Test
    fun submit_prefers_an_explicit_url_over_the_definition_url() {
        val fake = FakeSurveyClient()
        val controller = controller(fake)
        controller.loadFromJson(SurveyFixtures.MINIMAL_JSON) // no submitUrl, no questions

        controller.submit(url = "https://override.example/r")

        assertEquals("https://override.example/r", fake.submissions.single().url)
        assertIs<SurveyState.Submitted>(controller.state.value)
    }

    @Test
    fun submit_without_any_url_reports_a_submit_error() {
        val controller = controller()
        controller.loadFromJson(SurveyFixtures.MINIMAL_JSON)

        controller.submit()

        assertTrue(controller.ready().submitError!!.contains("submit URL", ignoreCase = true))
    }

    @Test
    fun submit_failure_keeps_the_survey_ready_with_an_error() {
        val fake = FakeSurveyClient(submitError = SurveyServerException(503, "Survey submit failed: HTTP 503"))
        val controller = controller(fake)
        controller.loadFromJson(SurveyFixtures.MINIMAL_JSON)

        controller.submit(url = "https://x")

        val ready = controller.ready()
        assertEquals(false, ready.submitting)
        assertTrue(ready.submitError!!.contains("503"))
    }

    @Test
    fun a_second_submit_while_one_is_in_flight_is_ignored() {
        val gate = CompletableDeferred<Unit>()
        val fake = FakeSurveyClient().apply { submitGate = gate }
        val controller = controller(fake)
        controller.loadFromJson(SurveyFixtures.MINIMAL_JSON)

        controller.submit(url = "https://x")
        assertTrue(controller.ready().submitting) // suspended on the gate

        controller.submit(url = "https://x") // must be a no-op
        gate.complete(Unit)

        assertEquals(1, fake.submissions.size)
        assertIs<SurveyState.Submitted>(controller.state.value)
    }

    @Test
    fun answering_a_flagged_question_clears_its_validation_error() {
        val controller = controller()
        controller.loadFromJson(SurveyFixtures.FULL_SURVEY_JSON)

        controller.submit()
        assertTrue("role" in controller.ready().validationErrors)

        controller.engine.triggerAction(SurveyActions.option("role", "pm"))
        assertTrue("role" !in controller.ready().validationErrors)
    }
}
