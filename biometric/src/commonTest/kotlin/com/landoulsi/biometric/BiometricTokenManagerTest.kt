package com.landoulsi.biometric

import com.landoulsi.biometric.testing.FakeBiometricAuthenticator
import com.landoulsi.biometric.testing.FakeSecureStorage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BiometricTokenManagerTest {

    private val fakeAuthenticator = FakeBiometricAuthenticator()
    private val fakeSecureStorage = FakeSecureStorage()
    private val tokenManager = BiometricTokenManager(fakeAuthenticator, fakeSecureStorage)

    private val defaultPromptInfo = BiometricPromptInfo(
        title = "Authenticate Driver",
        subtitle = "Confirm your identity",
        negativeButtonText = "Use Password"
    )

    @Test
    fun storeSession_whenSuccessful_persistsEncryptedTokenAndEnablesBiometrics() = runTest {
        val result = tokenManager.storeSession(
            token = "jwt-secret-token-123",
            driverId = "driver-456",
            promptInfo = defaultPromptInfo
        )

        assertIs<BiometricCryptoResult.Success>(result)
        assertTrue(tokenManager.isBiometricEnabled())
        assertTrue(tokenManager.hasStoredSession())
        assertTrue(fakeAuthenticator.hasBiometricKey())
    }

    @Test
    fun retrieveSession_whenBiometricDisabled_returnsBiometricDisabled() = runTest {
        tokenManager.storeSession("token", "driver-1", defaultPromptInfo)
        tokenManager.setBiometricEnabled(false)

        val result = tokenManager.retrieveSession(defaultPromptInfo)
        assertEquals(BiometricSessionResult.BiometricDisabled, result)
    }

    @Test
    fun retrieveSession_whenNoSessionStored_returnsNoStoredSession() = runTest {
        tokenManager.setBiometricEnabled(true)
        val result = tokenManager.retrieveSession(defaultPromptInfo)
        assertEquals(BiometricSessionResult.NoStoredSession, result)
    }

    @Test
    fun retrieveSession_whenStored_decryptsAndReturnsTokenAndDriverId() = runTest {
        tokenManager.storeSession("jwt-access-token-abc", "driver-789", defaultPromptInfo)

        val result = tokenManager.retrieveSession(defaultPromptInfo)
        assertIs<BiometricSessionResult.Success>(result)
        assertEquals("jwt-access-token-abc", result.token)
        assertEquals("driver-789", result.driverId)
    }

    @Test
    fun retrieveSession_whenKeyPermanentlyInvalidated_clearsSessionAndReturnsInvalidated() = runTest {
        tokenManager.storeSession("jwt-token", "driver-1", defaultPromptInfo)

        fakeAuthenticator.cryptoResult = BiometricCryptoResult.KeyPermanentlyInvalidated(
            "Biometrics changed on device"
        )

        val result = tokenManager.retrieveSession(defaultPromptInfo)
        assertIs<BiometricSessionResult.KeyPermanentlyInvalidated>(result)
        assertFalse(tokenManager.hasStoredSession())
        assertFalse(fakeAuthenticator.hasBiometricKey())
    }

    @Test
    fun retrieveSession_whenCancelled_returnsCancelled() = runTest {
        tokenManager.storeSession("jwt-token", "driver-1", defaultPromptInfo)
        fakeAuthenticator.cryptoResult = BiometricCryptoResult.Cancelled

        val result = tokenManager.retrieveSession(defaultPromptInfo)
        assertEquals(BiometricSessionResult.Cancelled, result)
    }

    @Test
    fun retrieveSession_whenLockedOut_returnsLockout() = runTest {
        tokenManager.storeSession("jwt-token", "driver-1", defaultPromptInfo)
        fakeAuthenticator.cryptoResult = BiometricCryptoResult.Lockout(
            isPermanent = false,
            message = "Too many attempts"
        )

        val result = tokenManager.retrieveSession(defaultPromptInfo)
        assertIs<BiometricSessionResult.Lockout>(result)
        assertFalse(result.isPermanent)
        assertEquals("Too many attempts", result.message)
    }

    @Test
    fun clearBiometricSession_removesAllStoredDataAndDeletesKey() = runTest {
        tokenManager.storeSession("token-to-delete", "driver-999", defaultPromptInfo)
        assertTrue(tokenManager.hasStoredSession())

        tokenManager.clearBiometricSession()
        assertFalse(tokenManager.hasStoredSession())
        assertFalse(fakeAuthenticator.hasBiometricKey())
    }

    @Test
    fun biometricAvailability_correctlyReflectsAuthenticatorStatus() {
        fakeAuthenticator.status = BiometricStatus.AVAILABLE
        assertTrue(tokenManager.isBiometricAvailable())

        fakeAuthenticator.status = BiometricStatus.NOT_ENROLLED
        assertFalse(tokenManager.isBiometricAvailable())

        fakeAuthenticator.status = BiometricStatus.NO_HARDWARE
        assertFalse(tokenManager.isBiometricAvailable())
    }
}
