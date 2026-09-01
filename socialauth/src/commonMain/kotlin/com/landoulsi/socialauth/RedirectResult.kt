package com.landoulsi.socialauth

/**
 * Turns the query parameters of an OAuth redirect URI into an [AuthorizationResult].
 * Shared by the Android and iOS authorizers so redirect handling is defined once
 * and unit-tested in common code.
 */
internal object RedirectResult {

    fun from(
        code: String?,
        state: String?,
        error: String?,
        errorDescription: String?,
    ): AuthorizationResult = when {
        !error.isNullOrBlank() ->
            AuthorizationResult.Failure(AuthorizationError.ProviderReported(error), errorDescription)
        !code.isNullOrBlank() -> AuthorizationResult.Success(code, state)
        else -> AuthorizationResult.Failure(
            AuthorizationError.InvalidRedirect,
            "redirect carried neither 'code' nor 'error'",
        )
    }
}
