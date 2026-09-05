package com.landoulsi.demo.ui

import com.landoulsi.integrity.DefaultIntegrityDetector
import com.landoulsi.integrity.SignalEvaluator
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.RiskLevel
import com.landoulsi.integrity.model.SignalSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntegrityShowcaseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState() {
        val viewModel = IntegrityShowcaseViewModel()
        val state = viewModel.uiState.value

        assertFalse(state.isScanning)
        assertEquals(IntegrityScenario.LIVE_DEVICE, state.selectedScenario)
        assertNull(state.scanResult)
        assertNull(state.riskScore)
        assertNull(state.selectedSignal)
        assertEquals(IntegrityCategory.entries.toSet(), state.expandedCategories)
    }

    @Test
    fun testCategoryToggleExpandAndCollapse() {
        val viewModel = IntegrityShowcaseViewModel()

        viewModel.toggleCategory(IntegrityCategory.ROOT_OR_JAILBREAK)
        assertFalse(IntegrityCategory.ROOT_OR_JAILBREAK in viewModel.uiState.value.expandedCategories)

        viewModel.toggleCategory(IntegrityCategory.ROOT_OR_JAILBREAK)
        assertTrue(IntegrityCategory.ROOT_OR_JAILBREAK in viewModel.uiState.value.expandedCategories)

        viewModel.collapseAllCategories()
        assertTrue(viewModel.uiState.value.expandedCategories.isEmpty())

        viewModel.expandAllCategories()
        assertEquals(IntegrityCategory.entries.toSet(), viewModel.uiState.value.expandedCategories)
    }

    @Test
    fun testSelectSignalForInspection() {
        val viewModel = IntegrityShowcaseViewModel()
        val testSignal = IntegritySignal(
            id = "test_signal",
            name = "Test Signal",
            category = IntegrityCategory.ROOT_OR_JAILBREAK,
            severity = SignalSeverity.CRITICAL,
        )

        viewModel.selectSignalForInspection(testSignal)
        assertEquals(testSignal, viewModel.uiState.value.selectedSignal)

        viewModel.selectSignalForInspection(null)
        assertNull(viewModel.uiState.value.selectedSignal)
    }

    @Test
    fun testRunSweepExecutesSinglePassWithoutDoubleExecution() = runTest(testDispatcher) {
        var evaluateCallCount = 0
        val countingEvaluator = object : SignalEvaluator {
            override val category: IntegrityCategory = IntegrityCategory.ROOT_OR_JAILBREAK
            override val knownSignalIds: Set<String> = setOf("test_count_signal")

            override suspend fun evaluate(): List<IntegritySignal> {
                evaluateCallCount++
                return listOf(
                    IntegritySignal(
                        id = "test_count_signal",
                        name = "Count Signal",
                        category = IntegrityCategory.ROOT_OR_JAILBREAK,
                        severity = SignalSeverity.HIGH,
                    ),
                )
            }
        }

        val viewModel = IntegrityShowcaseViewModel(
            evaluatorProvider = { _, _ -> listOf(countingEvaluator) },
        )

        viewModel.runSweep(scenario = IntegrityScenario.ROOT_BREACH)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isScanning)
        assertNotNull(state.scanResult)
        assertNotNull(state.riskScore)
        assertEquals("Evaluator should be executed exactly once per sweep", 1, evaluateCallCount)
        assertEquals(1, state.scanResult?.fired?.size)
        assertEquals(state.riskScore?.score?.toInt(), state.scanResult?.integrityScore)
    }

    @Test
    fun testAllScenariosBuildEvaluatorsAndProduceExpectedScores() = runTest {
        IntegrityScenario.entries.forEach { scenario ->
            val evaluators = IntegrityScenarioFixtures.buildEvaluators(scenario, context = null)
            assertTrue("Evaluators list for $scenario should not be empty", evaluators.isNotEmpty())

            val detector = DefaultIntegrityDetector(evaluators = evaluators)
            val score = detector.evaluateRisk()

            when (scenario) {
                IntegrityScenario.CLEAN_BASELINE -> {
                    assertEquals(0.0, score.score, 0.001)
                    assertEquals(RiskLevel.LOW, score.riskLevel)
                    assertTrue(score.signals.isEmpty())
                }

                IntegrityScenario.ROOT_BREACH -> {
                    assertTrue(score.score > 0.0)
                    assertTrue(score.signals.any { it.category == IntegrityCategory.ROOT_OR_JAILBREAK })
                }

                IntegrityScenario.DYNAMIC_HOOKING -> {
                    assertTrue(score.score > 0.0)
                    assertTrue(score.signals.any { it.category == IntegrityCategory.HOOKING_OR_TAMPERING })
                }

                IntegrityScenario.GPS_SPOOFING -> {
                    assertTrue(score.score > 0.0)
                    assertTrue(score.signals.any { it.category == IntegrityCategory.MOCK_LOCATION })
                }

                IntegrityScenario.NETWORK_ANOMALY -> {
                    assertTrue(score.score > 0.0)
                    assertTrue(score.signals.any { it.category == IntegrityCategory.NETWORK_ANOMALY })
                }

                IntegrityScenario.CRITICAL_ATTACK -> {
                    assertTrue(score.score >= 80.0)
                    assertEquals(RiskLevel.CRITICAL, score.riskLevel)
                    assertTrue(score.signals.size >= 4)
                }

                IntegrityScenario.LIVE_DEVICE -> {
                    // With null context, falls back to clean baseline evaluators
                    assertEquals(0.0, score.score, 0.001)
                    assertEquals(RiskLevel.LOW, score.riskLevel)
                }
            }
        }
    }

    @Test
    fun testCategoryImplementationCheck() {
        assertTrue(IntegrityScenarioFixtures.isCategoryImplemented(IntegrityCategory.ROOT_OR_JAILBREAK))
        assertTrue(IntegrityScenarioFixtures.isCategoryImplemented(IntegrityCategory.HOOKING_OR_TAMPERING))
        assertTrue(IntegrityScenarioFixtures.isCategoryImplemented(IntegrityCategory.MOCK_LOCATION))
        assertTrue(IntegrityScenarioFixtures.isCategoryImplemented(IntegrityCategory.NETWORK_ANOMALY))
        assertTrue(IntegrityScenarioFixtures.isCategoryImplemented(IntegrityCategory.VIRTUAL_OS_OR_EMULATOR))

        assertFalse(IntegrityScenarioFixtures.isCategoryImplemented(IntegrityCategory.APP_CLONING))
        assertFalse(IntegrityScenarioFixtures.isCategoryImplemented(IntegrityCategory.DEBUGGER_ATTACHED))
        assertFalse(IntegrityScenarioFixtures.isCategoryImplemented(IntegrityCategory.UNTRUSTED_INSTALLER))
    }
}
