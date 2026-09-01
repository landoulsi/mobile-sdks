package com.landoulsi.fraud.model

import kotlinx.serialization.Serializable

/**
 * Categorization of device threats and fraud vectors.
 */
@Serializable
enum class FraudCategory {
    /** Rooted Android device or jailbroken iOS environment. */
    ROOT_OR_JAILBREAK,

    /** Emulated hardware, virtual machine, or hypervisor sandbox. */
    VIRTUAL_OS_OR_EMULATOR,

    /** Mock location providers, fake GPS software, or location jumps. */
    MOCK_LOCATION,

    /** Dynamic hooking frameworks (Frida, Xposed) or binary tampering. */
    HOOKING_OR_TAMPERING,

    /** Managed/native debugger attached or ptrace interception. */
    DEBUGGER_ATTACHED,

    /** App cloning, dual space, or multi-instance sandboxes. */
    APP_CLONING,

    /** Suspicious network configurations such as VPNs, proxies, or ADB debugging. */
    NETWORK_ANOMALY,

    /** Sideloaded binaries or untrusted installation sources. */
    UNTRUSTED_INSTALLER
}

/**
 * Severity level of an individual fraud signal.
 */
@Serializable
enum class SignalSeverity {
    /** Informational observation with negligible immediate threat. */
    INFO,

    /** Minor anomaly or suspicious indicator. */
    LOW,

    /** Moderate threat indicator requiring elevated vigilance. */
    MEDIUM,

    /** Severe threat indicator suggesting compromised environment. */
    HIGH,

    /** Critical compromise indicator representing immediate security failure. */
    CRITICAL
}

/**
 * Qualitative classification of aggregate fraud risk.
 */
@Serializable
enum class RiskLevel {
    /** Safe device environment; low or no threat indicators. */
    LOW,

    /** Minor anomalies detected; requires monitoring. */
    MEDIUM,

    /** Substantial threats detected; step-up verification advised. */
    HIGH,

    /** Critical compromise detected; operations should be restricted. */
    CRITICAL
}

/**
 * Recommended mitigation action determined by the scoring model.
 */
@Serializable
enum class FraudMitigationAction {
    /** Standard user operation permitted. */
    ALLOW,

    /** Allow operation but record audit telemetry or notify backend. */
    WARN,

    /** Require step-up authentication (e.g. 2FA, biometric, CAPTCHA). */
    CHALLENGE,

    /** Terminate operation or lock sensitive features immediately. */
    BLOCK
}

/**
 * Represents a single detected fraud or tampering threat event.
 *
 * @property id Unique identifier of the signal (e.g. "root_su_binary").
 * @property name Human-readable title of the detected threat.
 * @property category Threat vector category.
 * @property severity Assessed severity level of the signal.
 * @property confidence Confidence score of the detection in the range [0.0, 1.0].
 * @property details Diagnostic details or contextual evidence for the detection.
 * @property detectedAt Epoch timestamp in milliseconds when the signal was detected.
 * @property metadata Key-value feature attributes supporting ML telemetry and downstream inference.
 */
@Serializable
data class FraudSignal(
    val id: String,
    val name: String,
    val category: FraudCategory,
    val severity: SignalSeverity,
    val confidence: Double = 1.0,
    val details: String = "",
    val detectedAt: Long = 0L,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(confidence in 0.0..1.0) {
            "Confidence must be between 0.0 and 1.0, got: $confidence"
        }
    }
}

/**
 * Decision thresholds defining risk boundary triggers.
 *
 * @property warnThreshold Score at or above which [FraudMitigationAction.WARN] triggers.
 * @property challengeThreshold Score at or above which [FraudMitigationAction.CHALLENGE] triggers.
 * @property blockThreshold Score at or above which [FraudMitigationAction.BLOCK] triggers.
 */
@Serializable
data class FraudThresholds(
    val warnThreshold: Double = 20.0,
    val challengeThreshold: Double = 50.0,
    val blockThreshold: Double = 80.0
) {
    init {
        require(warnThreshold >= 0.0) { "warnThreshold must be non-negative" }
        require(challengeThreshold >= warnThreshold) { "challengeThreshold must be >= warnThreshold" }
        require(blockThreshold >= challengeThreshold) { "blockThreshold must be >= challengeThreshold" }
    }
}

