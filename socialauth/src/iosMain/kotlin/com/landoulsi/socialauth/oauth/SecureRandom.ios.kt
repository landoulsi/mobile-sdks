package com.landoulsi.socialauth.oauth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.arc4random_buf

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformRandomBytes(size: Int): ByteArray {
    if (size <= 0) return ByteArray(0)
    return ByteArray(size).apply {
        usePinned { pinned -> arc4random_buf(pinned.addressOf(0), size.toULong()) }
    }
}
