package com.landoulsi.diagnostic

import com.landoulsi.design.components.BadgeTone
import com.landoulsi.design.components.StatusIconVariant
import com.landoulsi.diagnostic.ui.badgeTone
import com.landoulsi.diagnostic.ui.displayLabel
import com.landoulsi.diagnostic.ui.formatSummaryText
import com.landoulsi.diagnostic.ui.statusIconVariant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DiagnosticEngineTest {

    private class FakeDiagnosticCheck(
        override val id: String,
        override val name: String,
        private val resultState: DiagnosticState,
        private val cause: String? = null,
    ) : DiagnosticCheck {
        override suspend fun run(): DiagnosticResult {
            return DiagnosticResult(
                id = id,
                title = name,
                state = resultState,
                cause = cause,
            )
        }
    }

    private class ThrowingDiagnosticCheck(
        override val id: String,
        override val name: String,
    ) : DiagnosticCheck {
        override suspend fun run(): DiagnosticResult {
            throw RuntimeException("Check exploded")
        }
    }

    private class SuspendingDiagnosticCheck(
        override val id: String,
        override val name: String,
        private val deferred: CompletableDeferred<DiagnosticResult>,
    ) : DiagnosticCheck {
        override suspend fun run(): DiagnosticResult {
            return deferred.await()
        }
    }

    @Test
    fun testInitialState() {
        val engine = DiagnosticEngine(emptyList())
        val state = engine.uiState.value

        assertEquals(DiagnosticExecutionState.IDLE, state.executionState)
        assertTrue(state.results.isEmpty())
        assertEquals(DiagnosticState.PASS, state.overallState)
        assertEquals(0, state.passCount)
        assertEquals(0, state.warningCount)
        assertEquals(0, state.errorCount)
    }

    @Test
    fun testAllPassResultsInPassOverall() = runTest {
        val checks = listOf(
            FakeDiagnosticCheck("1", "Check 1", DiagnosticState.PASS),
            FakeDiagnosticCheck("2", "Check 2", DiagnosticState.PASS),
        )
        val engine = DiagnosticEngine(checks)

        engine.runDiagnostics()

        val state = engine.uiState.value
        assertEquals(DiagnosticExecutionState.COMPLETED, state.executionState)
        assertEquals(2, state.results.size)
        assertEquals(DiagnosticState.PASS, state.overallState)
        assertEquals(2, state.passCount)
        assertEquals(0, state.warningCount)
        assertEquals(0, state.errorCount)
    }

    @Test
    fun testWarningResultsInWarningOverall() = runTest {
        val checks = listOf(
            FakeDiagnosticCheck("1", "Check 1", DiagnosticState.PASS),
            FakeDiagnosticCheck("2", "Check 2", DiagnosticState.WARNING, "Warning cause"),
        )
        val engine = DiagnosticEngine(checks)

        engine.runDiagnostics()

        val state = engine.uiState.value
        assertEquals(DiagnosticExecutionState.COMPLETED, state.executionState)
        assertEquals(DiagnosticState.WARNING, state.overallState)
        assertEquals(1, state.passCount)
        assertEquals(1, state.warningCount)
        assertEquals(0, state.errorCount)
    }

    @Test
    fun testErrorDominatesWarning() = runTest {
        val checks = listOf(
            FakeDiagnosticCheck("1", "Check 1", DiagnosticState.PASS),
            FakeDiagnosticCheck("2", "Check 2", DiagnosticState.WARNING, "Warning cause"),
            FakeDiagnosticCheck("3", "Check 3", DiagnosticState.ERROR, "Error cause"),
        )
        val engine = DiagnosticEngine(checks)

        engine.runDiagnostics()

        val state = engine.uiState.value
        assertEquals(DiagnosticExecutionState.COMPLETED, state.executionState)
        assertEquals(DiagnosticState.ERROR, state.overallState)
        assertEquals(1, state.passCount)
        assertEquals(1, state.warningCount)
        assertEquals(1, state.errorCount)
    }

    @Test
    fun testExceptionCaughtAsError() = runTest {
        val checks = listOf(
            ThrowingDiagnosticCheck("1", "Crashing Check"),
        )
        val engine = DiagnosticEngine(checks)

        engine.runDiagnostics()

        val state = engine.uiState.value
        assertEquals(DiagnosticExecutionState.COMPLETED, state.executionState)
        assertEquals(DiagnosticState.ERROR, state.overallState)
        assertEquals(1, state.errorCount)
        assertEquals("Check exploded", state.results.first().cause)
    }

    @Test
    fun testCancellationExceptionIsRethrownAndResetsState() = runTest {
        val deferred = CompletableDeferred<DiagnosticResult>()
        val check = SuspendingDiagnosticCheck("1", "Suspending Check", deferred)
        val engine = DiagnosticEngine(listOf(check))

        val job = launch {
            engine.runDiagnostics()
        }

        testScheduler.runCurrent()
        assertEquals(DiagnosticExecutionState.RUNNING, engine.uiState.value.executionState)

        job.cancelAndJoin()
        assertEquals(DiagnosticExecutionState.IDLE, engine.uiState.value.executionState)
    }

    @Test
    fun testReentrancyGuard() = runTest {
        val deferred = CompletableDeferred<DiagnosticResult>()
        val check = SuspendingDiagnosticCheck("1", "Suspending Check", deferred)
        val engine = DiagnosticEngine(listOf(check))

        launch {
            engine.runDiagnostics()
        }

        testScheduler.runCurrent()
        assertEquals(DiagnosticExecutionState.RUNNING, engine.uiState.value.executionState)

        // Second call while running should early-return without disrupting state
        engine.runDiagnostics()
        assertEquals(DiagnosticExecutionState.RUNNING, engine.uiState.value.executionState)

        deferred.complete(DiagnosticResult("1", "Suspending Check", DiagnosticState.PASS))
        testScheduler.runCurrent()
        assertEquals(DiagnosticExecutionState.COMPLETED, engine.uiState.value.executionState)
    }

    @Test
    fun testDisplayLabelMapping() {
        assertEquals("Pass", DiagnosticState.PASS.displayLabel())
        assertEquals("Warning", DiagnosticState.WARNING.displayLabel())
        assertEquals("Error", DiagnosticState.ERROR.displayLabel())
    }

    @Test
    fun testBadgeToneMapping() {
        assertEquals(com.landoulsi.design.components.BadgeTone.Success, DiagnosticState.PASS.badgeTone())
        assertEquals(com.landoulsi.design.components.BadgeTone.Tertiary, DiagnosticState.WARNING.badgeTone())
        assertEquals(com.landoulsi.design.components.BadgeTone.Error, DiagnosticState.ERROR.badgeTone())
    }

    @Test
    fun testStatusIconVariantMapping() {
        assertEquals(com.landoulsi.design.components.StatusIconVariant.Success, DiagnosticState.PASS.statusIconVariant())
        assertEquals(com.landoulsi.design.components.StatusIconVariant.Warning, DiagnosticState.WARNING.statusIconVariant())
        assertEquals(com.landoulsi.design.components.StatusIconVariant.Error, DiagnosticState.ERROR.statusIconVariant())
    }

    @Test
    fun testFormatSummaryText() {
        val passState = DiagnosticUiState(
            executionState = DiagnosticExecutionState.COMPLETED,
            overallState = DiagnosticState.PASS,
            passCount = 3,
        )
        assertEquals("All systems operational", com.landoulsi.diagnostic.ui.formatSummaryText(passState))

        val warningSingle = DiagnosticUiState(
            executionState = DiagnosticExecutionState.COMPLETED,
            overallState = DiagnosticState.WARNING,
            warningCount = 1,
        )
        assertEquals("1 warning detected", com.landoulsi.diagnostic.ui.formatSummaryText(warningSingle))

        val warningPlural = DiagnosticUiState(
            executionState = DiagnosticExecutionState.COMPLETED,
            overallState = DiagnosticState.WARNING,
            warningCount = 3,
        )
        assertEquals("3 warnings detected", com.landoulsi.diagnostic.ui.formatSummaryText(warningPlural))

        val errorSingle = DiagnosticUiState(
            executionState = DiagnosticExecutionState.COMPLETED,
            overallState = DiagnosticState.ERROR,
            errorCount = 1,
        )
        assertEquals("1 error detected", com.landoulsi.diagnostic.ui.formatSummaryText(errorSingle))

        val errorPlural = DiagnosticUiState(
            executionState = DiagnosticExecutionState.COMPLETED,
            overallState = DiagnosticState.ERROR,
            errorCount = 2,
        )
        assertEquals("2 errors detected", com.landoulsi.diagnostic.ui.formatSummaryText(errorPlural))
    }

    @Test
    fun testReset() = runTest {
        val checks = listOf(
            FakeDiagnosticCheck("1", "Check 1", DiagnosticState.ERROR, "Boom"),
        )
        val engine = DiagnosticEngine(checks)
        engine.runDiagnostics()
        assertEquals(DiagnosticExecutionState.COMPLETED, engine.uiState.value.executionState)

        engine.reset()
        assertEquals(DiagnosticExecutionState.IDLE, engine.uiState.value.executionState)
        assertTrue(engine.uiState.value.results.isEmpty())
    }
}
