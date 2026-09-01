package com.landoulsi.socialauth.oauth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Base64UrlTest {

    @Test
    fun encodeIsUnpaddedAndUrlSafe() {
        // 0xFF bytes exercise the '+'/'/' → '-'/'_' substitution.
        val encoded = Base64Url.encode(ByteArray(5) { 0xFF.toByte() })
        assertTrue('=' !in encoded)
        assertTrue('+' !in encoded && '/' !in encoded)
    }

    @Test
    fun roundTripsEveryTailLength() {
        for (length in 1..8) {
            val bytes = ByteArray(length) { (it * 37 + 11).toByte() }
            assertEquals(bytes.toList(), Base64Url.decode(Base64Url.encode(bytes)).toList(), "length=$length")
        }
    }

    @Test
    fun decodeToleratesInputThatAlreadyCarriesPadding() {
        val bytes = ByteArray(4) { it.toByte() }
        val unpadded = Base64Url.encode(bytes)
        val repadded = unpadded + "=".repeat((4 - unpadded.length % 4) % 4)
        assertEquals(bytes.toList(), Base64Url.decode(repadded).toList())
    }
}
