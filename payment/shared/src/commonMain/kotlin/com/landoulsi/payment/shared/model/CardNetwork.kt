package com.landoulsi.payment.shared.model

/**
 * Supported payment card networks.
 *
 * @property networkName Standard identifier conforming to Google Pay / payment gateway conventions.
 */
enum class CardNetwork(val networkName: String) {
    VISA("VISA"),
    MASTERCARD("MASTERCARD"),
    AMEX("AMEX"),
    DISCOVER("DISCOVER"),
    JCB("JCB"),
    INTERAC("INTERAC"),
    DINERS_CLUB("DINERS_CLUB"),
    UNION_PAY("UNIONPAY");

    companion object {
        fun fromName(name: String): CardNetwork? {
            val normalized = name.uppercase().replace("-", "_").replace(" ", "_")
            return entries.find { it.networkName == normalized || it.name == normalized }
        }
    }
}
