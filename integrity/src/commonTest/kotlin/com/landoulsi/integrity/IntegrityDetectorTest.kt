package com.landoulsi.integrity

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegrityMitigationAction
import com.landoulsi.integrity.model.IntegrityRiskScore
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.RiskLevel
import com.landoulsi.integrity.model.SignalSeverity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException

class IntegrityDetectorTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testSerializationRoundTrip() {
        val signal = IntegritySignal(
            id = "test_root_su",
            name = "SU Binary Detected",
            category = IntegrityCategory.ROOT_OR_JAILBREAK,
            severity = SignalSeverity.CRITICAL,
            confidence = 0.95,
            details = "/system/bin/su found",
            detectedAt = 1700000000000L,
            metadata = mapOf("path" to "/system/bin/su", "permissions" to "rwxr-xr-x")
        )

        val serializedSignal = json.encodeToString(signal)
        val deserializedSignal = json.decodeFromString<IntegritySignal>(serializedSignal)
        assertEquals(signal, deserializedSignal)

        val riskScore = IntegrityRiskScore(
            score = 85.5,
            riskLevel = RiskLevel.CRITICAL,
            action = IntegrityMitigationAction.BLOCK,
            signals = listOf(signal),
            categoryAttribution = mapOf(IntegrityCategory.ROOT_OR_JAILBREAK to 85.5),
            evaluatedAt = 1700000000000L
        )

