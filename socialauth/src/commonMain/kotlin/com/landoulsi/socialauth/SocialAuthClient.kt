package com.landoulsi.socialauth

import com.landoulsi.socialauth.model.AuthResult
import com.landoulsi.socialauth.model.AuthSession
import com.landoulsi.socialauth.model.AuthState
import kotlinx.coroutines.flow.StateFlow

/**
 * The public entry point for social sign-in.
 *
 * Obtain one from [SocialAuthClientFactory.create]. All suspending calls are
 * main-safe (work is dispatched internally) and safe to cancel.
 */
interface SocialAuthClient : AutoCloseable {

    /** Current auth state; emits [AuthState.SignedIn] once a session is established or restored. */
    val authState: StateFlow<AuthState>

    /** The active session, or null when signed out. Convenience over [authState]. */
    val currentSession: AuthSession?

    /**
     * Runs the full interactive sign-in flow: browser authorization → code exchange →
     * session persistence. Idempotent-ish: if a valid session already exists it is
     * returned without prompting.
     */
    suspend fun signIn(): AuthResult

    /**
     * Clears the local session and the backing [AuthSessionStore], and moves
     * [authState] to [AuthState.SignedOut].
     *
     * This is a *local* sign-out: it does not call the provider's token-revocation
     * endpoint, so any already-issued access token stays valid until it expires.
     * Revoke server-side separately if your threat model requires it.
     *
     * If a token refresh is in flight when this is called, it suspends until that
     * refresh finishes (bounded by the HTTP timeout) so the two can't race.
     */
    suspend fun signOut()

    /**
     * Returns a currently-valid access token, transparently refreshing first if the
     * stored one is expired and a refresh token is available. Null when signed out or
     * when a refresh was required but failed.
     */
    suspend fun currentAccessToken(): String?

    /**
     * Forces a token refresh using the stored refresh token.
     *
     * @return [AuthResult.Success] with the updated session, or [AuthResult.Failure]
     *   ([com.landoulsi.socialauth.model.AuthError.NO_ACTIVE_SESSION] when signed out or
     *   no refresh token is held).
     */
    suspend fun refreshSession(): AuthResult

    /**
     * Releases the underlying HTTP engine (thread pools, connections). Call when the
     * client is no longer needed. The client must not be used after this.
     */
    override fun close()
}
