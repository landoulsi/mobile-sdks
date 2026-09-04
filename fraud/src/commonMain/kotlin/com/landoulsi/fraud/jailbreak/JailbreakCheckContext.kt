package com.landoulsi.fraud.jailbreak

/**
 * Abstraction for platform-specific operations used by jailbreak detection checks.
 *
 * Decouples detection heuristics from iOS framework types ([NSFileManager], [NSProcessInfo])
 * to enable host testing with fakes.
 */
interface JailbreakCheckContext {
    fun fileExists(path: String): Boolean
    fun directoryContents(path: String): List<String>
    fun canFork(): Boolean
    fun canWriteOutsideSandbox(path: String): Boolean
}

