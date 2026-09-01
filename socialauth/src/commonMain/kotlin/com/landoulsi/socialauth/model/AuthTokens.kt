package com.landoulsi.socialauth.model

import com.landoulsi.socialauth.internal.DEFAULT_CLOCK_SKEW_LEEWAY_MILLIS
import kotlinx.serialization.Serializable

/**
 * OAuth token material for an authenticated session.
 *
 * @property accessToken bearer token for calling provider APIs. Always present.
 * @property refreshToken long-lived token used to mint new access tokens. Providers
 *   commonly return this only on the *first* consent (Google requires
 *   `access_type=offline` + `prompt=consent`), so it is nullable.
 * @property idToken signed OpenID Connect JWT carrying identity claims, when `openid`
 *   scope was requested. A refresh grant that returns no fresh `id_token` (Google never
 *   does) carries the previous one forward only while it is still unexpired; an expired
 *   one is dropped to null rather than handed on.
 * @property expiresAtEpochMillis wall-clock instant the [accessToken] stops being valid,
 *   derived from the `expires_in` response field at exchange time. Null when the provider
 *   does not report an expiry.
 * @property scopes scopes actually granted by the provider (may differ from those requested).
 */
@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String? = null,
    val idToken: String? = null,
    val expiresAtEpochMillis: Long? = null,
    val scopes: List<String> = emptyList(),
) {
    /**
     * Whether the access token is expired (or within [leewayMillis] of expiring) at [nowMillis].
     * Tokens with no known expiry are treated as still valid.
     *
     * The default [leewayMillis] assumes an access-token lifetime comfortably longer than a
     * minute (true for Google/Apple/Microsoft). Pass a smaller value if you know your provider
     * issues very short-lived tokens, or the SDK will refresh eagerly.
     */
    fun isExpiredAt(nowMillis: Long, leewayMillis: Long = DEFAULT_LEEWAY_MILLIS): Boolean {
        val expiry = expiresAtEpochMillis ?: return false
        return nowMillis >= expiry - leewayMillis
    }

    /** Redacted so tokens never reach logs / crash reporters via `"$authState"` etc. */
    override fun toString(): String = buildString {
        append("AuthTokens(accessToken=***")
        append(", refreshToken=").append(if (refreshToken != null) "***" else "null")
        append(", idToken=").append(if (idToken != null) "***" else "null")
        append(", expiresAtEpochMillis=").append(expiresAtEpochMillis)
        append(", scopes=").append(scopes)
        append(")")
    }

    companion object {
        const val DEFAULT_LEEWAY_MILLIS: Long = DEFAULT_CLOCK_SKEW_LEEWAY_MILLIS
    }
}
