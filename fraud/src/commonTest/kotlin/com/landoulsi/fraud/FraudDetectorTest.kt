package com.landoulsi.fraud

import com.landoulsi.fraud.engine.DefaultFraudRiskScoringEngine
import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudConfig
import com.landoulsi.fraud.model.FraudMitigationAction
import com.landoulsi.fraud.model.FraudRiskScore
import com.landoulsi.fraud.model.FraudSignal
import com.landoulsi.fraud.model.FraudThresholds
import com.landoulsi.fraud.model.ModelParameters
import com.landoulsi.fraud.model.RiskLevel
import com.landoulsi.fraud.model.SignalSeverity
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

class FraudDetectorTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testSerializationRoundTrip() {
        val signal = FraudSignal(
            id = "test_root_su",
            name = "SU Binary Detected",
            category = FraudCategory.ROOT_OR_JAILBREAK,
            severity = SignalSeverity.CRITICAL,
            confidence = 0.95,
            details = "/system/bin/su found",
            detectedAt = 1700000000000L,
            metadata = mapOf("path" to "/system/bin/su", "permissions" to "rwxr-xr-x")
        )

        val serializedSignal = json.encodeToString(signal)
        val deserializedSignal = json.decodeFromString<FraudSignal>(serializedSignal)
        assertEquals(signal, deserializedSignal)

        val riskScore = FraudRiskScore(
            score = 85.5,
            riskLevel = RiskLevel.CRITICAL,
            action = FraudMitigationAction.BLOCK,
            signals = listOf(signal),
            categoryAttribution = mapOf(FraudCategory.ROOT_OR_JAILBREAK to 85.5),
            evaluatedAt = 1700000000000L
        )

