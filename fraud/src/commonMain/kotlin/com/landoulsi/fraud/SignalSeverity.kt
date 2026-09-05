package com.landoulsi.fraud

/**
 * Severity levels for fraud signals.
 * Determines the appropriate mitigation action based on the signal severity.
 */
enum class SignalSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
