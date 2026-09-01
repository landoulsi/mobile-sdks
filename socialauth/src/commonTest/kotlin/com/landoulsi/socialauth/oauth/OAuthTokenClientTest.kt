package com.landoulsi.socialauth.oauth

import com.landoulsi.socialauth.FakeHttp
import com.landoulsi.socialauth.SocialAuthConfig
import com.landoulsi.socialauth.model.AuthError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OAuthTokenClientTest {

    private val config = SocialAuthConfig(
        clientId = "cid",
        redirectUri = "com.example.app:/cb",
    )

    private fun successBody(refresh: String? = "rt-1"): String {
        val idToken = testJwt(mapOf("sub" to "uid-1", "email" to "e@e.com"))
        val refreshLine = refresh?.let { "\"refresh_token\":\"$it\"," } ?: ""
        return """{"access_token":"at-1",$refreshLine"id_token":"$idToken","expires_in":3599}"""
    }

    @Test
    fun exchangeSendsCorrectFormAndParsesResponse() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, successBody())
        val client = OAuthTokenClient(http.client)

        val response = client.exchangeAuthorizationCode(config, code = "auth-code", codeVerifier = "verifier-1")

        assertEquals("at-1", response.accessToken)
        assertEquals("rt-1", response.refreshToken)
        assertEquals("uid-1", response.uid)

        val form = http.formFields.single()
        assertEquals("authorization_code", form["grant_type"])
        assertEquals("auth-code", form["code"])
        assertEquals("cid", form["client_id"])
        assertEquals("com.example.app:/cb", form["redirect_uri"])
        assertEquals("verifier-1", form["code_verifier"])
        assertTrue("client_secret" !in form, "public client must not send a secret")
    }

    @Test
    fun refreshSendsRefreshGrant() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, successBody(refresh = null))
        val client = OAuthTokenClient(http.client)

        val response = client.refreshAccessToken(config, refreshToken = "rt-existing")

        assertEquals("at-1", response.accessToken)
        val form = http.formFields.single()
        assertEquals("refresh_token", form["grant_type"])
        assertEquals("rt-existing", form["refresh_token"])
    }

    @Test
    fun refreshAcceptsBareResponseWithoutIdentity() = runTest {
        // Real providers return only access_token/expires_in on a refresh grant.
        val http = FakeHttp(HttpStatusCode.OK, """{"access_token":"at-2","expires_in":3600,"token_type":"Bearer"}""")
        val response = OAuthTokenClient(http.client).refreshAccessToken(config, "rt")
        assertEquals("at-2", response.accessToken)
    }

    @Test
    fun includesClientSecretWhenConfigured() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, successBody())
        val confidential = config.copy(clientSecret = "shh")
        OAuthTokenClient(http.client).exchangeAuthorizationCode(confidential, "c", null)
        assertEquals("shh", http.formFields.single()["client_secret"])
    }

    @Test
    fun rfcErrorBodyBecomesTypedException() = runTest {
        val http = FakeHttp(HttpStatusCode.BadRequest, """{"error":"invalid_grant","error_description":"expired"}""")
        val exception = assertFailsWith<OAuthException> {
            OAuthTokenClient(http.client).exchangeAuthorizationCode(config, "c", "v")
        }
        assertEquals(AuthError.TOKEN_EXCHANGE_FAILED, exception.error)
        assertTrue(exception.message.contains("invalid_grant"))
        assertTrue(exception.message.contains("expired"))
    }

    @Test
    fun refreshErrorUsesRefreshFailedCategory() = runTest {
        val http = FakeHttp(HttpStatusCode.BadRequest, """{"error":"invalid_grant"}""")
        val exception = assertFailsWith<OAuthException> {
            OAuthTokenClient(http.client).refreshAccessToken(config, "rt")
        }
        assertEquals(AuthError.TOKEN_REFRESH_FAILED, exception.error)
    }

    @Test
    fun unrecognizedBodyBecomesException() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, "gateway timeout, plain text")
        val exception = assertFailsWith<OAuthException> {
            OAuthTokenClient(http.client).exchangeAuthorizationCode(config, "c", "v")
        }
        assertEquals(AuthError.TOKEN_EXCHANGE_FAILED, exception.error)
    }

    @Test
    fun cancellationPropagatesAndIsNotWrappedAsNetworkError() = runTest {
        val slowClient = HttpClient(MockEngine) {
            engine {
                addHandler {
                    delay(10_000)
                    respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
        }
        // withTimeout cancels the in-flight request; the CancellationException must
        // pass through, not be caught and re-thrown as OAuthException(NETWORK).
        assertFailsWith<TimeoutCancellationException> {
            withTimeout(100) {
                OAuthTokenClient(slowClient).exchangeAuthorizationCode(config, "c", "v")
            }
        }
    }
}
