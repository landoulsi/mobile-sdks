package com.landoulsi.integrity.jailbreak

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.posix.EXIT_SUCCESS
import platform.posix._exit
import platform.posix.fork
import platform.posix.waitpid

/**
 * iOS implementation of [JailbreakCheckContext] wiring platform framework types
 * ([NSFileManager], `fork()`) into jailbreak detection heuristics.
 */
@OptIn(ExperimentalForeignApi::class)
class IosJailbreakCheckContext : JailbreakCheckContext {

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

    override fun canFork(): Boolean {
        if (NSProcessInfo.processInfo.environment["SIMULATOR_DEVICE_NAME"] != null) {
            return false
        }
        val processId = fork()
        return when {
            processId == 0 -> {
                _exit(EXIT_SUCCESS)
                false
            }
            processId > 0 -> {
                waitpid(processId, null, 0)
                true
            }
            else -> false
        }
    }

    override fun canWriteOutsideSandbox(path: String): Boolean = try {
        val created = fileManager.createFileAtPath(path, contents = null, attributes = null)
        if (created) {
            fileManager.removeItemAtPath(path, error = null)
            true
        } else {
            false
        }
    } catch (_: Exception) {
        try {
            fileManager.removeItemAtPath(path, error = null)
        } catch (_: Exception) {
            // Best-effort cleanup
        }
        false
    }
}

