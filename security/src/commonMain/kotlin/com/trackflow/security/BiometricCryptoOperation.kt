package com.trackflow.security

/**
 * Cryptographic operation to execute inside hardware-backed TEE / StrongBox once authenticated.
 */
sealed interface BiometricCryptoOperation {
    data class Encrypt(val plainText: ByteArray) : BiometricCryptoOperation {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Encrypt) return false
            return plainText.contentEquals(other.plainText)
        }

        override fun hashCode(): Int = plainText.contentHashCode()
    }

    data class Decrypt(
        val cipherText: ByteArray,
        val initializationVector: ByteArray
    ) : BiometricCryptoOperation {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Decrypt) return false
            if (!cipherText.contentEquals(other.cipherText)) return false
            return initializationVector.contentEquals(other.initializationVector)
        }

        override fun hashCode(): Int {
            var result = cipherText.contentHashCode()
            result = 31 * result + initializationVector.contentHashCode()
            return result
        }
    }
}
