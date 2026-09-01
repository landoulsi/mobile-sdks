package com.landoulsi.socialauth.oauth

import com.landoulsi.socialauth.SocialAuthConfig
import com.landoulsi.socialauth.model.AuthError
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ParametersBuilder
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException

/**
 * Thrown when a token-endpoint call fails. Carries a coarse [error] category, the raw
 * RFC 6749 [oauthErrorCode] when the body had one (e.g. `invalid_grant`), and a
 * log-safe [message]; token material is never included.
 */
internal open class OAuthException(
    val error: AuthError,
    override val message: String,
    val oauthErrorCode: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * The `sub` of an id_token returned on a refresh differs from the session's — a possible
 * token-substitution signal. Distinct type (not a synthetic [OAuthException.oauthErrorCode])
 * so the caller can wipe the session without string-matching an RFC protocol field.
 */
internal class SubjectChangedException :
    OAuthException(AuthError.TOKEN_REFRESH_FAILED, "id_token sub changed across refresh")

/** The token-endpoint operations [DefaultSocialAuthClient] depends on. */
internal interface TokenEndpointClient : AutoCloseable {
    suspend fun exchangeAuthorizationCode(
        config: SocialAuthConfig,
        code: String,
        codeVerifier: String?,
    ): OAuthTokenResponse

    suspend fun refreshAccessToken(config: SocialAuthConfig, refreshToken: String): OAuthTokenResponse
}

/**
 * Ktor-backed [TokenEndpointClient]: authorization-code exchange and refresh.
 *
 * Bodies are read as text and handed to [OAuthTokenParser] so both success and
 * RFC 6749 error responses can be surfaced with a useful message.
 */
internal class OAuthTokenClient(
    private val httpClient: HttpClient,
    private val parser: OAuthTokenParser = OAuthTokenParser(),
    /** When false, [close] leaves [httpClient] alone (the caller owns its lifecycle). */
    private val ownsHttpClient: Boolean = true,
) : TokenEndpointClient {

    override fun close() {
        if (ownsHttpClient) httpClient.close()
    }

    /** Exchanges an authorization [code] (+ PKCE [codeVerifier]) for tokens. */
    override suspend fun exchangeAuthorizationCode(
        config: SocialAuthConfig,
        code: String,
        codeVerifier: String?,
    ): OAuthTokenResponse = post(
        endpoint = config.tokenEndpoint,
        failure = AuthError.TOKEN_EXCHANGE_FAILED,
        form = {
            append("grant_type", "authorization_code")
            append("code", code)
            append("client_id", config.clientId)
            append("redirect_uri", config.redirectUri)
            config.clientSecret?.let { append("client_secret", it) }
            codeVerifier?.let { append("code_verifier", it) }
        },
    )

    /** Mints a fresh access token from a [refreshToken]. */
    override suspend fun refreshAccessToken(
        config: SocialAuthConfig,
        refreshToken: String,
    ): OAuthTokenResponse = post(
        endpoint = config.tokenEndpoint,
        failure = AuthError.TOKEN_REFRESH_FAILED,
        form = {
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
            append("client_id", config.clientId)
            config.clientSecret?.let { append("client_secret", it) }
        },
    )

    private suspend fun post(
        endpoint: String,
        failure: AuthError,
        form: ParametersBuilder.() -> Unit,
    ): OAuthTokenResponse {
        val (status, body) = try {
            val response = httpClient.submitForm(url = endpoint, formParameters = parameters(form)) {
                // A consumer-supplied client may set expectSuccess = true globally; a 4xx from
                // the token endpoint carries the RFC 6749 error body we need to read.
                expectSuccess = false
            }
            response.status to response.bodyAsText() // body streaming can fail too — keep it inside
        } catch (e: CancellationException) {
            throw e
        } catch (e: OAuthException) {
            throw e
        } catch (e: IllegalStateException) {
            // e.g. the HttpClient was already closed — not a retryable network problem.
            throw OAuthException(AuthError.UNKNOWN, "Token endpoint call could not be made: ${e.message}", cause = e)
        } catch (e: Exception) {
            throw OAuthException(AuthError.NETWORK, "Token endpoint request failed: ${e.message}", cause = e)
        }

        if (status.isSuccess()) {
            parser.parseTokenResponse(body)?.let { return it }
        }

        val errorResponse = parser.parseErrorResponse(body)
        val detail = when {
            errorResponse?.errorDescription != null -> "${errorResponse.error}: ${errorResponse.errorDescription}"
            errorResponse != null -> errorResponse.error
            !status.isSuccess() -> "HTTP ${status.value}: unrecognized token endpoint response"
            else -> "unrecognized token endpoint response"
        }
        throw OAuthException(failure, detail, oauthErrorCode = errorResponse?.error)
    }
}
