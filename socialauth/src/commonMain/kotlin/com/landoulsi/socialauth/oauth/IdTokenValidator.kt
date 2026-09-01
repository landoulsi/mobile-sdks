package com.landoulsi.socialauth.oauth

import com.landoulsi.socialauth.internal.DEFAULT_CLOCK_SKEW_LEEWAY_MILLIS
import com.landoulsi.socialauth.internal.MILLIS_PER_SECOND
import kotlinx.serialization.json.JsonObject

/**
 * Outcome of an [IdTokenValidator] check. [Expired] is split out from [Rejected] because
 * a merely-stale id_token is not a token-substitution signal — a caller deciding whether
 * to wipe a session should keep it for [Expired] and drop it for [Rejected].
 */
internal sealed interface IdTokenCheck {
    /** A log-safe reason string, or null when the token is absent or valid. */
    val diagnostic: String?

    data object Valid : IdTokenCheck {
        override val diagnostic: String? = null
    }

    data object Expired : IdTokenCheck {
        override val diagnostic: String = "id_token expired"
    }

    data class Rejected(val reason: String) : IdTokenCheck {
        override val diagnostic: String get() = reason
    }
}

/**
 * OpenID Connect Core §3.1.3.7 claim checks for an `id_token` received directly from
 * the token endpoint over TLS. The signature is **not** verified (the channel is
 * trusted); this guards claim substitution and replay:
 * - `sub` must be present (OIDC Core: REQUIRED);
 * - `aud` must list [clientId], and `azp` (present, or required for a multi-audience
 *   token) must be [clientId];
 * - `iss` must match [issuer] when one is configured (Google's schemeless form allowed);
 * - `exp` is mandatory and must be in the future, minus a small clock-skew leeway;
 * - `nonce` must equal the one sent, when a nonce was used.
 *
 * A missing `id_token` is not this class's concern — a refresh grant legitimately omits
 * one, and the "openid ⇒ id_token" rule for the initial exchange is enforced by the caller.
 */
internal class IdTokenValidator(
    private val clientId: String,
    private val issuer: String?,
    private val currentTimeMillis: () -> Long,
    private val clockSkewLeewayMillis: Long = DEFAULT_CLOCK_SKEW_LEEWAY_MILLIS,
) {
    /** Classifies [idToken]; [IdTokenCheck.Valid] when it is absent or passes every check. */
    fun check(idToken: String?, expectedNonce: String? = null): IdTokenCheck {
        if (idToken == null) return IdTokenCheck.Valid
        val claims = Jwt.claims(idToken) ?: return IdTokenCheck.Rejected("id_token is not a well-formed JWT")
        return checkClaims(claims, expectedNonce)
    }

    /** @return the [IdTokenCheck.diagnostic] of [check] — null when [idToken] is absent or valid. */
    fun validate(idToken: String?, expectedNonce: String? = null): String? =
        check(idToken, expectedNonce).diagnostic

    /**
     * True only if [idToken] passes [check] **and** has not passed its `exp` at all (no
     * clock-skew tolerance). Use this to decide whether a stored id_token may be carried
     * forward onto a refreshed session — a backend will reject one that is even slightly
     * expired, so the permissive leeway [check] uses is wrong for that decision.
     */
    fun isRetainable(idToken: String): Boolean {
        val claims = Jwt.claims(idToken) ?: return false
        if (checkClaims(claims, expectedNonce = null) != IdTokenCheck.Valid) return false
        val expirationSeconds = Jwt.longClaimOf(claims, "exp") ?: return false
        return currentTimeMillis() < epochSecondsToMillis(expirationSeconds)
    }

    private fun checkClaims(claims: JsonObject, expectedNonce: String?): IdTokenCheck {
        if (Jwt.stringClaimOf(claims, "sub").isNullOrBlank()) return IdTokenCheck.Rejected("id_token missing sub claim")

        val audiences = Jwt.audiencesOf(claims)
        if (clientId !in audiences) return IdTokenCheck.Rejected("id_token audience mismatch")

        val authorizedParty = Jwt.stringClaimOf(claims, "azp")
        if (audiences.size > 1 && authorizedParty == null) return IdTokenCheck.Rejected("id_token azp missing")
        if (authorizedParty != null && authorizedParty != clientId) return IdTokenCheck.Rejected("id_token azp mismatch")

        if (issuer != null && !issuerMatches(Jwt.stringClaimOf(claims, "iss"), issuer)) {
            return IdTokenCheck.Rejected("id_token issuer mismatch")
        }

        val expirationSeconds = Jwt.longClaimOf(claims, "exp")
            ?: return IdTokenCheck.Rejected("id_token missing exp claim")
        if (currentTimeMillis() - clockSkewLeewayMillis >= epochSecondsToMillis(expirationSeconds)) {
            return IdTokenCheck.Expired
        }

        // OIDC Core §3.1.3.7: an `iat` in the future beyond clock skew is a forgery/replay
        // signal. Reject that, but never reject for age alone, and tolerate a missing `iat`
        // (some non-conformant providers omit it).
        val issuedAtSeconds = Jwt.longClaimOf(claims, "iat")
        if (issuedAtSeconds != null &&
            epochSecondsToMillis(issuedAtSeconds) - clockSkewLeewayMillis > currentTimeMillis()
        ) {
            return IdTokenCheck.Rejected("id_token issued in the future")
        }

        if (expectedNonce != null && Jwt.stringClaimOf(claims, "nonce") != expectedNonce) {
            return IdTokenCheck.Rejected("id_token nonce mismatch")
        }
        return IdTokenCheck.Valid
    }

    // Clamp so a malformed, oversized `exp`/`iat` can't overflow `Long` when scaled to millis.
    private fun epochSecondsToMillis(epochSeconds: Long): Long =
        epochSeconds.coerceIn(0L, MAX_EPOCH_SECONDS) * MILLIS_PER_SECOND

    private companion object {
        // Epoch seconds for the year 2100 — a claim past this is capped here so scaling it
        // to milliseconds can never overflow `Long`. No legitimate id_token time claim is later.
        const val MAX_EPOCH_SECONDS = 4_102_444_800L

        fun issuerMatches(actual: String?, expected: String): Boolean =
            actual != null && bareIssuer(actual) == bareIssuer(expected)

        // OIDC issuers are https; Google's only quirk is sometimes omitting the scheme.
        fun bareIssuer(issuer: String): String = issuer.removePrefix("https://").trimEnd('/')
    }
}