/**
 * Machine learning and scoring engine parameters governing feature weights and non-linear saturation.
 *
 * @property severityWeights Base points assigned to each [SignalSeverity] level.
 * @property categoryMultipliers Multipliers applied to specific [FraudCategory] threat vectors (default 1.0).
 * @property saturationScalingFactor Non-linear saturation curve parameter preventing unbounded score inflation.
 */
@Serializable
data class ModelParameters(
    val severityWeights: Map<SignalSeverity, Double> = DEFAULT_SEVERITY_WEIGHTS,
    val categoryMultipliers: Map<FraudCategory, Double> = emptyMap(),
    val saturationScalingFactor: Double = 60.0
) {
    init {
        require(saturationScalingFactor > 0.0) { "saturationScalingFactor must be positive" }
    }

    companion object {
        val DEFAULT_SEVERITY_WEIGHTS: Map<SignalSeverity, Double> = mapOf(
            SignalSeverity.INFO to 0.0,
            SignalSeverity.LOW to 5.0,
            SignalSeverity.MEDIUM to 15.0,
            SignalSeverity.HIGH to 25.0,
            SignalSeverity.CRITICAL to 40.0
        )
    }
}

/**
 * Runtime configuration for fraud detection and risk scoring.
 *
 * @property enabledCategories Active fraud threat vectors evaluated during sweeps.
 * @property thresholds Decision boundary thresholds for mitigation action mapping.
 * @property modelParameters Model weights and saturation parameters for score calculation.
 */
@Serializable
data class FraudConfig(
    val enabledCategories: Set<FraudCategory> = FraudCategory.entries.toSet(),
    val thresholds: FraudThresholds = FraudThresholds(),
    val modelParameters: ModelParameters = ModelParameters()
)

/**
 * Composite fraud evaluation verdict containing normalized risk score and explainability breakdown.
 *
 * @property score Normalized overall risk score in the range [0.0, 100.0].
 * @property riskLevel Qualitative risk band ([RiskLevel.LOW], [RiskLevel.MEDIUM], [RiskLevel.HIGH], [RiskLevel.CRITICAL]).
 * @property action Prescribed mitigation decision ([FraudMitigationAction.ALLOW], [FraudMitigationAction.WARN], [FraudMitigationAction.CHALLENGE], [FraudMitigationAction.BLOCK]).
 * @property signals List of active threat signals detected and evaluated.
 * @property categoryAttribution Explainability attribution map showing score contribution points per [FraudCategory].
 * @property evaluatedAt Epoch timestamp in milliseconds when evaluation occurred.
 */
@Serializable
data class FraudRiskScore(
    val score: Double,
    val riskLevel: RiskLevel,
    val action: FraudMitigationAction,
    val signals: List<FraudSignal> = emptyList(),
    val categoryAttribution: Map<FraudCategory, Double> = emptyMap(),
    val evaluatedAt: Long = 0L
) {
    init {
        require(score in 0.0..100.0) {
            "FraudRiskScore score must be between 0.0 and 100.0, got: $score"
        }
    }

    val isAllowed: Boolean get() = action == FraudMitigationAction.ALLOW
    val isBlocked: Boolean get() = action == FraudMitigationAction.BLOCK
    val isChallengeRequired: Boolean get() = action == FraudMitigationAction.CHALLENGE
    val isWarned: Boolean get() = action == FraudMitigationAction.WARN

    companion object {
        /**
         * Clean baseline verdict for zero detected threats.
         */
        fun clean(evaluatedAt: Long = 0L): FraudRiskScore = FraudRiskScore(
            score = 0.0,
            riskLevel = RiskLevel.LOW,
            action = FraudMitigationAction.ALLOW,
            signals = emptyList(),
            categoryAttribution = emptyMap(),
            evaluatedAt = evaluatedAt
        )
    }
}
