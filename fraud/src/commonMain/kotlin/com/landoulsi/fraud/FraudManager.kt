package com.landoulsi.fraud

import com.landoulsi.fraud.category.FraudCategory
import com.landoulsi.fraud.severity.SignalSeverity
import com.landoulsi.fraud.model.FraudRiskScore
import com.landoulsi.fraud.model.FraudSignal
import kotlin.coroutines.async
import kotlin.coroutines.join
import kotlin.coroutines.CoroutineScope
import kotlin.coroutines.contexts.DefaultScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-level fraud management class that orchestrates multiple detection evaluators.
 * 
 * The FraudManager coordinates all fraud signal detection checks and provides
 * a unified interface for evaluating device integrity and fraud risk.
 * 
 * Architecture follows the multi-vector signal collection approach with
 * non-blocking asynchronous sweeps and composite weighted scoring.
 */
class FraudManager(
    private val config: FraudConfig = FraudConfig(),
    private val evaluators: List<FraudEvaluator> = FraudEvaluator.values()
) {

    /**
     * Evaluates all fraud signals and returns a comprehensive FraudRiskScore.
     * 
     * This method runs all enabled detection evaluators asynchronously and
     * aggregates the results into a single risk score with associated severity
     * and mitigation state.
     */
    suspend fun evaluate(): FraudRiskScore {
        // Run all evaluators asynchronously
        val results = withContext(Dispatchers.Default) {
            evaluators.asyncMap { it.evaluate() }
        }
        
        // Collect all signals from evaluators
        val allSignals = results.flatMap { it.signals }
        
        // Filter to only enabled categories
        val enabledSignals = allSignals.filter { 
            config.enabledCategories.contains(it.category) 
        }
        
        // Mark signals as suspicious based on config
        val processedSignals = enabledSignals.map { signal ->
            // Apply config-based filtering
            val isSuspicious = signal.isSuspicious && isCategoryEnabled(signal.category)
            signal.copy(isSuspicious = isSuspicious)
        }
        
        // Calculate risk score
        val riskScore = calculateRiskScore(processedSignals)
        
        // Determine severity
        val severity = determineSeverity(riskScore)
        
        // Extract triggered categories
        val triggeredCategories = extractedTriggeredCategories(processedSignals)
        
        // Build signal details map
        val signalDetails = buildSignalDetails(processedSignals)
        
        return FraudRiskScore(
            score = riskScore,
            severity = severity,
            triggeredCategories = triggeredCategories,
            signalDetails = signalDetails
        )
    }

    /**
     * Checks if a fraud category is enabled in the config.
     */
    private fun isCategoryEnabled(category: FraudCategory): Boolean {
        return config.enabledCategories.contains(category)
    }

    /**
     * Calculates the weighted risk score based on detected signals.
     */
    private fun calculateRiskScore(signals: List<FraudSignal>): Int {
        if (signals.isEmpty()) return 0

        // Severity weights
        val severityWeights = mapOf(
            SignalSeverity.INFO to 1,
            SignalSeverity.LOW to 10,
            SignalSeverity.MEDIUM to 25,
            SignalSeverity.HIGH to 50,
            SignalSeverity.CRITICAL to 80
        )

        var totalScore = signals
            .map { severityWeights[it.severity] ?: 0 }
            .sum()

        // Cap at 100
        totalScore = Math.min(totalScore, 100)

        // Config-based adjustments
        if (config.blockFrida && processedSignals.any { it.category == FraudCategory.FRIDA_HOOKING }) {
            totalScore = Math.min(totalScore + 15, 100)
        }
        if (config.blockXposed && processedSignals.any { it.category == FraudCategory.XPOSED_HOOKING }) {
            totalScore = Math.min(totalScore + 15, 100)
        }
        if (config.blockSubstrate && processedSignals.any { it.category == FraudCategory.SUBSTRATE_HOOKING }) {
            totalScore = Math.min(totalScore + 10, 100)
        }

        return totalScore
    }

    /**
     * Determines severity level based on risk score and config thresholds.
     */
    private fun determineSeverity(score: Int): SignalSeverity {
        return when {
            score >= config.riskScoreThresholds[SignalSeverity.CRITICAL]!! -> SignalSeverity.CRITICAL
            score >= config.riskScoreThresholds[SignalSeverity.HIGH]!! -> SignalSeverity.HIGH
            score >= config.riskScoreThresholds[SignalSeverity.MEDIUM]!! -> SignalSeverity.MEDIUM
            score >= config.riskScoreThresholds[SignalSeverity.LOW]!! -> SignalSeverity.LOW
            else -> SignalSeverity.INFO
        }
    }

    /**
     * Extracts unique triggered categories from signals.
     */
    private fun extractedTriggeredCategories(signals: List<FraudSignal>): List<FraudCategory> {
        return signals
            .filter { it.isSuspicious }
            .map { it.category }
            .distinct()
    }

    /**
     * Builds a map of FraudCategory to FraudSignal for details.
     */
    private fun buildSignalDetails(signals: List<FraudSignal>): Map<FraudCategory, FraudSignal> {
        return signals
            .filter { it.isSuspicious }
            .associate { it.category to it }
    }
}

