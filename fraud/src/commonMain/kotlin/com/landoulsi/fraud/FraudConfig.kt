package com.landoulsi.fraud

/**
 * Configuration for the FraudDetector. Allows customization of detection thresholds,
 * mitigation states, and which checks to enable/disable.
 */
data class FraudConfig(
    val enabledCategories: Set<FraudCategory> = FraudCategory.values().toSet(),
    val riskScoreThresholds: Map<SignalSeverity, Int> = mapOf(
        SignalSeverity.INFO to 0,
        SignalSeverity.LOW to 25,
        SignalSeverity.MEDIUM to 50,
        SignalSeverity.HIGH to 75,
        SignalSeverity.CRITICAL to 90
    ),
    val mitigationStates: Map<SignalSeverity, String> = mapOf(
        SignalSeverity.INFO to "ALLOW",
        SignalSeverity.LOW to "ALLOW",
        SignalSeverity.MEDIUM to "WARN",
        SignalSeverity.HIGH to "CHALLENGE",
        SignalSeverity.CRITICAL to "BLOCK"
    ),
    val allowNonRootedEmulated: Boolean = false,
    val blockDebuggerAttached: Boolean = true,
    val blockFrida: Boolean = true,
    val blockXposed: Boolean = true,
    val blockSubstrate: Boolean = true
)