package com.landoulsi.socialauth.oauth

import com.landoulsi.socialauth.internal.socialAuthJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Minimal, unverified reader for the *payload* of a JWT.
 *
 * This does **not** validate the signature — it is only used to lift identity
 * claims (`sub`, `email`, `name`, `picture`, `aud`, `iss`, `exp`) out of an
 * `id_token` that the SDK itself just received over TLS directly from the
 * provider's token endpoint. Never use it on a token from an untrusted channel.
 *
 * Callers decode the payload once via [claims] and read individual values with the
 * `*Of(JsonObject)` helpers.
 */
internal object Jwt {

    /** A compact JWT (RFC 7519 §3) is exactly header.payload.signature. */
    private const val JWT_SEGMENT_COUNT = 3
    private const val JWT_PAYLOAD_INDEX = 1

    fun claims(jwt: String?): JsonObject? {
        val parts = jwt?.split(".") ?: return null
        if (parts.size != JWT_SEGMENT_COUNT) return null
        return try {
            val decoded = Base64Url.decode(parts[JWT_PAYLOAD_INDEX]).decodeToString()
            socialAuthJson.parseToJsonElement(decoded) as? JsonObject
        } catch (e: Exception) {
            null
        }
    }

    fun stringClaimOf(claims: JsonObject, name: String): String? =
        (claims[name] as? JsonPrimitive)?.contentOrNull

    fun longClaimOf(claims: JsonObject, name: String): Long? =
        (claims[name] as? JsonPrimitive)?.longOrNull

    /** The `aud` claim as a list — it may be a single string or an array (RFC 7519 §4.1.3). */
    fun audiencesOf(claims: JsonObject): List<String> = when (val aud = claims["aud"]) {
        is JsonArray -> aud.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        is JsonPrimitive -> listOfNotNull(aud.contentOrNull)
        else -> emptyList()
    }
}
