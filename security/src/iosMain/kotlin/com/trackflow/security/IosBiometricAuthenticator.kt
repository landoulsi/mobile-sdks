package com.trackflow.security

import com.trackflow.logger.Logger
import dev.zacsweers.metro.Inject

/**
 * iOS stub implementation of [BiometricAuthenticator].
 * Returns UNSUPPORTED to indicate biometric authentication is not yet implemented on iOS.
 * This prevents silent authentication bypasses while keeping the module compileable.
 */
@Inject
class IosBiometricAuthenticator : BiometricAuthenticator {

    override fun canAuthenticate(): BiometricStatus {
        return BiometricStatus.UNSUPPORTED
    }

    override fun isBiometricOptedIn(): Boolean = false

    override fun setBiometricOptIn(enabled: Boolean) {}

    override fun hasBiometricKey(): Boolean = false

    override fun deleteBiometricKey() {}

    override suspend fun authenticate(promptInfo: BiometricPromptInfo): BiometricAuthResult {
        Logger.w(TAG, "authenticate is not implemented on iOS yet.")
        return BiometricAuthResult.Error(ERROR_NOT_IMPLEMENTED, "Biometric authentication is not implemented on iOS yet.")
    }

    override suspend fun authenticateWithCrypto(
        promptInfo: BiometricPromptInfo,
        operation: BiometricCryptoOperation
    ): BiometricCryptoResult {
        Logger.w(TAG, "authenticateWithCrypto is not implemented on iOS yet.")
        return BiometricCryptoResult.Error(ERROR_NOT_IMPLEMENTED, "Biometric crypto operation is not implemented on iOS yet.")
    }

    companion object {
        private const val TAG = "IosBiometricAuthenticator"
        private val ERROR_NOT_IMPLEMENTED = BiometricErrorCode.SystemError(-1)
    }
}

