package com.landoulsi.socialauth.oauth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdTokenValidatorTest {

    private val farFuture = 4_000_000_000L
    private fun validator(issuer: String? = "https://accounts.google.com", now: Long = 0L) =
        IdTokenValidator(clientId = "cid", issuer = issuer, currentTimeMillis = { now })

    // `sub` is REQUIRED on every id_token, so inject a default unless the caller set one.
    private fun token(vararg claims: Pair<String, Any>): String {
        val withSub = if (claims.any { it.first == "sub" }) claims.toList() else listOf("sub" to "u", *claims)
        val json = withSub.joinToString(",", "{", "}") { (k, v) ->
            when (v) {
                is String -> "\"$k\":\"$v\""
                is List<*> -> "\"$k\":[${v.joinToString(",") { "\"$it\"" }}]"
                else -> "\"$k\":$v"
            }
        }
        return testJwtRaw(json)
    }

    @Test
    fun nullTokenIsAllowed() {
        assertNull(validator().validate(null))
    }

    @Test
    fun validTokenPasses() {
        assertNull(
            validator().validate(
                token("aud" to "cid", "iss" to "https://accounts.google.com", "exp" to farFuture),
            ),
        )
    }

    @Test
    fun rejectsWrongAudience() {
        assertEquals(
            "id_token audience mismatch",
            validator().validate(token("aud" to "other", "iss" to "https://accounts.google.com", "exp" to farFuture)),
        )
    }

    @Test
    fun rejectsMissingExp() {
        assertEquals(
            "id_token missing exp claim",
            validator().validate(token("aud" to "cid", "iss" to "https://accounts.google.com")),
        )
    }

    @Test
    fun rejectsMissingSub() {
        // OIDC Core: `sub` is REQUIRED in an id_token.
        val noSub = testJwtRaw("""{"aud":"cid","iss":"https://accounts.google.com","exp":$farFuture}""")
        assertEquals("id_token missing sub claim", validator().validate(noSub))
    }

    @Test
    fun rejectsExpiredBeyondLeeway() {
        val result = validator(now = 5_000_000L).validate(
            token("aud" to "cid", "iss" to "https://accounts.google.com", "exp" to 1_000L),
        )
        assertEquals("id_token expired", result)
    }

    @Test
    fun toleratesClockDriftWithinLeeway() {
        // exp is 30s in the "past" but within the 60s leeway.
        assertNull(
            validator(now = 1_030_000L).validate(
                token("aud" to "cid", "iss" to "https://accounts.google.com", "exp" to 1_000L),
            ),
        )
    }

    @Test
    fun acceptsSchemelessGoogleIssuer() {
        assertNull(
            validator().validate(token("aud" to "cid", "iss" to "accounts.google.com", "exp" to farFuture)),
        )
    }

    @Test
    fun rejectsIssuerMismatch() {
        assertEquals(
            "id_token issuer mismatch",
            validator().validate(token("aud" to "cid", "iss" to "https://evil.example.com", "exp" to farFuture)),
        )
    }

    @Test
    fun skipsIssuerCheckWhenNotConfigured() {
        assertNull(
            validator(issuer = null).validate(token("aud" to "cid", "iss" to "https://anything", "exp" to farFuture)),
        )
    }

    @Test
    fun enforcesAzpRules() {
        // multi-aud without azp
        assertEquals(
            "id_token azp missing",
            validator().validate(
                token("aud" to listOf("cid", "b"), "iss" to "https://accounts.google.com", "exp" to farFuture),
            ),
        )
        // azp for someone else
        assertEquals(
            "id_token azp mismatch",
            validator().validate(
                token("aud" to "cid", "azp" to "b", "iss" to "https://accounts.google.com", "exp" to farFuture),
            ),
        )
    }

    @Test
    fun enforcesNonceWhenExpected() {
        val t = token("aud" to "cid", "iss" to "https://accounts.google.com", "exp" to farFuture, "nonce" to "n1")
        assertNull(validator().validate(t, expectedNonce = "n1"))
        assertEquals("id_token nonce mismatch", validator().validate(t, expectedNonce = "n2"))
    }

    @Test
    fun rejectsMalformedJwt() {
        assertTrue(validator().validate("not-a-jwt")!!.contains("well-formed"))
    }

    @Test
    fun rejectsIatInTheFutureButToleratesAMissingIat() {
        val future = token(
            "aud" to "cid", "iss" to "https://accounts.google.com", "exp" to farFuture, "iat" to 10_000L,
        )
        // now = 1s, iat = 10_000s → well beyond leeway → forgery/replay signal.
        assertEquals(
            IdTokenCheck.Rejected("id_token issued in the future"),
            validator(now = 1_000L).check(future),
        )
        // A near-past iat is fine; a completely absent iat is tolerated.
        assertEquals(IdTokenCheck.Valid, validator(now = 20_000_000L).check(future))
        val noIat = token("aud" to "cid", "iss" to "https://accounts.google.com", "exp" to farFuture)
        assertEquals(IdTokenCheck.Valid, validator().check(noIat))
    }

    @Test
    fun checkClassifiesExpiredSeparatelyFromRejected() {
        val expired = token("aud" to "cid", "iss" to "https://accounts.google.com", "exp" to 1_000L)
        assertEquals(IdTokenCheck.Expired, validator(now = 5_000_000L).check(expired))

        val wrongAud = token("aud" to "other", "iss" to "https://accounts.google.com", "exp" to farFuture)
        assertEquals(IdTokenCheck.Rejected("id_token audience mismatch"), validator().check(wrongAud))

        val good = token("aud" to "cid", "iss" to "https://accounts.google.com", "exp" to farFuture)
        assertEquals(IdTokenCheck.Valid, validator().check(good))
        assertEquals(IdTokenCheck.Valid, validator().check(null))
    }

    @Test
    fun isRetainableAppliesNoPastExpiryLeeway() {
        val expiring = token("aud" to "cid", "iss" to "https://accounts.google.com", "exp" to 1_000L)
        // 30s past exp: validate() tolerates it (clock-skew leeway) but it must NOT be retained.
        assertNull(validator(now = 1_030_000L).validate(expiring))
        assertFalse(validator(now = 1_030_000L).isRetainable(expiring))
        // Comfortably before exp: retainable.
        assertTrue(validator(now = 500_000L).isRetainable(expiring))
        // Fails the claim checks (wrong aud) → not retainable regardless of expiry.
        val wrongAud = token("aud" to "other", "iss" to "https://accounts.google.com", "exp" to farFuture)
        assertFalse(validator().isRetainable(wrongAud))
    }

    @Test
    fun oversizedExpDoesNotOverflow() {
        val huge = token("aud" to "cid", "iss" to "https://accounts.google.com", "exp" to Long.MAX_VALUE)
        // Capped, not overflowed to a negative instant — so it reads as "not expired".
        assertNull(validator(now = 1_000L).validate(huge))
        assertTrue(validator(now = 1_000L).isRetainable(huge))
    }
}