/**
 * Base trait for fraud evaluators.
 */
abstract class FraudEvaluator {

    /** Signals detected by this evaluator */
    val signals: List<FraudSignal>

    /**
     * Evaluates fraud signals for this specific check type.
     * Subclasses should implement this suspend function.
     */
    abstract suspend fun evaluate()
}

/**
 * Pre-defined evaluator instances for common fraud check types.
 */
object FraudEvaluator {
    val root: FraudEvaluator = object : FraudEvaluator() {
        override val signals: List<FraudSignal> = listOf(
            FraudSignal(
                category = FraudCategory.ROOT_DETECTION,
                severity = SignalSeverity.HIGH,
                isSuspicious = false,
                description = "Root detection check"
            )
        )

        override suspend fun evaluate() {
            // TODO: Implement root detection
        }
    }

    val emulator: FraudEvaluator = object : FraudEvaluator() {
        override val signals: List<FraudSignal> = listOf(
            FraudSignal(
                category = FraudCategory.EMULATOR_DETECTION,
                severity = SignalSeverity.MEDIUM,
                isSuspicious = false,
                description = "Emulator detection check"
            )
        )

        override suspend fun evaluate() {
            // TODO: Implement emulator detection
        }
    }

    val jailbreak: FraudEvaluator = object : FraudEvaluator() {
        override val signals: List<FraudSignal> = listOf(
            FraudSignal(
                category = FraudCategory.JAILBREAK_DETECTION,
                severity = SignalSeverity.HIGH,
                isSuspicious = false,
                description = "Jailbreak detection check"
            )
        )

        override suspend fun evaluate() {
            // TODO: Implement jailbreak detection
        }
    }

    val mockLocation: FraudEvaluator = object : FraudEvaluator() {
        override val signals: List<FraudSignal> = listOf(
            FraudSignal(
                category = FraudCategory.MOCK_LOCATION_DETECTION,
                severity = SignalSeverity.MEDIUM,
                isSuspicious = false,
                description = "Mock location detection check"
            )
        )

        override suspend fun evaluate() {
            // TODO: Implement mock location detection
        }
    }

    val frida: FraudEvaluator = object : FraudEvaluator() {
        override val signals: List<FraudSignal> = listOf(
            FraudSignal(
                category = FraudCategory.FRIDA_HOOKING,
                severity = SignalSeverity.HIGH,
                isSuspicious = false,
                description = "Frida hooking detection check"
            )
        )

        override suspend fun evaluate() {
            // TODO: Implement Frida detection
        }
    }

    val xposed: FraudEvaluator = object : FraudEvaluator() {
        override val signals: List<FraudSignal> = listOf(
            FraudSignal(
                category = FraudCategory.XPOSED_HOOKING,
                severity = SignalSeverity.HIGH,
                isSuspicious = false,
                description = "Xposed hooking detection check"
            )
        )

        override suspend fun evaluate() {
            // TODO: Implement Xposed detection
        }
    }

    val substrate: FraudEvaluator = object : FraudEvaluator() {
        override val signals: List<FraudSignal> = listOf(
            FraudSignal(
                category = FraudCategory.SUBSTRATE_HOOKING,
                severity = SignalSeverity.HIGH,
                isSuspicious = false,
                description = "Substrate hooking detection check"
            )
        )

        override suspend fun evaluate() {
            // TODO: Implement Substrate detection
        }
    }
}
