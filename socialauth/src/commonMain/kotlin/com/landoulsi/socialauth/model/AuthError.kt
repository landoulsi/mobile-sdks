package com.landoulsi.socialauth.model

/**
 * Coarse, stable failure categories for social sign-in.
 *
 * Kept deliberately small so callers can branch on it exhaustively; the
 * originating exception (when any) travels alongside in [AuthResult.Failure.cause].
 */
enum class AuthError {
    /** Network transport failed (no connectivity, timeout, TLS, DNS). */
    NETWORK,

    /** The provider is not usable on this platform / no authorizer was bound. */
    PROVIDER_UNAVAILABLE,

    /** The supplied [com.landoulsi.socialauth.SocialAuthConfig] is missing or malformed. */
    INVALID_CONFIGURATION,

    /** The browser authorization step failed or returned an error / mismatched state. */
    AUTHORIZATION_FAILED,

    /** Exchanging the authorization code for tokens failed. */
    TOKEN_EXCHANGE_FAILED,

    /** Refreshing an expired access token failed. */
    TOKEN_REFRESH_FAILED,

    /** An operation needs an active session but none exists. */
    NO_ACTIVE_SESSION,

    /** Anything not covered above. */
    UNKNOWN,
}
