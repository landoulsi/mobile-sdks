package com.landoulsi.socialauth.testing

import com.landoulsi.socialauth.SocialAuthClient
import com.landoulsi.socialauth.internal.DEFAULT_OIDC_SCOPES
import com.landoulsi.socialauth.model.AuthError
import com.landoulsi.socialauth.model.AuthResult
import com.landoulsi.socialauth.model.AuthSession
import com.landoulsi.socialauth.model.AuthState
import com.landoulsi.socialauth.model.AuthTokens
import com.landoulsi.socialauth.model.AuthUser
import com.landoulsi.socialauth.model.SocialProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [SocialAuthClient] for tests and previews — no browser, no network.
 *
 * [signIn] returns [signInResult]; when that is [AuthResult.Success] the session is
 * adopted as current and [authState] updates. [signOut] always clears.
 */
class FakeSocialAuthClient(
    initialSession: AuthSession? = null,
    var signInResult: AuthResult = AuthResult.Success(defaultSession()),
) : SocialAuthClient {

    private val _authState = MutableStateFlow<AuthState>(
        initialSession?.let { AuthState.SignedIn(it) } ?: AuthState.SignedOut,
    )
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    override val currentSession: AuthSession?
        get() = (_authState.value as? AuthState.SignedIn)?.session

    var signInInvocations: Int = 0
        private set
    var signOutInvocations: Int = 0
        private set

    override suspend fun signIn(): AuthResult {
        signInInvocations++
        (signInResult as? AuthResult.Success)?.let { _authState.value = AuthState.SignedIn(it.session) }
        return signInResult
    }

    override suspend fun signOut() {
        signOutInvocations++
        _authState.value = AuthState.SignedOut
    }

    override suspend fun currentAccessToken(): String? = currentSession?.tokens?.accessToken

    override suspend fun refreshSession(): AuthResult {
        // Mirror DefaultSocialAuthClient's contract so caller tests don't get false positives.
        val session = currentSession
            ?: return AuthResult.Failure(AuthError.NO_ACTIVE_SESSION, "Not signed in")
        if (session.tokens.refreshToken == null) {
            return AuthResult.Failure(AuthError.NO_ACTIVE_SESSION, "No refresh token held")
        }
        return AuthResult.Success(session)
    }

    override fun close() = Unit

    /** Directly install a session without going through [signIn]. */
    fun setSession(session: AuthSession?) {
        _authState.value = session?.let { AuthState.SignedIn(it) } ?: AuthState.SignedOut
    }

    companion object {
        fun defaultSession(): AuthSession = AuthSession(
            user = AuthUser(
                uid = "fake-uid",
                email = "user@example.com",
                displayName = "Test User",
                provider = SocialProvider.GOOGLE,
            ),
            tokens = AuthTokens(
                accessToken = "fake-access-token",
                refreshToken = "fake-refresh-token",
                idToken = null,
                expiresAtEpochMillis = null,
                scopes = DEFAULT_OIDC_SCOPES,
            ),
        )
    }
}
