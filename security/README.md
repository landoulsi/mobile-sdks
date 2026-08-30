# Landoulsi Security Module (`:security`)

A robust, Kotlin Multiplatform (KMP) security module providing cryptographic and biometric authentication abstractions across Android and iOS platforms.

---

## 1. Overview & Architecture

The `:security` module abstracts platform-specific biometric APIs behind the unified, platform-agnostic `BiometricAuthenticator` and `BiometricTokenManager` interfaces in `commonMain`.

### Architecture Diagram

```
+-------------------------------------------------------------+
|                      :driver-android                        |
|   (LoginScreen, SettingsScreen, DriverApplication)          |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                          :shared                            |
|             (Metro DI Scope: LandoulsiScope)                |
+-------------------------------------------------------------+
                              |
                              v
+-------------------------------------------------------------+
|                         :security                           |
|                                                             |
|  [commonMain]                                               |
|  - BiometricAuthenticator (Interface)                       |
|  - BiometricTokenManager (Session lifecycle & crypto token) |
|  - BiometricStatus, BiometricAuthResult, BiometricCrypto... |
|  - testing/ (FakeBiometricAuthenticator, FakeSecureStorage) |
|                                                             |
|  [androidMain]                                              |
|  - AndroidBiometricAuthenticator (AndroidX BiometricPrompt) |
|  - BiometricKeyStoreManager (Android Keystore AES-256 GCM)  |
|                                                             |
|  [iosMain]                                                  |
|  - IosBiometricAuthenticator (stub returning UNSUPPORTED)   |
+-------------------------------------------------------------+
```

---

## 2. Security Best Practices & Design Principles

### 1. Hardware-Backed Cryptography & Keystore
- Android implementation utilizes the **Android Keystore provider** (`AndroidKeyStore`) with 256-bit AES keys (`AES/GCM/NoPadding`).
- Keystore keys are generated with `setUserAuthenticationRequired(true)` and `AUTH_BIOMETRIC_STRONG`.
- Sensitive operations are bound to an authenticated `CryptoObject` containing the initialized `Cipher`.

### 2. Privacy & Zero Biometric Exposure
- Biometric sensor data (fingerprints, facial geometries) **never leaves the device secure enclave / Trusted Execution Environment (TEE)**.
- The application never touches or processes biometric templates; it only interacts with the Keystore `Cipher` unlocked via Android's `BiometricPrompt` pass/fail signal.

### 3. Key Invalidation Handling (`KeyPermanentlyInvalidatedException`)
- Keys are created with `setInvalidatedByBiometricEnrollment(true)`.
- When a user changes their device lock screen (PIN/pattern/password) or enrolls/removes biometric credentials:
  - The Keystore automatically permanently invalidates the biometric key.
  - `BiometricKeyStoreManager` catches `KeyPermanentlyInvalidatedException` and returns `BiometricCryptoResult.KeyPermanentlyInvalidated`.
  - `BiometricTokenManager` automatically deletes the invalidated key and clears local encrypted token storage, guiding the driver to re-authenticate with their password.

### 4. Lockout & Fallback Handling
- **Temporary Lockout (`ERROR_LOCKOUT`)**: 5 unsuccessful biometric attempts triggers a 30-second sensor lockout. Handled gracefully with warning messages and instant fallback to password input.
- **Permanent Lockout (`ERROR_LOCKOUT_PERMANENT`)**: Continued failures lock the sensor until device PIN/pattern is entered. The app directs the user to sign in with password credentials.
- **Fallback**: The login screen always provides password input as an active alternative.

---

## 3. User Experience Guidelines

1. **Post-Login Opt-In**:
   - Biometric enrollment is prompted **after** initial successful login with password credentials, not during initial sign-up or setup, increasing opt-in conversion rates.
2. **User Control**:
   - Biometric login is completely optional. Drivers can enable or disable biometric login at any time in **Settings > Security & Access**.
3. **Concise User Guidance**:
   - Prompts avoid verbose instructions and present clear titles ("Driver Sign In") and negative button actions ("Password").

---

## 4. API Reference & Usage

### Checking Biometric Availability
```kotlin
val status: BiometricStatus = biometricAuthenticator.canAuthenticate()
when (status) {
    BiometricStatus.AVAILABLE -> { /* Show biometric prompt */ }
    BiometricStatus.NOT_ENROLLED -> { /* Biometric hardware exists but driver has not enrolled */ }
    BiometricStatus.NO_HARDWARE -> { /* Device lacks biometric sensors */ }
    BiometricStatus.HARDWARE_UNAVAILABLE -> { /* Sensor currently busy */ }
    BiometricStatus.SECURITY_UPDATE_REQUIRED -> { /* System security update required */ }
    BiometricStatus.UNSUPPORTED -> { /* Not supported on this OS */ }
}
```

### Storing an Encrypted Session (`BiometricTokenManager`)
```kotlin
val promptInfo = BiometricPromptInfo(
    title = "Enable Biometric Login",
    subtitle = "Confirm your fingerprint or face",
    description = "Authenticate to enable fast unlock",
    negativeButtonText = "Cancel"
)

val result = biometricTokenManager.storeSession(
    token = session.accessToken,
    driverId = session.driverId,
    promptInfo = promptInfo
)
```

### Retrieving and Unlocking Session
```kotlin
val result = biometricTokenManager.retrieveSession(promptInfo)
when (result) {
    is BiometricSessionResult.Success -> {
        // Unlock session with result.token and result.driverId
    }
    is BiometricSessionResult.KeyPermanentlyInvalidated -> {
        // Prompt password re-authentication
    }
    is BiometricSessionResult.Lockout -> {
        // Inform user of lockout and offer password sign-in
    }
    is BiometricSessionResult.Failure -> {
        // Handle failure reason
    }
    is BiometricSessionResult.Cancelled -> {
        // User cancelled prompt
    }
    is BiometricSessionResult.NoStoredSession,
    is BiometricSessionResult.BiometricDisabled -> {
        // Show standard login form
    }
}
```

---

## 5. Testing & Verification

Unit tests and fake implementations are provided under `com.landoulsi.security.testing.*`:
- `FakeBiometricAuthenticator`: Configurable fake for biometric authentication states and crypto results.
- `FakeSecureStorage`: In-memory storage for token lifecycle tests.

Run tests using Gradle:
```bash
./gradlew :security:testDebugUnitTest
./gradlew :driver-android:testDebugUnitTest
```
