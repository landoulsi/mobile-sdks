package com.landoulsi.integrity

import com.landoulsi.integrity.engine.DefaultIntegrityRiskScoringEngine
import com.landoulsi.integrity.engine.IntegrityRiskScoringEngine
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegrityConfig
import com.landoulsi.integrity.model.IntegrityRiskScore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.math.roundToInt

/**
 * High-level entry point that runs every registered detection vector and folds the
 * outcome into a single flat [IntegrityResult] (each known signal as a boolean plus the
 * composite integrity score).
 *
 * This is a thin facade over [IntegrityDetector] and [IntegrityRiskScoringEngine]; it adds
 * no detection or scoring logic of its own, only aggregation and reshaping.
 *
 * @property detector Orchestrator that executes the evaluators and computes the risk score.
 */
class IntegrityManager(
    private val detector: IntegrityDetector,
    knownSignalIds: Set<String>,
) {

    /** Full catalog of signal ids used to report not-fired checks as explicit `false`. */
    private val catalog: Set<String> = knownSignalIds.toSet()

    /**
     * Executes a one-shot detection sweep across all enabled categories and returns
     * the aggregated [IntegrityResult].
     */
    suspend fun scan(): IntegrityResult {
        val score: IntegrityRiskScore = detector.evaluateRisk()

        val firedIds = score.signals.mapTo(mutableSetOf()) { it.id }
        val firedCategories = score.signals.mapTo(mutableSetOf()) { it.category }

        return IntegrityResult(
            integrityScore = score.score.roundToInt(),
            riskLevel = score.riskLevel,
            action = score.action,
            // `catalog + firedIds` keeps a positive even if an evaluator emits an id
            // it never declared in knownSignalIds, rather than silently dropping it.
            signals = (catalog + firedIds).associateWith { it in firedIds },
            categories = IntegrityCategory.entries.associateWith { it in firedCategories },
            fired = score.signals,
            evaluatedAt = score.evaluatedAt,
        )
    }

    companion object {
        /**
         * Builds a [IntegrityManager] from a list of [evaluators], wiring a
         * [DefaultIntegrityDetector] internally and deriving the signal catalog from
         * each evaluator's [SignalEvaluator.knownSignalIds]. This keeps the evaluator
         * list as the single source of truth.
         */
        fun from(
            evaluators: List<SignalEvaluator>,
            scoringEngine: IntegrityRiskScoringEngine = DefaultIntegrityRiskScoringEngine(),
            config: IntegrityConfig = IntegrityConfig(),
            dispatcher: CoroutineDispatcher = Dispatchers.Default,
        ): IntegrityManager = IntegrityManager(
            detector = DefaultIntegrityDetector(
                evaluators = evaluators,
                scoringEngine = scoringEngine,
                initialConfig = config,
                dispatcher = dispatcher,
            ),
            knownSignalIds = evaluators.flatMapTo(mutableSetOf()) { it.knownSignalIds },
        )
    }
}
