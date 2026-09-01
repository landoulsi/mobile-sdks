package com.landoulsi.fraud.engine

import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudConfig
import com.landoulsi.fraud.model.FraudMitigationAction
import com.landoulsi.fraud.model.FraudRiskScore
import com.landoulsi.fraud.model.FraudSignal
import com.landoulsi.fraud.model.ModelParameters
import com.landoulsi.fraud.model.RiskLevel
import com.landoulsi.fraud.model.SignalSeverity
import kotlin.math.exp
import kotlin.math.round

/**
 * Contract for evaluating fraud signals into composite risk scores with explainability breakdowns.
 */
interface FraudRiskScoringEngine {
    /**
     * Computes the composite [FraudRiskScore] from the supplied [signals] based on [config].
     *
     * @param signals Collection of observed threat signals.
     * @param config Detection and scoring configuration containing active categories, weights, and thresholds.
     * @param timestamp Epoch timestamp in milliseconds of this evaluation run.
     * @return Deterministic [FraudRiskScore] verdict including category attribution.
     */
    fun calculateScore(
        signals: List<FraudSignal>,
        config: FraudConfig = FraudConfig(),
        timestamp: Long = 0L
    ): FraudRiskScore
}

/**
 * Standard implementation of [FraudRiskScoringEngine] utilizing a confidence-weighted
 * statistical scoring model with non-linear saturation and explainability attribution.
 */
class DefaultFraudRiskScoringEngine : FraudRiskScoringEngine {

    override fun calculateScore(
        signals: List<FraudSignal>,
        config: FraudConfig,
        timestamp: Long
    ): FraudRiskScore {
        val enabledSignals = signals.filter { it.category in config.enabledCategories }

        if (enabledSignals.isEmpty()) {
            return FraudRiskScore.clean(evaluatedAt = timestamp)
        }

        val params = config.modelParameters
        val rawCategoryScores = mutableMapOf<FraudCategory, Double>()

        for (signal in enabledSignals) {
            val baseWeight = params.severityWeights[signal.severity]
                ?: ModelParameters.DEFAULT_SEVERITY_WEIGHTS[signal.severity]
                ?: 0.0
            val categoryMultiplier = params.categoryMultipliers[signal.category] ?: 1.0
            val confidence = signal.confidence.coerceIn(0.0, 1.0)

            val signalScore = baseWeight * confidence * categoryMultiplier
            rawCategoryScores[signal.category] = (rawCategoryScores[signal.category] ?: 0.0) + signalScore
        }

        val totalRawScore = rawCategoryScores.values.sum()
        if (totalRawScore <= 0.0) {
            return FraudRiskScore.clean(evaluatedAt = timestamp).copy(signals = enabledSignals)
        }

        // Non-linear saturation curve: S = 100 * (1 - exp(-raw / saturationScalingFactor))
        val saturatedScore = 100.0 * (1.0 - exp(-totalRawScore / params.saturationScalingFactor))
        val normalizedScore = round(saturatedScore.coerceIn(0.0, 100.0) * 100.0) / 100.0

        // Calculate explainability attribution (normalized points per category)
        val categoryAttribution = rawCategoryScores.mapValues { (_, rawScore) ->
            round((normalizedScore * (rawScore / totalRawScore)) * 100.0) / 100.0
        }

        val thresholds = config.thresholds
        val (riskLevel, action) = when {
            normalizedScore >= thresholds.blockThreshold -> RiskLevel.CRITICAL to FraudMitigationAction.BLOCK
            normalizedScore >= thresholds.challengeThreshold -> RiskLevel.HIGH to FraudMitigationAction.CHALLENGE
            normalizedScore >= thresholds.warnThreshold -> RiskLevel.MEDIUM to FraudMitigationAction.WARN
            else -> RiskLevel.LOW to FraudMitigationAction.ALLOW
        }

        return FraudRiskScore(
            score = normalizedScore,
            riskLevel = riskLevel,
            action = action,
            signals = enabledSignals,
            categoryAttribution = categoryAttribution,
            evaluatedAt = timestamp
        )
    }
}
