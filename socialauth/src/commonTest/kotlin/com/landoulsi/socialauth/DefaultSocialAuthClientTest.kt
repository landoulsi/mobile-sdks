package com.landoulsi.socialauth

import com.landoulsi.socialauth.model.AuthError
import com.landoulsi.socialauth.model.AuthResult
import com.landoulsi.socialauth.model.AuthSession
import com.landoulsi.socialauth.model.AuthState
import com.landoulsi.socialauth.oauth.OAuthTokenClient
import com.landoulsi.socialauth.oauth.OAuthTokenResponse
import com.landoulsi.socialauth.oauth.TokenEndpointClient
import com.landoulsi.socialauth.oauth.testJwtRaw
import com.landoulsi.socialauth.testing.FakeAuthorizationCodeProvider
import com.landoulsi.socialauth.testing.FakeSocialAuthClient
import com.landoulsi.timeprovider.FakeTimeProvider
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultSocialAuthClientTest {

    private val config = SocialAuthConfig(
        clientId = "cid",
        redirectUri = "com.example.app:/cb",
    )

    // Deterministic RNG for state/PKCE/nonce: bytes 1..16 → this hex string.
    private val testRandomBytes: (Int) -> ByteArray = { size -> ByteArray(size) { (it + 1).toByte() } }
    private val testStateAndNonce = "0102030405060708090a0b0c0d0e0f10"

    // aud defaults to the config clientId ("cid"); pass aud = null to omit it,
    // idToken = false for no id_token at all, expEpochSeconds = null to omit `exp`
    // (far-future by default), iss / nonce override those claims.
    private fun tokenBody(
        accessToken: String = "at-1",
        refreshToken: String? = "rt-1",
        expiresIn: Long = 3600,
        sub: String = "uid-1",
        aud: String? = "cid",
        idToken: Boolean = true,
        iss: String? = "https://accounts.google.com",
        nonce: String? = testStateAndNonce,
        expEpochSeconds: Long? = 4_000_000_000L,
    ): String {
        val refreshLine = refreshToken?.let { "\"refresh_token\":\"$it\"," } ?: ""
        val idTokenLine = if (idToken) {
            val stringClaims = buildMap {
                put("sub", sub)
                put("email", "e@e.com")
                put("name", "Ada")
                aud?.let { put("aud", it) }
                iss?.let { put("iss", it) }
                nonce?.let { put("nonce", it) }
            }
            val entries = stringClaims.entries.joinToString(",") { (k, v) -> "\"$k\":\"$v\"" }
            // email_verified as a real JSON boolean, so the parser surfaces the email.
            val expEntry = expEpochSeconds?.let { ",\"exp\":$it" } ?: ""
            "\"id_token\":\"${testJwtRaw("{$entries,\"email_verified\":true$expEntry}")}\","
        } else {
            ""
        }
        return """{"access_token":"$accessToken",$refreshLine$idTokenLine"expires_in":$expiresIn}"""
    }

    private fun client(
        http: FakeHttp,
        authorizer: FakeAuthorizationCodeProvider = FakeAuthorizationCodeProvider(),
        store: AuthSessionStore = InMemoryAuthSessionStore(),
        time: FakeTimeProvider = FakeTimeProvider(0L),
        clientConfig: SocialAuthConfig = config,
    ) = DefaultSocialAuthClient(
        config = clientConfig,
        authorizationCodeProvider = authorizer,
        tokenClient = OAuthTokenClient(http.client),
        sessionStore = store,
        timeProvider = time,
        randomBytes = testRandomBytes,
        dispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun signInOnAClosedClientFailsWithoutOpeningTheBrowser() = runTest {
        val authorizer = FakeAuthorizationCodeProvider()
        val auth = client(FakeHttp(HttpStatusCode.OK, tokenBody()), authorizer = authorizer)
        auth.close()

        val failure = assertIs<AuthResult.Failure>(auth.signIn())
        assertEquals(AuthError.UNKNOWN, failure.error)
        assertEquals(0, authorizer.invocations, "a closed client must not launch the authorization step")
        assertIs<AuthResult.Failure>(auth.refreshSession())
        assertNull(auth.currentAccessToken())
    }

    @Test
    fun scopeOmittedFromTokenResponseFallsBackToRequestedScopes() = runTest {
        // RFC 6749 §5.1: providers omit `scope` when it equals what was requested.
        val idToken = testJwtRaw(
            """{"sub":"uid-1","aud":"cid","iss":"https://accounts.google.com","exp":4000000000,"nonce":"$testStateAndNonce"}""",
        )
        val http = FakeHttp(HttpStatusCode.OK, """{"access_token":"at","id_token":"$idToken","expires_in":3600}""")
        val success = assertIs<AuthResult.Success>(client(http).signIn())
        assertEquals(listOf("openid", "email", "profile"), success.session.tokens.scopes)
    }

    @Test
    fun nonPositiveExpiresInMeansImmediatelyExpired() = runTest {
        val idToken = testJwtRaw(
            """{"sub":"uid-1","aud":"cid","iss":"https://accounts.google.com","exp":4000000000,"nonce":"$testStateAndNonce"}""",
        )
        val http = FakeHttp(HttpStatusCode.OK, """{"access_token":"at","refresh_token":"rt","id_token":"$idToken","expires_in":0}""")
        val time = FakeTimeProvider(1_000_000L)
        val success = assertIs<AuthResult.Success>(client(http, time = time).signIn())
        assertEquals(1_000_000L, success.session.tokens.expiresAtEpochMillis)
        assertTrue(success.session.tokens.isExpiredAt(1_000_001L, leewayMillis = 0L), "must not be treated as never-expiring")
    }

    @Test
    fun closeDelegatesToTheTokenClient() = runTest {
        var closed = false
        val fakeTokenClient = object : TokenEndpointClient {
            override suspend fun exchangeAuthorizationCode(config: SocialAuthConfig, code: String, codeVerifier: String?) =
                error("unused")
            override suspend fun refreshAccessToken(config: SocialAuthConfig, refreshToken: String) = error("unused")
            override fun close() { closed = true }
        }
        val auth = DefaultSocialAuthClient(
            config = config,
            authorizationCodeProvider = FakeAuthorizationCodeProvider(),
            tokenClient = fakeTokenClient,
            dispatcher = Dispatchers.Unconfined,
        )
        auth.close()
        assertTrue(closed)
    }

    @Test
    fun fullSignInSucceedsAndPersists() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val store = InMemoryAuthSessionStore()
        val auth = client(http, store = store)

        val result = auth.signIn()

        val success = assertIs<AuthResult.Success>(result)
        assertEquals("uid-1", success.session.user.uid)
        assertEquals("e@e.com", success.session.user.email)
        assertEquals("at-1", success.session.tokens.accessToken)
        assertEquals("rt-1", success.session.tokens.refreshToken)
        assertEquals(3_600_000L, success.session.tokens.expiresAtEpochMillis)
        assertIs<AuthState.SignedIn>(auth.authState.value)
        assertEquals(success.session, store.load())
    }

    @Test
    fun authorizationUrlCarriesPkceAndState() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val authorizer = FakeAuthorizationCodeProvider()
        client(http, authorizer = authorizer).signIn()

        val request = authorizer.lastRequest!!
        assertTrue(request.authorizationUrl.contains("code_challenge="))
        assertTrue(request.authorizationUrl.contains("code_challenge_method=S256"))
        assertTrue(request.authorizationUrl.contains("state=${request.state}"))
        // the PKCE verifier stays inside the SDK but must reach the token endpoint
        assertTrue(http.formFields.single()["code_verifier"]?.isNotBlank() == true)
    }

    @Test
    fun userCancellationYieldsCancelled() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val authorizer = FakeAuthorizationCodeProvider(nextResult = AuthorizationResult.Cancelled)
        val auth = client(http, authorizer = authorizer)

        assertIs<AuthResult.Cancelled>(auth.signIn())
        assertEquals(0, http.callCount, "no token call after cancellation")
        assertIs<AuthState.SignedOut>(auth.authState.value)
    }

    @Test
    fun providerErrorYieldsAuthorizationFailed() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val authorizer = FakeAuthorizationCodeProvider(
            nextResult = AuthorizationResult.Failure(
                AuthorizationError.ProviderReported("access_denied"), "user said no",
            ),
        )
        val result = client(http, authorizer = authorizer).signIn()

        val failure = assertIs<AuthResult.Failure>(result)
        assertEquals(AuthError.AUTHORIZATION_FAILED, failure.error)
        assertTrue(failure.message!!.contains("access_denied"))
    }

    @Test
    fun stateMismatchIsRejected() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val authorizer = FakeAuthorizationCodeProvider(
            respond = { AuthorizationResult.Success(code = "c", state = "not-the-state") },
        )
        val result = client(http, authorizer = authorizer).signIn()

        val failure = assertIs<AuthResult.Failure>(result)
        assertEquals(AuthError.AUTHORIZATION_FAILED, failure.error)
        assertEquals(0, http.callCount)
    }

    @Test
    fun missingStateIsRejected() = runTest {
        // RFC 6749 §10.12: a redirect that drops `state` entirely must not be accepted.
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val authorizer = FakeAuthorizationCodeProvider(
            respond = { AuthorizationResult.Success(code = "c", state = null) },
        )
        val result = client(http, authorizer = authorizer).signIn()

        val failure = assertIs<AuthResult.Failure>(result)
        assertEquals(AuthError.AUTHORIZATION_FAILED, failure.error)
        assertEquals(0, http.callCount, "no token exchange on a stateless redirect")
    }

    @Test
    fun unboundProviderErrorMapsToProviderUnavailable() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val authorizer = FakeAuthorizationCodeProvider(
            nextResult = AuthorizationResult.Failure(AuthorizationError.ProviderUnavailable, "nothing bound"),
        )
        val failure = assertIs<AuthResult.Failure>(client(http, authorizer = authorizer).signIn())
        assertEquals(AuthError.PROVIDER_UNAVAILABLE, failure.error)
    }

    @Test
    fun tokenExchangeFailureIsSurfaced() = runTest {
        val http = FakeHttp(HttpStatusCode.BadRequest, """{"error":"invalid_grant","error_description":"bad"}""")
        val result = client(http).signIn()

        val failure = assertIs<AuthResult.Failure>(result)
        assertEquals(AuthError.TOKEN_EXCHANGE_FAILED, failure.error)
        assertIs<AuthState.SignedOut>(client(http).authState.value)
    }

    @Test
    fun invalidConfigFailsFastAtConstruction() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        // Config is validated in the client's init — no code path can reach a bad endpoint.
        assertFailsWith<IllegalArgumentException> { client(http, clientConfig = config.copy(clientId = "  ")) }
        assertFailsWith<IllegalArgumentException> {
            client(http, clientConfig = config.copy(tokenEndpoint = "http://evil.example.com/token"))
        }
    }

    @Test
    fun secondSignInReturnsExistingValidSessionWithoutBrowser() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val authorizer = FakeAuthorizationCodeProvider()
        val time = FakeTimeProvider(0L)
        val auth = client(http, authorizer = authorizer, time = time)

        assertIs<AuthResult.Success>(auth.signIn())
        assertEquals(1, authorizer.invocations)

        // still well within the 1h token lifetime
        time.advanceBy(60_000L)
        assertIs<AuthResult.Success>(auth.signIn())
        assertEquals(1, authorizer.invocations, "no second browser round-trip")
    }

    @Test
    fun expiredSessionOnSignInRefreshesInsteadOfPrompting() = runTest {
        val http = FakeHttp(
            listOf(
                HttpStatusCode.OK to tokenBody(accessToken = "at-1", refreshToken = "rt-1"),
                HttpStatusCode.OK to tokenBody(accessToken = "at-2", refreshToken = null),
            ),
        )
        val authorizer = FakeAuthorizationCodeProvider()
        val time = FakeTimeProvider(0L)
        val auth = client(http, authorizer = authorizer, time = time)

        assertIs<AuthResult.Success>(auth.signIn())
        time.advanceBy(3_600_001L) // past expiry

        val result = auth.signIn()
        val success = assertIs<AuthResult.Success>(result)
        assertEquals("at-2", success.session.tokens.accessToken)
        assertEquals("rt-1", success.session.tokens.refreshToken, "refresh token carried forward")
        assertEquals(1, authorizer.invocations, "refresh path must not open the browser")
    }

    @Test
    fun refreshInheritsIdentityFromBareProviderResponse() = runTest {
        // Real refresh grants return only access_token/expires_in — no id_token.
        val http = FakeHttp(
            listOf(
                HttpStatusCode.OK to tokenBody(accessToken = "at-1", refreshToken = "rt-1", sub = "uid-42"),
                HttpStatusCode.OK to """{"access_token":"at-2","expires_in":3600,"token_type":"Bearer"}""",
            ),
        )
        val time = FakeTimeProvider(0L)
        val auth = client(http, time = time)

        assertIs<AuthResult.Success>(auth.signIn())
        time.advanceBy(3_600_001L)

        val success = assertIs<AuthResult.Success>(auth.refreshSession())
        assertEquals("at-2", success.session.tokens.accessToken)
        assertEquals("uid-42", success.session.user.uid, "identity inherited from the prior session")
        assertEquals("rt-1", success.session.tokens.refreshToken)
        assertEquals("e@e.com", success.session.user.email, "email inherited too")
    }

    @Test
    fun refreshCarriesForwardAStillValidIdTokenWhenTheGrantReturnsNone() = runTest {
        val http = FakeHttp(
            listOf(
                HttpStatusCode.OK to tokenBody(accessToken = "at-1", refreshToken = "rt-1"),
                HttpStatusCode.OK to """{"access_token":"at-2","expires_in":3600}""",
            ),
        )
        val time = FakeTimeProvider(0L)
        val auth = client(http, time = time)

        val original = assertIs<AuthResult.Success>(auth.signIn()).session.tokens.idToken
        assertTrue(original != null && original.isNotBlank())

        val success = assertIs<AuthResult.Success>(auth.refreshSession())
        assertEquals(original, success.session.tokens.idToken, "unexpired id_token kept across a bare refresh")
    }

    @Test
    fun refreshDropsAnExpiredPriorIdTokenWhenTheGrantReturnsNone() = runTest {
        // id_token exp is 2000s; the access token (expires_in 3600) outlives it here only
        // because we force the refresh explicitly after advancing past the id_token's exp.
        val http = FakeHttp(
            listOf(
                HttpStatusCode.OK to tokenBody(accessToken = "at-1", refreshToken = "rt-1", expEpochSeconds = 2_000L),
                HttpStatusCode.OK to """{"access_token":"at-2","expires_in":3600}""",
            ),
        )
        val time = FakeTimeProvider(0L)
        val auth = client(http, time = time)

        assertIs<AuthResult.Success>(auth.signIn())
        time.advanceBy(3_000_000L) // past the id_token's exp (2000s), plus leeway

        val success = assertIs<AuthResult.Success>(auth.refreshSession())
        assertNull(success.session.tokens.idToken, "an expired prior id_token must not be carried forward")
    }

    @Test
    fun rejectedRefreshTokenFallsThroughToInteractiveSignIn() = runTest {
        val http = FakeHttp(
            listOf(
                HttpStatusCode.OK to tokenBody(accessToken = "at-1", refreshToken = "rt-1", sub = "uid-1"),
                HttpStatusCode.BadRequest to """{"error":"invalid_grant","error_description":"revoked"}""",
                HttpStatusCode.OK to tokenBody(accessToken = "at-fresh", refreshToken = "rt-2", sub = "uid-1"),
            ),
        )
        val authorizer = FakeAuthorizationCodeProvider()
        val time = FakeTimeProvider(0L)
        val auth = client(http, authorizer = authorizer, time = time)

        assertIs<AuthResult.Success>(auth.signIn())
        time.advanceBy(3_600_001L)

        val result = auth.signIn()
        val success = assertIs<AuthResult.Success>(result)
        assertEquals("at-fresh", success.session.tokens.accessToken)
        assertEquals(2, authorizer.invocations, "browser re-opened after refresh token was rejected")
    }

    @Test
    fun currentAccessTokenRefreshesWhenExpired() = runTest {
        val http = FakeHttp(
            listOf(
                HttpStatusCode.OK to tokenBody(accessToken = "at-1"),
                HttpStatusCode.OK to tokenBody(accessToken = "at-2", refreshToken = null),
            ),
        )
        val time = FakeTimeProvider(0L)
        val auth = client(http, time = time)

        auth.signIn()
        assertEquals("at-1", auth.currentAccessToken())

        time.advanceBy(3_600_001L)
        assertEquals("at-2", auth.currentAccessToken())
    }

    @Test
    fun currentAccessTokenNullWhenSignedOut() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        assertNull(client(http).currentAccessToken())
    }

    @Test
    fun concurrentSignInCallsOpenOnlyOneBrowser() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val gate = CompletableDeferred<Unit>()
        var captured: AuthorizationRequest? = null
        val authorizer = FakeAuthorizationCodeProvider(
            respond = { request -> captured = request; AuthorizationResult.Success("code", request.state) },
        )
        // Make the first authorize() genuinely in-flight when the second signIn() starts.
        val gating = AuthorizationCodeProvider { request ->
            gate.await()
            authorizer.authorize(request)
        }
        val auth = DefaultSocialAuthClient(
            config = config,
            authorizationCodeProvider = gating,
            tokenClient = OAuthTokenClient(http.client),
            randomBytes = testRandomBytes,
            dispatcher = Dispatchers.Unconfined,
        )

        var first: AuthResult? = null
        var second: AuthResult? = null
        val job1 = launch(Dispatchers.Unconfined) { first = auth.signIn() }
        val job2 = launch(Dispatchers.Unconfined) { second = auth.signIn() }
        gate.complete(Unit)
        job1.join()
        job2.join()

        assertIs<AuthResult.Success>(first)
        assertIs<AuthResult.Success>(second)
        assertEquals(1, authorizer.invocations, "the second caller reuses the freshly-minted session")
        assertEquals(1, http.callCount, "only one token exchange")
    }

    @Test
    fun queuedSignInRefreshesInsteadOfOpeningASecondBrowserWhenTheSessionExpiredWhileWaiting() = runTest {
        val expiredIdToken = testJwtRaw(
            """{"sub":"uid-1","aud":"cid","iss":"https://accounts.google.com","exp":4000000000,"nonce":"$testStateAndNonce"}""",
        )
        val http = FakeHttp(
            listOf(
                // First sign-in mints a session that is already expired (expires_in: 0) but refreshable.
                HttpStatusCode.OK to
                    """{"access_token":"at-1","refresh_token":"rt-1","id_token":"$expiredIdToken","expires_in":0}""",
                HttpStatusCode.OK to tokenBody(accessToken = "at-2"),
            ),
        )
        val gate = CompletableDeferred<Unit>()
        val inner = FakeAuthorizationCodeProvider(
            respond = { request -> AuthorizationResult.Success("code", request.state) },
        )
        val gating = AuthorizationCodeProvider { request -> gate.await(); inner.authorize(request) }
        val auth = DefaultSocialAuthClient(
            config = config,
            authorizationCodeProvider = gating,
            tokenClient = OAuthTokenClient(http.client),
            randomBytes = testRandomBytes,
            dispatcher = Dispatchers.Unconfined,
        )

        var second: AuthResult? = null
        val job1 = launch(Dispatchers.Unconfined) { auth.signIn() }
        val job2 = launch(Dispatchers.Unconfined) { second = auth.signIn() }
        gate.complete(Unit)
        job1.join()
        job2.join()

        assertIs<AuthResult.Success>(second)
        assertEquals("at-2", (second as AuthResult.Success).session.tokens.accessToken)
        assertEquals(1, inner.invocations, "the queued sign-in refreshed rather than opening a second browser")
    }

    @Test
    fun transientRefreshFailureKeepsTheStoredSession() = runTest {
        val http = FakeHttp(
            listOf(
                HttpStatusCode.OK to tokenBody(accessToken = "at-1", refreshToken = "rt-1"),
                HttpStatusCode.InternalServerError to "<html>503</html>",
            ),
        )
        val store = InMemoryAuthSessionStore()
        val time = FakeTimeProvider(0L)
        val auth = client(http, store = store, time = time)

        auth.signIn()
        time.advanceBy(3_600_001L)

        assertNull(auth.currentAccessToken())
        assertTrue(store.load() != null, "a 5xx must not drop the session — only invalid_grant does")
        assertIs<AuthState.SignedIn>(auth.authState.value)
    }

    @Test
    fun transientRefreshFailureFallsBackToTheStillUsableCurrentToken() = runTest {
        val http = FakeHttp(
            listOf(
                HttpStatusCode.OK to tokenBody(accessToken = "at-1", refreshToken = "rt-1"),
                HttpStatusCode.InternalServerError to "<html>503</html>",
            ),
        )
        val time = FakeTimeProvider(0L)
        val auth = client(http, time = time)

        auth.signIn()
        time.advanceBy(3_580_000L) // inside the 60s proactive leeway, 20s before hard expiry

        assertEquals("at-1", auth.currentAccessToken(), "a network blip must not discard a still-valid token")
        assertIs<AuthState.SignedIn>(auth.authState.value)
    }

    @Test
    fun startupDiscardsAnExpiredUnrefreshableSession() = runTest {
        val expired = FakeSocialAuthClient.defaultSession().let {
            it.copy(tokens = it.tokens.copy(refreshToken = null, expiresAtEpochMillis = 1_000L))
        }
        val store = InMemoryAuthSessionStore(initial = expired)
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())

        val auth = client(http, store = store, time = FakeTimeProvider(10_000_000L))

        assertIs<AuthState.SignedOut>(auth.authState.value)
        assertNull(store.load())
    }

    @Test
    fun currentAccessTokenClearsAnExpiredUnrefreshableSession() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody(refreshToken = null))
        val store = InMemoryAuthSessionStore()
        val time = FakeTimeProvider(0L)
        val auth = client(http, store = store, time = time)

        auth.signIn()
        assertTrue(store.load() != null)
        time.advanceBy(3_600_001L)

        assertNull(auth.currentAccessToken())
        assertNull(store.load(), "dead session dropped from storage")
        assertIs<AuthState.SignedOut>(auth.authState.value)
    }

    @Test
    fun currentAccessTokenReturnsAnUnrefreshableTokenThatIsOnlyInsideTheLeewayWindow() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody(accessToken = "at-1", refreshToken = null))
        val store = InMemoryAuthSessionStore()
        val time = FakeTimeProvider(0L)
        val auth = client(http, store = store, time = time)

        auth.signIn()
        time.advanceBy(3_580_000L) // 20s before hard expiry — inside the 60s proactive leeway

        assertEquals("at-1", auth.currentAccessToken(), "a still-valid token must not be dropped")
        assertTrue(store.load() != null, "session kept while the token is still usable")
        assertIs<AuthState.SignedIn>(auth.authState.value)
    }

    @Test
    fun idTokenWithWrongAudienceIsRejected() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody(aud = "some-other-client"))
        val failure = assertIs<AuthResult.Failure>(client(http).signIn())
        assertEquals(AuthError.TOKEN_EXCHANGE_FAILED, failure.error)
        assertTrue(failure.message!!.contains("audience"))
    }

    @Test
    fun idTokenWithMatchingAudiencePasses() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody(aud = "cid"))
        assertIs<AuthResult.Success>(client(http).signIn())
    }

    @Test
    fun idTokenWithNoAudienceClaimIsRejected() = runTest {
        // OIDC §3.1.3.7: an id_token without an aud is invalid.
        val http = FakeHttp(HttpStatusCode.OK, tokenBody(aud = null))
        val failure = assertIs<AuthResult.Failure>(client(http).signIn())
        assertEquals(AuthError.TOKEN_EXCHANGE_FAILED, failure.error)
        assertTrue(failure.message!!.contains("audience"))
    }

    @Test
    fun noIdTokenIsFineForNonOpenIdScopes() = runTest {
        // A non-OIDC provider (no `openid` scope) may return only an access_token + user_id.
        val http = FakeHttp(HttpStatusCode.OK, """{"access_token":"at","user_id":"legacy-1","expires_in":3600}""")
        val nonOidc = client(http, clientConfig = config.copy(scopes = listOf("email")))
        val success = assertIs<AuthResult.Success>(nonOidc.signIn())
        assertEquals("legacy-1", success.session.user.uid)
    }

    @Test
    fun missingIdTokenIsRejectedWhenOpenIdRequested() = runTest {
        // Default config requests `openid` → the token response MUST carry an id_token.
        val http = FakeHttp(HttpStatusCode.OK, """{"access_token":"at","user_id":"x","expires_in":3600}""")
        val failure = assertIs<AuthResult.Failure>(client(http).signIn())
        assertEquals(AuthError.TOKEN_EXCHANGE_FAILED, failure.error)
        assertTrue(failure.message!!.contains("id_token"))
    }

    @Test
    fun expiredIdTokenIsRejected() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody(expEpochSeconds = 1_000L)) // long past
        val time = FakeTimeProvider(10_000_000_000L)
        val failure = assertIs<AuthResult.Failure>(client(http, time = time).signIn())
        assertEquals(AuthError.TOKEN_EXCHANGE_FAILED, failure.error)
        assertTrue(failure.message!!.contains("expired"))
    }

    // idTokenClaims: the id_token payload minus the nonce, which is injected to match testRandomBytes.
    private fun rawTokenBody(idTokenClaims: String): String {
        val withNonce = idTokenClaims.dropLast(1) + ",\"nonce\":\"$testStateAndNonce\"}"
        val idToken = testJwtRaw(withNonce)
        return """{"access_token":"at-1","refresh_token":"rt-1","id_token":"$idToken","expires_in":3600}"""
    }

    @Test
    fun multiAudienceIdTokenNeedsMatchingAzp() = runTest {
        val exp = 4_000_000_000L
        val ok = rawTokenBody(
            """{"sub":"u","iss":"https://accounts.google.com","aud":["cid","other-client"],"azp":"cid","exp":$exp}""",
        )
        assertIs<AuthResult.Success>(client(FakeHttp(HttpStatusCode.OK, ok)).signIn())

        val badAzp = rawTokenBody(
            """{"sub":"u","iss":"https://accounts.google.com","aud":["cid","other-client"],"azp":"other-client","exp":$exp}""",
        )
        val failure = assertIs<AuthResult.Failure>(client(FakeHttp(HttpStatusCode.OK, badAzp)).signIn())
        assertEquals(AuthError.TOKEN_EXCHANGE_FAILED, failure.error)
        assertTrue(failure.message!!.contains("azp"))

        val missingAzp = rawTokenBody(
            """{"sub":"u","iss":"https://accounts.google.com","aud":["cid","other-client"],"exp":$exp}""",
        )
        assertIs<AuthResult.Failure>(client(FakeHttp(HttpStatusCode.OK, missingAzp)).signIn())

        // Single audience but an azp for another client is still rejected (OIDC §3.1.3.7 step 5).
        val singleAudWrongAzp = rawTokenBody(
            """{"sub":"u","iss":"https://accounts.google.com","aud":"cid","azp":"other-client","exp":$exp}""",
        )
        assertIs<AuthResult.Failure>(client(FakeHttp(HttpStatusCode.OK, singleAudWrongAzp)).signIn())
    }

    @Test
    fun idTokenWithMismatchedNonceIsRejected() = runTest {
        // The browser step used a fresh nonce; an id_token echoing a different one means
        // the response isn't bound to this sign-in (OIDC Core §3.1.3.7 step 11).
        val http = FakeHttp(HttpStatusCode.OK, tokenBody(nonce = "not-the-nonce-we-sent"))
        val failure = assertIs<AuthResult.Failure>(client(http).signIn())
        assertEquals(AuthError.TOKEN_EXCHANGE_FAILED, failure.error)
        assertTrue(failure.message!!.contains("nonce"))
    }

    @Test
    fun idTokenMissingExpClaimIsRejected() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody(expEpochSeconds = null))
        val failure = assertIs<AuthResult.Failure>(client(http).signIn())
        assertEquals(AuthError.TOKEN_EXCHANGE_FAILED, failure.error)
        assertTrue(failure.message!!.contains("exp"))
    }

    private fun issuerClient(http: FakeHttp) =
        client(http, clientConfig = config.copy(issuer = "https://accounts.google.com"))

    @Test
    fun issuerMismatchIsRejectedWhenIssuerConfigured() = runTest {
        val failure = assertIs<AuthResult.Failure>(
            issuerClient(FakeHttp(HttpStatusCode.OK, tokenBody(iss = "https://evil.example.com"))).signIn(),
        )
        assertEquals(AuthError.TOKEN_EXCHANGE_FAILED, failure.error)
        assertTrue(failure.message!!.contains("issuer"))
    }

    @Test
    fun bareGoogleIssuerFormIsAccepted() = runTest {
        // Google issues `iss` with or without the https:// prefix.
        assertIs<AuthResult.Success>(
            issuerClient(FakeHttp(HttpStatusCode.OK, tokenBody(iss = "accounts.google.com"))).signIn(),
        )
    }

    @Test
    fun currentAccessTokenClearsSessionWhenRefreshTokenRevoked() = runTest {
        val http = FakeHttp(
            listOf(
                HttpStatusCode.OK to tokenBody(accessToken = "at-1", refreshToken = "rt-1"),
                HttpStatusCode.BadRequest to """{"error":"invalid_grant","error_description":"revoked"}""",
            ),
        )
        val store = InMemoryAuthSessionStore()
        val time = FakeTimeProvider(0L)
        val auth = client(http, store = store, time = time)

        auth.signIn()
        time.advanceBy(3_600_001L)

        assertNull(auth.currentAccessToken())
        assertNull(store.load(), "revoked session cleared from storage")
        assertIs<AuthState.SignedOut>(auth.authState.value)
    }

    @Test
    fun refreshWithAChangedSubjectIsRejectedAndClearsTheSession() = runTest {
        val http = FakeHttp(
            listOf(
                HttpStatusCode.OK to tokenBody(accessToken = "at-1", refreshToken = "rt-1", sub = "uid-1"),
                HttpStatusCode.OK to tokenBody(accessToken = "at-2", sub = "uid-attacker"),
            ),
        )
        val store = InMemoryAuthSessionStore()
        val time = FakeTimeProvider(0L)
        val auth = client(http, store = store, time = time)
        assertIs<AuthResult.Success>(auth.signIn())
        time.advanceBy(3_600_001L)

        val failure = assertIs<AuthResult.Failure>(auth.refreshSession())
        assertEquals(AuthError.TOKEN_REFRESH_FAILED, failure.error)
        assertTrue(failure.message!!.contains("sub"))
        // A subject change is a token-substitution signal — the session must not survive it.
        assertNull(store.load(), "session with a changed subject cleared from storage")
        assertIs<AuthState.SignedOut>(auth.authState.value)
    }

    @Test
    fun refreshGrantIdTokenAudienceIsAlsoValidatedAndClearsTheSession() = runTest {
        val http = FakeHttp(
            listOf(
                HttpStatusCode.OK to tokenBody(accessToken = "at-1", refreshToken = "rt-1", aud = "cid"),
                HttpStatusCode.OK to tokenBody(accessToken = "at-2", aud = "attacker-client"),
            ),
        )
        val store = InMemoryAuthSessionStore()
        val time = FakeTimeProvider(0L)
        val auth = client(http, store = store, time = time)
        assertIs<AuthResult.Success>(auth.signIn())
        time.advanceBy(3_600_001L)

        val failure = assertIs<AuthResult.Failure>(auth.refreshSession())
        assertEquals(AuthError.TOKEN_REFRESH_FAILED, failure.error)
        assertTrue(failure.message!!.contains("audience"))
        // A wrong-audience id_token on refresh is a substitution signal — drop the session.
        assertNull(store.load(), "untrusted-id_token refresh cleared the session")
        assertIs<AuthState.SignedOut>(auth.authState.value)
    }

    @Test
    fun refreshWithAnExpiredIdTokenSurfacesFailureButKeepsTheSession() = runTest {
        val http = FakeHttp(
            listOf(
                HttpStatusCode.OK to tokenBody(accessToken = "at-1", refreshToken = "rt-1"),
                // Refresh returns an id_token that is long past exp — odd, but not an attack.
                HttpStatusCode.OK to tokenBody(accessToken = "at-2", expEpochSeconds = 1_000L),
            ),
        )
        val store = InMemoryAuthSessionStore()
        val time = FakeTimeProvider(0L)
        val auth = client(http, store = store, time = time)
        assertIs<AuthResult.Success>(auth.signIn())
        time.advanceBy(3_600_001L)

        val failure = assertIs<AuthResult.Failure>(auth.refreshSession())
        assertEquals(AuthError.TOKEN_REFRESH_FAILED, failure.error)
        assertTrue(failure.message!!.contains("expired"))
        assertTrue(store.load() != null, "a merely-expired id_token must not nuke the session")
        assertIs<AuthState.SignedIn>(auth.authState.value)
    }

    @Test
    fun exchangeWithNoUserIdentityFails() = runTest {
        // Non-OIDC scopes (no id_token required) but also no user_id → cannot identify the user.
        val http = FakeHttp(HttpStatusCode.OK, """{"access_token":"at","expires_in":3600}""")
        val nonOidc = client(http, clientConfig = config.copy(scopes = listOf("email")))
        val failure = assertIs<AuthResult.Failure>(nonOidc.signIn())
        assertEquals(AuthError.TOKEN_EXCHANGE_FAILED, failure.error)
        assertTrue(failure.message!!.contains("identity"))
    }

    @Test
    fun serverErrorOnRefreshIsSurfacedWithoutOpeningBrowser() = runTest {
        val http = FakeHttp(
            listOf(
                HttpStatusCode.OK to tokenBody(accessToken = "at-1", refreshToken = "rt-1"),
                HttpStatusCode.InternalServerError to "<html>502 Bad Gateway</html>",
            ),
        )
        val authorizer = FakeAuthorizationCodeProvider()
        val time = FakeTimeProvider(0L)
        val auth = client(http, authorizer = authorizer, time = time)

        assertIs<AuthResult.Success>(auth.signIn())
        time.advanceBy(3_600_001L)

        val failure = assertIs<AuthResult.Failure>(auth.signIn())
        assertEquals(AuthError.TOKEN_REFRESH_FAILED, failure.error)
        assertEquals(1, authorizer.invocations, "a 5xx must not trigger an interactive sign-in")
    }

    @Test
    fun signOutDuringInteractiveSignInDiscardsTheResultingSession() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val gate = CompletableDeferred<AuthorizationResult>()
        var captured: AuthorizationRequest? = null
        val authorizer = AuthorizationCodeProvider { request ->
            captured = request
            gate.await()
        }
        val auth = DefaultSocialAuthClient(
            config = config,
            authorizationCodeProvider = authorizer,
            tokenClient = OAuthTokenClient(http.client),
            randomBytes = testRandomBytes,
            dispatcher = Dispatchers.Unconfined,
        )

        var result: AuthResult? = null
        val job = launch(Dispatchers.Unconfined) { result = auth.signIn() }

        // signIn is now parked on the browser step; sign out underneath it.
        auth.signOut()
        gate.complete(AuthorizationResult.Success("code", captured!!.state))
        job.join()

        assertIs<AuthResult.Cancelled>(result)
        assertIs<AuthState.SignedOut>(auth.authState.value)
        assertNull(auth.currentSession)
        assertEquals(0, http.callCount, "no token exchange after signOut() superseded the sign-in")
    }

    @Test
    fun signInQueuedBehindAnotherFailsWithoutOpeningABrowserWhenClosedWhileWaiting() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val gate = CompletableDeferred<Unit>()
        val inner = FakeAuthorizationCodeProvider(
            respond = { request -> AuthorizationResult.Success("code", request.state) },
        )
        val gating = AuthorizationCodeProvider { request -> gate.await(); inner.authorize(request) }
        val auth = DefaultSocialAuthClient(
            config = config,
            authorizationCodeProvider = gating,
            tokenClient = OAuthTokenClient(http.client, ownsHttpClient = false),
            randomBytes = testRandomBytes,
            dispatcher = Dispatchers.Unconfined,
        )

        var second: AuthResult? = null
        val job1 = launch(Dispatchers.Unconfined) { auth.signIn() }
        val job2 = launch(Dispatchers.Unconfined) { second = auth.signIn() }

        auth.close() // both sign-ins are queued; job2 is parked on interactiveMutex
        gate.complete(Unit)
        job1.join()
        job2.join()

        assertIs<AuthResult.Failure>(second)
        assertEquals(1, inner.invocations, "the queued sign-in must not open a browser on a closed client")
    }

    @Test
    fun signInThatCompletesAfterCloseDoesNotPersistOrReportSuccess() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val gate = CompletableDeferred<AuthorizationResult>()
        var captured: AuthorizationRequest? = null
        val authorizer = AuthorizationCodeProvider { request -> captured = request; gate.await() }
        val store = InMemoryAuthSessionStore()
        val auth = DefaultSocialAuthClient(
            config = config,
            authorizationCodeProvider = authorizer,
            // ownsHttpClient = false so close() leaves the engine open and the token exchange
            // still succeeds — it's the post-exchange isClosed gate that must reject the session.
            tokenClient = OAuthTokenClient(http.client, ownsHttpClient = false),
            sessionStore = store,
            randomBytes = testRandomBytes,
            dispatcher = Dispatchers.Unconfined,
        )

        var result: AuthResult? = null
        val job = launch(Dispatchers.Unconfined) { result = auth.signIn() }

        auth.close() // client torn down while the browser step is still parked
        gate.complete(AuthorizationResult.Success("code", captured!!.state))
        job.join()

        assertIs<AuthResult.Failure>(result)
        assertEquals(AuthError.UNKNOWN, (result as AuthResult.Failure).error)
        assertNull(store.load(), "a closed client must not persist a session")
        assertIs<AuthState.SignedOut>(auth.authState.value)
    }

    @Test
    fun refreshThatCompletesAfterCloseDoesNotPersist() = runTest {
        val exchange = tokenBody(accessToken = "at-1", refreshToken = "rt-1")
        val gate = CompletableDeferred<Unit>()
        val gatingTokenClient = object : TokenEndpointClient {
            private val real = OAuthTokenClient(FakeHttp(HttpStatusCode.OK, exchange).client, ownsHttpClient = false)
            override suspend fun exchangeAuthorizationCode(config: SocialAuthConfig, code: String, codeVerifier: String?) =
                real.exchangeAuthorizationCode(config, code, codeVerifier)
            override suspend fun refreshAccessToken(config: SocialAuthConfig, refreshToken: String): OAuthTokenResponse {
                gate.await()
                return OAuthTokenResponse(accessToken = "at-2", expiresInSeconds = 3600)
            }
            override fun close() = Unit
        }
        val store = InMemoryAuthSessionStore()
        val time = FakeTimeProvider(0L)
        val auth = DefaultSocialAuthClient(
            config = config,
            authorizationCodeProvider = FakeAuthorizationCodeProvider(),
            tokenClient = gatingTokenClient,
            sessionStore = store,
            timeProvider = time,
            randomBytes = testRandomBytes,
            dispatcher = Dispatchers.Unconfined,
        )
        assertIs<AuthResult.Success>(auth.signIn())
        time.advanceBy(3_600_001L)

        var result: AuthResult? = null
        val job = launch(Dispatchers.Unconfined) { result = auth.refreshSession() }
        auth.close() // torn down while refreshAccessToken() is parked on the gate
        gate.complete(Unit)
        job.join()

        assertIs<AuthResult.Failure>(result)
        assertEquals("at-1", store.load()?.tokens?.accessToken, "the refreshed session was not persisted")
    }

    @Test
    fun invalidGrantOnReuseClearsTheStoredSessionThenReAuths() = runTest {
        val http = FakeHttp(
            listOf(
                HttpStatusCode.OK to tokenBody(accessToken = "at-1", refreshToken = "rt-1"),
                HttpStatusCode.BadRequest to """{"error":"invalid_grant","error_description":"revoked"}""",
            ),
        )
        val authorizer = FakeAuthorizationCodeProvider()
        val store = InMemoryAuthSessionStore()
        val time = FakeTimeProvider(0L)
        val auth = client(http, authorizer = authorizer, store = store, time = time)

        assertIs<AuthResult.Success>(auth.signIn())
        assertTrue(store.load() != null)
        time.advanceBy(3_600_001L)

        // Refresh is rejected with invalid_grant → stale session dropped → interactive
        // sign-in launched → user cancels.
        authorizer.nextResult = AuthorizationResult.Cancelled
        assertIs<AuthResult.Cancelled>(auth.signIn())
        assertNull(store.load(), "revoked session dropped from storage")
        assertIs<AuthState.SignedOut>(auth.authState.value)
        assertEquals(2, authorizer.invocations, "one browser trip for the original sign-in, one for the re-auth")
    }

    @Test
    fun refreshSessionWithoutSessionFails() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val failure = assertIs<AuthResult.Failure>(client(http).refreshSession())
        assertEquals(AuthError.NO_ACTIVE_SESSION, failure.error)
    }

    @Test
    fun signOutClearsSessionAndStore() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val store = InMemoryAuthSessionStore()
        val auth = client(http, store = store)

        auth.signIn()
        auth.signOut()

        assertIs<AuthState.SignedOut>(auth.authState.value)
        assertNull(auth.currentSession)
        assertNull(store.load())
    }

    @Test
    fun restoresSessionFromStoreOnConstruction() = runTest {
        val http = FakeHttp(HttpStatusCode.OK, tokenBody())
        val store = InMemoryAuthSessionStore()
        client(http, store = store).signIn()

        // A brand new client over the same store starts signed in.
        val revived = client(FakeHttp(HttpStatusCode.OK, tokenBody()), store = store)
        assertIs<AuthState.SignedIn>(revived.authState.value)
        assertEquals("uid-1", revived.currentSession!!.user.uid)
    }

    /** A store whose writes fail (locked keystore, full disk, …) but whose reads work. */
    private class WriteFailingStore(private val delegate: AuthSessionStore = InMemoryAuthSessionStore()) :
        AuthSessionStore by delegate {
        override fun save(session: AuthSession): Unit = throw IllegalStateException("keystore locked")
        override fun clear(): Unit = throw IllegalStateException("keystore locked")
    }

    /** A store whose load() throws — e.g. a corrupt or key-revoked encrypted store. */
    private class LoadFailingStore : AuthSessionStore {
        override fun load(): AuthSession? = throw IllegalStateException("keystore key revoked")
        override fun save(session: AuthSession) = Unit
        override fun clear() = Unit
    }

    @Test
    fun constructionStartsSignedOutWhenTheStoreThrowsOnLoad() = runTest {
        val auth = client(FakeHttp(HttpStatusCode.OK, tokenBody()), store = LoadFailingStore())
        assertIs<AuthState.SignedOut>(auth.authState.value)
        assertNull(auth.currentSession)
    }

    @Test
    fun signInSucceedsInMemoryEvenIfPersistingThrows() = runTest {
        val auth = client(FakeHttp(HttpStatusCode.OK, tokenBody()), store = WriteFailingStore())
        val success = assertIs<AuthResult.Success>(auth.signIn())
        assertEquals("uid-1", success.session.user.uid)
        assertIs<AuthState.SignedIn>(auth.authState.value)
    }

    @Test
    fun signOutMovesToSignedOutEvenIfClearingThrows() = runTest {
        val auth = client(FakeHttp(HttpStatusCode.OK, tokenBody()), store = WriteFailingStore())
        assertIs<AuthResult.Success>(auth.signIn())

        auth.signOut()

        assertIs<AuthState.SignedOut>(auth.authState.value)
        assertNull(auth.currentSession)
    }

    @Test
    fun expiredUnrefreshableSessionClearsToSignedOutEvenIfClearingThrows() = runTest {
        val time = FakeTimeProvider(0L)
        val auth = client(
            FakeHttp(HttpStatusCode.OK, tokenBody(refreshToken = null)),
            store = WriteFailingStore(),
            time = time,
        )
        assertIs<AuthResult.Success>(auth.signIn())
        time.advanceBy(3_600_001L)

        assertNull(auth.currentAccessToken())
        assertIs<AuthState.SignedOut>(auth.authState.value)
    }
}
