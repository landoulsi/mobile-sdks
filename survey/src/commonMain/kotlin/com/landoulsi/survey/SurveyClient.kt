package com.landoulsi.survey

import com.landoulsi.survey.internal.surveyJson
import com.landoulsi.survey.model.SurveyDefinition
import com.landoulsi.survey.model.SurveyResponse
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/**
 * Talks to the survey backend: pulls a [SurveyDefinition] and pushes a [SurveyResponse].
 *
 * Obtain one from [SurveyClientFactory]. The client owns its HTTP engine unless you pass
 * your own to the factory; [close] releases whatever it owns.
 */
interface SurveyClient : AutoCloseable {

    /**
     * GETs [url] and parses the body as a [SurveyDefinition].
     *
     * @throws SurveyNetworkException on a transport failure.
     * @throws SurveyServerException on a non-2xx response.
     * @throws SurveyParseException if the body is not a valid survey document.
     */
    suspend fun fetchDefinition(url: String): SurveyDefinition

    /**
     * POSTs [response] to [url] as JSON.
     *
     * @throws SurveyNetworkException on a transport failure.
     * @throws SurveyServerException on a non-2xx response.
     */
    suspend fun submit(url: String, response: SurveyResponse)
}

/** A survey request could not be completed because of a transport-level failure. */
class SurveyNetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** The survey server answered with a non-2xx status. */
class SurveyServerException(val status: Int, message: String) : Exception(message)

/**
 * Factory for [SurveyClient].
 *
 * @param httpClient an HTTP client to reuse. When null a platform-default engine (OkHttp on
 *   Android, Darwin on iOS) is created and owned by the returned client. A client you pass in
 *   is left untouched by [SurveyClient.close] — you keep its lifecycle.
 */
object SurveyClientFactory {
    fun create(httpClient: HttpClient? = null): SurveyClient = KtorSurveyClient(
        httpClient = httpClient ?: defaultSurveyHttpClient(),
        ownsHttpClient = httpClient == null,
    )
}

/** Platform-default Ktor client (OkHttp on Android, Darwin on iOS). */
internal expect fun defaultSurveyHttpClient(): HttpClient

private const val CONNECT_TIMEOUT_MILLIS = 15_000L
private const val REQUEST_TIMEOUT_MILLIS = 30_000L
private const val SOCKET_TIMEOUT_MILLIS = 30_000L

/** Shared config applied to the platform-default engines. */
internal fun HttpClientConfig<*>.surveyHttpDefaults() {
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
    }
}

/** Ktor-backed [SurveyClient]. Reads bodies as text so parsing/errors go through one path. */
internal class KtorSurveyClient(
    private val httpClient: HttpClient,
    private val ownsHttpClient: Boolean = true,
    private val parser: SurveyParser = SurveyParser(),
) : SurveyClient {

    override suspend fun fetchDefinition(url: String): SurveyDefinition {
        val body = call("load") { httpClient.get(url) }
        return parser.parseOrThrow(body)
    }

    override suspend fun submit(url: String, response: SurveyResponse) {
        call("submit") {
            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(surveyJson.encodeToString(SurveyResponse.serializer(), response))
            }
        }
    }

    override fun close() {
        if (ownsHttpClient) httpClient.close()
    }

    private suspend inline fun call(verb: String, request: () -> HttpResponse): String {
        val response = try {
            request()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SurveyServerException) {
            throw e
        } catch (e: Exception) {
            throw SurveyNetworkException("Survey $verb request failed: ${e.message}", e)
        }
        val body = runCatching { response.bodyAsText() }.getOrDefault("")
        if (!response.status.isSuccess()) {
            throw SurveyServerException(
                response.status.value,
                "Survey $verb failed: HTTP ${response.status.value}",
            )
        }
        return body
    }
}
