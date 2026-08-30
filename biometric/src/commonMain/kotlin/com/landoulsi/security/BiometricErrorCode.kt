package com.landoulsi.security

/**
 * Categorized cause of a [BiometricAuthResult.Error] or [BiometricCryptoResult.Error].
 */
sealed interface BiometricErrorCode {
    data object NoActivity : BiometricErrorCode
    data class SystemError(val code: Int) : BiometricErrorCode
}
