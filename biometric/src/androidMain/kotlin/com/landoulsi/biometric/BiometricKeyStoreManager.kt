package com.landoulsi.biometric

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.landoulsi.logger.Logger
import dev.zacsweers.metro.Inject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages hardware-backed cryptographic keys inside the Android Keystore
 * tied strictly to BIOMETRIC_STRONG user authentication.
 */
@Inject
class BiometricKeyStoreManager {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    fun hasKey(alias: String = DEFAULT_KEY_ALIAS): Boolean {
        return try {
            keyStore.containsAlias(alias)
        } catch (e: Exception) {
            Logger.e(TAG, "Error checking keystore alias: ${e.message}", e)
            false
        }
    }

    fun deleteKey(alias: String = DEFAULT_KEY_ALIAS) {
        try {
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
                Logger.d(TAG, "Deleted key alias '$alias' from Android Keystore")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to delete key alias '$alias': ${e.message}", e)
        }
    }

    @Throws(KeyPermanentlyInvalidatedException::class)
    fun getOrCreateSecretKey(alias: String = DEFAULT_KEY_ALIAS): SecretKey {
        if (keyStore.containsAlias(alias)) {
            val key = keyStore.getKey(alias, null) as? SecretKey
            if (key != null) {
                return key
            }
        }
        return generateBiometricBoundKey(alias)
    }

    private fun getExistingSecretKey(alias: String = DEFAULT_KEY_ALIAS): SecretKey? {
        return try {
            if (keyStore.containsAlias(alias)) {
                keyStore.getKey(alias, null) as? SecretKey
            } else null
        } catch (e: KeyPermanentlyInvalidatedException) {
            deleteKey(alias)
            throw e
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting existing key: ${e.message}", e)
            null
        }
    }

    private fun generateBiometricBoundKey(alias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )

        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setUserAuthenticationRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0, // 0 = requires biometric authentication for every cryptographic operation
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setInvalidatedByBiometricEnrollment(true)
        }

        keyGenerator.init(builder.build())
        val key = keyGenerator.generateKey()
        Logger.d(TAG, "Generated new hardware-backed biometric key '$alias'")
        return key
    }

    @Throws(KeyPermanentlyInvalidatedException::class)
    fun createEncryptCipher(alias: String = DEFAULT_KEY_ALIAS): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getOrCreateSecretKey(alias)
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            return cipher
        } catch (e: KeyPermanentlyInvalidatedException) {
            Logger.w(TAG, "Key '$alias' was permanently invalidated (biometrics changed). Deleting key.")
            deleteKey(alias)
            throw e
        }
    }

    @Throws(KeyPermanentlyInvalidatedException::class)
    fun createDecryptCipher(
        initializationVector: ByteArray,
        alias: String = DEFAULT_KEY_ALIAS
    ): Cipher {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val secretKey = getExistingSecretKey(alias)
            ?: throw IllegalStateException("No biometric key exists for decryption.")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, initializationVector)
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            return cipher
        } catch (e: KeyPermanentlyInvalidatedException) {
            Logger.w(TAG, "Key '$alias' was permanently invalidated. Deleting key.")
            deleteKey(alias)
            throw e
        }
    }

    companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val DEFAULT_KEY_ALIAS = "landoulsi_biometric_auth_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val TAG = "BiometricKeyStoreManager"
    }
}
