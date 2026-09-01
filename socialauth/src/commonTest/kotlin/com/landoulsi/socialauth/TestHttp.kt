package com.landoulsi.socialauth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/**
 * A [MockEngine]-backed [HttpClient] that records every request and replies with a
 * fixed JSON body (optionally per call count).
 */
class FakeHttp(
    private val responses: List<Pair<HttpStatusCode, String>>,
) {
    constructor(status: HttpStatusCode, body: String) : this(listOf(status to body))

    val formFields = mutableListOf<Map<String, String>>()
    val urls = mutableListOf<String>()
    var callCount = 0
        private set

    val client: HttpClient = HttpClient(MockEngine) {
        engine {
            addHandler { request ->
                urls += request.url.toString()
                (request.body as? FormDataContent)?.let { form ->
                    formFields += form.formData.entries().associate { it.key to it.value.first() }
                }
                val (status, body) = responses[minOf(callCount, responses.lastIndex)]
                callCount++
                respond(
                    content = body,
                    status = status,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
    }
}
