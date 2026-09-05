package com.landoulsi.integrity.hooking.frida

import com.landoulsi.integrity.model.IntegritySignal

/**
 * Abstraction for platform-specific operations used by Frida detection checks.
 *
 * Decouples detection heuristics from platform framework types to enable
 * JVM host testing with fakes and to keep commonMain free of Android/iOS
 * dependencies (Clean Architecture dependency inversion).
 */
interface FridaCheckContext {
    /** Returns `true` if the file at [path] exists and is readable. */
    fun fileExists(path: String): Boolean

    /** Returns the lines of the file at [path], or an empty list on failure. */
    fun readFileLines(path: String): List<String>

    /** Attempts a short-lived TCP probe to [port] on localhost; returns `true`
      * if the connection succeeds within the platform-imposed timeout. */
    fun isPortOpen(port: Int): Boolean

    /** Returns `true` if a process named [processName] is currently running
      * on the device; implementation is platform-specific. */
    fun isProcessRunning(processName: String): Boolean

    /** Returns `true` if a package named [packageName] is installed; implementation
      * is platform-specific. */
    fun isPackageInstalled(packageName: String): Boolean
}