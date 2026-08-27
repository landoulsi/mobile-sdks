package com.trackflow.security

import com.trackflow.logger.Logger
import com.trackflow.storage.SecureStorage
import dev.zacsweers.metro.Inject

/**
 * Result of attempting to retrieve a session token via biometric authentication.
 */
sealed interface BiometricSessionResult {
    data class Success(val token: String, val driverId: String) : BiometricSessionResult
    data object NoStoredSession : BiometricSessionResult
    data object BiometricDisabled : BiometricSessionResult
    data class KeyPermanentlyInvalidated(val message: String) : BiometricSessionResult
    data object Cancelled : BiometricSessionResult
    data class Lockout(val isPermanent: Boolean, val message: String) : BiometricSessionResult
    data class Failure(val reason: String) : BiometricSessionResult
}

/**
 * Manages the lifecycle of encrypted auth credentials protected by hardware-backed biometrics.
 */
@Inject
class BiometricTokenManager(
    private val authenticator: BiometricAuthenticator,
    private val secureStorage: SecureStorage
) {

    fun isBiometricAvailable(): Boolean = authenticator.canAuthenticate().isAvailable

    fun isBiometricEnabled(): Boolean = authenticator.isBiometricOptedIn()

    fun setBiometricEnabled(enabled: Boolean) {
        authenticator.setBiometricOptIn(enabled)
        if (!enabled) {
            clearBiometricSession()
        }
    }

    fun hasStoredSession(): Boolean {
        val encryptedTokenHex = secureStorage.getString(KEY_BIOMETRIC_TOKEN)
        val ivHex = secureStorage.getString(KEY_BIOMETRIC_IV)
        return !encryptedTokenHex.isNullOrBlank() && !ivHex.isNullOrBlank()
    }

    suspend fun storeSession(
        token: String,
        driverId: String,
        promptInfo: BiometricPromptInfo
    ): BiometricCryptoResult {
        val plainText = "$token:$driverId".encodeToByteArray()
        val operation = BiometricCryptoOperation.Encrypt(plainText)

        val result = authenticator.authenticateWithCrypto(promptInfo, operation)
        if (result is BiometricCryptoResult.Success) {
            val encryptedHex = bytesToHex(result.outputData)
            val ivHex = result.initializationVector?.let { bytesToHex(it) }

            if (ivHex != null) {
                secureStorage.putString(KEY_BIOMETRIC_TOKEN, encryptedHex)
                secureStorage.putString(KEY_BIOMETRIC_IV, ivHex)
                secureStorage.putString(KEY_BIOMETRIC_DRIVER_ID, driverId)
                authenticator.setBiometricOptIn(true)
                Logger.i(TAG, "🔒 Successfully stored biometric-encrypted session for driver $driverId")
            } else {
                Logger.e(TAG, "Initialization vector was missing during biometric encryption")
                return BiometricCryptoResult.Failure("Missing initialization vector")
            }
        }
        return result
    }

    suspend fun retrieveSession(promptInfo: BiometricPromptInfo): BiometricSessionResult {
        if (!isBiometricEnabled()) {
            return BiometricSessionResult.BiometricDisabled
        }

        val encryptedTokenHex = secureStorage.getString(KEY_BIOMETRIC_TOKEN)
        val ivHex = secureStorage.getString(KEY_BIOMETRIC_IV)
        val storedDriverId = secureStorage.getString(KEY_BIOMETRIC_DRIVER_ID) ?: ""

        if (encryptedTokenHex.isNullOrBlank() || ivHex.isNullOrBlank()) {
            return BiometricSessionResult.NoStoredSession
        }

        val cipherText = hexToBytes(encryptedTokenHex)
        val initializationVector = hexToBytes(ivHex)
        val operation = BiometricCryptoOperation.Decrypt(cipherText, initializationVector)

        return when (val result = authenticator.authenticateWithCrypto(promptInfo, operation)) {
            is BiometricCryptoResult.Success -> {
                try {
                    val decryptedString = result.outputData.decodeToString()
                    val parts = decryptedString.split(":")
                    if (parts.size >= 2) {
                        val token = parts[0]
                        val driverId = parts.drop(1).joinToString(":")
                        BiometricSessionResult.Success(token = token, driverId = driverId)
                    } else {
                        BiometricSessionResult.Success(token = decryptedString, driverId = storedDriverId)
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Failed to decode decrypted token: ${e.message}", e)
                    BiometricSessionResult.Failure("Corrupted token payload")
                }
            }
            is BiometricCryptoResult.KeyPermanentlyInvalidated -> {
                Logger.w(TAG, "Biometric key was invalidated; clearing stored biometric session")
                clearBiometricSession()
                BiometricSessionResult.KeyPermanentlyInvalidated(result.message)
            }
            is BiometricCryptoResult.Cancelled -> BiometricSessionResult.Cancelled
            is BiometricCryptoResult.Lockout -> BiometricSessionResult.Lockout(result.isPermanent, result.message)
            is BiometricCryptoResult.Failure -> BiometricSessionResult.Failure(result.reason)
            is BiometricCryptoResult.Error -> BiometricSessionResult.Failure(result.errorMessage)
        }
    }

    fun clearBiometricSession() {
        secureStorage.remove(KEY_BIOMETRIC_TOKEN)
        secureStorage.remove(KEY_BIOMETRIC_IV)
        secureStorage.remove(KEY_BIOMETRIC_DRIVER_ID)
        authenticator.deleteBiometricKey()
        Logger.i(TAG, "🧹 Cleared biometric session and keystore key")
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789ABCDEF"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            result.append(hexChars[i shr 4])
            result.append(hexChars[i and 0x0F])
        }
        return result.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            val high = hexDigitToInt(hex[i])
            val low = hexDigitToInt(hex[i + 1])
            data[i / 2] = ((high shl 4) or low).toByte()
            i += 2
        }
        return data
    }

    private fun hexDigitToInt(ch: Char): Int = when (ch) {
        in '0'..'9' -> ch - '0'
        in 'a'..'f' -> ch - 'a' + 10
        in 'A'..'F' -> ch - 'A' + 10
        else -> 0
    }

    private companion object {
        const val TAG = "BiometricTokenManager"
        const val KEY_BIOMETRIC_TOKEN = "trackflow_biometric_token"
        const val KEY_BIOMETRIC_IV = "trackflow_biometric_iv"
        const val KEY_BIOMETRIC_DRIVER_ID = "trackflow_biometric_driver_id"
    }
}
