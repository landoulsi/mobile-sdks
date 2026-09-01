package com.landoulsi.socialauth.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthTokensTest {

    @Test
    fun neverExpiresWithoutExpiry() {
        val tokens = AuthTokens(accessToken = "a", expiresAtEpochMillis = null)
        assertFalse(tokens.isExpiredAt(Long.MAX_VALUE))
    }

    @Test
    fun expiredWhenPastExpiry() {
        val tokens = AuthTokens(accessToken = "a", expiresAtEpochMillis = 1_000_000L)
        assertTrue(tokens.isExpiredAt(1_000_001L, leewayMillis = 0L))
        assertFalse(tokens.isExpiredAt(999_999L, leewayMillis = 0L))
    }

    @Test
    fun leewayTreatsSoonToExpireAsExpired() {
        val tokens = AuthTokens(accessToken = "a", expiresAtEpochMillis = 1_000_000L)
        assertTrue(tokens.isExpiredAt(950_000L, leewayMillis = 60_000L))
        assertFalse(tokens.isExpiredAt(930_000L, leewayMillis = 60_000L))
    }
}
