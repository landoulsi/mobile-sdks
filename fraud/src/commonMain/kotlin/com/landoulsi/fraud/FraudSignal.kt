package com.landoulsi.fraud

/**
 * Represents a single fraud signal detected on the device.
 * A signal captures the result of a specific check (e.g., root detection, emulator detection)
 * along with its severity and details.
 */
data class FraudSignal(
    val category: FraudCategory,
    val severity: SignalSeverity,
    val isSuspicious: Boolean,
    val description: String,
    val evidence: Map<String, Any?> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)