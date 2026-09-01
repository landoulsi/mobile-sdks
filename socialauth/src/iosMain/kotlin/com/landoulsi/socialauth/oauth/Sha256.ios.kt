package com.landoulsi.socialauth.oauth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
internal actual fun sha256(bytes: ByteArray): ByteArray {
    val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
    digest.usePinned { output ->
        val outputPointer = output.addressOf(0).reinterpret<UByteVar>()
        if (bytes.isEmpty()) {
            CC_SHA256(null, 0u, outputPointer)
        } else {
            bytes.usePinned { input ->
                CC_SHA256(input.addressOf(0), bytes.size.toUInt(), outputPointer)
            }
        }
    }
    return digest
}
