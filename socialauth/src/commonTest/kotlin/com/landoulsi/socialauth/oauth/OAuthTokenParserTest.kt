package com.landoulsi.socialauth.oauth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OAuthTokenParserTest {

    private val parser = OAuthTokenParser()

    @Test
    fun parsesFullSuccessResponse() {
        val idToken = testJwt(
            mapOf(
                "sub" to "user-1", "email" to "u@x.com", "email_verified" to "true",
                "name" to "U", "picture" to "http://p",
            ),
        )
        val body = """
            {
              "access_token": "at-123",
              "refresh_token": "rt-456",
              "id_token": "$idToken",
              "expires_in": 3600,
              "scope": "openid email profile",
              "token_type": "Bearer"
            }
        """.trimIndent()

        val parsed = parser.parseTokenResponse(body)!!
        assertEquals("at-123", parsed.accessToken)
        assertEquals("rt-456", parsed.refreshToken)
        assertEquals(3600L, parsed.expiresInSeconds)
        assertEquals(listOf("openid", "email", "profile"), parsed.grantedScopes)
        assertEquals("user-1", parsed.uid)
        assertEquals("u@x.com", parsed.email)
        assertEquals("U", parsed.displayName)
        assertEquals("http://p", parsed.photoUrl)
    }

    @Test
    fun missingRefreshTokenIsTolerated() {
        val idToken = testJwt(mapOf("sub" to "user-2"))
        val body = """{"access_token":"at","id_token":"$idToken","expires_in":100}"""
        val parsed = parser.parseTokenResponse(body)!!
        assertEquals("at", parsed.accessToken)
        assertNull(parsed.refreshToken)
        assertEquals("user-2", parsed.uid)
    }

    @Test
    fun blankRefreshTokenBecomesNull() {
        val idToken = testJwt(mapOf("sub" to "u"))
        val body = """{"access_token":"at","refresh_token":"  ","id_token":"$idToken"}"""
        assertNull(parser.parseTokenResponse(body)!!.refreshToken)
    }

    @Test
    fun jsonNullFieldsAreNullNotTheStringNull() {
        // JsonNull.content is the literal "null"; contentOrNull must be used instead.
        val idToken = testJwtRaw("""{"sub":"u","name":null,"picture":null}""")
        val body = """{"access_token":"at","refresh_token":null,"id_token":"$idToken","scope":null}"""
        val parsed = parser.parseTokenResponse(body)!!
        assertNull(parsed.refreshToken)
        assertNull(parsed.displayName)
        assertNull(parsed.photoUrl)
        assertEquals("u", parsed.uid)
    }

    @Test
    fun missingAccessTokenFailsParse() {
        assertNull(parser.parseTokenResponse("""{"refresh_token":"rt"}"""))
    }

    @Test
    fun idTokenEmailIsDroppedWhenEmailVerifiedIsFalse() {
        val idToken = testJwtRaw("""{"sub":"u","email":"spoof@x.com","email_verified":false}""")
        val parsed = parser.parseTokenResponse("""{"access_token":"at","id_token":"$idToken"}""")!!
        assertNull(parsed.email, "an id_token email marked email_verified:false must not be surfaced")

        val verified = testJwtRaw("""{"sub":"u","email":"real@x.com","email_verified":true}""")
        assertEquals(
            "real@x.com",
            parser.parseTokenResponse("""{"access_token":"at","id_token":"$verified"}""")!!.email,
        )
    }

    @Test
    fun aTopLevelEmailIsNotUsedWhenAnIdTokenIsPresentButOmitsEmail() {
        // With an id_token the id_token is authoritative for identity — a body `email`
        // beside an id_token that has no email claim is ignored, not trusted.
        val idToken = testJwtRaw("""{"sub":"u"}""")
        val body = """{"access_token":"at","id_token":"$idToken","email":"body@x.com"}"""
        assertNull(parser.parseTokenResponse(body)!!.email)
    }

    @Test
    fun refreshStyleResponseParsesWithNullUid() {
        // A refresh grant returns no id_token/user_id — that is valid, not a parse failure.
        val parsed = parser.parseTokenResponse("""{"access_token":"at-new","expires_in":3599}""")!!
        assertEquals("at-new", parsed.accessToken)
        assertNull(parsed.uid)
        assertEquals(3599L, parsed.expiresInSeconds)
    }

    @Test
    fun fallsBackToUserIdField() {
        val body = """{"access_token":"at","user_id":"legacy-9","email":"e@e.com"}"""
        val parsed = parser.parseTokenResponse(body)!!
        assertEquals("legacy-9", parsed.uid)
        assertEquals("e@e.com", parsed.email)
    }

    @Test
    fun fallsBackToTopLevelProfileFields() {
        val body = """{"access_token":"at","user_id":"u","name":"Grace Hopper","picture":"https://p/x.png"}"""
        val parsed = parser.parseTokenResponse(body)!!
        assertEquals("Grace Hopper", parsed.displayName)
        assertEquals("https://p/x.png", parsed.photoUrl)
    }

    @Test
    fun garbageIsNull() {
        assertNull(parser.parseTokenResponse("not json"))
        assertNull(parser.parseTokenResponse(""))
    }

    @Test
    fun parsesErrorResponse() {
        val errorResponse = parser.parseErrorResponse("""{"error":"invalid_grant","error_description":"bad code"}""")!!
        assertEquals("invalid_grant", errorResponse.error)
        assertEquals("bad code", errorResponse.errorDescription)
    }

    @Test
    fun errorParserRejectsSuccessBody() {
        assertNull(parser.parseErrorResponse("""{"access_token":"at"}"""))
    }

    @Test
    fun successParserRejectsErrorBody() {
        assertTrue(parser.parseTokenResponse("""{"error":"invalid_grant"}""") == null)
    }
}
