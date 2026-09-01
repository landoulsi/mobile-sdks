package com.landoulsi.socialauth.oauth

import com.landoulsi.socialauth.SocialAuthConfig
import com.landoulsi.socialauth.internal.DEFAULT_OIDC_SCOPES
import com.landoulsi.socialauth.internal.socialAuthJson
import com.landoulsi.socialauth.model.SocialProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * The OAuth client stanza from a Google Cloud `client_secret.json`
 * (the `"installed"` or `"web"` object).
 */
@Serializable
data class GoogleOAuthClient(
    @SerialName("client_id") val clientId: String,
    @SerialName("client_secret") val clientSecret: String? = null,
    @SerialName("auth_uri") val authorizationUri: String = "https://accounts.google.com/o/oauth2/v2/auth",
    @SerialName("token_uri") val tokenUri: String = "https://oauth2.googleapis.com/token",
    @SerialName("redirect_uris") val redirectUris: List<String> = emptyList(),
) {
    /** Redacted — [clientSecret] must not reach logs. */
    override fun toString(): String =
        "GoogleOAuthClient(clientId=$clientId, clientSecret=${if (clientSecret != null) "***" else "null"}, " +
            "authorizationUri=$authorizationUri, tokenUri=$tokenUri, redirectUris=$redirectUris)"
}

/**
 * Parses Google `client_secret.json` files into a [GoogleOAuthClient], and builds a
 * ready-to-use [SocialAuthConfig] from one.
 */
object GoogleCredentialsParser {

    /** @return the client stanza (`installed` preferred, then `web`), or null if unparseable. */
    fun parse(jsonString: String): GoogleOAuthClient? {
        return try {
            val file = socialAuthJson.decodeFromString<GoogleClientSecretFile>(jsonString)
            file.installed ?: file.web
        } catch (e: Exception) {
            null
        }
    }

    /** A parsed client is usable only if it carries a client id. */
    fun isValid(client: GoogleOAuthClient?): Boolean =
        client != null && client.clientId.isNotBlank()

    /**
     * Builds a [SocialAuthConfig] for Google from a parsed [client].
     *
     * @param redirectUri overrides the redirect URI; defaults to the first one in the file.
     * @param scopes scopes to request.
     * @param includeClientSecret when false (the default for mobile public clients) the
     *   secret is dropped even if present in the file, so PKCE alone secures the exchange.
     * @throws IllegalArgumentException if no redirect URI is available.
     */
    fun toConfig(
        client: GoogleOAuthClient,
        redirectUri: String? = null,
        scopes: List<String> = DEFAULT_OIDC_SCOPES,
        includeClientSecret: Boolean = false,
    ): SocialAuthConfig {
        require(isValid(client)) { "GoogleOAuthClient has no client_id" }
        val resolvedRedirectUri = redirectUri
            ?: client.redirectUris.firstOrNull()
        requireNotNull(resolvedRedirectUri) { "No redirect_uris in client_secret.json and none supplied" }
        return SocialAuthConfig(
            clientId = client.clientId,
            redirectUri = resolvedRedirectUri,
            provider = SocialProvider.GOOGLE,
            scopes = scopes,
            authorizationEndpoint = client.authorizationUri,
            tokenEndpoint = client.tokenUri,
            clientSecret = client.clientSecret?.takeIf { includeClientSecret && it.isNotBlank() },
            issuer = "https://accounts.google.com",
        )
    }
}

@Serializable
private data class GoogleClientSecretFile(
    @SerialName("installed") val installed: GoogleOAuthClient? = null,
    @SerialName("web") val web: GoogleOAuthClient? = null,
)
