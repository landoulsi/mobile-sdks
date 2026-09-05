package com.landoulsi.integrity.hooking.substrate

import android.content.Context
import java.io.File

/**
 * Android implementation of [SubstrateCheckContext] for the initial pass.
 *
 * Cydia Substrate detection on Android is out of scope for this release;
 * this minimal implementation checks for known Cydia Substrate paths but
 * does not attempt full runtime introspection. A follow-up can add deeper
 * Cydia Substrate detection.
 */
class AndroidSubstrateCheckContext(
    context: Context,
) : SubstrateCheckContext {

    private val applicationContext: Context = context.applicationContext ?: context

    override fun fileExists(path: String): Boolean = try {
        File(path).exists()
    } catch (_: Exception) {
        false
    }

    override fun directoryContents(path: String): List<String> = try {
        File(path).listFiles()?.mapNotNull { it.name } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}