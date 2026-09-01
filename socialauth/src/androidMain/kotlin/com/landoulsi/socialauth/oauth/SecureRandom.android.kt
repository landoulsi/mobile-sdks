package com.landoulsi.socialauth.oauth

import java.security.SecureRandom

private val secureRandom = SecureRandom()

internal actual fun platformRandomBytes(size: Int): ByteArray =
    ByteArray(size).also(secureRandom::nextBytes)
