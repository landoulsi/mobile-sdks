package com.landoulsi.integrity.emulator

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.SignalSeverity
import kotlin.time.Clock

object EmulatorSignal {
    const val BUILD_GENERIC = "emulator_build_generic"
    const val BUILD_HARDWARE = "emulator_build_hardware"
    const val QEMU_FILE = "emulator_qemu_file"
    const val MANAGEMENT_APP = "emulator_management_app"
    const val SENSOR_DEFICIT = "emulator_sensor_deficit"

    /** Every signal id this vector can emit; used to seed the [IntegrityResult] catalog. */
    val all: Set<String> = setOf(
        BUILD_GENERIC,
        BUILD_HARDWARE,
        QEMU_FILE,
        MANAGEMENT_APP,
        SENSOR_DEFICIT,
    )

    object Check {
        const val BUILD_GENERIC = "build_generic"
        const val BUILD_HARDWARE = "build_hardware"
        const val QEMU_FILE = "qemu_file"
        const val MANAGEMENT_APP = "management_app"
        const val SENSOR_DEFICIT = "sensor_deficit"
    }
}

internal val GENERIC_FINGERPRINT_PREFIXES = listOf("generic", "unknown")

internal val GENERIC_MODEL_KEYWORDS = listOf(
    "google_sdk",
    "emulator",
    "android sdk built for x86",
    "sdk_gphone",
)

internal val GENERIC_MANUFACTURER_KEYWORDS = listOf("genymotion", "unknown")

internal val GENERIC_PRODUCT_KEYWORDS = listOf("sdk", "vbox86p", "emulator")

internal val EMULATOR_HARDWARE_KEYWORDS = listOf("goldfish", "ranchu", "vbox86", "nox", "ttvm_x86")

internal val QEMU_FILE_PATHS = listOf(
    "/dev/qemu_pipe",
    "/dev/socket/qemud",
    "/system/lib/libc_malloc_debug_qemu.so",
    "/sys/qemu_trace",
    "/system/bin/qemu-props",
    "/dev/socket/genyd",
    "/dev/socket/baseband_genyd",
)

/** Must stay 1:1 with the `<queries>` package visibility declarations in AndroidManifest.xml. */
internal val EMULATOR_MANAGEMENT_PACKAGES = listOf(
    "com.google.android.launcher.layouts.genymotion",
    "com.bluestacks",
    "com.bignox.app",
    "com.microvirt.market",
)

private const val BASE_GENERIC_CONFIDENCE = 0.5
private const val CONFIDENCE_PER_INDICATOR = 0.15
private const val MAX_GENERIC_CONFIDENCE = 0.95
private const val ZERO_SENSOR_COUNT = 0

internal fun checkGenericBuildProperties(context: EmulatorCheckContext): IntegritySignal? {
    val fingerprint = (try { context.getBuildFingerprint() } catch (_: Exception) { "" }).lowercase()
    val model = (try { context.getBuildModel() } catch (_: Exception) { "" }).lowercase()
    val manufacturer = (try { context.getBuildManufacturer() } catch (_: Exception) { "" }).lowercase()
    val brand = (try { context.getBuildBrand() } catch (_: Exception) { "" }).lowercase()
    val device = (try { context.getBuildDevice() } catch (_: Exception) { "" }).lowercase()
    val product = (try { context.getBuildProduct() } catch (_: Exception) { "" }).lowercase()

    val matchedIndicators = mutableListOf<String>()
    if (GENERIC_FINGERPRINT_PREFIXES.any { fingerprint.startsWith(it) }) matchedIndicators.add("fingerprint")
    if (GENERIC_MODEL_KEYWORDS.any { model.contains(it) }) matchedIndicators.add("model")
    if (GENERIC_MANUFACTURER_KEYWORDS.any { manufacturer.contains(it) }) matchedIndicators.add("manufacturer")
    if (brand.startsWith("generic") && device.startsWith("generic")) matchedIndicators.add("brand_device")
    if (GENERIC_PRODUCT_KEYWORDS.any { product.contains(it) }) matchedIndicators.add("product")

    if (matchedIndicators.isEmpty()) return null

    // Confidence scales with the number of independently-matched properties so a single weak
    // indicator (e.g. a legitimate device reporting MANUFACTURER "unknown") doesn't carry the
    // same weight as several corroborating matches.
    val confidence = (BASE_GENERIC_CONFIDENCE + CONFIDENCE_PER_INDICATOR * matchedIndicators.size)
        .coerceAtMost(MAX_GENERIC_CONFIDENCE)

    return IntegritySignal(
        id = EmulatorSignal.BUILD_GENERIC,
        name = "Generic Emulator Build Properties Detected",
        category = IntegrityCategory.VIRTUAL_OS_OR_EMULATOR,
        severity = SignalSeverity.MEDIUM,
        confidence = confidence,
        details = "Build properties match known emulator patterns: ${matchedIndicators.joinToString(", ")}",
        detectedAt = Clock.System.now().toEpochMilliseconds(),
        metadata = mapOf(
            "matched" to matchedIndicators.joinToString(","),
            "check" to EmulatorSignal.Check.BUILD_GENERIC,
        ),
    )
}

