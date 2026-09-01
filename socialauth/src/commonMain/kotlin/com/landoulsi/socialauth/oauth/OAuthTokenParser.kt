package com.landoulsi.socialauth.oauth

import com.landoulsi.socialauth.internal.socialAuthJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/**
 * Normalized view of a provider token-endpoint response, with identity claims
 * already lifted out of the `id_token`.
 *
 * @property refreshToken null when the provider withheld one (common on repeat consent).
 * @property expiresInSeconds lifetime of [accessToken] in seconds, when reported.
 * @property uid stable user id from the `id_token` `sub` claim (or a `user_id` field).
 *   **Null on a `refresh_token` grant** — most providers omit identity data there, and
 *   the caller is expected to carry the id forward from the prior session.
 */
internal data class OAuthTokenResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val idToken: String? = null,
    val expiresInSeconds: Long? = null,
    val grantedScopes: List<String> = emptyList(),
    val uid: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
) {
    /** Redacted — carries bearer tokens. */
    override fun toString(): String =
        "OAuthTokenResponse(accessToken=***, refreshToken=${if (refreshToken != null) "***" else "null"}, " +
            "idToken=${if (idToken != null) "***" else "null"}, expiresInSeconds=$expiresInSeconds, " +
            "grantedScopes=$grantedScopes, uid=$uid)"
}

/**
 * A structured error body returned by a token endpoint (RFC 6749 §5.2).
 */
internal data class OAuthErrorResponse(
    val error: String,
    val errorDescription: String? = null,
)

/**
 * Parses raw token-endpoint response bodies. Stateless; safe to reuse.
 */
internal class OAuthTokenParser {

    /**
     * Parses a **successful** token response body.
     *
     * Unlike the reference implementation this was adapted from, only `access_token`
     * is required — a missing `refresh_token` yields [OAuthTokenResponse.refreshToken] `= null`
     * rather than a parse failure, because providers legitimately omit it on repeat consent.
     *
     * @return the parsed response, or null if the body is not valid JSON or lacks `access_token`.
     */
    fun parseTokenResponse(body: String): OAuthTokenResponse? {
        return try {
            val jsonObject = socialAuthJson.parseToJsonElement(body).jsonObject

            val accessToken = jsonObject.string("access_token")
            if (accessToken.isNullOrBlank()) return null

            val idToken = jsonObject.string("id_token")
            // Decode the id_token payload once, not once per claim.
            val claims: JsonObject? = Jwt.claims(idToken)
            // as? JsonPrimitive tolerates an unexpected object/array; contentOrNull, not
            // content, because JsonNull.content is the literal string "null".
            fun claim(name: String) = (claims?.get(name) as? JsonPrimitive)?.contentOrNull

            OAuthTokenResponse(
                accessToken = accessToken,
                refreshToken = jsonObject.string("refresh_token")?.takeIf { it.isNotBlank() },
                idToken = idToken,
                expiresInSeconds = (jsonObject["expires_in"] as? JsonPrimitive)?.longOrNull,
                grantedScopes = jsonObject.string("scope")?.split(" ")?.filter { it.isNotBlank() } ?: emptyList(),
                // Prefer the OpenID `sub` claim; fall back to a provider `user_id`. May be
                // absent on a refresh grant — that is not a parse failure.
                // Prefer the id_token claim, then a top-level body field of the same name.
                uid = claim("sub")?.takeIf { it.isNotBlank() }
                    ?: jsonObject.string("user_id")?.takeIf { it.isNotBlank() },
                // Email is an account-linking hazard unless verified. When an id_token is
                // present it is authoritative: surface its `email` only when `email_verified`
                // is explicitly true (OIDC Core §5.1 — an absent claim is NOT "verified"),
                // and never fall back to a body field it omitted. Only a non-OIDC response
                // (no id_token at all) may use the top-level `email`. `email_verified` may be
                // a JSON boolean or the string "true"; contentOrNull normalizes both.
                email = when {
                    claims != null -> claim("email")?.takeIf { claim("email_verified") == "true" }
                    else -> jsonObject.string("email")
                },
                displayName = claim("name") ?: jsonObject.string("name"),
                photoUrl = claim("picture") ?: jsonObject.string("picture"),
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Parses an RFC 6749 error body, or null if it is not one. */
    fun parseErrorResponse(body: String): OAuthErrorResponse? {
        return try {
            val jsonObject = socialAuthJson.parseToJsonElement(body).jsonObject
            val error = jsonObject.string("error") ?: return null
            OAuthErrorResponse(error = error, errorDescription = jsonObject.string("error_description"))
        } catch (e: Exception) {
            null
        }
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
