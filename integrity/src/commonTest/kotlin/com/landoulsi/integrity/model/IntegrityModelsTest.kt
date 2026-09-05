package com.landoulsi.integrity.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntegrityModelsTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
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
    fun testIntegrityConfigDefaultEnablesEveryCategory() {
        val config = IntegrityConfig()

        // A regression here would silently disable detection wholesale for any category
        // left out of the default set, with nothing else in the suite positioned to catch it.
        assertEquals(IntegrityCategory.entries.toSet(), config.enabledCategories)
    }

    @Test
    fun testIntegrityConfigSerializationRoundTrip() {
        val config = IntegrityConfig(
            enabledCategories = setOf(IntegrityCategory.ROOT_OR_JAILBREAK, IntegrityCategory.NETWORK_ANOMALY),
            thresholds = IntegrityThresholds(warnThreshold = 10.0, challengeThreshold = 40.0, blockThreshold = 90.0),
            modelParameters = ModelParameters(
                severityWeights = mapOf(SignalSeverity.HIGH to 30.0, SignalSeverity.CRITICAL to 45.0),
                categoryMultipliers = mapOf(IntegrityCategory.ROOT_OR_JAILBREAK to 1.5),
                saturationScalingFactor = 75.0
            )
        )

        val serialized = json.encodeToString(config)
        val deserialized = json.decodeFromString<IntegrityConfig>(serialized)

        assertEquals(config, deserialized)
    }

    @Test
    fun testModelParametersSerializationRoundTripPreservesEnumKeyedMaps() {
        // severityWeights and categoryMultipliers are enum-keyed maps, a known rough edge
        // for kotlinx.serialization's default map serializer — verify it actually round-trips
        // rather than assuming it does.
        val params = ModelParameters(
            severityWeights = mapOf(
                SignalSeverity.INFO to 1.0,
                SignalSeverity.LOW to 6.0,
                SignalSeverity.MEDIUM to 16.0,
                SignalSeverity.HIGH to 26.0,
                SignalSeverity.CRITICAL to 41.0
            ),
            categoryMultipliers = mapOf(
                IntegrityCategory.HOOKING_OR_TAMPERING to 2.0,
                IntegrityCategory.MOCK_LOCATION to 0.5
            ),
            saturationScalingFactor = 55.0
        )

        val serialized = json.encodeToString(params)
        val deserialized = json.decodeFromString<ModelParameters>(serialized)

        assertEquals(params, deserialized)
        assertEquals(26.0, deserialized.severityWeights[SignalSeverity.HIGH])
        assertEquals(2.0, deserialized.categoryMultipliers[IntegrityCategory.HOOKING_OR_TAMPERING])
    }

    @Test
    fun testIntegrityThresholdsSerializationRoundTrip() {
        val thresholds = IntegrityThresholds(warnThreshold = 15.0, challengeThreshold = 55.0, blockThreshold = 85.0)

        val serialized = json.encodeToString(thresholds)
        val deserialized = json.decodeFromString<IntegrityThresholds>(serialized)

        assertEquals(thresholds, deserialized)
    }
}
