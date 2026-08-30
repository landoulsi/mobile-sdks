package com.landoulsi.screenshot.network

import com.landoulsi.screenshot.config.RetryPolicy
import com.landoulsi.screenshot.config.ServerConfig
import com.landoulsi.screenshot.model.ScreenshotPayload
import com.landoulsi.screenshot.model.UploadResponse
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Standard implementation of [ScreenshotUploader] using Ktor HTTP Client.
 */
class KtorScreenshotUploader(
    private val httpClient: HttpClient = createDefaultHttpClient()
) : ScreenshotUploader {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    override suspend fun upload(
        payload: ScreenshotPayload,
        serverConfig: ServerConfig
    ): Result<UploadResponse> = runCatching {
        val metadataJsonString = json.encodeToString(payload.metadata)

        val multipartBody = MultiPartFormDataContent(
            formData {
                // 1. Image binary part
                append(
                    key = serverConfig.fileFieldName,
                    value = payload.image.bytes,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, payload.image.format.mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"${payload.image.fileName}\"")
                    }
                )

                // 2. Metadata part
                append(
                    key = serverConfig.metadataFieldName,
                    value = metadataJsonString,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, "application/json")
                    }
                )

                // 3. Any additional configured static form fields
                serverConfig.additionalFields.forEach { (key, value) ->
                    append(key, value)
                }
            }
        )

        executeWithRetry(serverConfig.retryPolicy) {
            val response = httpClient.request(serverConfig.endpointUrl) {
                this.method = HttpMethod.parse(serverConfig.method.uppercase())
                this.setBody(multipartBody)

                // Attach custom headers
                serverConfig.headers.forEach { (name, value) ->
                    header(name, value)
                }

                // Attach Auth token if provided
                serverConfig.authToken?.let { token ->
                    if (!serverConfig.headers.containsKey(HttpHeaders.Authorization)) {
                        header(HttpHeaders.Authorization, if (token.startsWith("Bearer ", ignoreCase = true)) token else "Bearer $token")
                    }
                }
            }

            val statusCode = response.status.value
            val responseBody = response.bodyAsText()

            if (response.status.isSuccess()) {
                UploadResponse(
                    isSuccess = true,
                    statusCode = statusCode,
                    responseBody = responseBody
                )
            } else {
                val errorMsg = "Server responded with HTTP $statusCode: $responseBody"
                if (statusCode in 500..599 || statusCode == 429) {
                    // Throw to trigger retry on transient server errors
                    throw IllegalStateException(errorMsg)
                }
                UploadResponse(
                    isSuccess = false,
                    statusCode = statusCode,
                    responseBody = responseBody,
                    errorMessage = errorMsg
                )
            }
        }
    }

    private suspend fun <T> executeWithRetry(
        policy: RetryPolicy,
        block: suspend () -> T
    ): T {
        var currentAttempt = 0
        var currentBackoff = policy.initialBackoffMillis

        while (true) {
            try {
                return block()
            } catch (e: Throwable) {
                currentAttempt++
                if (currentAttempt > policy.maxRetries) {
                    throw e
                }
                delay(currentBackoff)
                currentBackoff = (currentBackoff * policy.backoffMultiplier).toLong()
            }
        }
    }

    companion object {
        fun createDefaultHttpClient(): HttpClient {
            return HttpClient {
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000L
                    connectTimeoutMillis = 15_000L
                    socketTimeoutMillis = 30_000L
                }
            }
        }
    }
}
