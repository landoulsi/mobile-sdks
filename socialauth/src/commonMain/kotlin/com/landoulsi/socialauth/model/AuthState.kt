package com.landoulsi.socialauth.model

/**
 * Observable authentication state exposed by [com.landoulsi.socialauth.SocialAuthClient.authState].
 */
sealed interface AuthState {

    /** No session. This is the initial state before the store is consulted. */
    data object SignedOut : AuthState

    /** A session is active (restored from storage or just obtained). */
    data class SignedIn(val session: AuthSession) : AuthState
}
