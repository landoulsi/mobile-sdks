package com.landoulsi.payment.shared.model

/**
 * Represents an ISO 4217 currency.
 *
 * @property code The 3-letter ISO 4217 currency code (e.g., "USD", "EUR").
 * @property symbol The display symbol for the currency (e.g., "$", "€").
 * @property decimalPlaces The number of decimal places / fraction digits (e.g., 2 for USD, 0 for JPY).
 */
data class Currency(
    val code: String,
    val symbol: String = code,
    val decimalPlaces: Int = 2
) {
    companion object {
        val USD = Currency("USD", "$", 2)
        val EUR = Currency("EUR", "€", 2)
        val GBP = Currency("GBP", "£", 2)
        val CAD = Currency("CAD", "CA$", 2)
        val AUD = Currency("AUD", "A$", 2)
        val JPY = Currency("JPY", "¥", 0)
        val CHF = Currency("CHF", "CHF", 2)
        val AED = Currency("AED", "AED", 2)
        val SAR = Currency("SAR", "SAR", 2)

        private val all = listOf(USD, EUR, GBP, CAD, AUD, JPY, CHF, AED, SAR)

        fun fromCode(code: String): Currency {
            val upperCode = code.uppercase()
            return all.find { it.code == upperCode } ?: Currency(upperCode)
        }
    }
}
