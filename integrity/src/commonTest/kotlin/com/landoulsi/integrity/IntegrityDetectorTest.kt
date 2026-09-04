package com.landoulsi.integrity

import com.landoulsi.integrity.engine.DefaultIntegrityRiskScoringEngine
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegrityConfig
import com.landoulsi.integrity.model.IntegrityMitigationAction
import com.landoulsi.integrity.model.IntegrityRiskScore
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.IntegrityThresholds
import com.landoulsi.integrity.model.ModelParameters
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
import kotlin.test.assertNotNull
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
    fun testScoringEngineZeroThreatsProducesCleanScore() {
        val engine = DefaultIntegrityRiskScoringEngine()
        val score = engine.calculateScore(emptyList(), IntegrityConfig())

        assertEquals(0.0, score.score)
        assertEquals(RiskLevel.LOW, score.riskLevel)
        assertEquals(IntegrityMitigationAction.ALLOW, score.action)
        assertTrue(score.signals.isEmpty())
        assertTrue(score.categoryAttribution.isEmpty())
        assertTrue(score.isAllowed)
    }

    @Test
    fun testScoringEngineZeroRawScoreWithSignals() {
        val engine = DefaultIntegrityRiskScoringEngine()
        val infoSignal = IntegritySignal(
            id = "test_info_signal",
            name = "Informational Device Stat",
            category = IntegrityCategory.NETWORK_ANOMALY,
            severity = SignalSeverity.INFO,
            confidence = 1.0
        )

        val score = engine.calculateScore(listOf(infoSignal), IntegrityConfig())
        assertEquals(0.0, score.score)
        assertEquals(RiskLevel.LOW, score.riskLevel)
        assertEquals(IntegrityMitigationAction.ALLOW, score.action)
        assertEquals(1, score.signals.size)
        assertTrue(score.categoryAttribution.isEmpty())
        assertTrue(score.isAllowed)
    }

    @Test
    fun testScoringEngineSingleCriticalSignal() {
        val engine = DefaultIntegrityRiskScoringEngine()
        val signal = IntegritySignal(
            id = "test_frida_hook",
            name = "Frida Server Active",
            category = IntegrityCategory.HOOKING_OR_TAMPERING,
            severity = SignalSeverity.CRITICAL,
            confidence = 1.0
        )

        val score = engine.calculateScore(listOf(signal), IntegrityConfig())
        assertTrue(score.score > 0.0)
        assertTrue(score.score <= 100.0)
        assertEquals(1, score.signals.size)
        assertNotNull(score.categoryAttribution[IntegrityCategory.HOOKING_OR_TAMPERING])
    }

    @Test
    fun testScoringEngineConfidenceScaling() {
        val engine = DefaultIntegrityRiskScoringEngine()
        val fullConfidenceSignal = IntegritySignal(
            id = "mock_gps",
            name = "Mock GPS",
            category = IntegrityCategory.MOCK_LOCATION,
            severity = SignalSeverity.HIGH,
            confidence = 1.0
        )
        val lowConfidenceSignal = fullConfidenceSignal.copy(confidence = 0.2)

        val fullScore = engine.calculateScore(listOf(fullConfidenceSignal))
        val lowScore = engine.calculateScore(listOf(lowConfidenceSignal))

        assertTrue(fullScore.score > lowScore.score)
    }

    @Test
    fun testScoringEngineDisabledCategoryFilter() {
        val engine = DefaultIntegrityRiskScoringEngine()
        val signal = IntegritySignal(
            id = "vpn_active",
            name = "Active VPN",
            category = IntegrityCategory.NETWORK_ANOMALY,
            severity = SignalSeverity.HIGH
        )

        val config = IntegrityConfig(
            enabledCategories = setOf(IntegrityCategory.ROOT_OR_JAILBREAK) // NETWORK_ANOMALY disabled
        )

        val score = engine.calculateScore(listOf(signal), config)
        assertEquals(0.0, score.score)
        assertEquals(IntegrityMitigationAction.ALLOW, score.action)
    }

    @Test
    fun testScoringEngineCustomThresholds() {
        val engine = DefaultIntegrityRiskScoringEngine()
        val signal = IntegritySignal(
            id = "test_signal",
            name = "Test Signal",
            category = IntegrityCategory.VIRTUAL_OS_OR_EMULATOR,
            severity = SignalSeverity.HIGH,
            confidence = 1.0
        )

        // Strict thresholds
        val strictConfig = IntegrityConfig(
            thresholds = IntegrityThresholds(
                warnThreshold = 5.0,
                challengeThreshold = 10.0,
                blockThreshold = 20.0
            )
        )

        val score = engine.calculateScore(listOf(signal), strictConfig)
        assertEquals(IntegrityMitigationAction.BLOCK, score.action)
        assertEquals(RiskLevel.CRITICAL, score.riskLevel)
    }

    @Test
    fun testModelValidationFailures() {
        // IntegritySignal confidence validation
        assertFailsWith<IllegalArgumentException> {
            IntegritySignal(
                id = "invalid_conf_high",
                name = "Invalid Confidence",
                category = IntegrityCategory.ROOT_OR_JAILBREAK,
                severity = SignalSeverity.LOW,
                confidence = 1.5
            )
        }
        assertFailsWith<IllegalArgumentException> {
            IntegritySignal(
                id = "invalid_conf_low",
                name = "Invalid Confidence",
                category = IntegrityCategory.ROOT_OR_JAILBREAK,
                severity = SignalSeverity.LOW,
                confidence = -0.1
            )
        }

        // IntegrityThresholds validation
        assertFailsWith<IllegalArgumentException> {
            IntegrityThresholds(warnThreshold = -1.0)
        }
        assertFailsWith<IllegalArgumentException> {
            IntegrityThresholds(warnThreshold = 50.0, challengeThreshold = 40.0)
        }
        assertFailsWith<IllegalArgumentException> {
            IntegrityThresholds(warnThreshold = 20.0, challengeThreshold = 50.0, blockThreshold = 40.0)
        }

        // ModelParameters validation
        assertFailsWith<IllegalArgumentException> {
            ModelParameters(saturationScalingFactor = 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            ModelParameters(saturationScalingFactor = -10.0)
        }

        // IntegrityRiskScore validation
        assertFailsWith<IllegalArgumentException> {
            IntegrityRiskScore(
                score = -0.5,
                riskLevel = RiskLevel.LOW,
                action = IntegrityMitigationAction.ALLOW
            )
        }
        assertFailsWith<IllegalArgumentException> {
            IntegrityRiskScore(
                score = 100.5,
                riskLevel = RiskLevel.CRITICAL,
                action = IntegrityMitigationAction.BLOCK
            )
        }
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
