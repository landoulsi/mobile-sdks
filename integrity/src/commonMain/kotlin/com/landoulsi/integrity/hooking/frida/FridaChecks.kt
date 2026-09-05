package com.landoulsi.integrity.hooking.frida

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.SignalSeverity
import kotlin.time.Clock

object FridaSignal {
    const val FRIDA_SERVER_PROCESS = "frida_server_process"
    const val FRIDA_PORT_OPEN = "frida_port_open"
    const val FRIDA_GADGET_MAPS = "frida_gadget_maps"
    const val FRIDA_GADGET_FILE = "frida_gadget_file"

    /** Every signal id this vector can emit; used to seed the [IntegrityResult] catalog. */
    val all: Set<String> = setOf(
        FRIDA_SERVER_PROCESS,
        FRIDA_PORT_OPEN,
        FRIDA_GADGET_MAPS,
        FRIDA_GADGET_FILE,
    )

    object Check {
        const val FRIDA_SERVER_PROCESS = "frida_server_process"
        const val FRIDA_PORT_OPEN = "frida_port_open"
        const val FRIDA_GADGET_MAPS = "frida_gadget_maps"
        const val FRIDA_GADGET_FILE = "frida_gadget_file"
    }
}

internal val FRIDA_LIBRARY_NAMES = listOf(
    "libfrida-gadget.so",
    "libfrida-agent.so",
    "frida-gadget.so",
    "frida-agent.dylib",
    "frida-gadget.dylib",
    "gum-js-loop",
)

internal val FRIDA_PROCESS_NAMES = listOf(
    "frida-server",
    "frida-helper",
    "frida-agent",
    "gum-js-loop",
)

internal val FRIDA_GADGET_PATHS = listOf(
    "/system/lib/libfrida-gadget.so",
    "/system/lib64/libfrida-gadget.so",
    "/data/local/frida-gadget.so",
    "/data/local/tmp/frida-server",
    "/data/local/tmp/re.frida.server",
    "/usr/lib/frida/frida-gadget.dylib",
    "/usr/lib/frida/frida-agent.dylib",
)

internal fun checkFridaServerProcess(context: FridaCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    for (procName in FRIDA_PROCESS_NAMES) {
        try {
            if (context.isProcessRunning(procName)) {
                signals.add(
                    IntegritySignal(
                        id = FridaSignal.FRIDA_SERVER_PROCESS,
                        name = "Frida Server Process Detected",
                        category = IntegrityCategory.HOOKING_OR_TAMPERING,
                        severity = SignalSeverity.HIGH,
                        confidence = 0.9,
                        details = "Frida server process is running on this device: $procName",
                        detectedAt = currentTimestampMs,
                        metadata = mapOf(
                            "process" to procName,
                            "check" to FridaSignal.Check.FRIDA_SERVER_PROCESS,
                        ),
                    ),
                )
            }
        } catch (_: Exception) {
            // Process inspection not accessible — best-effort only
        }
    }

    return signals
}

internal fun checkFridaPortOpen(context: FridaCheckContext): List<IntegritySignal> {
    try {
        if (context.isPortOpen(27042)) {
            val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
            return listOf(
                IntegritySignal(
                    id = FridaSignal.FRIDA_PORT_OPEN,
                    name = "Frida Port Open",
                    category = IntegrityCategory.HOOKING_OR_TAMPERING,
                    severity = SignalSeverity.MEDIUM,
                    confidence = 0.7,
                    details = "Frida protocol port (27042) is open on localhost",
                    detectedAt = currentTimestampMs,
                    metadata = mapOf(
                        "port" to "27042",
                        "check" to FridaSignal.Check.FRIDA_PORT_OPEN,
                    ),
                ),
            )
        }
    } catch (_: Exception) {
        // Port probing not accessible — best-effort only
    }
    return emptyList()
}

internal fun checkFridaGadgetMaps(context: FridaCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    try {
        val mapsLines = context.readFileLines("/proc/self/maps")
        val fridaMatches = mapsLines.filter { line ->
            FRIDA_LIBRARY_NAMES.any { line.contains(it, ignoreCase = true) }
        }
        if (fridaMatches.isNotEmpty()) {
            signals.add(
                IntegritySignal(
                    id = FridaSignal.FRIDA_GADGET_MAPS,
                    name = "Frida Gadget Mappings Detected",
                    category = IntegrityCategory.HOOKING_OR_TAMPERING,
                    severity = SignalSeverity.HIGH,
                    confidence = minOf(0.85, 0.5 + 0.05 * fridaMatches.size),
                    details = "Frida/gadget library mappings found in /proc/self/maps: ${fridaMatches.joinToString(", ")}",
                    detectedAt = currentTimestampMs,
                    metadata = mapOf(
                        "matches" to fridaMatches.joinToString(","),
                        "check" to FridaSignal.Check.FRIDA_GADGET_MAPS,
                    ),
                ),
            )
        }
    } catch (_: Exception) {
        // /proc/self/maps not readable — best-effort only
    }

    return signals
}

internal fun checkFridaGadgetFile(context: FridaCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    val signals = mutableListOf<IntegritySignal>()

    for (path in FRIDA_GADGET_PATHS) {
        try {
            if (context.fileExists(path)) {
                signals.add(
                    IntegritySignal(
                        id = FridaSignal.FRIDA_GADGET_FILE,
                        name = "Frida Gadget Library Detected",
                        category = IntegrityCategory.HOOKING_OR_TAMPERING,
                        severity = SignalSeverity.HIGH,
                        confidence = 0.8,
                        details = "Frida gadget library found at: $path",
                        detectedAt = currentTimestampMs,
                        metadata = mapOf(
                            "path" to path,
                            "check" to FridaSignal.Check.FRIDA_GADGET_FILE,
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