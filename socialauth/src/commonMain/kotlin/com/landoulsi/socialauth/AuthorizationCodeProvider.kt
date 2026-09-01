package com.landoulsi.socialauth

/**
 * A single browser round-trip request: send the user to [authorizationUrl] and
 * capture the redirect back to [redirectUri].
 *
 * The PKCE `code_challenge` is already baked into [authorizationUrl]; the secret
 * `code_verifier` deliberately never crosses this boundary — it stays inside the SDK
 * for the token exchange.
 *
 * @property state opaque CSRF token that must come back unchanged on the redirect.
 */
data class AuthorizationRequest(
    val authorizationUrl: String,
    val redirectUri: String,
    val state: String,
)

/**
 * The outcome of one [AuthorizationCodeProvider.authorize] call.
 */
sealed interface AuthorizationResult {

    /** The provider redirected back with an authorization `code`. */
    data class Success(val code: String, val state: String?) : AuthorizationResult

    /** The user closed the browser / cancelled. */
    data object Cancelled : AuthorizationResult

    /** The provider redirected back with an `error`, or the flow could not start. */
    data class Failure(val error: AuthorizationError, val description: String? = null) : AuthorizationResult
}

/**
 * Why an [AuthorizationResult.Failure] happened. The SDK-side cases are exhaustive so a
 * consumer's `when` needs no `else`; [ProviderReported] carries an open-ended RFC 6749
 * error code echoed back by the identity provider on the redirect (e.g. `access_denied`).
 */
sealed interface AuthorizationError {
    /** The browser / Custom Tab could not be launched. */
    data object LaunchFailed : AuthorizationError

    /** The redirect came back without a usable `code` or `error`. */
    data object InvalidRedirect : AuthorizationError

    /** No authorizer is bound / available on this platform. */
    data object ProviderUnavailable : AuthorizationError

    /** The request was malformed (e.g. an unusable `redirectUri`). */
    data object InvalidConfiguration : AuthorizationError

    /** The platform's interactive browser session reported a non-cancellation error. */
    data object InteractiveFlowFailed : AuthorizationError

    /** The platform's interactive browser session refused to start. */
    data object InteractiveFlowStartFailed : AuthorizationError

    /** An RFC 6749 §4.1.2.1 error code the provider put on the redirect. */
    data class ProviderReported(val code: String) : AuthorizationError
}

/**
 * Platform seam for the interactive "get an authorization code" step.
 *
 * `commonMain` owns all OAuth logic (URL building, PKCE, token exchange, session
 * persistence); this is the only piece that must touch a browser. Android ships
 * [com.landoulsi.socialauth.RedirectAuthorizer] (Custom Tabs); other hosts bind
 * their own via `SocialAuthClientFactory.bindAuthorizer { }`.
 */
fun interface AuthorizationCodeProvider {
    suspend fun authorize(request: AuthorizationRequest): AuthorizationResult
}
