package com.landoulsi.integrity.root

/**
 * Abstraction for platform-specific operations used by root detection checks.
 *
 * Decouples detection heuristics from Android framework types ([android.content.Context],
 * [java.io.File], [android.os.Build]) to enable JVM host testing with fakes.
 */
interface RootCheckContext {
    fun fileExists(path: String): Boolean
    fun readFileLines(path: String): List<String>
    fun getBuildTag(): String
    fun isPackageInstalled(packageName: String): Boolean
}
