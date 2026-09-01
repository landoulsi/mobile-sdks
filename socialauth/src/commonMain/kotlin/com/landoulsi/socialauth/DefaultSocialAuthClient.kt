package com.landoulsi.socialauth

import com.landoulsi.logger.Logger
import com.landoulsi.socialauth.internal.DEFAULT_CLOCK_SKEW_LEEWAY_MILLIS
import com.landoulsi.socialauth.internal.MAX_EXPIRES_IN_SECONDS
import com.landoulsi.socialauth.internal.MILLIS_PER_SECOND
import com.landoulsi.socialauth.internal.SOCIAL_AUTH_LOG_TAG
import com.landoulsi.socialauth.internal.socialAuthIoDispatcher
import com.landoulsi.socialauth.model.AuthError
import com.landoulsi.socialauth.model.AuthResult
import com.landoulsi.socialauth.model.AuthSession
import com.landoulsi.socialauth.model.AuthState
import com.landoulsi.socialauth.model.AuthTokens
import com.landoulsi.socialauth.model.AuthUser
import com.landoulsi.socialauth.oauth.AuthorizationUrl
import com.landoulsi.socialauth.oauth.IdTokenCheck
import com.landoulsi.socialauth.oauth.IdTokenValidator
import com.landoulsi.socialauth.oauth.OAuthException
import com.landoulsi.socialauth.oauth.OAuthTokenResponse
import com.landoulsi.socialauth.oauth.Pkce
import com.landoulsi.socialauth.oauth.PkceCodes
import com.landoulsi.socialauth.oauth.SubjectChangedException
import com.landoulsi.socialauth.oauth.TokenEndpointClient
import com.landoulsi.socialauth.oauth.secureRandomBytes
import com.landoulsi.timeprovider.SystemTimeProvider
import com.landoulsi.timeprovider.TimeProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.concurrent.Volatile

/**
 * The platform-agnostic [SocialAuthClient]. All OAuth logic lives here; the only
 * injected platform concern is [authorizationCodeProvider] (the browser step).
 *
 * @param config provider + client configuration.
 * @param authorizationCodeProvider interactive "get an authorization code" step.
 * @param tokenClient token-endpoint client (code exchange + refresh).
 * @param sessionStore where the [AuthSession] is persisted. Defaults to in-memory.
 *   Its `load()` is called once from the constructing thread — keep it cheap or
 *   pre-warm it off the main thread.
 * @param timeProvider clock used to compute token expiry. Defaults to the system clock.
 * @param randomBytes CSPRNG for `state` / PKCE verifier. Overridable for deterministic tests.
 * @param dispatcher dispatcher work is moved to, keeping suspend calls main-safe.
 */
