package com.landoulsi.socialauth.oauth

import com.landoulsi.socialauth.SocialAuthConfig
import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthorizationUrlTest {

    private val config = SocialAuthConfig(
        clientId = "client-123",
        redirectUri = "com.example.app:/oauth2redirect",
        scopes = listOf("openid", "email", "profile"),
    )

    @Test
    fun buildsExpectedQueryWithPkce() {
        val pkce = PkceCodes("verifier", "challenge-value")
        val url = Url(AuthorizationUrl.build(config, state = "st-1", pkce = pkce, nonce = "n-1"))

        assertEquals("accounts.google.com", url.host)
        assertEquals("client-123", url.parameters["client_id"])
        assertEquals("com.example.app:/oauth2redirect", url.parameters["redirect_uri"])
        assertEquals("code", url.parameters["response_type"])
        assertEquals("openid email profile", url.parameters["scope"])
        assertEquals("st-1", url.parameters["state"])
        assertEquals("n-1", url.parameters["nonce"])
        assertEquals("challenge-value", url.parameters["code_challenge"])
        assertEquals("S256", url.parameters["code_challenge_method"])
        assertEquals("offline", url.parameters["access_type"])
        assertEquals("consent", url.parameters["prompt"])
    }

    @Test
    fun omitsPkceParamsWhenNull() {
        val url = Url(AuthorizationUrl.build(config, state = "s", pkce = null, nonce = null))
        assertNull(url.parameters["code_challenge"])
        assertNull(url.parameters["code_challenge_method"])
    }

    @Test
    fun customAuthParamsDoNotOverrideCoreParams() {
        val custom = config.copy(additionalAuthParams = mapOf("state" to "hacked", "login_hint" to "a@b.com"))
        val url = Url(AuthorizationUrl.build(custom, state = "real-state", pkce = null, nonce = null))
        assertEquals("real-state", url.parameters["state"])
        assertEquals("a@b.com", url.parameters["login_hint"])
    }

    @Test
    fun encodesReservedCharactersInValues() {
        val custom = config.copy(scopes = listOf("scope with space", "a/b"))
        val raw = AuthorizationUrl.build(custom, state = "s", pkce = null, nonce = null)
        // raw string must not contain a literal space in the query
        assertTrue(!raw.substringAfter('?').contains(' '))
        assertEquals("scope with space a/b", Url(raw).parameters["scope"])
    }
}
