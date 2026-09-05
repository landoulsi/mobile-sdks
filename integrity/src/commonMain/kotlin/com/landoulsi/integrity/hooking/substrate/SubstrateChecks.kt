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

    try {
        if (context.fileExists("/Library/MobileSubstrate/MobileSubstrate.dylib")) {
            signals.add(
                IntegritySignal(
                    id = SubstrateSignal.SUBSTRATE_FRAMEWORK,
                    name = "Substrate Framework Detected",
                    category = IntegrityCategory.HOOKING_OR_TAMPERING,
                    severity = SignalSeverity.MEDIUM,
                    confidence = 0.9,
                    details = "MobileSubstrate.dylib is present on this device",
                    detectedAt = currentTimestampMs,
                    metadata = mapOf("check" to SubstrateSignal.Check.SUBSTRATE_FRAMEWORK),
                ),
            )
        }
    } catch (_: Exception) {
        // File not accessible — best-effort only
    }

    return signals
}

internal fun checkSubstrateTweakInject(context: SubstrateCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    try {
        if (context.fileExists("/usr/lib/TweakInject")) {
            signals.add(
                IntegritySignal(
                    id = SubstrateSignal.SUBSTRATE_TWEAK_INJECT,
                    name = "Tweak Inject Present",
                    category = IntegrityCategory.HOOKING_OR_TAMPERING,
                    severity = SignalSeverity.LOW,
                    confidence = 0.6,
                    details = "/usr/lib/TweakInject is present, indicating substrate-based tweak installation",
                    detectedAt = currentTimestampMs,
                    metadata = mapOf("check" to SubstrateSignal.Check.SUBSTRATE_TWEAK_INJECT),
                ),
            )
        }
    } catch (_: Exception) {
        // File not accessible — best-effort only
    }

    return signals
}