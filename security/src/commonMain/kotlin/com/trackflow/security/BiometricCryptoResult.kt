package com.trackflow.security

/**
 * Result of a hardware-backed cryptographic biometric operation.
 */
sealed interface BiometricCryptoResult {
    data class Success(
        val outputData: ByteArray,
        val initializationVector: ByteArray? = null
    ) : BiometricCryptoResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Success) return false
            if (!outputData.contentEquals(other.outputData)) return false
            if (initializationVector != null) {
                if (other.initializationVector == null) return false
                if (!initializationVector.contentEquals(other.initializationVector)) return false
            } else if (other.initializationVector != null) return false
            return true
        }

        override fun hashCode(): Int {
            var result = outputData.contentHashCode()
            result = 31 * result + (initializationVector?.contentHashCode() ?: 0)
            return result
        }
    }

    data class KeyPermanentlyInvalidated(
        val message: String = "Biometric credentials changed or invalidated on this device."
    ) : BiometricCryptoResult

    data class Failure(val reason: String) : BiometricCryptoResult
    data object Cancelled : BiometricCryptoResult
    data class Lockout(val isPermanent: Boolean, val message: String) : BiometricCryptoResult
    data class Error(val errorCode: BiometricErrorCode, val errorMessage: String) : BiometricCryptoResult
}
