package com.landoulsi.integrity.hooking.substrate

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager

/**
 * iOS implementation of [SubstrateCheckContext] wiring platform framework types
 * ([NSFileManager]) into substrate hooking framework detection heuristics.
 *
 * Reuses the [NSFileManager] wiring already proven in [IosJailbreakCheckContext].
 */
@OptIn(ExperimentalForeignApi::class)
class IosSubstrateCheckContext : SubstrateCheckContext {

    private val fileManager = NSFileManager.defaultManager

    override fun fileExists(path: String): Boolean =
        fileManager.fileExistsAtPath(path)

    override fun directoryContents(path: String): List<String> =
        try {
            fileManager.contentsOfDirectoryAtPath(path, error = null)
                ?.mapNotNull { it?.toString() }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
}