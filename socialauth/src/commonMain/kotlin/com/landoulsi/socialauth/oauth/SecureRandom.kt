package com.landoulsi.socialauth.oauth

/**
 * Cryptographically secure random bytes.
 *
 * PKCE (RFC 7636 §7.1) and the OAuth `state` parameter both require a CSPRNG;
 * `kotlin.random.Random` is not one on any target.
 */
internal fun secureRandomBytes(size: Int): ByteArray {
    require(size >= 0) { "size must be non-negative, was $size" }
    return if (size == 0) ByteArray(0) else platformRandomBytes(size)
}

/**
 * Platform CSPRNG: `java.security.SecureRandom` on Android, `arc4random_buf` on Apple.
 * [size] is guaranteed `> 0` by [secureRandomBytes].
 */
internal expect fun platformRandomBytes(size: Int): ByteArray
