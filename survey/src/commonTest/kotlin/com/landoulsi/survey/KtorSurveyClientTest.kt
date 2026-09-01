package com.landoulsi.survey

import com.landoulsi.survey.model.SurveyAnswer
import com.landoulsi.survey.model.SurveyResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class KtorSurveyClientTest {

    private fun clientReturning(
        status: HttpStatusCode,
        body: String = "",
        record: ((HttpRequestData) -> Unit)? = null,
    ) = KtorSurveyClient(
        HttpClient(
            MockEngine { request ->
                record?.invoke(request)
                respond(
                    content = ByteReadChannel(body),
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ),
    )

    @Test
    fun fetchDefinition_parses_the_response_body() = runTest {
        val c = clientReturning(HttpStatusCode.OK, SurveyFixtures.FULL_SURVEY_JSON)

        val def = c.fetchDefinition("https://api.example.com/surveys/demo")

        assertEquals("demo", def.id)
        assertEquals(6, def.questions.size)
    }

    @Test
    fun fetchDefinition_maps_a_non_2xx_to_SurveyServerException() = runTest {
        val c = clientReturning(HttpStatusCode.NotFound, "nope")

        val e = assertFailsWith<SurveyServerException> { c.fetchDefinition("https://x") }
        assertEquals(404, e.status)
    }

    @Test
    fun fetchDefinition_maps_malformed_body_to_SurveyParseException() = runTest {
        val c = clientReturning(HttpStatusCode.OK, "{ not a survey")

        assertFailsWith<SurveyParseException> { c.fetchDefinition("https://x") }
    }

    @Test
    fun submit_posts_the_response_as_json() = runTest {
        var seen: HttpRequestData? = null
        val c = clientReturning(HttpStatusCode.Accepted, record = { seen = it })

        val response = SurveyResponse(
            surveyId = "demo",
            answers = listOf(SurveyAnswer("nps", listOf("9"))),
            submittedAtMillis = 42L,
        )
        c.submit(SurveyFixtures.SUBMIT_URL, response)

        val request = requireNotNull(seen)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(SurveyFixtures.SUBMIT_URL, request.url.toString())
        val body = (request.body as TextContent).text
        assertTrue("\"surveyId\":\"demo\"" in body, body)
        assertTrue("\"submittedAtMillis\":42" in body, body)
    }

    @Test
    fun submit_maps_a_transport_failure_to_SurveyNetworkException() = runTest {
        val c = KtorSurveyClient(
            HttpClient(MockEngine { _ -> throw RuntimeException("socket closed") }),
        )

        assertFailsWith<SurveyNetworkException> {
            c.submit("https://x", SurveyResponse("demo", emptyList(), 0L))
        }
    }
}
