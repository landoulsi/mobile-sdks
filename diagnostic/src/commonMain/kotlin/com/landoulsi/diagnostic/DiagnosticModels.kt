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

/**
 * Configuration for a single diagnostic check, allowing checks to be enabled/disabled
 * and their parameters to be customised without coupling the engine to concrete check types.
 *
 * @property checkId Unique identifier matching a [DiagnosticCheck.id].
 * @property enabled Whether this check should be included in the suite.
 * @property parameters Check-specific key-value parameters (e.g. threshold values).
 */
data class DiagnosticCheckConfig(
    val checkId: String,
    val enabled: Boolean = true,
    val parameters: Map<String, String> = emptyMap(),
)

/**
 * Applies a [DiagnosticCheckConfig] to a [DiagnosticCheck], returning a configured instance.
 *
 * Implementations are responsible for mapping config parameters onto concrete check
 * constructors. The default implementation returns the check unchanged.
 */
fun interface DiagnosticCheckFactory {
    fun create(check: DiagnosticCheck, config: DiagnosticCheckConfig): DiagnosticCheck
}

/**
 * Default [DiagnosticCheckFactory] that returns checks unchanged. Concrete check types
 * that support parameters can provide their own factory or be handled by a custom one.
 */
object DefaultDiagnosticCheckFactory : DiagnosticCheckFactory {
    override fun create(check: DiagnosticCheck, config: DiagnosticCheckConfig): DiagnosticCheck = check
}
