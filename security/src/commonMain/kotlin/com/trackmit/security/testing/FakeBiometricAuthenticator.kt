package com.trackmit.security.testing

import com.trackmit.security.BiometricAuthResult
import com.trackmit.security.BiometricAuthenticator
import com.trackmit.security.BiometricCryptoOperation
import com.trackmit.security.BiometricCryptoResult
import com.trackmit.security.BiometricPromptInfo
import com.trackmit.security.BiometricStatus

class FakeBiometricAuthenticator : BiometricAuthenticator {

    var status: BiometricStatus = BiometricStatus.AVAILABLE
    var optedIn: Boolean = false
    var keyExists: Boolean = false

    var authResult: BiometricAuthResult = BiometricAuthResult.Success
    var cryptoResult: BiometricCryptoResult? = null

    override fun canAuthenticate(): BiometricStatus = status

    override fun isBiometricOptedIn(): Boolean = optedIn

    override fun setBiometricOptIn(enabled: Boolean) {
        optedIn = enabled
    }

    override fun hasBiometricKey(): Boolean = keyExists

    override fun deleteBiometricKey() {
        keyExists = false
    }

    override suspend fun authenticate(promptInfo: BiometricPromptInfo): BiometricAuthResult {
        return authResult
    }

    override suspend fun authenticateWithCrypto(
        promptInfo: BiometricPromptInfo,
        operation: BiometricCryptoOperation
    ): BiometricCryptoResult {
        cryptoResult?.let { return it }

        return when (operation) {
            is BiometricCryptoOperation.Encrypt -> {
                keyExists = true
                BiometricCryptoResult.Success(
                    outputData = operation.plainText,
                    initializationVector = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
                )
            }
            is BiometricCryptoOperation.Decrypt -> {
                BiometricCryptoResult.Success(
                    outputData = operation.cipherText,
                    initializationVector = operation.initializationVector
                )
            }
        }
    }
}
