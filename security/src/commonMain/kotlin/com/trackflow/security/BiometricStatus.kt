package com.trackflow.security

/**
 * Indicates the current availability and readiness of biometric hardware on the device.
 */
enum class BiometricStatus {
    /** Hardware is available, functional, and the user has enrolled strong biometric credentials. */
    AVAILABLE,

    /** Biometric hardware is present, but no biometric credentials (fingerprints/face) are enrolled. */
    NOT_ENROLLED,

    /** Device hardware does not support biometric authentication. */
    NO_HARDWARE,

    /** Biometric hardware is temporarily busy or unavailable. */
    HARDWARE_UNAVAILABLE,

    /** A security update is required for the biometric sensor. */
    SECURITY_UPDATE_REQUIRED,

    /** Biometric authentication is unsupported on this platform or configuration. */
    UNSUPPORTED;

    val isAvailable: Boolean
        get() = this == AVAILABLE
}
