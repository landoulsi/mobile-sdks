package com.landoulsi.payment.shared.model

/**
 * Represents a billing or shipping postal address.
 */
data class Address(
    val name: String? = null,
    val address1: String? = null,
    val address2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val countryCode: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null
)