        val serializedScore = json.encodeToString(riskScore)
        val deserializedScore = json.decodeFromString<FraudRiskScore>(serializedScore)
        assertEquals(riskScore, deserializedScore)
        assertTrue(deserializedScore.isBlocked)
        assertFalse(deserializedScore.isAllowed)
    }

    @Test
    fun testScoringEngineZeroThreatsProducesCleanScore() {
        val engine = DefaultFraudRiskScoringEngine()
        val score = engine.calculateScore(emptyList(), FraudConfig())

        assertEquals(0.0, score.score)
        assertEquals(RiskLevel.LOW, score.riskLevel)
        assertEquals(FraudMitigationAction.ALLOW, score.action)
        assertTrue(score.signals.isEmpty())
        assertTrue(score.categoryAttribution.isEmpty())
        assertTrue(score.isAllowed)
    }

    @Test
    fun testScoringEngineZeroRawScoreWithSignals() {
        val engine = DefaultFraudRiskScoringEngine()
        val infoSignal = FraudSignal(
            id = "test_info_signal",
            name = "Informational Device Stat",
            category = FraudCategory.NETWORK_ANOMALY,
            severity = SignalSeverity.INFO,
            confidence = 1.0
        )

        val score = engine.calculateScore(listOf(infoSignal), FraudConfig())
        assertEquals(0.0, score.score)
        assertEquals(RiskLevel.LOW, score.riskLevel)
        assertEquals(FraudMitigationAction.ALLOW, score.action)
        assertEquals(1, score.signals.size)
        assertTrue(score.categoryAttribution.isEmpty())
        assertTrue(score.isAllowed)
    }

    @Test
    fun testScoringEngineSingleCriticalSignal() {
        val engine = DefaultFraudRiskScoringEngine()
        val signal = FraudSignal(
            id = "test_frida_hook",
            name = "Frida Server Active",
            category = FraudCategory.HOOKING_OR_TAMPERING,
            severity = SignalSeverity.CRITICAL,
            confidence = 1.0
        )

        val score = engine.calculateScore(listOf(signal), FraudConfig())
        assertTrue(score.score > 0.0)
        assertTrue(score.score <= 100.0)
        assertEquals(1, score.signals.size)
        assertNotNull(score.categoryAttribution[FraudCategory.HOOKING_OR_TAMPERING])
    }

    @Test
    fun testScoringEngineConfidenceScaling() {
        val engine = DefaultFraudRiskScoringEngine()
        val fullConfidenceSignal = FraudSignal(
            id = "mock_gps",
            name = "Mock GPS",
            category = FraudCategory.MOCK_LOCATION,
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
        val engine = DefaultFraudRiskScoringEngine()
        val signal = FraudSignal(
            id = "vpn_active",
            name = "Active VPN",
            category = FraudCategory.NETWORK_ANOMALY,
            severity = SignalSeverity.HIGH
        )

        val config = FraudConfig(
            enabledCategories = setOf(FraudCategory.ROOT_OR_JAILBREAK) // NETWORK_ANOMALY disabled
        )

        val score = engine.calculateScore(listOf(signal), config)
        assertEquals(0.0, score.score)
        assertEquals(FraudMitigationAction.ALLOW, score.action)
    }

    @Test
    fun testScoringEngineCustomThresholds() {
        val engine = DefaultFraudRiskScoringEngine()
        val signal = FraudSignal(
            id = "test_signal",
            name = "Test Signal",
            category = FraudCategory.VIRTUAL_OS_OR_EMULATOR,
            severity = SignalSeverity.HIGH,
            confidence = 1.0
        )

        // Strict thresholds
        val strictConfig = FraudConfig(
            thresholds = FraudThresholds(
                warnThreshold = 5.0,
                challengeThreshold = 10.0,
                blockThreshold = 20.0
            )
        )

        val score = engine.calculateScore(listOf(signal), strictConfig)
        assertEquals(FraudMitigationAction.BLOCK, score.action)
        assertEquals(RiskLevel.CRITICAL, score.riskLevel)
    }

    @Test
    fun testModelValidationFailures() {
        // FraudSignal confidence validation
        assertFailsWith<IllegalArgumentException> {
            FraudSignal(
                id = "invalid_conf_high",
                name = "Invalid Confidence",
                category = FraudCategory.ROOT_OR_JAILBREAK,
                severity = SignalSeverity.LOW,
                confidence = 1.5
            )
        }
        assertFailsWith<IllegalArgumentException> {
            FraudSignal(
                id = "invalid_conf_low",
                name = "Invalid Confidence",
                category = FraudCategory.ROOT_OR_JAILBREAK,
                severity = SignalSeverity.LOW,
                confidence = -0.1
            )
        }

        // FraudThresholds validation
        assertFailsWith<IllegalArgumentException> {
            FraudThresholds(warnThreshold = -1.0)
        }
        assertFailsWith<IllegalArgumentException> {
            FraudThresholds(warnThreshold = 50.0, challengeThreshold = 40.0)
        }
        assertFailsWith<IllegalArgumentException> {
            FraudThresholds(warnThreshold = 20.0, challengeThreshold = 50.0, blockThreshold = 40.0)
        }

        // ModelParameters validation
        assertFailsWith<IllegalArgumentException> {
            ModelParameters(saturationScalingFactor = 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            ModelParameters(saturationScalingFactor = -10.0)
        }

        // FraudRiskScore validation
        assertFailsWith<IllegalArgumentException> {
            FraudRiskScore(
                score = -0.5,
                riskLevel = RiskLevel.LOW,
                action = FraudMitigationAction.ALLOW
            )
        }
        assertFailsWith<IllegalArgumentException> {
            FraudRiskScore(
                score = 100.5,
                riskLevel = RiskLevel.CRITICAL,
                action = FraudMitigationAction.BLOCK
            )
        }
    }

    @Test
    fun testDefaultFraudDetectorAggregation() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val rootEvaluator = object : SignalEvaluator {
            override val category: FraudCategory = FraudCategory.ROOT_OR_JAILBREAK
            override suspend fun evaluate(): List<FraudSignal> = listOf(
                FraudSignal(
                    id = "root_su",
                    name = "SU Binary Detected",
                    category = FraudCategory.ROOT_OR_JAILBREAK,
                    severity = SignalSeverity.CRITICAL
                )
            )
        }

        val emulatorEvaluator = object : SignalEvaluator {
            override val category: FraudCategory = FraudCategory.VIRTUAL_OS_OR_EMULATOR
            override suspend fun evaluate(): List<FraudSignal> = listOf(
                FraudSignal(
                    id = "qemu_props",
                    name = "QEMU Driver Detected",
                    category = FraudCategory.VIRTUAL_OS_OR_EMULATOR,
                    severity = SignalSeverity.HIGH
                )
            )
        }

        val detector = DefaultFraudDetector(
            evaluators = listOf(rootEvaluator, emulatorEvaluator),
            dispatcher = testDispatcher
        )

        val detected = detector.detectSignals()
        assertEquals(2, detected.size)

        val riskScore = detector.evaluateRisk()
        assertTrue(riskScore.score > 0.0)
        assertEquals(2, riskScore.signals.size)
        assertTrue(riskScore.categoryAttribution.containsKey(FraudCategory.ROOT_OR_JAILBREAK))
        assertTrue(riskScore.categoryAttribution.containsKey(FraudCategory.VIRTUAL_OS_OR_EMULATOR))

        val rootOnly = detector.evaluateCategory(FraudCategory.ROOT_OR_JAILBREAK)
        assertEquals(1, rootOnly.size)
        assertEquals("root_su", rootOnly.first().id)
    }

    @Test
    fun testDefaultFraudDetectorFaultTolerance() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val faultyEvaluator = object : SignalEvaluator {
            override val category: FraudCategory = FraudCategory.DEBUGGER_ATTACHED
            override suspend fun evaluate(): List<FraudSignal> {
                throw IllegalStateException("Failed to inspect ptrace")
            }
        }

        val goodEvaluator = object : SignalEvaluator {
            override val category: FraudCategory = FraudCategory.APP_CLONING
            override suspend fun evaluate(): List<FraudSignal> = listOf(
                FraudSignal(
                    id = "parallel_space",
                    name = "Parallel Space Path",
                    category = FraudCategory.APP_CLONING,
                    severity = SignalSeverity.MEDIUM
                )
            )
        }

        val detector = DefaultFraudDetector(
            evaluators = listOf(faultyEvaluator, goodEvaluator),
            dispatcher = testDispatcher
        )

        val signals = detector.detectSignals()
        assertEquals(1, signals.size)
        assertEquals("parallel_space", signals.first().id)
    }

    @Test
    fun testDefaultFraudDetectorCancellationPreserved() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val cancelingEvaluator = object : SignalEvaluator {
            override val category: FraudCategory = FraudCategory.DEBUGGER_ATTACHED
            override suspend fun evaluate(): List<FraudSignal> {
                throw CancellationException("Simulated coroutine cancellation")
            }
        }

        val detector = DefaultFraudDetector(
            evaluators = listOf(cancelingEvaluator),
            dispatcher = testDispatcher
        )

        assertFailsWith<CancellationException> {
            detector.detectSignals()
        }

        assertFailsWith<CancellationException> {
            detector.evaluateCategory(FraudCategory.DEBUGGER_ATTACHED)
        }
    }

    @Test
    fun testDefaultFraudDetectorDynamicConfigUpdate() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val rootEvaluator = object : SignalEvaluator {
            override val category: FraudCategory = FraudCategory.ROOT_OR_JAILBREAK
            override suspend fun evaluate(): List<FraudSignal> = listOf(
                FraudSignal(
                    id = "root_su",
                    name = "SU Binary Detected",
                    category = FraudCategory.ROOT_OR_JAILBREAK,
                    severity = SignalSeverity.CRITICAL
                )
            )
        }

        val detector = DefaultFraudDetector(
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
    fun testDefaultFraudDetectorObserveSignals() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val evaluator = object : SignalEvaluator {
            override val category: FraudCategory = FraudCategory.MOCK_LOCATION
            override suspend fun evaluate(): List<FraudSignal> = listOf(
                FraudSignal(
                    id = "mock_location_app",
                    name = "Mock Provider Active",
                    category = FraudCategory.MOCK_LOCATION,
                    severity = SignalSeverity.HIGH
                )
            )
        }

        val detector = DefaultFraudDetector(
            evaluators = listOf(evaluator),
            dispatcher = testDispatcher
        )

        val firstEmission = detector.observeSignals().first()
        assertEquals(1, firstEmission.size)
        assertEquals("mock_location_app", firstEmission.first().id)
    }

    @Test
    fun testDefaultFraudDetectorObserveRisk() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)

        val evaluator = object : SignalEvaluator {
            override val category: FraudCategory = FraudCategory.MOCK_LOCATION
            override suspend fun evaluate(): List<FraudSignal> = listOf(
                FraudSignal(
                    id = "mock_location_app",
                    name = "Mock Provider Active",
                    category = FraudCategory.MOCK_LOCATION,
                    severity = SignalSeverity.CRITICAL
                )
            )
        }

        val detector = DefaultFraudDetector(
            evaluators = listOf(evaluator),
            dispatcher = testDispatcher
        )

        val score = detector.observeRisk().first()
        assertTrue(score.score > 0.0)
        assertEquals(1, score.signals.size)
    }

    @Test
    fun testInvalidPollIntervalThrows() {
        val detector = DefaultFraudDetector()

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
