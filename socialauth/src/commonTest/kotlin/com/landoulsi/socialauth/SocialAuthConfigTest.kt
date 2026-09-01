package com.landoulsi.socialauth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SocialAuthConfigTest {

    private val valid = SocialAuthConfig(clientId = "cid", redirectUri = "com.example.app:/cb")

    @Test
    fun validConfigPasses() {
        valid.validate()
    }

    @Test
    fun blankClientIdIsRejected() {
        assertFailsWith<IllegalArgumentException> { valid.copy(clientId = "  ").validate() }
    }

    @Test
    fun blankRedirectUriIsRejected() {
        assertFailsWith<IllegalArgumentException> { valid.copy(redirectUri = "").validate() }
    }

    @Test
    fun redirectUriWithoutASchemeIsRejected() {
        assertFailsWith<IllegalArgumentException> { valid.copy(redirectUri = "oauth2redirect").validate() }
        assertFailsWith<IllegalArgumentException> { valid.copy(redirectUri = ":/cb").validate() }
        // Both a custom scheme and an https App Link are accepted.
        valid.copy(redirectUri = "com.example.app:/oauth2redirect").validate()
        valid.copy(redirectUri = "https://example.com/oauth2redirect").validate()
    }

    @Test
    fun redirectUriWithAFragmentOrUserInfoIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            valid.copy(redirectUri = "com.example.app:/cb#section").validate()
        }
        assertFailsWith<IllegalArgumentException> {
            valid.copy(redirectUri = "https://user:pass@example.com/oauth2redirect").validate()
        }
    }

    @Test
    fun dangerousRedirectUriSchemesAreRejected() {
        for (uri in listOf(
            "javascript:alert(1)", "data:text/html,x", "file:///etc/passwd", "content://x/y",
            "intent://x/#Intent;package=com.evil;end",
        )) {
            assertFailsWith<IllegalArgumentException>("expected $uri to be rejected") {
                valid.copy(redirectUri = uri).validate()
            }
        }
    }

    @Test
    fun cleartextHttpRedirectUriIsRejectedUnlessLoopback() {
        // RFC 8252 §7.1 / §8.1: a web redirect on mobile must be https.
        assertFailsWith<IllegalArgumentException> {
            valid.copy(redirectUri = "http://example.com/oauth2redirect").validate()
        }
        // Loopback http is fine for desktop / local testing.
        valid.copy(redirectUri = "http://localhost:8080/cb").validate()
        valid.copy(redirectUri = "http://127.0.0.1/cb").validate()
        // A user-info segment must not let a non-loopback host masquerade as loopback.
        assertFailsWith<IllegalArgumentException> {
            valid.copy(redirectUri = "http://localhost:80@attacker.com/oauth2redirect").validate()
        }
    }

    @Test
    fun blankAuthorizationEndpointIsRejected() {
        assertFailsWith<IllegalArgumentException> { valid.copy(authorizationEndpoint = "").validate() }
    }

    @Test
    fun blankTokenEndpointIsRejected() {
        assertFailsWith<IllegalArgumentException> { valid.copy(tokenEndpoint = " ").validate() }
    }

    @Test
    fun emptyScopesIsRejected() {
        assertFailsWith<IllegalArgumentException> { valid.copy(scopes = emptyList()).validate() }
    }

    @Test
    fun endpointWithAFragmentIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            valid.copy(authorizationEndpoint = "https://accounts.example.com/authorize#x").validate()
        }
    }

    @Test
    fun cleartextEndpointIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            valid.copy(authorizationEndpoint = "http://accounts.example.com/authorize").validate()
        }
        assertFailsWith<IllegalArgumentException> {
            valid.copy(tokenEndpoint = "http://example.com/token").validate()
        }
    }

    @Test
    fun localhostHttpIsAllowedForTests() {
        valid.copy(
            authorizationEndpoint = "http://localhost:8080/authorize",
            tokenEndpoint = "http://127.0.0.1:8080/token",
        ).validate()
        valid.copy(authorizationEndpoint = "http://localhost").validate()
    }

    @Test
    fun lookalikeLoopbackHostIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            valid.copy(tokenEndpoint = "http://localhost.attacker.com/token").validate()
        }
        assertFailsWith<IllegalArgumentException> {
            valid.copy(tokenEndpoint = "http://127.0.0.1.attacker.com/token").validate()
        }
    }

    @Test
    fun userInfoInEndpointIsRejected() {
        // http://localhost:80@evil.com parses to host=evil.com, user-info=localhost:80.
        assertFailsWith<IllegalArgumentException> {
            valid.copy(tokenEndpoint = "http://localhost:80@evil.com/token").validate()
        }
        assertFailsWith<IllegalArgumentException> {
            valid.copy(authorizationEndpoint = "https://user:pass@accounts.example.com/authorize").validate()
        }
    }

    @Test
    fun publicClientWithoutPkceIsRejected() {
        assertFailsWith<IllegalArgumentException> { valid.copy(usePkce = false).validate() }
        // A confidential client (has a secret) may disable PKCE.
        valid.copy(usePkce = false, clientSecret = "shh").validate()
    }

    @Test
    fun googleFactorySetsTheIssuer() {
        assertEquals(
            "https://accounts.google.com",
            SocialAuthConfig.google(clientId = "c", redirectUri = "app:/cb").issuer,
        )
    }
}