        val serializedScore = json.encodeToString(riskScore)
        val deserializedScore = json.decodeFromString<IntegrityRiskScore>(serializedScore)
        assertEquals(riskScore, deserializedScore)
        assertTrue(deserializedScore.isBlocked)
        assertFalse(deserializedScore.isAllowed)
    }

    @Test
    fun testDefaultIntegrityDetectorAggregation() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val rootEvaluator = object : SignalEvaluator {
            override val category: IntegrityCategory = IntegrityCategory.ROOT_OR_JAILBREAK
            override suspend fun evaluate(): List<IntegritySignal> = listOf(
                IntegritySignal(
                    id = "root_su",
                    name = "SU Binary Detected",
                    category = IntegrityCategory.ROOT_OR_JAILBREAK,
                    severity = SignalSeverity.CRITICAL
                )
            )
        }

        val emulatorEvaluator = object : SignalEvaluator {
            override val category: IntegrityCategory = IntegrityCategory.VIRTUAL_OS_OR_EMULATOR
            override suspend fun evaluate(): List<IntegritySignal> = listOf(
                IntegritySignal(
                    id = "qemu_props",
                    name = "QEMU Driver Detected",
                    category = IntegrityCategory.VIRTUAL_OS_OR_EMULATOR,
                    severity = SignalSeverity.HIGH
                )
            )
        }

        val detector = DefaultIntegrityDetector(
            evaluators = listOf(rootEvaluator, emulatorEvaluator),
            dispatcher = testDispatcher
        )

        val detected = detector.detectSignals()
        assertEquals(2, detected.size)

        val riskScore = detector.evaluateRisk()
        assertTrue(riskScore.score > 0.0)
        assertEquals(2, riskScore.signals.size)
        assertTrue(riskScore.categoryAttribution.containsKey(IntegrityCategory.ROOT_OR_JAILBREAK))
        assertTrue(riskScore.categoryAttribution.containsKey(IntegrityCategory.VIRTUAL_OS_OR_EMULATOR))

        val rootOnly = detector.evaluateCategory(IntegrityCategory.ROOT_OR_JAILBREAK)
        assertEquals(1, rootOnly.size)
        assertEquals("root_su", rootOnly.first().id)
    }

    @Test
    fun testDefaultIntegrityDetectorFaultTolerance() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val faultyEvaluator = object : SignalEvaluator {
            override val category: IntegrityCategory = IntegrityCategory.DEBUGGER_ATTACHED
            override suspend fun evaluate(): List<IntegritySignal> {
                throw IllegalStateException("Failed to inspect ptrace")
            }
        }

        val goodEvaluator = object : SignalEvaluator {
            override val category: IntegrityCategory = IntegrityCategory.APP_CLONING
            override suspend fun evaluate(): List<IntegritySignal> = listOf(
                IntegritySignal(
                    id = "parallel_space",
                    name = "Parallel Space Path",
                    category = IntegrityCategory.APP_CLONING,
                    severity = SignalSeverity.MEDIUM
                )
            )
        }

        val detector = DefaultIntegrityDetector(
            evaluators = listOf(faultyEvaluator, goodEvaluator),
            dispatcher = testDispatcher
        )

        val signals = detector.detectSignals()
        assertEquals(1, signals.size)
        assertEquals("parallel_space", signals.first().id)
    }

    @Test
    fun testDefaultIntegrityDetectorCancellationPreserved() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val cancelingEvaluator = object : SignalEvaluator {
            override val category: IntegrityCategory = IntegrityCategory.DEBUGGER_ATTACHED
            override suspend fun evaluate(): List<IntegritySignal> {
                throw CancellationException("Simulated coroutine cancellation")
            }
        }

        val detector = DefaultIntegrityDetector(
            evaluators = listOf(cancelingEvaluator),
            dispatcher = testDispatcher
        )

        assertFailsWith<CancellationException> {
            detector.detectSignals()
        }

        assertFailsWith<CancellationException> {
            detector.evaluateCategory(IntegrityCategory.DEBUGGER_ATTACHED)
        }
    }

    @Test
    fun testDefaultIntegrityDetectorDynamicConfigUpdate() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val rootEvaluator = object : SignalEvaluator {
            override val category: IntegrityCategory = IntegrityCategory.ROOT_OR_JAILBREAK
            override suspend fun evaluate(): List<IntegritySignal> = listOf(
                IntegritySignal(
                    id = "root_su",
                    name = "SU Binary Detected",
                    category = IntegrityCategory.ROOT_OR_JAILBREAK,
                    severity = SignalSeverity.CRITICAL
                )
            )
        }

        val detector = DefaultIntegrityDetector(
            evaluators = listOf(rootEvaluator),
            dispatcher = testDispatcher
        )

        assertEquals(1, detector.detectSignals().size)

        // Dynamically disable ROOT_OR_JAILBREAK
        detector.updateConfig(
            detector.currentConfig.copy(
                enabledCategories = emptySet()
            )
        )

        assertEquals(0, detector.detectSignals().size)
        val score = detector.evaluateRisk()
        assertEquals(0.0, score.score)
        assertTrue(score.isAllowed)
    }

    @Test
    fun testDefaultIntegrityDetectorObserveSignals() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val evaluator = object : SignalEvaluator {
            override val category: IntegrityCategory = IntegrityCategory.MOCK_LOCATION
            override suspend fun evaluate(): List<IntegritySignal> = listOf(
                IntegritySignal(
                    id = "mock_location_app",
                    name = "Mock Provider Active",
                    category = IntegrityCategory.MOCK_LOCATION,
                    severity = SignalSeverity.HIGH
                )
            )
        }

        val detector = DefaultIntegrityDetector(
            evaluators = listOf(evaluator),
            dispatcher = testDispatcher
        )

        val firstEmission = detector.observeSignals().first()
        assertEquals(1, firstEmission.size)
        assertEquals("mock_location_app", firstEmission.first().id)
    }

    @Test
    fun testDefaultIntegrityDetectorObserveRisk() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val evaluator = object : SignalEvaluator {
            override val category: IntegrityCategory = IntegrityCategory.MOCK_LOCATION
            override suspend fun evaluate(): List<IntegritySignal> = listOf(
                IntegritySignal(
                    id = "mock_location_app",
                    name = "Mock Provider Active",
                    category = IntegrityCategory.MOCK_LOCATION,
                    severity = SignalSeverity.CRITICAL
                )
            )
        }

        val detector = DefaultIntegrityDetector(
            evaluators = listOf(evaluator),
            dispatcher = testDispatcher
        )

        val score = detector.observeRisk().first()
        assertTrue(score.score > 0.0)
        assertEquals(1, score.signals.size)
    }

    @Test
    fun testInvalidPollIntervalThrows() {
        val detector = DefaultIntegrityDetector()

        assertFailsWith<IllegalArgumentException> {
            detector.observeSignals(0L)
        }
        assertFailsWith<IllegalArgumentException> {
            detector.observeSignals(-500L)
        }
        assertFailsWith<IllegalArgumentException> {
            detector.observeRisk(0L)
        }
        assertFailsWith<IllegalArgumentException> {
            detector.observeRisk(-500L)
        }
    }
}
