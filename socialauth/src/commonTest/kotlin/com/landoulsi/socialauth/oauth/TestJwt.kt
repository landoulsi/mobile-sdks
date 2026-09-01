package com.landoulsi.socialauth.oauth

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
private fun segment(json: String) = Base64.UrlSafe.encode(json.encodeToByteArray()).trimEnd('=')

/**
 * Builds an unsigned JWT (`alg: none`, empty signature segment) whose payload is
 * the given string claims. Only the payload matters to [Jwt], which never verifies signatures.
 */
fun testJwt(claims: Map<String, String>): String {
    val payloadJson = claims.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
        "\"$key\":\"$value\""
    }
    return testJwtRaw(payloadJson)
}

/** As [testJwt] but takes the raw payload JSON, for non-string claims like an `aud` array. */
fun testJwtRaw(payloadJson: String): String =
    "${segment("""{"alg":"none","typ":"JWT"}""")}.${segment(payloadJson)}."
