package com.landoulsi.socialauth.oauth

import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JwtTest {

    private fun claim(jwt: String?, name: String): String? =
        Jwt.claims(jwt)?.get(name)?.jsonPrimitive?.contentOrNull

    @Test
    fun extractsPayloadClaims() {
        val token = testJwt(mapOf("sub" to "12345", "email" to "a@b.com", "name" to "Ada"))
        assertEquals("12345", claim(token, "sub"))
        assertEquals("a@b.com", claim(token, "email"))
        assertEquals("Ada", claim(token, "name"))
    }

    @Test
    fun missingOrMalformedIsNull() {
        assertNull(Jwt.claims(null))
        assertNull(Jwt.claims("not-a-jwt"))
        assertNull(Jwt.claims("only.two"))
        assertNull(Jwt.claims("a.b.c.d"), "4-part token is not a valid JWT")
    }

    @Test
    fun audiencesOfHandlesStringAndArray() {
        assertEquals(listOf("client-x"), Jwt.audiencesOf(Jwt.claims(testJwt(mapOf("aud" to "client-x")))!!))
        assertEquals(
            listOf("client-x", "client-y"),
            Jwt.audiencesOf(Jwt.claims(testJwtRaw("""{"aud":["client-x","client-y"]}"""))!!),
        )
        assertEquals(emptyList(), Jwt.audiencesOf(Jwt.claims(testJwt(mapOf("sub" to "u")))!!))
    }

    @Test
    fun stringAndLongClaimOf() {
        val claims = Jwt.claims(testJwtRaw("""{"iss":"https://accounts.google.com","exp":1700000000}"""))!!
        assertEquals("https://accounts.google.com", Jwt.stringClaimOf(claims, "iss"))
        assertEquals(1_700_000_000L, Jwt.longClaimOf(claims, "exp"))
        assertNull(Jwt.stringClaimOf(claims, "missing"))
        assertNull(Jwt.longClaimOf(claims, "iss"))
    }
}
