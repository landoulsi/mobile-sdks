package com.landoulsi.socialauth.oauth

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PkceTest {

    private val verifierChars = Regex("^[A-Za-z0-9\\-._~]+$")

    /** Deterministic byte source seeded from [seed], for reproducible assertions. */
    private fun seeded(seed: Int): (Int) -> ByteArray = { size -> Random(seed).nextBytes(size) }

    @Test
    fun verifierMeetsRfc7636() {
        val codes = Pkce.generate(seeded(42))
        assertTrue(codes.codeVerifier.length in 43..128, "verifier length ${codes.codeVerifier.length}")
        assertTrue(verifierChars.matches(codes.codeVerifier), "verifier has invalid chars: ${codes.codeVerifier}")
    }

    @Test
    fun challengeIsBase64UrlSha256OfVerifierWithoutPadding() {
        val codes = Pkce.generate(seeded(7))
        val expected = Base64Url.encode(sha256(codes.codeVerifier.encodeToByteArray()))
        assertEquals(expected, codes.codeChallenge)
        assertEquals("S256", PkceCodes.CHALLENGE_METHOD)
        assertTrue(!codes.codeChallenge.contains('='), "challenge must not be padded")
        assertTrue(!codes.codeChallenge.contains('+') && !codes.codeChallenge.contains('/'), "must be url-safe")
    }

    @Test
    fun matchesRfc7636AppendixBVector() {
        // RFC 7636 §4.1/§4.2 worked example.
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        val challenge = Base64Url.encode(sha256(verifier.encodeToByteArray()))
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", challenge)
    }

    @Test
    fun verifierIsUnpaddedUrlSafeBase64OfTheRandomBytes() {
        val bytes = ByteArray(48) { it.toByte() }
        val codes = Pkce.generate { bytes }
        assertEquals(Base64Url.encode(bytes), codes.codeVerifier)
        assertTrue(!codes.codeVerifier.contains('='))
    }

    @Test
    fun deterministicForSameByteSource() {
        assertEquals(Pkce.generate(seeded(99)), Pkce.generate(seeded(99)))
    }

    @Test
    fun differsAcrossCallsWithSecureRandom() {
        assertTrue(Pkce.generate() != Pkce.generate())
    }

    @Test
    fun toStringRedactsTheVerifier() {
        val rendered = Pkce.generate(seeded(1)).toString()
        assertTrue("codeVerifier=***" in rendered, rendered)
    }
}
