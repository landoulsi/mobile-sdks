package com.landoulsi.demo.ui

import android.content.Context
import com.landoulsi.integrity.DefaultIntegrityDetector
import com.landoulsi.integrity.IntegrityResult
import com.landoulsi.integrity.SignalEvaluator
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegrityRiskScore
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.viewmodel.ViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Pre-configured simulated integrity breach scenarios for developer showcase and verification.
 */
enum class IntegrityScenario(
    val label: String,
    val description: String,
) {
    LIVE_DEVICE(
        label = "Live Device",
        description = "Live security sweep evaluating real-world OS, hardware, and runtime signals.",
    ),
    ROOT_BREACH(
        label = "Root Breach",
        description = "Simulates su binary detection, Magisk mounts, and unlocked bootloader keys.",
    ),
    DYNAMIC_HOOKING(
        label = "Frida / Xposed",
        description = "Simulates active Frida instrumentation daemon and loaded Xposed framework hooks.",
    ),
    GPS_SPOOFING(
        label = "Mock Location",
        description = "Simulates fake GPS providers, mock settings, and coordinate teleportation jumps.",
    ),
    NETWORK_ANOMALY(
        label = "Network Threat",
        description = "Simulates active VPN tunnels, MITM system proxies, and ADB developer debugging.",
    ),
    CRITICAL_ATTACK(
        label = "Severe Attack",
        description = "Simulates multi-vector simultaneous compromise (Root + Frida + Mock GPS + Proxy).",
    ),
    CLEAN_BASELINE(
        label = "Clean Baseline",
        description = "Simulates a pristine baseline device with 0 threat signals detected.",
    ),
}

/**
 * Immutable UI state for the Integrity Detection Showcase Screen.
 */
data class IntegrityShowcaseUiState(
    val isScanning: Boolean = false,
    val selectedScenario: IntegrityScenario = IntegrityScenario.LIVE_DEVICE,
    val scanResult: IntegrityResult? = null,
    val riskScore: IntegrityRiskScore? = null,
    val expandedCategories: Set<IntegrityCategory> = IntegrityCategory.entries.toSet(),
    val selectedSignal: IntegritySignal? = null,
    val lastScanTimestamp: Long = 0L,
)

/**
 * State orchestrator for the Integrity Detection Showcase.
 */
class IntegrityShowcaseViewModel(
    private val evaluatorProvider: (IntegrityScenario, Context?) -> List<SignalEvaluator> =
        IntegrityScenarioFixtures::buildEvaluators,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(IntegrityShowcaseUiState())
    val uiState: StateFlow<IntegrityShowcaseUiState> = _uiState.asStateFlow()

    /**
     * Executes a single-pass detection sweep against the given [scenario].
     * Evaluators are executed once via [DefaultIntegrityDetector], and both
     * [IntegrityRiskScore] and [IntegrityResult] are derived from that single evaluation.
     */
    fun runSweep(
        context: Context? = null,
        scenario: IntegrityScenario = _uiState.value.selectedScenario,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, selectedScenario = scenario) }

            val evaluators = evaluatorProvider(scenario, context)
            val detector = DefaultIntegrityDetector(evaluators = evaluators, dispatcher = dispatcher)

            // Single evaluation execution: avoids duplicate filesystem / package manager checks
            val riskScore = detector.evaluateRisk()

            val firedIds = riskScore.signals.mapTo(mutableSetOf()) { it.id }
            val firedCategories = riskScore.signals.mapTo(mutableSetOf()) { it.category }
            val knownSignalIds = evaluators.flatMapTo(mutableSetOf()) { it.knownSignalIds }

            val scanResult = IntegrityResult(
                integrityScore = riskScore.score.roundToInt(),
                riskLevel = riskScore.riskLevel,
                action = riskScore.action,
                signals = (knownSignalIds + firedIds).associateWith { it in firedIds },
                categories = IntegrityCategory.entries.associateWith { it in firedCategories },
                fired = riskScore.signals,
                evaluatedAt = riskScore.evaluatedAt,
            )

            _uiState.update {
                it.copy(
                    isScanning = false,
                    scanResult = scanResult,
                    riskScore = riskScore,
                    lastScanTimestamp = riskScore.evaluatedAt,
                )
            }
        }
    }

    fun selectScenario(scenario: IntegrityScenario, context: Context? = null) {
        runSweep(context = context, scenario = scenario)
    }

    fun toggleCategory(category: IntegrityCategory) {
        _uiState.update { state ->
            val updated = if (category in state.expandedCategories) {
                state.expandedCategories - category
            } else {
                state.expandedCategories + category
            }
            state.copy(expandedCategories = updated)
        }
    }

    fun expandAllCategories() {
        _uiState.update {
            it.copy(expandedCategories = IntegrityCategory.entries.toSet())
        }
    }

    fun collapseAllCategories() {
        _uiState.update { it.copy(expandedCategories = emptySet()) }
    }

    fun selectSignalForInspection(signal: IntegritySignal?) {
        _uiState.update { it.copy(selectedSignal = signal) }
    }
}
