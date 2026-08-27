package com.trackflow.security

/**
 * Result of a standard biometric authentication attempt.
 */
sealed interface BiometricAuthResult {
    data object Success : BiometricAuthResult
    data class Failure(val reason: String) : BiometricAuthResult
    data object Cancelled : BiometricAuthResult
    data class Lockout(val isPermanent: Boolean, val message: String) : BiometricAuthResult
    data class Error(val errorCode: BiometricErrorCode, val errorMessage: String) : BiometricAuthResult
}
