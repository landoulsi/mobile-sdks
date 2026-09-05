package com.landoulsi.integrity.engine

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegrityConfig
import com.landoulsi.integrity.model.IntegrityMitigationAction
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.IntegrityThresholds
import com.landoulsi.integrity.model.ModelParameters
import com.landoulsi.integrity.model.RiskLevel
import com.landoulsi.integrity.model.SignalSeverity
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IntegrityRiskScoringEngineTest {

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
    fun testScoringEngineCategoryMultiplierIncreasesAttributedScore() {
        val engine = DefaultIntegrityRiskScoringEngine()
        val boostedSignal = IntegritySignal(
            id = "root_su",
            name = "SU Binary Detected",
            category = IntegrityCategory.ROOT_OR_JAILBREAK,
            severity = SignalSeverity.HIGH,
            confidence = 1.0
        )
        val evenSignal = IntegritySignal(
            id = "vpn_active",
            name = "Active VPN",
            category = IntegrityCategory.NETWORK_ANOMALY,
            severity = SignalSeverity.HIGH,
            confidence = 1.0
        )

        val baselineScore = engine.calculateScore(listOf(boostedSignal, evenSignal), IntegrityConfig())

        val boostedConfig = IntegrityConfig(
            modelParameters = ModelParameters(
                categoryMultipliers = mapOf(IntegrityCategory.ROOT_OR_JAILBREAK to 3.0)
            )
        )
        val boostedScore = engine.calculateScore(listOf(boostedSignal, evenSignal), boostedConfig)

        // Equal base severity/confidence would normally split attribution evenly; the
        // multiplier on ROOT_OR_JAILBREAK alone should skew its share upward and raise
        // the overall composite score relative to the unmultiplied baseline.
        assertTrue(boostedScore.score > baselineScore.score)
        assertTrue(
            boostedScore.categoryAttribution.getValue(IntegrityCategory.ROOT_OR_JAILBREAK) >
                boostedScore.categoryAttribution.getValue(IntegrityCategory.NETWORK_ANOMALY)
        )
    }

    @Test
    fun testScoringEngineSeverityWeightFallsBackToDefaultForUnlistedSeverity() {
        val engine = DefaultIntegrityRiskScoringEngine()
        val highSeveritySignal = IntegritySignal(
            id = "test_signal",
            name = "Test Signal",
            category = IntegrityCategory.DEBUGGER_ATTACHED,
            severity = SignalSeverity.HIGH,
            confidence = 1.0
        )

        // Only CRITICAL is overridden; HIGH is absent from the map entirely.
        val partialWeightsConfig = IntegrityConfig(
            modelParameters = ModelParameters(severityWeights = mapOf(SignalSeverity.CRITICAL to 100.0))
        )

        val fallbackScore = engine.calculateScore(listOf(highSeveritySignal), partialWeightsConfig)
        val defaultScore = engine.calculateScore(listOf(highSeveritySignal), IntegrityConfig())

        // A HIGH signal missing from the override map must fall back to
        // ModelParameters.DEFAULT_SEVERITY_WEIGHTS[HIGH], matching the fully-default config exactly.
        assertEquals(defaultScore.score, fallbackScore.score)

        // But a severity explicitly present in the override map (CRITICAL) must use the
        // overridden weight rather than the default, proving overrides still take precedence.
        val criticalSignal = highSeveritySignal.copy(severity = SignalSeverity.CRITICAL)
        val overriddenCriticalScore = engine.calculateScore(listOf(criticalSignal), partialWeightsConfig)
        val defaultCriticalScore = engine.calculateScore(listOf(criticalSignal), IntegrityConfig())
        assertTrue(overriddenCriticalScore.score > defaultCriticalScore.score)
    }

    @Test
    fun testScoringEngineCategoryAttributionSumsToScoreWithinRoundingTolerance() {
        val engine = DefaultIntegrityRiskScoringEngine()
        val signals = listOf(
            IntegritySignal(
                id = "root_su",
                name = "SU Binary Detected",
                category = IntegrityCategory.ROOT_OR_JAILBREAK,
                severity = SignalSeverity.CRITICAL,
                confidence = 1.0
            ),
            IntegritySignal(
                id = "qemu_props",
                name = "QEMU Driver Detected",
                category = IntegrityCategory.VIRTUAL_OS_OR_EMULATOR,
                severity = SignalSeverity.HIGH,
                confidence = 0.9
            ),
            IntegritySignal(
                id = "vpn_active",
                name = "Active VPN",
                category = IntegrityCategory.NETWORK_ANOMALY,
                severity = SignalSeverity.MEDIUM,
                confidence = 0.7
            ),
        )

        val score = engine.calculateScore(signals, IntegrityConfig())

        // Each category's attribution is independently rounded to 2 decimals, so the sum can
        // drift from the overall score by up to ~0.01 per category; scale the tolerance accordingly.
        val tolerance = 0.02 * score.categoryAttribution.size
        val attributionSum = score.categoryAttribution.values.sum()
        assertTrue(
            abs(attributionSum - score.score) <= tolerance,
            "Expected category attribution sum ($attributionSum) to be within $tolerance of score (${score.score})"
        )
    }

    @Test
    fun testScoringEngineThresholdBoundaryIsInclusive() {
        val engine = DefaultIntegrityRiskScoringEngine()
        val signal = IntegritySignal(
            id = "test_signal",
            name = "Test Signal",
            category = IntegrityCategory.APP_CLONING,
            severity = SignalSeverity.HIGH,
            confidence = 1.0
        )

        // First pass: compute the raw score under permissive thresholds so we know its exact value.
        val baselineScore = engine.calculateScore(
            listOf(signal),
            IntegrityConfig(thresholds = IntegrityThresholds(warnThreshold = 0.0, challengeThreshold = 1000.0, blockThreshold = 2000.0))
        ).score

        // Second pass: set challengeThreshold to exactly the computed score. The engine uses
        // `>=` for threshold comparisons, so landing exactly on the boundary must still trigger
        // CHALLENGE/HIGH rather than falling through to the lower WARN band.
        val boundaryConfig = IntegrityConfig(
            thresholds = IntegrityThresholds(
                warnThreshold = 0.0,
                challengeThreshold = baselineScore,
                blockThreshold = baselineScore + 50.0
            )
        )
        val boundaryResult = engine.calculateScore(listOf(signal), boundaryConfig)

        assertEquals(baselineScore, boundaryResult.score)
        assertEquals(IntegrityMitigationAction.CHALLENGE, boundaryResult.action)
        assertEquals(RiskLevel.HIGH, boundaryResult.riskLevel)
    }
}
