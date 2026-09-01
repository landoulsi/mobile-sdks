package com.landoulsi.fraud

import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudSignal
import com.landoulsi.fraud.model.SignalSeverity
import kotlin.time.Clock

object JailbreakSignal {
    const val APP_BUNDLE = "jailbreak_app_bundle"
    const val SYSTEM_BINARY = "jailbreak_system_binary"
    const val FORK_CAPABILITY = "jailbreak_fork"
    const val SANDBOX_ESCAPE = "jailbreak_sandbox_escape"
    const val DYLIB_INJECTION = "jailbreak_dylib_injection"

    /** Every signal id this vector can emit; used to seed the [FraudResult] catalog. */
    val all: Set<String> = setOf(
        APP_BUNDLE,
        SYSTEM_BINARY,
        FORK_CAPABILITY,
        SANDBOX_ESCAPE,
        DYLIB_INJECTION,
    )

    object Check {
        const val APP_BUNDLE = "app_bundle"
        const val SYSTEM_BINARY = "system_binary"
        const val FORK_CAPABILITY = "fork_capability"
        const val SANDBOX_ESCAPE = "sandbox_escape"
        const val DYLIB_INJECTION = "dylib_injection"
    }
}

internal val JAILBREAK_APP_PATHS = listOf(
    "/Applications/Cydia.app",
    "/Applications/Sileo.app",
    "/Applications/Zebra.app",
    "/Applications/Filza.app",
    "/Applications/MxTube.app",
    "/Applications/RockApp.app",
    "/Applications/Icy.app",
    "/Applications/IntelliScreen.app",
    "/Applications/SBSettings.app",
    "/var/jb/Applications/Cydia.app",
    "/var/jb/Applications/Sileo.app",
    "/var/jb/Applications/Zebra.app",
)

internal val JAILBREAK_SYSTEM_PATHS = listOf(
    "/bin/bash",
    "/bin/sh",
    "/usr/sbin/sshd",
    "/usr/bin/ssh",
    "/etc/apt",
    "/private/var/lib/apt/",
    "/private/var/stash",
    "/private/var/tmp/cydia.log",
    "/Library/MobileSubstrate/MobileSubstrate.dylib",
    "/var/cache/apt",
    "/var/log/syslog",
    "/var/jb/usr/bin",
    "/var/jb/sbin",
)

internal val JAILBREAK_DYLIB_PATHS = listOf(
    "/Library/MobileSubstrate/DynamicLibraries",
    "/usr/lib/TweakInject",
)

internal const val SANDBOX_TEST_PATH = "/private/jailbreak_test_fraud.txt"

internal fun checkJailbreakApps(context: JailbreakCheckContext): List<FraudSignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    return JAILBREAK_APP_PATHS.filter { path ->
        try {
            context.fileExists(path)
        } catch (_: Exception) {
            false
        }
    }.map { path ->
        FraudSignal(
            id = JailbreakSignal.APP_BUNDLE,
            name = "Jailbreak Application Detected",
            category = FraudCategory.ROOT_OR_JAILBREAK,
            severity = SignalSeverity.HIGH,
            confidence = 1.0,
            details = "Jailbreak application bundle found at: $path",
            detectedAt = currentTimestampMs,
            metadata = mapOf("path" to path, "check" to JailbreakSignal.Check.APP_BUNDLE),
        )
    }
}

internal fun checkSystemBinaries(context: JailbreakCheckContext): List<FraudSignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    return JAILBREAK_SYSTEM_PATHS.filter { path ->
        try {
            context.fileExists(path)
        } catch (_: Exception) {
            false
        }
    }.map { path ->
        FraudSignal(
            id = JailbreakSignal.SYSTEM_BINARY,
            name = "Jailbreak System Path Detected",
            category = FraudCategory.ROOT_OR_JAILBREAK,
            severity = SignalSeverity.MEDIUM,
            confidence = 0.9,
            details = "Jailbreak-associated path found at: $path",
            detectedAt = currentTimestampMs,
            metadata = mapOf("path" to path, "check" to JailbreakSignal.Check.SYSTEM_BINARY),
        )
    }
}

internal fun checkForkCapability(context: JailbreakCheckContext): FraudSignal? {
    val forkSucceeded = try {
        context.canFork()
    } catch (_: Exception) {
        false
    }

    if (forkSucceeded) {
        return FraudSignal(
            id = JailbreakSignal.FORK_CAPABILITY,
            name = "Fork Capability Detected",
            category = FraudCategory.ROOT_OR_JAILBREAK,
            severity = SignalSeverity.CRITICAL,
            confidence = 1.0,
            details = "Process forking succeeded — stock iOS sandbox restricts fork()",
            detectedAt = Clock.System.now().toEpochMilliseconds(),
            metadata = mapOf("check" to JailbreakSignal.Check.FORK_CAPABILITY),
        )
    }

    return null
}

internal fun checkSandboxIntegrity(context: JailbreakCheckContext): FraudSignal? {
    val sandboxWriteSucceeded = try {
        context.canWriteOutsideSandbox(SANDBOX_TEST_PATH)
    } catch (_: Exception) {
        false
    }

    if (sandboxWriteSucceeded) {
        return FraudSignal(
            id = JailbreakSignal.SANDBOX_ESCAPE,
            name = "Sandbox Integrity Violation",
            category = FraudCategory.ROOT_OR_JAILBREAK,
            severity = SignalSeverity.HIGH,
            confidence = 1.0,
            details = "Able to write outside app sandbox at: $SANDBOX_TEST_PATH",
            detectedAt = Clock.System.now().toEpochMilliseconds(),
            metadata = mapOf("path" to SANDBOX_TEST_PATH, "check" to JailbreakSignal.Check.SANDBOX_ESCAPE),
        )
    }

    return null
}

internal fun checkDylibInjection(context: JailbreakCheckContext): List<FraudSignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<FraudSignal>()

    for (path in JAILBREAK_DYLIB_PATHS) {
        val directoryExists = try {
            context.fileExists(path)
        } catch (_: Exception) {
            false
        }
        if (directoryExists) {
            val directoryEntries = try {
                context.directoryContents(path)
            } catch (_: Exception) {
                emptyList()
            }
            if (directoryEntries.isNotEmpty()) {
                signals.add(
                    FraudSignal(
                        id = JailbreakSignal.DYLIB_INJECTION,
                        name = "Dynamic Library Injection Detected",
                        category = FraudCategory.ROOT_OR_JAILBREAK,
                        severity = SignalSeverity.MEDIUM,
                        confidence = 0.85,
                        details = "Tweak injection directory found at: $path (${directoryEntries.size} files)",
                        detectedAt = currentTimestampMs,
                        metadata = mapOf(
                            "path" to path,
                            "file_count" to directoryEntries.size.toString(),
                            "check" to JailbreakSignal.Check.DYLIB_INJECTION,
                        ),
                    ),
                )
            }
        }
    }

    return signals
}