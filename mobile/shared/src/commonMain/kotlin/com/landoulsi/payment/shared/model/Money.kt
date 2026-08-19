package com.landoulsi.payment.shared.model

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Represents a monetary amount in minor units (e.g. cents, pence, fils) to avoid floating point inaccuracies.
 *
 * @property amountMinorUnits The monetary amount in minor units (e.g., 1000 for $10.00 USD, 1000 for ¥1000 JPY).
 * @property currency The currency associated with this amount.
 */
data class Money(
    val amountMinorUnits: Long,
    val currency: Currency = Currency.USD
) : Comparable<Money> {

    /**
     * Formats the amount as a standard decimal string without currency symbol (e.g., "10.00" or "10").
     * Ideal for payment gateway payloads and Google Pay `TransactionInfo.totalPrice`.
     */
    fun formattedAmount(): String {
        if (currency.decimalPlaces == 0) {
            return amountMinorUnits.toString()
        }
        val isNegative = amountMinorUnits < 0
        val absAmount = abs(amountMinorUnits)
        val divisor = 10.0.pow(currency.decimalPlaces.toDouble()).toLong()
        val major = absAmount / divisor
        val minor = absAmount % divisor
        val minorStr = minor.toString().padStart(currency.decimalPlaces, '0')
        val sign = if (isNegative) "-" else ""
        return "$sign$major.$minorStr"
    }

    /**
     * Formats the amount with its currency symbol (e.g., "$10.00", "€10.00", "¥1000").
     */
    fun formattedWithSymbol(): String {
        val formatted = formattedAmount()
        return if (formatted.startsWith("-")) {
            "-${currency.symbol}${formatted.removePrefix("-")}"
        } else {
            "${currency.symbol}$formatted"
        }
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) {
            "Cannot add money with different currencies: ${currency.code} vs ${other.currency.code}"
        }
        return Money(amountMinorUnits + other.amountMinorUnits, currency)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) {
            "Cannot subtract money with different currencies: ${currency.code} vs ${other.currency.code}"
        }
        return Money(amountMinorUnits - other.amountMinorUnits, currency)
    }

    override fun compareTo(other: Money): Int {
        require(currency == other.currency) {
            "Cannot compare money with different currencies: ${currency.code} vs ${other.currency.code}"
        }
        return amountMinorUnits.compareTo(other.amountMinorUnits)
    }

    companion object {
        val ZERO = Money(0, Currency.USD)

        /**
         * Creates a [Money] instance from a major unit amount (e.g., 10.50 -> 1050 cents).
         */
        fun fromMajorUnits(amount: Double, currency: Currency = Currency.USD): Money {
            val factor = 10.0.pow(currency.decimalPlaces.toDouble())
            val minorUnits = (amount * factor).roundToLong()
            return Money(minorUnits, currency)
        }

        /**
         * Creates a [Money] instance from a major unit amount integer (e.g., 10 -> 1000 cents for USD).
         */
        fun fromMajorUnits(amount: Long, currency: Currency = Currency.USD): Money {
            val factor = 10.0.pow(currency.decimalPlaces.toDouble()).toLong()
            return Money(amount * factor, currency)
        }

        /**
         * Creates a [Money] instance directly from minor units (cents).
         */
        fun ofCents(cents: Long, currency: Currency = Currency.USD): Money {
            return Money(cents, currency)
        }
    }
}
