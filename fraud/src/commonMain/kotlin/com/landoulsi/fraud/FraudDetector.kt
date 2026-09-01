package com.landoulsi.fraud

import com.landoulsi.fraud.engine.DefaultFraudRiskScoringEngine
import com.landoulsi.fraud.engine.FraudRiskScoringEngine
import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudConfig
import com.landoulsi.fraud.model.FraudRiskScore
import com.landoulsi.fraud.model.FraudSignal
import com.landoulsi.logger.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * Pluggable evaluator contract for a specific fraud detection vector.
 *
 * Each implementation encapsulates detection logic for a single [FraudCategory],
 * adhering to the Single Responsibility and Open/Closed principles.
 */
interface SignalEvaluator {
    /** The threat vector category handled by this evaluator. */
    val category: FraudCategory

    /**
     * Executes threat detection checks for this vector and returns any observed signals.
     */
    suspend fun evaluate(): List<FraudSignal>
}

/**
 * Main orchestrator contract for device fraud, tampering, and threat detection.
 */
interface FraudDetector {
    /** Current runtime configuration. */
    val currentConfig: FraudConfig

    /**
     * Updates the runtime configuration dynamically (e.g. from remote configuration).
     */
    fun updateConfig(config: FraudConfig)

    /**
     * Executes a one-shot threat detection sweep across all enabled categories.
     *
     * @return List of all detected [FraudSignal] instances.
     */
    suspend fun detectSignals(): List<FraudSignal>

    /**
     * Executes a full detection sweep and computes the composite [FraudRiskScore]
     * using the scoring engine and active configuration.
     *
     * @return Evaluated [FraudRiskScore] verdict.
     */
    suspend fun evaluateRisk(): FraudRiskScore

    /**
     * Runs detection checks specifically for the requested [category].
     *
     * @param category The specific fraud vector to evaluate.
     * @return List of detected [FraudSignal] instances for that category.
     */
    suspend fun evaluateCategory(category: FraudCategory): List<FraudSignal>

    /**
     * Returns a cold reactive [Flow] that periodically evaluates threat signals.
     *
     * @param pollIntervalMs Polling interval in milliseconds. Must be > 0.
     */
    fun observeSignals(pollIntervalMs: Long = 5000L): Flow<List<FraudSignal>>

    /**
     * Returns a cold reactive [Flow] that periodically evaluates the composite risk score.
     *
     * @param pollIntervalMs Polling interval in milliseconds. Must be > 0.
     */
    fun observeRisk(pollIntervalMs: Long = 5000L): Flow<FraudRiskScore>
}

/**
 * Default implementation of [FraudDetector] coordinating registered [SignalEvaluator] instances
 * concurrently across coroutines.
 *
 * @property evaluators List of registered [SignalEvaluator] vector checkers.
 * @property scoringEngine Engine responsible for computing risk scores and attribution.
 * @property initialConfig Initial configuration for active categories, thresholds, and weights.
 * @property dispatcher Coroutine dispatcher for background asynchronous sweeps.
 */
class DefaultFraudDetector(
    private val evaluators: List<SignalEvaluator> = emptyList(),
    private val scoringEngine: FraudRiskScoringEngine = DefaultFraudRiskScoringEngine(),
    initialConfig: FraudConfig = FraudConfig(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : FraudDetector {

    private val _configFlow = MutableStateFlow(initialConfig)
    override val currentConfig: FraudConfig get() = _configFlow.value

    override fun updateConfig(config: FraudConfig) {
        _configFlow.value = config
    }

    override suspend fun detectSignals(): List<FraudSignal> = withContext(dispatcher) {
        val config = currentConfig
        val activeEvaluators = evaluators.filter { it.category in config.enabledCategories }

        coroutineScope {
            val deferredSignals = activeEvaluators.map { evaluator ->
                async {
                    try {
                        evaluator.evaluate()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Logger.e(
                            TAG,
                            "Signal evaluation failed for category: ${evaluator.category}",
                            e
                        )
                        emptyList()
                    }
                }
            }
            deferredSignals
                .flatMap { it.await() }
                .filter { it.category in config.enabledCategories }
        }
    }

    override suspend fun evaluateRisk(): FraudRiskScore = withContext(dispatcher) {
        val config = currentConfig
        val signals = detectSignals()
        val timestamp = Clock.System.now().toEpochMilliseconds()
        scoringEngine.calculateScore(signals, config, timestamp)
    }

    override suspend fun evaluateCategory(category: FraudCategory): List<FraudSignal> = withContext(dispatcher) {
        val activeEvaluators = evaluators.filter { it.category == category }
        coroutineScope {
            val deferredSignals = activeEvaluators.map { evaluator ->
                async {
                    try {
                        evaluator.evaluate()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Logger.e(
                            TAG,
                            "Signal evaluation failed for category: $category",
                            e
                        )
                        emptyList()
                    }
                }
            }
            deferredSignals.flatMap { it.await() }
        }
    }

    override fun observeSignals(pollIntervalMs: Long): Flow<List<FraudSignal>> {
        require(pollIntervalMs > 0L) { "pollIntervalMs must be positive, got: $pollIntervalMs" }
        return flow {
            while (true) {
                emit(detectSignals())
                delay(pollIntervalMs)
            }
        }.flowOn(dispatcher)
    }

    override fun observeRisk(pollIntervalMs: Long): Flow<FraudRiskScore> {
        require(pollIntervalMs > 0L) { "pollIntervalMs must be positive, got: $pollIntervalMs" }
        return flow {
            while (true) {
                emit(evaluateRisk())
                delay(pollIntervalMs)
            }
        }.flowOn(dispatcher)
    }

    companion object {
        private const val TAG = "DefaultFraudDetector"
    }
}
