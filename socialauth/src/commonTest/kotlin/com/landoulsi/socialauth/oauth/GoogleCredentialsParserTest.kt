package com.landoulsi.socialauth.oauth

import com.landoulsi.socialauth.model.SocialProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GoogleCredentialsParserTest {

    private val installedJson = """
        {
          "installed": {
            "client_id": "abc.apps.googleusercontent.com",
            "client_secret": "secret-xyz",
            "auth_uri": "https://accounts.google.com/o/oauth2/auth",
            "token_uri": "https://oauth2.googleapis.com/token",
            "redirect_uris": ["com.example.app:/oauth2redirect", "http://localhost"]
          }
        }
    """.trimIndent()

    @Test
    fun parsesInstalledStanza() {
        val client = GoogleCredentialsParser.parse(installedJson)!!
        assertEquals("abc.apps.googleusercontent.com", client.clientId)
        assertEquals("secret-xyz", client.clientSecret)
        assertEquals(2, client.redirectUris.size)
        assertTrue(GoogleCredentialsParser.isValid(client))
    }

    @Test
    fun parsesWebStanza() {
        val webJson = """{"web":{"client_id":"web-id","client_secret":"ws"}}"""
        val client = GoogleCredentialsParser.parse(webJson)!!
        assertEquals("web-id", client.clientId)
    }

    @Test
    fun invalidJsonIsNull() {
        assertNull(GoogleCredentialsParser.parse("{ not json"))
        assertNull(GoogleCredentialsParser.parse("""{"other":{}}"""))
    }

    @Test
    fun isValidRejectsBlankClientId() {
        assertFalse(GoogleCredentialsParser.isValid(GoogleOAuthClient(clientId = "")))
        assertFalse(GoogleCredentialsParser.isValid(null))
    }

    @Test
    fun toConfigDropsSecretByDefaultAndUsesFirstRedirect() {
        val client = GoogleCredentialsParser.parse(installedJson)!!
        val config = GoogleCredentialsParser.toConfig(client)

        assertEquals(SocialProvider.GOOGLE, config.provider)
        assertEquals("abc.apps.googleusercontent.com", config.clientId)
        assertEquals("com.example.app:/oauth2redirect", config.redirectUri)
        assertNull(config.clientSecret)
        assertEquals("https://oauth2.googleapis.com/token", config.tokenEndpoint)
        assertTrue(config.usePkce)
    }

    @Test
    fun toConfigKeepsSecretWhenRequested() {
        val client = GoogleCredentialsParser.parse(installedJson)!!
        val config = GoogleCredentialsParser.toConfig(client, includeClientSecret = true)
        assertEquals("secret-xyz", config.clientSecret)
    }

    @Test
    fun toConfigWithNoRedirectUriThrows() {
        val client = GoogleOAuthClient(clientId = "id", redirectUris = emptyList())
        assertFailsWith<IllegalArgumentException> { GoogleCredentialsParser.toConfig(client) }
    }

    @Test
    fun toConfigWithNoClientIdThrows() {
        val client = GoogleOAuthClient(clientId = "  ", redirectUris = listOf("myapp://cb"))
        assertFailsWith<IllegalArgumentException> { GoogleCredentialsParser.toConfig(client) }
    }

    @Test
    fun toConfigHonoursRedirectOverride() {
        val client = GoogleCredentialsParser.parse(installedJson)!!
        val config = GoogleCredentialsParser.toConfig(client, redirectUri = "myapp://cb")
        assertEquals("myapp://cb", config.redirectUri)
    }
}
