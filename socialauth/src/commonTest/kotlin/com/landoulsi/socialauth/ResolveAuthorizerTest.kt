package com.landoulsi.socialauth

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ResolveAuthorizerTest {

    private val request = AuthorizationRequest(
        authorizationUrl = "https://example.com/auth",
        redirectUri = "com.example.app:/cb",
        state = "s",
    )

    @Test
    fun explicitAuthorizerWins() = runTest {
        val explicit = AuthorizationCodeProvider { AuthorizationResult.Success("explicit", it.state) }
        val bound = AuthorizationCodeProvider { AuthorizationResult.Success("bound", it.state) }
        val resolved = resolveAuthorizer(explicit) { bound }
        assertEquals("explicit", (resolved.authorize(request) as AuthorizationResult.Success).code)
    }

    @Test
    fun fallsBackToBoundAuthorizer() = runTest {
        val bound = AuthorizationCodeProvider { AuthorizationResult.Success("bound", it.state) }
        val resolved = resolveAuthorizer(explicit = null) { bound }
        assertEquals("bound", (resolved.authorize(request) as AuthorizationResult.Success).code)
    }

    @Test
    fun noAuthorizerDegradesToProviderUnavailableFailure() = runTest {
        val resolved = resolveAuthorizer(explicit = null) { null }
        val failure = assertIs<AuthorizationResult.Failure>(resolved.authorize(request))
        assertEquals(AuthorizationError.ProviderUnavailable, failure.error)
    }
}
