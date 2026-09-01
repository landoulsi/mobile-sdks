package com.landoulsi.socialauth.oauth

/**
 * A PKCE (RFC 7636) code pair. The challenge derivation method is always
 * [CHALLENGE_METHOD] (`S256`); PKCE `plain` is not offered.
 *
 * @property codeVerifier high-entropy secret kept on the client and sent with the token request.
 * @property codeChallenge value sent on the authorization request.
 */
internal data class PkceCodes(
    val codeVerifier: String,
    val codeChallenge: String,
) {
    /** Redacted — [codeVerifier] is a secret. */
    override fun toString(): String = "PkceCodes(codeVerifier=***, codeChallenge=$codeChallenge)"

    companion object {
        const val CHALLENGE_METHOD = "S256"
    }
}

/**
 * Generates PKCE code pairs.
 */
internal object Pkce {

    // 48 random bytes → 64 base64url chars, comfortably inside RFC 7636's 43..128 range.
    // Base64url-encoding a byte string avoids the modulo bias of mapping bytes onto an alphabet.
    private const val VERIFIER_BYTES = 48

    /**
     * @param randomBytes source of cryptographically secure random bytes. Defaults to
     *   the platform CSPRNG; tests inject a deterministic source.
     */
    fun generate(randomBytes: (Int) -> ByteArray = ::secureRandomBytes): PkceCodes {
        val verifier = Base64Url.encode(randomBytes(VERIFIER_BYTES))
        val challenge = Base64Url.encode(sha256(verifier.encodeToByteArray()))
        return PkceCodes(codeVerifier = verifier, codeChallenge = challenge)
    }
}
