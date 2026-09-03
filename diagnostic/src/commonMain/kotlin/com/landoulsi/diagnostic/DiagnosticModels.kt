package com.landoulsi.diagnostic

/**
 * Result state of a diagnostic evaluation.
 */
enum class DiagnosticState {
    PASS,
    WARNING,
    ERROR
}

/**
 * Represents the outcome of a diagnostic evaluation.
 *
 * @property id Unique identifier for the diagnostic check.
 * @property title Human-readable title of the check.
 * @property state Outcome status (PASS, WARNING, or ERROR).
 * @property cause Human-readable explanation when state is WARNING or ERROR, or null if PASS.
 * @property timestamp Epoch timestamp in milliseconds when the check was performed.
 * @property metadata Key-value pairs containing detailed telemetry/context about the check.
 */
data class DiagnosticResult(
    val id: String,
    val title: String,
    val state: DiagnosticState,
    val cause: String? = null,
    val timestamp: Long = platformTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Contract for all diagnostic checks.
 */
interface DiagnosticCheck {
    val id: String
    val name: String
    suspend fun run(): DiagnosticResult
}
