package com.landoulsi.integrity.engine

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegrityConfig
import com.landoulsi.integrity.model.IntegrityMitigationAction
import com.landoulsi.integrity.model.IntegrityRiskScore
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.ModelParameters
import com.landoulsi.integrity.model.RiskLevel
import com.landoulsi.integrity.model.SignalSeverity
import kotlin.math.exp
import kotlin.math.round

/**
 * Contract for evaluating integrity signals into composite risk scores with explainability breakdowns.
 */
interface IntegrityRiskScoringEngine {
    /**
     * Computes the composite [IntegrityRiskScore] from the supplied [signals] based on [config].
     *
     * @param signals Collection of observed threat signals.
     * @param config Detection and scoring configuration containing active categories, weights, and thresholds.
     * @param timestamp Epoch timestamp in milliseconds of this evaluation run.
     * @return Deterministic [IntegrityRiskScore] verdict including category attribution.
     */
    fun calculateScore(
        signals: List<IntegritySignal>,
        config: IntegrityConfig = IntegrityConfig(),
        timestamp: Long = 0L
    ): IntegrityRiskScore
}

/**
 * Standard implementation of [IntegrityRiskScoringEngine] utilizing a confidence-weighted
 * statistical scoring model with non-linear saturation and explainability attribution.
 */
class DefaultIntegrityRiskScoringEngine : IntegrityRiskScoringEngine {

    override fun calculateScore(
        signals: List<IntegritySignal>,
        config: IntegrityConfig,
        timestamp: Long
    ): IntegrityRiskScore {
        val enabledSignals = signals.filter { it.category in config.enabledCategories }

        if (enabledSignals.isEmpty()) {
            return IntegrityRiskScore.clean(evaluatedAt = timestamp)
        }

        val params = config.modelParameters
        val rawCategoryScores = mutableMapOf<IntegrityCategory, Double>()

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
            return IntegrityRiskScore.clean(evaluatedAt = timestamp).copy(signals = enabledSignals)
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
            normalizedScore >= thresholds.blockThreshold -> RiskLevel.CRITICAL to IntegrityMitigationAction.BLOCK
            normalizedScore >= thresholds.challengeThreshold -> RiskLevel.HIGH to IntegrityMitigationAction.CHALLENGE
            normalizedScore >= thresholds.warnThreshold -> RiskLevel.MEDIUM to IntegrityMitigationAction.WARN
            else -> RiskLevel.LOW to IntegrityMitigationAction.ALLOW
        }

        return IntegrityRiskScore(
            score = normalizedScore,
            riskLevel = riskLevel,
            action = action,
            signals = enabledSignals,
            categoryAttribution = categoryAttribution,
            evaluatedAt = timestamp
        )
    }
}
