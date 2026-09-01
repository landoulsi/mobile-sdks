package com.landoulsi.socialauth.testing

import com.landoulsi.socialauth.AuthorizationCodeProvider
import com.landoulsi.socialauth.AuthorizationRequest
import com.landoulsi.socialauth.AuthorizationResult

/**
 * Scriptable [AuthorizationCodeProvider] for tests.
 *
 * By default it "succeeds" by echoing the request state and returning [code].
 * Set [nextResult] to force a specific outcome, or [respond] for per-request logic.
 * The most recent [AuthorizationRequest] is captured in [lastRequest].
 */
class FakeAuthorizationCodeProvider(
    var code: String = "fake-auth-code",
    var nextResult: AuthorizationResult? = null,
    var respond: ((AuthorizationRequest) -> AuthorizationResult)? = null,
) : AuthorizationCodeProvider {

    var lastRequest: AuthorizationRequest? = null
        private set

    var invocations: Int = 0
        private set

    override suspend fun authorize(request: AuthorizationRequest): AuthorizationResult {
        lastRequest = request
        invocations++
        respond?.let { return it(request) }
        nextResult?.let { return it }
        return AuthorizationResult.Success(code = code, state = request.state)
    }
}
