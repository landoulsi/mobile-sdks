package com.landoulsi.integrity.hooking.substrate

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.SignalSeverity
import kotlin.time.Clock

object SubstrateSignal {
    const val SUBSTRATE_DYLIB_INJECTION = "substrate_dylib_injection"
    const val SUBSTRATE_FRAMEWORK = "substrate_framework"
    const val SUBSTRATE_TWEAK_INJECT = "substrate_tweak_inject"
    const val SUBSTRATE_LOADED = "substrate_loaded"

    /** Every signal id this vector can emit; used to seed the [IntegrityResult] catalog. */
    val all: Set<String> = setOf(
        SUBSTRATE_DYLIB_INJECTION,
        SUBSTRATE_FRAMEWORK,
        SUBSTRATE_TWEAK_INJECT,
        SUBSTRATE_LOADED,
    )

    object Check {
        const val SUBSTRATE_DYLIB_INJECTION = "substrate_dylib_injection"
        const val SUBSTRATE_FRAMEWORK = "substrate_framework"
        const val SUBSTRATE_TWEAK_INJECT = "substrate_tweak_inject"
        const val SUBSTRATE_LOADED = "substrate_loaded"
    }
}

internal val SUBSTRATE_DYLIB_PATHS = listOf(
    "/Library/MobileSubstrate/DynamicLibraries",
    "/usr/lib/TweakInject",
    "/var/jb/Library/MobileSubstrate/DynamicLibraries",
    "/var/jb/usr/lib/TweakInject",
)

internal val SUBSTRATE_FRAMEWORK_PATHS = listOf(
    "/Library/MobileSubstrate/MobileSubstrate.dylib",
    "/var/jb/Library/MobileSubstrate/MobileSubstrate.dylib",
)

internal val SUBSTRATE_TWEAK_INJECT_PATHS = listOf(
    "/usr/lib/TweakInject",
    "/var/jb/usr/lib/TweakInject",
)

internal val SUBSTRATE_LOADER_PATHS = listOf(
    "/usr/lib/substrate/SubstrateLoader.dylib",
    "/usr/lib/libsubstrate.dylib",
    "/usr/lib/substrate/SubstrateInserter.dylib",
    "/Library/Frameworks/CydiaSubstrate.framework/CydiaSubstrate",
    "/var/jb/usr/lib/libsubstrate.dylib",
)

internal fun checkSubstrateDylibInjection(context: SubstrateCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    for (path in SUBSTRATE_DYLIB_PATHS) {
        try {
            val directoryEntries = context.directoryContents(path)
            if (directoryEntries.isNotEmpty()) {
                signals.add(
                    IntegritySignal(
                        id = SubstrateSignal.SUBSTRATE_DYLIB_INJECTION,
                        name = "Substrate Dynamic Library Injection Detected",
                        category = IntegrityCategory.HOOKING_OR_TAMPERING,
                        severity = SignalSeverity.MEDIUM,
                        confidence = 0.85,
                        details = "Tweak injection directory found at: $path (${directoryEntries.size} files)",
                        detectedAt = currentTimestampMs,
                        metadata = mapOf(
                            "path" to path,
                            "file_count" to directoryEntries.size.toString(),
                            "check" to SubstrateSignal.Check.SUBSTRATE_DYLIB_INJECTION,
                        ),
                    ),
                )
            }
        } catch (_: Exception) {
            // Directory not accessible — best-effort only
        }
    }

    return signals
}

internal fun checkSubstrateFramework(context: SubstrateCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    for (path in SUBSTRATE_FRAMEWORK_PATHS) {
        try {
            if (context.fileExists(path)) {
                signals.add(
                    IntegritySignal(
                        id = SubstrateSignal.SUBSTRATE_FRAMEWORK,
                        name = "Substrate Framework Detected",
                        category = IntegrityCategory.HOOKING_OR_TAMPERING,
                        severity = SignalSeverity.MEDIUM,
                        confidence = 0.9,
                        details = "MobileSubstrate framework binary found at: $path",
                        detectedAt = currentTimestampMs,
                        metadata = mapOf(
                            "path" to path,
                            "check" to SubstrateSignal.Check.SUBSTRATE_FRAMEWORK,
                        ),
                    ),
                )
            }
        } catch (_: Exception) {
            // File not accessible — best-effort only
        }
    }

    return signals
}

internal fun checkSubstrateTweakInject(context: SubstrateCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    for (path in SUBSTRATE_TWEAK_INJECT_PATHS) {
        try {
            if (context.fileExists(path)) {
                signals.add(
                    IntegritySignal(
                        id = SubstrateSignal.SUBSTRATE_TWEAK_INJECT,
                        name = "Tweak Inject Present",
                        category = IntegrityCategory.HOOKING_OR_TAMPERING,
                        severity = SignalSeverity.LOW,
                        confidence = 0.6,
                        details = "$path is present, indicating substrate-based tweak installation",
                        detectedAt = currentTimestampMs,
                        metadata = mapOf(
                            "path" to path,
                            "check" to SubstrateSignal.Check.SUBSTRATE_TWEAK_INJECT,
                        ),
                    ),
                )
            }
        } catch (_: Exception) {
            // File not accessible — best-effort only
        }
    }

    return signals
}

internal fun checkSubstrateLoaded(context: SubstrateCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    for (path in SUBSTRATE_LOADER_PATHS) {
        try {
            if (context.fileExists(path)) {
                signals.add(
                    IntegritySignal(
                        id = SubstrateSignal.SUBSTRATE_LOADED,
                        name = "Substrate Loader Detected",
                        category = IntegrityCategory.HOOKING_OR_TAMPERING,
                        severity = SignalSeverity.HIGH,
                        confidence = 0.9,
                        details = "Substrate loader binary found at: $path",
                        detectedAt = currentTimestampMs,
                        metadata = mapOf(
                            "path" to path,
                            "check" to SubstrateSignal.Check.SUBSTRATE_LOADED,
                        ),
                    ),
                )
            }
        } catch (_: Exception) {
            // File not accessible — best-effort only
        }
    }

    return signals
}