internal fun checkEmulatorHardware(context: EmulatorCheckContext): IntegritySignal? {
    val hardware = (try { context.getBuildHardware() } catch (_: Exception) { "" }).lowercase()
    val product = (try { context.getBuildProduct() } catch (_: Exception) { "" }).lowercase()

    val matchedKeyword = EMULATOR_HARDWARE_KEYWORDS.firstOrNull { hardware.contains(it) || product.contains(it) }
        ?: return null

    return IntegritySignal(
        id = EmulatorSignal.BUILD_HARDWARE,
        name = "Known Emulator Hardware Backend Detected",
        category = IntegrityCategory.VIRTUAL_OS_OR_EMULATOR,
        severity = SignalSeverity.HIGH,
        confidence = 0.9,
        details = "Hardware/product identifiers match known emulator backend: $matchedKeyword",
        detectedAt = Clock.System.now().toEpochMilliseconds(),
        metadata = mapOf("keyword" to matchedKeyword, "check" to EmulatorSignal.Check.BUILD_HARDWARE),
    )
}

internal fun checkQemuFiles(context: EmulatorCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    return QEMU_FILE_PATHS.filter { path ->
        try {
            context.fileExists(path)
        } catch (_: Exception) {
            false
        }
    }.map { path ->
        IntegritySignal(
            id = EmulatorSignal.QEMU_FILE,
            name = "QEMU/Virtualization Artifact Detected",
            category = IntegrityCategory.VIRTUAL_OS_OR_EMULATOR,
            severity = SignalSeverity.HIGH,
            confidence = 1.0,
            details = "Emulator-specific system path found at: $path",
            detectedAt = currentTimestampMs,
            metadata = mapOf("path" to path, "check" to EmulatorSignal.Check.QEMU_FILE),
        )
    }
}

internal fun checkEmulatorManagementApps(context: EmulatorCheckContext): List<IntegritySignal> {
    val currentTimestampMs = Clock.System.now().toEpochMilliseconds()
    return EMULATOR_MANAGEMENT_PACKAGES.filter { packageName ->
        try {
            context.isPackageInstalled(packageName)
        } catch (_: Exception) {
            false
        }
    }.map { packageName ->
        IntegritySignal(
            id = EmulatorSignal.MANAGEMENT_APP,
            name = "Emulator Management App Detected",
            category = IntegrityCategory.VIRTUAL_OS_OR_EMULATOR,
            severity = SignalSeverity.HIGH,
            confidence = 0.9,
            details = "Emulator player/launcher package is installed: $packageName",
            detectedAt = currentTimestampMs,
            metadata = mapOf("package" to packageName, "check" to EmulatorSignal.Check.MANAGEMENT_APP),
        )
    }
}

internal fun checkSensorDeficit(context: EmulatorCheckContext): IntegritySignal? {
    val sensorCount = (
        try {
            context.getSensorCount()
        } catch (_: Exception) {
            null
        }
        ) ?: return null

    if (sensorCount > ZERO_SENSOR_COUNT) return null

    return IntegritySignal(
        id = EmulatorSignal.SENSOR_DEFICIT,
        name = "No Hardware Sensors Reported",
        category = IntegrityCategory.VIRTUAL_OS_OR_EMULATOR,
        severity = SignalSeverity.LOW,
        confidence = 0.4,
        details = "Device reports zero hardware sensors, typical of bare-bones emulated environments",
        detectedAt = Clock.System.now().toEpochMilliseconds(),
        metadata = mapOf("sensor_count" to sensorCount.toString(), "check" to EmulatorSignal.Check.SENSOR_DEFICIT),
    )
}
