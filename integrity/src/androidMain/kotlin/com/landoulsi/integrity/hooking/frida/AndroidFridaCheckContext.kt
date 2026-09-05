package com.landoulsi.integrity.hooking.frida

import android.content.Context
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Android implementation of [FridaCheckContext] wiring platform framework types
 * ([Context], [File], [Socket]) into Frida hooking framework detection heuristics.
 *
 * Ensures [Context.getApplicationContext] is retained to prevent leaking
 * short-lived [android.app.Activity] contexts across background detection sweeps.
 */
class AndroidFridaCheckContext(
    context: Context,
) : FridaCheckContext {

    private val applicationContext: Context = context.applicationContext ?: context

    override fun fileExists(path: String): Boolean = try {
        File(path).exists()
    } catch (_: Exception) {
        false
    }

    override fun readFileLines(path: String): List<String> = try {
        File(path).takeIf { it.exists() && it.canRead() }?.readLines() ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    override fun isPortOpen(port: Int): Boolean = try {
        val socket = Socket()
        socket.connect(InetSocketAddress("127.0.0.1", port), 500)
        socket.close()
        true
    } catch (_: Exception) {
        false
    }

    override fun isProcessRunning(processName: String): Boolean = try {
        val process = Runtime.getRuntime().exec("ps -w")
        val output = process.inputStream.bufferedReader().readLines()
        process.waitFor()
        output.any { it.contains(processName, ignoreCase = true) }
    } catch (_: Exception) {
        false
    }

    override fun isPackageInstalled(packageName: String): Boolean = try {
        applicationContext.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }
}