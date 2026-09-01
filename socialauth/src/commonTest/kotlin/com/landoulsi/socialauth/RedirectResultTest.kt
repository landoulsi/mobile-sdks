package com.landoulsi.socialauth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RedirectResultTest {

    @Test
    fun codePlusStateIsSuccess() {
        val result = RedirectResult.from(code = "abc", state = "xyz", error = null, errorDescription = null)
        val success = assertIs<AuthorizationResult.Success>(result)
        assertEquals("abc", success.code)
        assertEquals("xyz", success.state)
    }

    @Test
    fun errorTakesPrecedenceOverCode() {
        val result = RedirectResult.from(code = "abc", state = null, error = "access_denied", errorDescription = "no")
        val failure = assertIs<AuthorizationResult.Failure>(result)
        assertEquals(AuthorizationError.ProviderReported("access_denied"), failure.error)
        assertEquals("no", failure.description)
    }

    @Test
    fun blankErrorIsIgnored() {
        val result = RedirectResult.from(code = "abc", state = null, error = "", errorDescription = null)
        assertIs<AuthorizationResult.Success>(result)
    }

    @Test
    fun neitherCodeNorErrorIsInvalidRedirect() {
        val result = RedirectResult.from(code = null, state = "s", error = null, errorDescription = null)
        val failure = assertIs<AuthorizationResult.Failure>(result)
        assertEquals(AuthorizationError.InvalidRedirect, failure.error)
    }

    @Test
    fun blankCodeIsInvalidRedirect() {
        val result = RedirectResult.from(code = "  ", state = null, error = null, errorDescription = null)
        assertIs<AuthorizationResult.Failure>(result)
    }
}
