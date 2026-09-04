package com.landoulsi.integrity

import com.landoulsi.integrity.engine.DefaultIntegrityRiskScoringEngine
import com.landoulsi.integrity.engine.IntegrityRiskScoringEngine
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegrityConfig
import com.landoulsi.integrity.model.IntegrityRiskScore
import com.landoulsi.integrity.model.IntegritySignal
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
 * Pluggable evaluator contract for a specific integrity detection vector.
 *
 * Each implementation encapsulates detection logic for a single [IntegrityCategory],
 * adhering to the Single Responsibility and Open/Closed principles.
 */
interface SignalEvaluator {
    /** The threat vector category handled by this evaluator. */
    val category: IntegrityCategory

    /**
     * Stable identifiers of every [IntegritySignal] this evaluator is capable of emitting.
     *
     * Used to assemble an exhaustive signal catalog so aggregated results
     * (see [IntegrityResult]) can report not-yet-fired checks as an explicit `false`
     * rather than omitting them. Defaults to empty for evaluators that do not
     * publish a catalog.
     */
    val knownSignalIds: Set<String> get() = emptySet()

    /**
     * Executes threat detection checks for this vector and returns any observed signals.
     */
    suspend fun evaluate(): List<IntegritySignal>
}

/**
 * Main orchestrator contract for device integrity, tampering, and threat detection.
 */
interface IntegrityDetector {
    /** Current runtime configuration. */
    val currentConfig: IntegrityConfig

    /**
     * Updates the runtime configuration dynamically (e.g. from remote configuration).
     */
    fun updateConfig(config: IntegrityConfig)

    /**
     * Executes a one-shot threat detection sweep across all enabled categories.
     *
     * @return List of all detected [IntegritySignal] instances.
     */
    suspend fun detectSignals(): List<IntegritySignal>

    /**
     * Executes a full detection sweep and computes the composite [IntegrityRiskScore]
     * using the scoring engine and active configuration.
     *
     * @return Evaluated [IntegrityRiskScore] verdict.
     */
    suspend fun evaluateRisk(): IntegrityRiskScore

    /**
     * Runs detection checks specifically for the requested [category].
     *
     * @param category The specific integrity vector to evaluate.
     * @return List of detected [IntegritySignal] instances for that category.
     */
    suspend fun evaluateCategory(category: IntegrityCategory): List<IntegritySignal>

    /**
     * Returns a cold reactive [Flow] that periodically evaluates threat signals.
     *
     * @param pollIntervalMs Polling interval in milliseconds. Must be > 0.
     */
    fun observeSignals(pollIntervalMs: Long = 5000L): Flow<List<IntegritySignal>>

    /**
     * Returns a cold reactive [Flow] that periodically evaluates the composite risk score.
     *
     * @param pollIntervalMs Polling interval in milliseconds. Must be > 0.
     */
    fun observeRisk(pollIntervalMs: Long = 5000L): Flow<IntegrityRiskScore>
}

/**
 * Default implementation of [IntegrityDetector] coordinating registered [SignalEvaluator] instances
 * concurrently across coroutines.
 *
 * @property evaluators List of registered [SignalEvaluator] vector checkers.
 * @property scoringEngine Engine responsible for computing risk scores and attribution.
 * @property initialConfig Initial configuration for active categories, thresholds, and weights.
 * @property dispatcher Coroutine dispatcher for background asynchronous sweeps.
 */
class DefaultIntegrityDetector(
    private val evaluators: List<SignalEvaluator> = emptyList(),
    private val scoringEngine: IntegrityRiskScoringEngine = DefaultIntegrityRiskScoringEngine(),
    initialConfig: IntegrityConfig = IntegrityConfig(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : IntegrityDetector {

    private val _configFlow = MutableStateFlow(initialConfig)
    override val currentConfig: IntegrityConfig get() = _configFlow.value

    override fun updateConfig(config: IntegrityConfig) {
        _configFlow.value = config
    }

    override suspend fun detectSignals(): List<IntegritySignal> = withContext(dispatcher) {
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

    override suspend fun evaluateRisk(): IntegrityRiskScore = withContext(dispatcher) {
        val config = currentConfig
        val signals = detectSignals()
        val timestamp = Clock.System.now().toEpochMilliseconds()
        scoringEngine.calculateScore(signals, config, timestamp)
    }

    override suspend fun evaluateCategory(category: IntegrityCategory): List<IntegritySignal> = withContext(dispatcher) {
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

    override fun observeSignals(pollIntervalMs: Long): Flow<List<IntegritySignal>> {
        require(pollIntervalMs > 0L) { "pollIntervalMs must be positive, got: $pollIntervalMs" }
        return flow {
            while (true) {
                emit(detectSignals())
                delay(pollIntervalMs)
            }
        }.flowOn(dispatcher)
    }

    override fun observeRisk(pollIntervalMs: Long): Flow<IntegrityRiskScore> {
        require(pollIntervalMs > 0L) { "pollIntervalMs must be positive, got: $pollIntervalMs" }
        return flow {
            while (true) {
                emit(evaluateRisk())
                delay(pollIntervalMs)
            }
        }.flowOn(dispatcher)
    }

    companion object {
        private const val TAG = "DefaultIntegrityDetector"
    }
}
