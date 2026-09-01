package com.landoulsi.fraud

import com.landoulsi.fraud.engine.DefaultFraudRiskScoringEngine
import com.landoulsi.fraud.engine.FraudRiskScoringEngine
import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudConfig
import com.landoulsi.fraud.model.FraudRiskScore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.math.roundToInt

/**
 * High-level entry point that runs every registered detection vector and folds the
 * outcome into a single flat [FraudResult] (each known signal as a boolean plus the
 * composite fraud score).
 *
 * This is a thin facade over [FraudDetector] and [FraudRiskScoringEngine]; it adds
 * no detection or scoring logic of its own, only aggregation and reshaping.
 *
 * @property detector Orchestrator that executes the evaluators and computes the risk score.
 */
class FraudManager(
    private val detector: FraudDetector,
    knownSignalIds: Set<String>,
) {

    /** Full catalog of signal ids used to report not-fired checks as explicit `false`. */
    private val catalog: Set<String> = knownSignalIds.toSet()

    /**
     * Executes a one-shot detection sweep across all enabled categories and returns
     * the aggregated [FraudResult].
     */
    suspend fun scan(): FraudResult {
        val score: FraudRiskScore = detector.evaluateRisk()

        val firedIds = score.signals.mapTo(mutableSetOf()) { it.id }
        val firedCategories = score.signals.mapTo(mutableSetOf()) { it.category }

        return FraudResult(
            fraudScore = score.score.roundToInt(),
            riskLevel = score.riskLevel,
            action = score.action,
            // `catalog + firedIds` keeps a positive even if an evaluator emits an id
            // it never declared in knownSignalIds, rather than silently dropping it.
            signals = (catalog + firedIds).associateWith { it in firedIds },
            categories = FraudCategory.entries.associateWith { it in firedCategories },
            fired = score.signals,
            evaluatedAt = score.evaluatedAt,
        )
    }

    companion object {
        /**
         * Builds a [FraudManager] from a list of [evaluators], wiring a
         * [DefaultFraudDetector] internally and deriving the signal catalog from
         * each evaluator's [SignalEvaluator.knownSignalIds]. This keeps the evaluator
         * list as the single source of truth.
         */
        fun from(
            evaluators: List<SignalEvaluator>,
            scoringEngine: FraudRiskScoringEngine = DefaultFraudRiskScoringEngine(),
            config: FraudConfig = FraudConfig(),
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): FraudManager = FraudManager(
            detector = DefaultFraudDetector(
                evaluators = evaluators,
                scoringEngine = scoringEngine,
                initialConfig = config,
                dispatcher = dispatcher,
            ),
            knownSignalIds = evaluators.flatMapTo(mutableSetOf()) { it.knownSignalIds },
        )
    }
}
