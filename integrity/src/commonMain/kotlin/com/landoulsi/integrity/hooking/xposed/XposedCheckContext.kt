package com.landoulsi.integrity.hooking.xposed

import com.landoulsi.integrity.model.IntegritySignal

/**
 * Abstraction for platform-specific operations used by Xposed detection checks.
 *
 * Xposed is Android‑only; the interface is deliberately kept minimal and
 * Android‑biased. No iOS actual is provided — the evaluator is constructed
 * with a concrete Android context only, matching the KMP convention for
 * platform‑specific detectors (see `AndroidXposedCheckContext`).
 */
interface XposedCheckContext {
    /** Returns `true` if the file at [path] exists and is readable. */
    fun fileExists(path: String): Boolean

    /** Returns the lines of the file at [path], or an empty list on failure. */
    fun readFileLines(path: String): List<String>

    /** Returns `true` if a package named [packageName] is installed; implementation
      * is platform‑specific. */
    fun isPackageInstalled(packageName: String): Boolean

    /** Returns `true` if the class named [className] can be reflectively loaded
      * by the runtime class loader; implementation is platform‑specific. */
    fun isClassLoadable(className: String): Boolean
}