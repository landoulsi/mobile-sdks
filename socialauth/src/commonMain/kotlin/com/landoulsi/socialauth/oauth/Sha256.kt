package com.landoulsi.socialauth.oauth

/**
 * SHA-256 digest, delegating to the platform's audited implementation
 * (`java.security.MessageDigest` on Android, CommonCrypto `CC_SHA256` on Apple).
 * Used only for the PKCE `S256` code challenge.
 */
internal expect fun sha256(bytes: ByteArray): ByteArray
