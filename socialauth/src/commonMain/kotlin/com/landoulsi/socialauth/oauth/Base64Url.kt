package com.landoulsi.socialauth.oauth

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * base64url (RFC 4648 §5) helpers without padding, as required by JWT and PKCE.
 */
@OptIn(ExperimentalEncodingApi::class)
internal object Base64Url {

    private val noPadding = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
    private val tolerant = Base64.UrlSafe.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

    fun encode(bytes: ByteArray): String = noPadding.encode(bytes)

    /** Decodes with or without `=` padding. */
    fun decode(value: String): ByteArray = tolerant.decode(value)
}
