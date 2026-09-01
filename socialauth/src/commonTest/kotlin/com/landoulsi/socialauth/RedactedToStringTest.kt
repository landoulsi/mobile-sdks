package com.landoulsi.socialauth

import com.landoulsi.socialauth.model.AuthSession
import com.landoulsi.socialauth.model.AuthState
import com.landoulsi.socialauth.model.AuthTokens
import com.landoulsi.socialauth.model.AuthUser
import com.landoulsi.socialauth.model.SocialProvider
import com.landoulsi.socialauth.oauth.GoogleOAuthClient
import com.landoulsi.socialauth.oauth.OAuthTokenResponse
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RedactedToStringTest {

    private val tokens = AuthTokens(
        accessToken = "SECRET-ACCESS",
        refreshToken = "SECRET-REFRESH",
        idToken = "SECRET-ID",
        expiresAtEpochMillis = 42L,
        scopes = listOf("openid"),
    )

    private fun assertNoSecrets(rendered: String) {
        assertFalse("SECRET" in rendered, "leaked a token: $rendered")
    }

    @Test
    fun authTokensToStringHidesTokens() {
        val rendered = tokens.toString()
        assertNoSecrets(rendered)
        assertTrue("expiresAtEpochMillis=42" in rendered)
        assertTrue("refreshToken=***" in rendered)
    }

    @Test
    fun authSessionAndStateDoNotLeakTokens() {
        val session = AuthSession(
            user = AuthUser(uid = "u", provider = SocialProvider.GOOGLE),
            tokens = tokens,
        )
        assertNoSecrets(session.toString())
        assertNoSecrets(AuthState.SignedIn(session).toString())
    }

    @Test
    fun oauthTokenResponseHidesTokens() {
        val response = OAuthTokenResponse(
            accessToken = "SECRET-ACCESS",
            refreshToken = "SECRET-REFRESH",
            idToken = "SECRET-ID",
            uid = "u",
        )
        assertNoSecrets(response.toString())
    }

    @Test
    fun configAndGoogleClientHideTheClientSecret() {
        val config = SocialAuthConfig(
            clientId = "cid",
            redirectUri = "com.example.app:/cb",
            clientSecret = "SECRET-CLIENT",
        )
        assertNoSecrets(config.toString())
        assertTrue("clientSecret=***" in config.toString())

        val googleClient = GoogleOAuthClient(clientId = "cid", clientSecret = "SECRET-CLIENT")
        assertNoSecrets(googleClient.toString())
    }
}
