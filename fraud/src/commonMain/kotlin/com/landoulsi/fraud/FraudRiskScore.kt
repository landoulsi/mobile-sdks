package com.landoulsi.fraud

/**
 * Represents a risk score resulting from fraud signal evaluation.
 * The score ranges from 0 to 100, where 0 is no risk and 100 is maximum risk.
 */
data class FraudRiskScore(
    val score: Int,
    val maxPossibleScore: Int = 100,
    val severity: SignalSeverity,
    val triggeredCategories: List<FraudCategory>,
    val signalDetails: Map<FraudCategory, FraudSignal>
) {
    /**
     * Checks if the risk score meets or exceeds the given threshold.
     */
    fun meetsThreshold(threshold: Int): Boolean {
        return score >= threshold
    }

    /**
     * Returns the mitigation state based on the score.
     */
    fun getMitigationState(): String {
        return when {
            score >= 80 -> "BLOCK"
            score >= 50 -> "CHALLENGE"
            score >= 25 -> "WARN"
            else -> "ALLOW"
        }
    }
}
