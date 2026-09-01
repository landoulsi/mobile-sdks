package com.landoulsi.socialauth.testing

import com.landoulsi.socialauth.model.AuthError
import com.landoulsi.socialauth.model.AuthResult
import com.landoulsi.socialauth.model.AuthState
import com.landoulsi.socialauth.model.AuthTokens
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class FakeSocialAuthClientTest {

    @Test
    fun signInAdoptsTheConfiguredSuccessSession() = runTest {
        val fake = FakeSocialAuthClient()
        assertIs<AuthState.SignedOut>(fake.authState.value)

        val result = assertIs<AuthResult.Success>(fake.signIn())
        assertEquals(result.session, fake.currentSession)
        assertIs<AuthState.SignedIn>(fake.authState.value)
        assertEquals(1, fake.signInInvocations)
    }

    @Test
    fun signOutClears() = runTest {
        val fake = FakeSocialAuthClient(initialSession = FakeSocialAuthClient.defaultSession())
        fake.signOut()
        assertNull(fake.currentSession)
        assertIs<AuthState.SignedOut>(fake.authState.value)
    }

    @Test
    fun refreshSessionMirrorsTheRealContract() = runTest {
        val fake = FakeSocialAuthClient()

        // signed out
        assertEquals(AuthError.NO_ACTIVE_SESSION, assertIs<AuthResult.Failure>(fake.refreshSession()).error)

        // signed in but no refresh token
        fake.setSession(
            FakeSocialAuthClient.defaultSession().copy(
                tokens = AuthTokens(accessToken = "a", refreshToken = null),
            ),
        )
        assertEquals(AuthError.NO_ACTIVE_SESSION, assertIs<AuthResult.Failure>(fake.refreshSession()).error)

        // signed in with a refresh token
        fake.setSession(FakeSocialAuthClient.defaultSession())
        assertIs<AuthResult.Success>(fake.refreshSession())
    }
}
