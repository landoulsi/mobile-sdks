package com.landoulsi.security

/**
 * Core interface providing abstracted biometric authentication and hardware-backed cryptographic operations.
 */
interface BiometricAuthenticator {

    /**
     * Checks whether the device hardware supports biometric authentication and whether
     * strong biometric credentials (BIOMETRIC_STRONG) are currently enrolled.
     */
    fun canAuthenticate(): BiometricStatus

    /**
     * Displays a biometric prompt to authenticate the user.
     * Biometric raw data never leaves the secure hardware; only a pass/fail signal is returned.
     */
    suspend fun authenticate(promptInfo: BiometricPromptInfo): BiometricAuthResult

    /**
     * Executes an authenticated cryptographic operation (encryption/decryption) backed by
     * a secure key inside the Android Keystore / Secure Enclave.
     */
    suspend fun authenticateWithCrypto(
        promptInfo: BiometricPromptInfo,
        operation: BiometricCryptoOperation
    ): BiometricCryptoResult

    /**
     * Returns true if the user has opted in to biometric login.
     */
    fun isBiometricOptedIn(): Boolean

    /**
     * Sets whether the user has opted in to biometric login.
     */
    fun setBiometricOptIn(enabled: Boolean)

    /**
     * Returns true if a hardware-backed biometric key currently exists.
     */
    fun hasBiometricKey(): Boolean

    /**
     * Deletes the biometric cryptographic key from the secure keystore.
     */
    fun deleteBiometricKey()
}
