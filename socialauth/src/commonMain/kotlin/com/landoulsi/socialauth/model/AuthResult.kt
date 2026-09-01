package com.landoulsi.socialauth.model

/**
 * Outcome of a sign-in / refresh attempt.
 */
sealed interface AuthResult {

    /** Authentication succeeded; [session] is now the active session. */
    data class Success(val session: AuthSession) : AuthResult

    /** The user dismissed the provider UI without completing sign-in. */
    data object Cancelled : AuthResult

    /**
     * Authentication failed.
     *
     * @property error stable category — branch on this, and derive any user-facing copy
     *   from it. Do not show [message] to end users.
     * @property message diagnostic detail for logs/bug reports. English, not localized,
     *   and free-form; safe to log (never contains tokens).
     * @property cause originating exception, when there was one.
     */
    data class Failure(
        val error: AuthError,
        val message: String? = null,
        val cause: Throwable? = null,
    ) : AuthResult
}
