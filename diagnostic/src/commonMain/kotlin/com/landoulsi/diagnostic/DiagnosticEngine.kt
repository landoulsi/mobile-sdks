package com.landoulsi.diagnostic

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Execution state of the diagnostic run.
 */
enum class DiagnosticExecutionState {
    IDLE,
    RUNNING,
    COMPLETED,
}

/**
 * Encapsulates a collection of [DiagnosticCheck]s to be evaluated.
 */
data class DiagnosticSuite(
    val checks: List<DiagnosticCheck>,
)

/**
 * UI state representing the current diagnostic execution and results.
 */
data class DiagnosticUiState(
    val executionState: DiagnosticExecutionState = DiagnosticExecutionState.IDLE,
    val results: List<DiagnosticResult> = emptyList(),
    val overallState: DiagnosticState = DiagnosticState.PASS,
    val passCount: Int = 0,
    val warningCount: Int = 0,
    val errorCount: Int = 0,
)

/**
 * Engine responsible for orchestrating diagnostic suites and publishing reactive [DiagnosticUiState].
 */
class DiagnosticEngine(
    private val suite: DiagnosticSuite,
) {
    constructor(checks: List<DiagnosticCheck>) : this(DiagnosticSuite(checks))

    private val _uiState = MutableStateFlow(DiagnosticUiState())
    val uiState: StateFlow<DiagnosticUiState> = _uiState.asStateFlow()

    /**
     * Executes all diagnostic checks in the suite concurrently and updates the UI state.
     */
    suspend fun runDiagnostics() {
        if (_uiState.value.executionState == DiagnosticExecutionState.RUNNING) {
            return
        }

        val previousState = _uiState.value
        _uiState.value = previousState.copy(
            executionState = DiagnosticExecutionState.RUNNING,
        )

        try {
            val results = coroutineScope {
                suite.checks.map { check ->
                    async {
                        try {
                            check.run()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            DiagnosticResult(
                                id = check.id,
                                title = check.name,
                                state = DiagnosticState.ERROR,
                                cause = e.message ?: "Unexpected diagnostic check failure",
                            )
                        }
                    }
                }.awaitAll()
            }

            val passCount = results.count { it.state == DiagnosticState.PASS }
            val warningCount = results.count { it.state == DiagnosticState.WARNING }
            val errorCount = results.count { it.state == DiagnosticState.ERROR }

            val overallState = when {
                errorCount > 0 -> DiagnosticState.ERROR
                warningCount > 0 -> DiagnosticState.WARNING
                else -> DiagnosticState.PASS
            }

            _uiState.value = DiagnosticUiState(
                executionState = DiagnosticExecutionState.COMPLETED,
                results = results,
                overallState = overallState,
                passCount = passCount,
                warningCount = warningCount,
                errorCount = errorCount,
            )
        } catch (e: CancellationException) {
            _uiState.value = _uiState.value.copy(
                executionState = if (previousState.executionState == DiagnosticExecutionState.RUNNING) {
                    DiagnosticExecutionState.IDLE
                } else {
                    previousState.executionState
                },
            )
            throw e
        }
    }

    /**
     * Resets the diagnostic engine state to IDLE.
     */
    fun reset() {
        _uiState.value = DiagnosticUiState()
    }
}
