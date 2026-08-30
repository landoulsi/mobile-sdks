package com.landoulsi.security

/**
 * Configuration and user-facing copy for the biometric authentication prompt.
 */
data class BiometricPromptInfo(
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val negativeButtonText: String = "Use Password",
    val confirmationRequired: Boolean = false
)
