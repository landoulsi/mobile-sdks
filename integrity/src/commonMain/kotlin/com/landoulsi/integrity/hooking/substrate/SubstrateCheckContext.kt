package com.landoulsi.integrity.hooking.substrate

import com.landoulsi.integrity.model.IntegritySignal

/**
 * Abstraction for platform-specific operations used by Substrate detection checks.
 *
 * Provides file system introspection shared by iOS (MobileSubstrate) and Android
 * (Cydia Substrate) implementations. Android Cydia Substrate detection is
 * intentionally limited for the initial pass; the iOS implementation is the
 * primary focus per the roadmap.
 */
interface SubstrateCheckContext {
    /** Returns `true` if the file at [path] exists and is readable. */
    fun fileExists(path: String): Boolean

    /** Returns the entries of the directory at [path], or an empty list on failure. */
    fun directoryContents(path: String): List<String>
}