internal class DefaultSocialAuthClient(
    private val config: SocialAuthConfig,
    private val authorizationCodeProvider: AuthorizationCodeProvider,
    private val tokenClient: TokenEndpointClient,
    private val sessionStore: AuthSessionStore = InMemoryAuthSessionStore(),
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val randomBytes: (Int) -> ByteArray = ::secureRandomBytes,
    private val dispatcher: CoroutineDispatcher = socialAuthIoDispatcher,
    private val idTokenValidator: IdTokenValidator = IdTokenValidator(
        clientId = config.clientId,
        issuer = config.issuer,
        currentTimeMillis = timeProvider::currentTimeMillis,
        clockSkewLeewayMillis = DEFAULT_CLOCK_SKEW_LEEWAY_MILLIS,
    ),
) : SocialAuthClient {

    init {
        // Fail fast on a bad config so no code path (refresh included) ever talks to a
        // non-HTTPS endpoint or a user-info-bearing URL.
        config.validate()
    }

    /** Guards session-state reads/writes and token-endpoint calls. Never held across the browser step. */
    private val mutex = Mutex()

    /** Serializes interactive sign-ins so two callers can't open two browser tabs. */
    private val interactiveMutex = Mutex()

    /** Bumped by [signOut]; lets an in-flight interactive sign-in detect it was superseded. */
    private var signOutEpoch = 0

    /** Set by [close]; read from coroutines on other threads, hence [Volatile]. */
    @Volatile
    private var isClosed = false

    private val _authState: MutableStateFlow<AuthState> = MutableStateFlow(restoreInitialAuthState())
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override val currentSession: AuthSession?
        get() = (_authState.value as? AuthState.SignedIn)?.session

    override suspend fun signIn(): AuthResult = withContext(dispatcher) {
        closedFailure()?.let { return@withContext it }
        // Fast path under the state lock: config check + reuse/refresh of an existing session.
        mutex.withLock { validateAndReuseExisting() }?.let { return@withContext it }

        // Interactive flow. The state lock is intentionally NOT held across the browser
        // round-trip, so currentAccessToken()/signOut()/refreshSession() stay responsive.
        interactiveMutex.withLock {
            // close() may have landed while this call was queued behind another sign-in.
            closedFailure()?.let { return@withContext it }
            // A sign-in queued behind another one may now find a valid — or refreshable —
            // session; re-run the full reuse/refresh check so we don't pop a needless browser.
            mutex.withLock { validateAndReuseExisting() }?.let { return@withContext it }
            runInteractiveSignIn()
        }
    }

    override suspend fun signOut() {
        withContext(dispatcher) {
            mutex.withLock {
                signOutEpoch++
                clearSessionState()
            }
        }
    }

    /**
     * A currently-valid access token, proactively refreshing first when the stored one is
     * within [AuthTokens.DEFAULT_LEEWAY_MILLIS] (60 s) of expiry.
     *
     * If the proactive refresh can't happen or fails (no refresh token, a network blip, a
     * 5xx) the stored token is still returned **as long as it has not actually passed its
     * expiry** — a caller gets the remaining seconds rather than a premature null. Null is
     * returned once the token is genuinely expired, or when a definitive `invalid_grant`
     * ends the session, or when the client is closed.
     */
    override suspend fun currentAccessToken(): String? = withContext(dispatcher) {
        if (isClosed) return@withContext null
        mutex.withLock {
            val session = currentSession ?: return@withLock null
            if (!session.tokens.isExpiredAt(timeProvider.currentTimeMillis())) {
                return@withLock session.tokens.accessToken
            }
            // Not yet past expiry, just inside the proactive-refresh window? Still usable now.
            fun stillUsable(): String? = session.tokens.accessToken
                .takeIf { !session.tokens.isExpiredAt(timeProvider.currentTimeMillis(), leewayMillis = 0L) }

            val refreshToken = session.tokens.refreshToken
            if (refreshToken == null) {
                stillUsable()?.let { return@withLock it }
                // Hard expired and unrefreshable — the session is dead; stop reporting it.
                clearSessionState()
                return@withLock null
            }
            when (refreshLocked(session, refreshToken)) {
                is AuthResult.Success -> (currentSession ?: session).tokens.accessToken
                // Refresh failed. On a transient failure (network, 5xx) the session is still
                // SignedIn — fall back to the current token if it hasn't expired *now* (the
                // refresh call may have eaten most of the leeway). invalid_grant / a
                // substitution signal already cleared the session, so currentSession is null.
                else -> if (currentSession != null) stillUsable() else null
            }
        }
    }

    override fun close() {
        isClosed = true
        tokenClient.close()
    }

    override suspend fun refreshSession(): AuthResult = withContext(dispatcher) {
        closedFailure()?.let { return@withContext it }
        mutex.withLock {
            val session = currentSession
                ?: return@withLock AuthResult.Failure(AuthError.NO_ACTIVE_SESSION, "Not signed in")
            val refreshToken = session.tokens.refreshToken
                ?: return@withLock AuthResult.Failure(
                    AuthError.NO_ACTIVE_SESSION,
                    "No refresh token held; a fresh sign-in is required",
                )
            refreshLocked(session, refreshToken)
        }
    }

    /**
     * Runs under [mutex]. Config is already validated in `init`.
     * @return a terminal [AuthResult], or null to proceed with an interactive sign-in.
     */
    private suspend fun validateAndReuseExisting(): AuthResult? {
        val existing = currentSession ?: return null
        if (!existing.tokens.isExpiredAt(timeProvider.currentTimeMillis())) {
            return AuthResult.Success(existing)
        }
        val refreshToken = existing.tokens.refreshToken
        if (refreshToken == null) {
            // Expired and unrefreshable — drop it now so a cancelled re-auth doesn't
            // leave the UI showing a stale "signed in" state.
            clearSessionState()
            return null
        }
        val refreshed = refreshLocked(existing, refreshToken)
        if (refreshed is AuthResult.Failure) {
            // A definitively rejected refresh token (RFC 6749 `invalid_grant`) justifies
            // a fresh interactive sign-in; refreshLocked has already cleared the dead
            // session. Network errors, 5xx, HTML error pages etc. are surfaced so we
            // don't pop a browser over a transient blip.
            val rejected = (refreshed.cause as? OAuthException)?.oauthErrorCode == OAUTH_ERROR_INVALID_GRANT
            return if (rejected) null else refreshed
        }
        // refreshLocked only ever returns Success or Failure.
        return refreshed
    }

    private suspend fun runInteractiveSignIn(): AuthResult {
        val startEpoch = mutex.withLock { signOutEpoch }
        val state = randomState()
        // OIDC nonce binds the browser session to the resulting id_token (Core §3.1.2.1).
        val nonce = if (OPENID_SCOPE in config.scopes) randomState() else null
        val pkce: PkceCodes? = if (config.usePkce) Pkce.generate(randomBytes) else null
        val url = AuthorizationUrl.build(config, state, pkce, nonce)

        val authResult = try {
            authorizationCodeProvider.authorize(
                AuthorizationRequest(
                    authorizationUrl = url,
                    redirectUri = config.redirectUri,
                    state = state,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(TAG, "Authorization step threw", e)
            return AuthResult.Failure(AuthError.AUTHORIZATION_FAILED, e.message, e)
        }

        val code = when (authResult) {
            is AuthorizationResult.Cancelled -> return AuthResult.Cancelled
            is AuthorizationResult.Failure -> {
                val errorLabel = when (val error = authResult.error) {
                    AuthorizationError.LaunchFailed -> "launch_failed"
                    AuthorizationError.InvalidRedirect -> "invalid_redirect"
                    AuthorizationError.ProviderUnavailable -> "provider_unavailable"
                    AuthorizationError.InvalidConfiguration -> "invalid_configuration"
                    AuthorizationError.InteractiveFlowFailed -> "interactive_flow_failed"
                    AuthorizationError.InteractiveFlowStartFailed -> "interactive_flow_start_failed"
                    is AuthorizationError.ProviderReported -> error.code
                }
                val detail = listOfNotNull(errorLabel, authResult.description).joinToString(": ")
                val category = when (authResult.error) {
                    AuthorizationError.ProviderUnavailable -> AuthError.PROVIDER_UNAVAILABLE
                    AuthorizationError.InvalidConfiguration -> AuthError.INVALID_CONFIGURATION
                    else -> AuthError.AUTHORIZATION_FAILED
                }
                return AuthResult.Failure(category, detail)
            }
            is AuthorizationResult.Success -> {
                // RFC 6749 §10.12: a `state` was sent, so an identical one MUST come back.
                // A null or differing value is rejected (missing state is a CSRF vector too).
                if (authResult.state != state) {
                    Logger.e(TAG, "OAuth state missing or mismatched — possible CSRF; rejecting redirect")
                    return AuthResult.Failure(AuthError.AUTHORIZATION_FAILED, "state parameter mismatch")
                }
                authResult.code
            }
        }

        // If close() happened while the browser was open, don't burn the auth code on a
        // client that can no longer be used.
        if (isClosed) {
            Logger.i(TAG, "Client closed before token exchange; aborting sign-in")
            return AuthResult.Failure(AuthError.UNKNOWN, "This SocialAuthClient has been closed")
        }
        // If signOut() ran while the browser was open, don't burn the auth code /
        // mint server-side tokens we'd only discard.
        if (mutex.withLock { signOutEpoch } != startEpoch) {
            Logger.i(TAG, "Sign-in superseded by signOut() before token exchange; aborting")
            return AuthResult.Cancelled
        }

        return try {
            val response = tokenClient.exchangeAuthorizationCode(config, code, pkce?.codeVerifier)
            // If signOut() landed while the exchange was in flight, discard the result.
            if (mutex.withLock { signOutEpoch } != startEpoch) {
                Logger.i(TAG, "Sign-in completed but was superseded by signOut(); discarding")
                return AuthResult.Cancelled
            }
            // OIDC §3.1.3.3: an `openid` request must yield an id_token on the code exchange.
            if (response.idToken == null && OPENID_SCOPE in config.scopes) {
                Logger.e(TAG, "openid was requested but the token response carried no id_token; rejecting")
                return AuthResult.Failure(AuthError.TOKEN_EXCHANGE_FAILED, "missing id_token in an OpenID response")
            }
            validateIdToken(response.idToken, expectedNonce = nonce)?.let { return it }
            val session = sessionFrom(response, previous = null)
            val persisted = mutex.withLock {
                if (isClosed || signOutEpoch != startEpoch) {
                    false
                } else {
                    persist(session)
                    true
                }
            }
            if (persisted) {
                Logger.i(TAG, "Sign-in complete for uid=${session.user.uid}")
                AuthResult.Success(session)
            } else if (isClosed) {
                Logger.i(TAG, "Sign-in completed after the client was closed; discarding session")
                AuthResult.Failure(AuthError.UNKNOWN, "This SocialAuthClient has been closed")
            } else {
                Logger.i(TAG, "Sign-in superseded by signOut() during persist; discarding session")
                AuthResult.Cancelled
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: OAuthException) {
            Logger.e(TAG, "Token exchange failed: ${e.message}")
            AuthResult.Failure(e.error, e.message, e)
        } catch (e: Exception) {
            Logger.e(TAG, "Unexpected sign-in failure", e)
            AuthResult.Failure(AuthError.UNKNOWN, e.message, e)
        }
    }

    /**
     * Validates the code-exchange id_token, wrapping any violation as a
     * [AuthError.TOKEN_EXCHANGE_FAILED] failure. (The refresh path calls
     * [IdTokenValidator.check] directly — it needs the [IdTokenCheck] variant to decide
     * whether to also wipe the session.)
     */
    private fun validateIdToken(idToken: String?, expectedNonce: String?): AuthResult? =
        idTokenValidator.check(idToken, expectedNonce).diagnostic?.let { reason ->
            Logger.e(TAG, "id_token rejected: $reason")
            AuthResult.Failure(AuthError.TOKEN_EXCHANGE_FAILED, reason)
        }

    /**
     * Refreshes [session] using [refreshToken] and persists the result on success.
     * Clears the stored session and moves state to [AuthState.SignedOut] when the failure
     * means the session can no longer be trusted — a definitive `invalid_grant`, an id_token
     * whose `sub` changed, or an id_token that fails claim validation (bad aud/azp/iss/sub).
     * A network error, 5xx, or a merely-expired id_token leaves the session intact.
     * Runs under [mutex].
     */
    private suspend fun refreshLocked(session: AuthSession, refreshToken: String): AuthResult {
        return try {
            val response = tokenClient.refreshAccessToken(config, refreshToken)
            when (val idTokenCheck = idTokenValidator.check(response.idToken)) {
                IdTokenCheck.Valid -> Unit
                // Stale but not an attack signal — surface it, keep the session.
                IdTokenCheck.Expired -> {
                    Logger.e(TAG, "id_token rejected on refresh: ${idTokenCheck.diagnostic}")
                    return AuthResult.Failure(AuthError.TOKEN_REFRESH_FAILED, idTokenCheck.diagnostic)
                }
                // Wrong aud/azp/iss, no sub, malformed: the session can't be trusted — drop it.
                is IdTokenCheck.Rejected -> {
                    Logger.e(TAG, "id_token rejected on refresh: ${idTokenCheck.reason}")
                    clearSessionState()
                    return AuthResult.Failure(AuthError.TOKEN_REFRESH_FAILED, idTokenCheck.reason)
                }
            }
            // close() can land while refreshAccessToken() is suspended — don't persist or
            // emit SignedIn onto a client that's already torn down.
            if (isClosed) return AuthResult.Failure(AuthError.UNKNOWN, "This SocialAuthClient has been closed")
            val updated = sessionFrom(response, previous = session)
            persist(updated)
            Logger.i(TAG, "Access token refreshed for uid=${updated.user.uid}")
            AuthResult.Success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SubjectChangedException) {
            // Identity changed under us — a possible token-substitution signal; drop the session.
            Logger.e(TAG, "Token refresh failed: ${e.message}")
            clearSessionState()
            AuthResult.Failure(e.error, e.message, e)
        } catch (e: OAuthException) {
            Logger.e(TAG, "Token refresh failed: ${e.message}")
            // A definitively revoked refresh token also means the stored session is dead.
            if (e.oauthErrorCode == OAUTH_ERROR_INVALID_GRANT) {
                clearSessionState()
            }
            AuthResult.Failure(e.error, e.message, e)
        } catch (e: Exception) {
            Logger.e(TAG, "Unexpected token refresh failure", e)
            AuthResult.Failure(AuthError.TOKEN_REFRESH_FAILED, e.message, e)
        }
    }

    private fun sessionFrom(response: OAuthTokenResponse, previous: AuthSession?): AuthSession {
        // OIDC Core §12.2: an id_token returned on a refresh MUST keep the same `sub`.
        if (previous != null && response.uid != null && response.uid != previous.user.uid) {
            throw SubjectChangedException()
        }
        // A refresh grant usually carries no identity; inherit it from the prior session.
        val identityFailure = if (previous != null) AuthError.TOKEN_REFRESH_FAILED else AuthError.TOKEN_EXCHANGE_FAILED
        val uid = response.uid ?: previous?.user?.uid ?: throw OAuthException(
            identityFailure,
            "token response contained no user identity (id_token/sub)",
        )
        val expiresAt = response.expiresInSeconds?.let { seconds ->
            val now = timeProvider.currentTimeMillis()
            // `<= 0` means already expired; clamp a huge value so `now + s*1000` can't overflow.
            if (seconds <= 0) now
            else now + seconds.coerceAtMost(MAX_EXPIRES_IN_SECONDS) * MILLIS_PER_SECOND
        }
        val tokens = AuthTokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken ?: previous?.tokens?.refreshToken,
            // A refresh grant usually returns no id_token (Google never does). Carry the
            // prior one forward only while it is strictly unexpired — never hand a backend a
            // stale id_token (no clock-skew leeway here, unlike a freshly-received token).
            idToken = response.idToken ?: previous?.tokens?.idToken?.takeIf { idTokenValidator.isRetainable(it) },
            expiresAtEpochMillis = expiresAt,
            // RFC 6749 §5.1: `scope` is omitted from the response when it equals what was
            // requested, so fall back to the prior session's scopes, then the requested ones.
            scopes = response.grantedScopes.ifEmpty { previous?.tokens?.scopes ?: config.scopes },
        )
        val user = AuthUser(
            uid = uid,
            email = response.email ?: previous?.user?.email,
            displayName = response.displayName ?: previous?.user?.displayName,
            photoUrl = response.photoUrl ?: previous?.user?.photoUrl,
            provider = config.provider,
        )
        return AuthSession(user, tokens)
    }

    /**
     * Reads the persisted session once, on the constructing thread (seeds [_authState]).
     * A store that throws on `load()` (corrupt keystore, revoked key, missing permission)
     * must not blow up the constructor — start signed out. A restored-but-dead session
     * (hard expired, no refresh token) is dropped so the UI never briefly shows the user
     * as signed in.
     */
    private fun restoreInitialAuthState(): AuthState {
        val restored = runCatching { sessionStore.load() }
            .onFailure { Logger.w(TAG, "Could not restore a session; starting signed out: ${it.message}") }
            .getOrNull() ?: return AuthState.SignedOut

        // leewayMillis = 0: only drop a session that is *hard* expired here. One with a few
        // seconds left is still usable — don't wipe it on app start just because it sits
        // inside the proactive-refresh window and has no refresh token.
        val hardExpiredAndUnrefreshable =
            restored.tokens.isExpiredAt(timeProvider.currentTimeMillis(), leewayMillis = 0L) &&
                restored.tokens.refreshToken == null
        return if (hardExpiredAndUnrefreshable) {
            runCatching { sessionStore.clear() }
            AuthState.SignedOut
        } else {
            AuthState.SignedIn(restored)
        }
    }

    /** A terminal failure if [close] has been called, else null. */
    private fun closedFailure(): AuthResult? =
        if (isClosed) AuthResult.Failure(AuthError.UNKNOWN, "This SocialAuthClient has been closed") else null

    /**
     * Drops the persisted session and moves state to [AuthState.SignedOut]. A store that
     * throws on `clear()` (e.g. a locked keystore) must still not leave the client stuck
     * reporting "signed in". Call sites run under [mutex].
     */
    private fun clearSessionState() {
        runCatching { sessionStore.clear() }
            .onFailure { Logger.w(TAG, "Could not clear the stored session: ${it.message}") }
        _authState.value = AuthState.SignedOut
    }

    private fun persist(session: AuthSession) {
        // A failed store write must not sink an otherwise-successful sign-in: the session
        // is still valid in memory for this process; it just won't survive a restart.
        runCatching { sessionStore.save(session) }
            .onFailure { Logger.w(TAG, "Could not persist the session: ${it.message}") }
        _authState.value = AuthState.SignedIn(session)
    }

    private fun randomState(): String =
        randomBytes(STATE_BYTES).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

    private companion object {
        const val TAG = SOCIAL_AUTH_LOG_TAG

        /** 16 bytes → 128 bits of CSRF-`state` / OIDC-`nonce` entropy. */
        const val STATE_BYTES = 16

        /** Its presence in the requested scopes is what makes this an OpenID Connect flow. */
        const val OPENID_SCOPE = "openid"

        const val OAUTH_ERROR_INVALID_GRANT = "invalid_grant"
    }
}
