package com.landoulsi.biometric

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.landoulsi.logger.Logger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import javax.crypto.Cipher
import kotlin.coroutines.resume

/**
 * Android implementation of [BiometricAuthenticator] utilizing AndroidX BiometricPrompt
 * and hardware-backed Android KeyStore AES-256 GCM cryptographic operations.
 */
@Inject
class AndroidBiometricAuthenticator(
    private val context: Context,
    private val keyStoreManager: BiometricKeyStoreManager
) : BiometricAuthenticator {

    private val sharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }


    init {
        if (isRegistered.compareAndSet(false, true)) {
            val callbacks = object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    if (activity is FragmentActivity) {
                        hostActivity.set(WeakReference(activity))
                    }
                }

                override fun onActivityPaused(activity: Activity) = Unit

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

                override fun onActivityStarted(activity: Activity) = Unit

                override fun onActivityStopped(activity: Activity) = Unit

                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

                override fun onActivityDestroyed(activity: Activity) {
                    val currentHost = hostActivity.get()?.get()
                    if (activity === currentHost) {
                        hostActivity.set(null)
                    }
                }
            }
            (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(callbacks)
                ?: Logger.w(TAG, "Application context is not an Application. Lifecycle callbacks not registered.")
        }
    }

    private val biometricManager: BiometricManager by lazy {
        BiometricManager.from(context)
    }

    private val mainExecutor: Executor by lazy {
        ContextCompat.getMainExecutor(context)
    }

    override fun canAuthenticate(): BiometricStatus {
        val result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return when (result) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HARDWARE_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SECURITY_UPDATE_REQUIRED
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricStatus.UNSUPPORTED
            else -> BiometricStatus.UNSUPPORTED
        }
    }

    override fun isBiometricOptedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_OPT_IN, false)
    }

    override fun setBiometricOptIn(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_OPT_IN, enabled).apply()
        Logger.d(TAG, "Biometric opt-in set to: $enabled")
    }

    override fun hasBiometricKey(): Boolean {
        return keyStoreManager.hasKey()
    }

    override fun deleteBiometricKey() {
        keyStoreManager.deleteKey()
    }

    private fun currentActivity(): FragmentActivity? {
        return hostActivity.get()?.get()
    }

    override suspend fun authenticate(promptInfo: BiometricPromptInfo): BiometricAuthResult {
        val status = canAuthenticate()
        if (!status.isAvailable) {
            Logger.w(TAG, "Biometric authentication not available: $status")
            return BiometricAuthResult.Error(
                errorCode = BiometricErrorCode.SystemError(BiometricPrompt.ERROR_HW_UNAVAILABLE),
                errorMessage = "Biometrics unavailable: $status"
            )
        }

        val activity = currentActivity() ?: return BiometricAuthResult.Error(
            errorCode = BiometricErrorCode.NoActivity,
            errorMessage = "No FragmentActivity attached to present BiometricPrompt"
        )

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        Logger.i(TAG, "Biometric authentication succeeded")
                        continuation.resume(BiometricAuthResult.Success)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        Logger.w(TAG, "Biometric authentication error [$errorCode]: $errString")
                        val authResult = when (errorCode) {
                            BiometricPrompt.ERROR_LOCKOUT -> {
                                BiometricAuthResult.Lockout(
                                    isPermanent = false,
                                    message = errString.toString()
                                )
                            }
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                                BiometricAuthResult.Lockout(
                                    isPermanent = true,
                                    message = errString.toString()
                                )
                            }
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_CANCELED -> {
                                BiometricAuthResult.Cancelled
                            }
                            else -> {
                                BiometricAuthResult.Error(
                                    errorCode = BiometricErrorCode.SystemError(errorCode),
                                    errorMessage = errString.toString()
                                )
                            }
                        }
                        continuation.resume(authResult)
                    }

                    override fun onAuthenticationFailed() {
                        Logger.d(TAG, "Biometric recognition failed (fingerprint/face not recognized)")
                    }
                }

                val prompt = BiometricPrompt(activity, mainExecutor, callback)
                val androidPromptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(promptInfo.title)
                    .apply {
                        promptInfo.subtitle?.let { setSubtitle(it) }
                        promptInfo.description?.let { setDescription(it) }
                        setNegativeButtonText(promptInfo.negativeButtonText)
                        setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        setConfirmationRequired(promptInfo.confirmationRequired)
                    }
                    .build()

                continuation.invokeOnCancellation {
                    try {
                        prompt.cancelAuthentication()
                    } catch (e: Exception) {
                        Logger.w(TAG, "Error cancelling prompt: ${e.message}")
                    }
                }

                prompt.authenticate(androidPromptInfo)
            }
        }
    }

    override suspend fun authenticateWithCrypto(
        promptInfo: BiometricPromptInfo,
        operation: BiometricCryptoOperation
    ): BiometricCryptoResult {
        val status = canAuthenticate()
        if (!status.isAvailable) {
            Logger.w(TAG, "Cannot authenticate with crypto: biometrics unavailable ($status)")
            return BiometricCryptoResult.Error(
                errorCode = BiometricErrorCode.SystemError(BiometricPrompt.ERROR_HW_UNAVAILABLE),
                errorMessage = "Biometrics unavailable: $status"
            )
        }

        val cipher: Cipher = try {
            when (operation) {
                is BiometricCryptoOperation.Encrypt -> keyStoreManager.createEncryptCipher()
                is BiometricCryptoOperation.Decrypt -> keyStoreManager.createDecryptCipher(operation.initializationVector)
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            Logger.w(TAG, "KeyPermanentlyInvalidatedException encountered during cipher setup")
            setBiometricOptIn(false)
            return BiometricCryptoResult.KeyPermanentlyInvalidated(
                "Biometric credentials changed or invalidated on this device. Please log in with password."
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to initialize crypto cipher: ${e.message}", e)
            return BiometricCryptoResult.Failure("Failed to initialize cipher: ${e.message}")
        }

        val activity = currentActivity() ?: return BiometricCryptoResult.Error(
            errorCode = BiometricErrorCode.NoActivity,
            errorMessage = "No FragmentActivity attached to present BiometricPrompt"
        )

        val cryptoObject = BiometricPrompt.CryptoObject(cipher)

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        val authenticatedCipher = result.cryptoObject?.cipher
                        if (authenticatedCipher == null) {
                            continuation.resume(BiometricCryptoResult.Failure("Authenticated CryptoObject cipher was null"))
                            return
                        }

                        try {
                            val output = when (operation) {
                                is BiometricCryptoOperation.Encrypt -> authenticatedCipher.doFinal(operation.plainText)
                                is BiometricCryptoOperation.Decrypt -> authenticatedCipher.doFinal(operation.cipherText)
                            }
                            val iv = authenticatedCipher.iv
                            continuation.resume(
                                BiometricCryptoResult.Success(
                                    outputData = output,
                                    initializationVector = iv
                                )
                            )
                        } catch (e: KeyPermanentlyInvalidatedException) {
                            Logger.w(TAG, "Key invalidated during doFinal: ${e.message}")
                            deleteBiometricKey()
                            setBiometricOptIn(false)
                            continuation.resume(
                                BiometricCryptoResult.KeyPermanentlyInvalidated(
                                    "Biometrics invalidated during cryptographic operation."
                                )
                            )
                        } catch (e: Exception) {
                            Logger.e(TAG, "Cryptographic operation failed: ${e.message}", e)
                            continuation.resume(BiometricCryptoResult.Failure("Crypto operation failed: ${e.message}"))
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        Logger.w(TAG, "Crypto biometric auth error [$errorCode]: $errString")
                        val cryptoResult = when (errorCode) {
                            BiometricPrompt.ERROR_LOCKOUT -> {
                                BiometricCryptoResult.Lockout(
                                    isPermanent = false,
                                    message = errString.toString()
                                )
                            }
                            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                                BiometricCryptoResult.Lockout(
                                    isPermanent = true,
                                    message = errString.toString()
                                )
                            }
                            BiometricPrompt.ERROR_USER_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                            BiometricPrompt.ERROR_CANCELED -> {
                                BiometricCryptoResult.Cancelled
                            }
                            else -> {
                                BiometricCryptoResult.Error(
                                    errorCode = BiometricErrorCode.SystemError(errorCode),
                                    errorMessage = errString.toString()
                                )
                            }
                        }
                        continuation.resume(cryptoResult)
                    }

                    override fun onAuthenticationFailed() {
                        Logger.d(TAG, "Biometric match failed during crypto auth")
                    }
                }

                val prompt = BiometricPrompt(activity, mainExecutor, callback)
                val androidPromptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(promptInfo.title)
                    .apply {
                        promptInfo.subtitle?.let { setSubtitle(it) }
                        promptInfo.description?.let { setDescription(it) }
                        setNegativeButtonText(promptInfo.negativeButtonText)
                        setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        setConfirmationRequired(promptInfo.confirmationRequired)
                    }
                    .build()

                continuation.invokeOnCancellation {
                    try {
                        prompt.cancelAuthentication()
                    } catch (e: Exception) {
                        Logger.w(TAG, "Error cancelling crypto prompt: ${e.message}")
                    }
                }

                prompt.authenticate(androidPromptInfo, cryptoObject)
            }
        }
    }

    @androidx.annotation.VisibleForTesting
    fun resetForTesting() {
        isRegistered.set(false)
        hostActivity.set(null)
    }

    private companion object {
        val hostActivity = java.util.concurrent.atomic.AtomicReference<WeakReference<FragmentActivity>?>(null)
        val isRegistered = java.util.concurrent.atomic.AtomicBoolean(false)
        const val PREFERENCES_NAME = "landoulsi_biometric_preferences"
        const val KEY_BIOMETRIC_OPT_IN = "biometric_opt_in_enabled"
        const val TAG = "AndroidBiometricAuthenticator"
    }
}
