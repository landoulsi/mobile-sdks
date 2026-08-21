package com.landoulsi.payment.shared.validation

import com.landoulsi.payment.shared.model.CardNetwork

object CardValidation {
    /**
     * Validates a card number using the Luhn algorithm (mod-10 checksum).
     *
     * Accepts raw digits or formatted card number strings containing standard
     * separator characters (spaces, hyphens), which are stripped prior to checksum calculation.
     *
     * @param cardNumber Raw or formatted card number string.
     * @return true if the extracted digits satisfy the Luhn mod-10 checksum and length >= 2.
     */
    fun luhnCheck(cardNumber: String): Boolean {
        val digitsOnly = cardNumber.filter { it.isDigit() }
        if (digitsOnly.length < 2) return false

        var sum = 0
        var alternate = false
        for (i in digitsOnly.length - 1 downTo 0) {
            var digit = digitsOnly[i].digitToInt()
            if (alternate) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    /**
     * Detects the card network based on the card number prefix (BIN/IIN).
     *
     * Prefixes are evaluated in descending order of specificity (4-digit, 3-digit,
     * 2-digit, and finally 1-digit) so that general prefixes (like Visa's "4")
     * do not shadow more specific network definitions.
     *
     * @param cardNumber Card number string (raw or formatted).
     * @return The detected [CardNetwork] or null if unrecognized or empty.
     */
    fun detectNetwork(cardNumber: String): CardNetwork? {
        val digitsOnly = cardNumber.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return null

        return when {
            // ── 4-digit specific ranges ─────────────────────────────
            digitsOnly.length >= 4 && digitsOnly.substring(0, 4).toIntOrNull() in 3528..3589 -> CardNetwork.JCB
            digitsOnly.length >= 4 && digitsOnly.substring(0, 4) == "3095" -> CardNetwork.DINERS_CLUB
            digitsOnly.length >= 4 && digitsOnly.substring(0, 4).toIntOrNull() in 2221..2720 -> CardNetwork.MASTERCARD
            digitsOnly.startsWith("6011") -> CardNetwork.DISCOVER

            // ── 3-digit specific ranges ─────────────────────────────
            digitsOnly.length >= 3 && digitsOnly.substring(0, 3).toIntOrNull() in 300..305 -> CardNetwork.DINERS_CLUB
            digitsOnly.length >= 3 && digitsOnly.substring(0, 3).toIntOrNull() in 352..358 -> CardNetwork.JCB
            digitsOnly.length >= 3 && digitsOnly.substring(0, 3).toIntOrNull() in 644..649 -> CardNetwork.DISCOVER
            digitsOnly.length == 3 && digitsOnly.substring(0, 3).toIntOrNull() in 222..272 -> CardNetwork.MASTERCARD

            // ── 2-digit specific ranges ─────────────────────────────
            digitsOnly.startsWith("34") || digitsOnly.startsWith("37") -> CardNetwork.AMEX
            digitsOnly.startsWith("35") -> CardNetwork.JCB
            digitsOnly.startsWith("36") || digitsOnly.startsWith("38") || digitsOnly.startsWith("39") -> CardNetwork.DINERS_CLUB
            digitsOnly.startsWith("51") || digitsOnly.startsWith("52") ||
            digitsOnly.startsWith("53") || digitsOnly.startsWith("54") ||
            digitsOnly.startsWith("55") -> CardNetwork.MASTERCARD
            digitsOnly.length == 2 && digitsOnly.substring(0, 2).toIntOrNull() in 22..27 -> CardNetwork.MASTERCARD
            digitsOnly.startsWith("65") -> CardNetwork.DISCOVER
            digitsOnly.startsWith("62") -> CardNetwork.UNION_PAY
            digitsOnly.startsWith("60") -> CardNetwork.INTERAC

            // ── 1-digit general range (checked last to prevent shadowing) ──
            digitsOnly.startsWith("4") -> CardNetwork.VISA

            else -> null
        }
    }

    /**
     * Returns the maximum length for a card number based on the network.
     */
    fun maxCardLength(network: CardNetwork?): Int {
        return when (network) {
            CardNetwork.AMEX -> 15
            CardNetwork.DINERS_CLUB -> 14
            CardNetwork.UNION_PAY -> 19
            CardNetwork.DISCOVER -> 16
            else -> 16
        }
    }

    /**
     * Returns the CVC length for a card network.
     */
    fun cvcLength(network: CardNetwork?): Int {
        return when (network) {
            CardNetwork.AMEX -> 4
            else -> 3
        }
    }

    /**
     * Formats a card number with spaces for display.
     * Groups of 4 digits, except Amex which uses 4-6-5 grouping.
     */
    fun formatCardNumber(cardNumber: String, network: CardNetwork? = null): String {
        val digitsOnly = cardNumber.filter { it.isDigit() }
        val detectedNetwork = network ?: detectNetwork(digitsOnly)
        val maxLength = maxCardLength(detectedNetwork)
        val limitedDigits = if (digitsOnly.length > maxLength) digitsOnly.substring(0, maxLength) else digitsOnly

        return when (detectedNetwork) {
            CardNetwork.AMEX -> formatAmex(limitedDigits)
            else -> formatStandard(limitedDigits)
        }
    }

    private fun formatStandard(digits: String): String {
        val builder = StringBuilder()
        for (i in digits.indices) {
            if (i > 0 && i % 4 == 0) builder.append(' ')
            builder.append(digits[i])
        }
        return builder.toString()
    }

    private fun formatAmex(digits: String): String {
        val builder = StringBuilder()
        for (i in digits.indices) {
            when (i) {
                4, 10 -> builder.append(' ')
            }
            builder.append(digits[i])
        }
        return builder.toString()
    }

    /**
     * Formats an expiry date as MM/YY.
     */
    fun formatExpiry(expiry: String): String {
        val digitsOnly = expiry.filter { it.isDigit() }
        val builder = StringBuilder()
        for (i in digitsOnly.indices) {
            if (i == 2) builder.append('/')
            if (i >= 4) break
            builder.append(digitsOnly[i])
        }
        return builder.toString()
    }

    /**
     * Parses an expiry string (MM/YY) into month and year components.
     * Returns null if the format is invalid.
     */
    fun parseExpiry(expiry: String): Pair<Int, Int>? {
        val formatted = formatExpiry(expiry)
        val parts = formatted.split('/')
        if (parts.size != 2) return null
        val month = parts[0].toIntOrNull()
        val year = parts[1].toIntOrNull()
        if (month == null || year == null) return null
        return month to year
    }

    /**
     * Validates an expiry date (MM/YY).
     * Returns true if the month is 01-12 and the date is not in the past.
     */
    fun isExpiryValid(expiry: String): Boolean {
        val parsed = parseExpiry(expiry) ?: return false
        val (month, year) = parsed
        if (month !in 1..12) return false

        val currentYear = currentYearTwoDigit()
        val currentMonth = currentMonth()

        if (year < currentYear) return false
        if (year == currentYear && month < currentMonth) return false

        return true
    }

    /**
     * Checks if an expiry string is complete (MM/YY format with 4 digits).
     */
    fun isExpiryComplete(expiry: String): Boolean {
        val formatted = formatExpiry(expiry)
        return formatted.length == 5 && formatted[2] == '/'
    }

    /**
     * Formats a CVC (digits only, max length based on network).
     */
    fun formatCvc(cvc: String, network: CardNetwork? = null): String {
        val digitsOnly = cvc.filter { it.isDigit() }
        val maxLen = cvcLength(network)
        return if (digitsOnly.length > maxLen) digitsOnly.substring(0, maxLen) else digitsOnly
    }

    /**
     * Checks if a CVC is valid for the given network (contains only digits and matches expected length).
     */
    fun isCvcValid(cvc: String, network: CardNetwork? = null): Boolean {
        val digitsOnly = cvc.filter { it.isDigit() }
        return digitsOnly.length == cvcLength(network) && cvc.length == digitsOnly.length
    }

    /**
     * Checks if a CVC is complete (reached expected length for network).
     */
    fun isCvcComplete(cvc: String, network: CardNetwork? = null): Boolean {
        val formatted = formatCvc(cvc, network)
        return formatted.length == cvcLength(network)
    }

    /**
     * Checks if a card number is complete (reached max length for detected network).
     */
    fun isCardNumberComplete(cardNumber: String): Boolean {
        val digitsOnly = cardNumber.filter { it.isDigit() }
        val network = detectNetwork(digitsOnly)
        return when (network) {
            CardNetwork.UNION_PAY -> digitsOnly.length in 16..19
            else -> digitsOnly.length == maxCardLength(network)
        }
    }

    /**
     * Validates a full card number (complete length + Luhn check).
     */
    fun isCardNumberValid(cardNumber: String): Boolean {
        val digitsOnly = cardNumber.filter { it.isDigit() }
        if (!isCardNumberComplete(cardNumber)) return false
        return luhnCheck(digitsOnly)
    }

    /**
     * Masks a card number according to PCI-DSS rules.
     * Preserves [preserveLeading] digits at the start and [preserveTrailing] digits at the end.
     * By default, preserves the last 4 digits (or fewer if card is shorter) and masks the rest.
     * Formats according to the detected network grouping if [format] is true.
     *
     * @param cardNumber Raw or formatted card number.
     * @param maskChar Character to use for masked digits (default '•').
     * @param preserveLeading Number of leading unmasked digits (e.g. 6 for BIN preservation).
     * @param preserveTrailing Number of trailing unmasked digits (e.g. 4 for last 4).
     * @param format Whether to apply network-specific space formatting to the output.
     */
    fun maskCardNumber(
        cardNumber: String,
        maskChar: Char = '•',
        preserveLeading: Int = 0,
        preserveTrailing: Int = 4,
        format: Boolean = true
    ): String {
        val digitsOnly = cardNumber.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return ""

        val len = digitsOnly.length
        val leadCount = preserveLeading.coerceIn(0, len)
        val trailCount = preserveTrailing.coerceIn(0, len - leadCount)
        val maskCount = (len - leadCount - trailCount).coerceAtLeast(0)

        val maskedDigits = StringBuilder().apply {
            if (leadCount > 0) {
                append(digitsOnly.substring(0, leadCount))
            }
            repeat(maskCount) {
                append(maskChar)
            }
            if (trailCount > 0) {
                append(digitsOnly.substring(len - trailCount))
            }
        }.toString()

        if (!format) return maskedDigits

        val network = detectNetwork(digitsOnly)
        return when (network) {
            CardNetwork.AMEX -> {
                val builder = StringBuilder()
                for (i in maskedDigits.indices) {
                    when (i) {
                        4, 10 -> builder.append(' ')
                    }
                    builder.append(maskedDigits[i])
                }
                builder.toString()
            }
            else -> {
                val builder = StringBuilder()
                for (i in maskedDigits.indices) {
                    if (i > 0 && i % 4 == 0) builder.append(' ')
                    builder.append(maskedDigits[i])
                }
                builder.toString()
            }
        }
    }

    /**
     * Redacts a Card Verification Code (CVC/CVV).
     *
     * @param cvc Raw CVC string.
     * @param maskChar Mask character (e.g. '•'). If null, returns "[REDACTED]".
     */
    fun redactCvc(cvc: String, maskChar: Char? = '•'): String {
        val digitsOnly = cvc.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return ""
        return if (maskChar != null) {
            maskChar.toString().repeat(digitsOnly.length)
        } else {
            "[REDACTED]"
        }
    }
}

// Top-level aliases for backwards compatibility
fun luhnCheck(cardNumber: String): Boolean = CardValidation.luhnCheck(cardNumber)
fun detectNetwork(cardNumber: String): CardNetwork? = CardValidation.detectNetwork(cardNumber)
fun maxCardLength(network: CardNetwork?): Int = CardValidation.maxCardLength(network)
fun cvcLength(network: CardNetwork?): Int = CardValidation.cvcLength(network)
fun formatCardNumber(cardNumber: String, network: CardNetwork? = null): String = CardValidation.formatCardNumber(cardNumber, network)
fun formatExpiry(expiry: String): String = CardValidation.formatExpiry(expiry)
fun parseExpiry(expiry: String): Pair<Int, Int>? = CardValidation.parseExpiry(expiry)
fun isExpiryValid(expiry: String): Boolean = CardValidation.isExpiryValid(expiry)
fun isExpiryComplete(expiry: String): Boolean = CardValidation.isExpiryComplete(expiry)
fun formatCvc(cvc: String, network: CardNetwork? = null): String = CardValidation.formatCvc(cvc, network)
fun isCvcValid(cvc: String, network: CardNetwork? = null): Boolean = CardValidation.isCvcValid(cvc, network)
fun isCvcComplete(cvc: String, network: CardNetwork? = null): Boolean = CardValidation.isCvcComplete(cvc, network)
fun isCardNumberComplete(cardNumber: String): Boolean = CardValidation.isCardNumberComplete(cardNumber)
fun isCardNumberValid(cardNumber: String): Boolean = CardValidation.isCardNumberValid(cardNumber)
fun maskCardNumber(
    cardNumber: String,
    maskChar: Char = '•',
    preserveLeading: Int = 0,
    preserveTrailing: Int = 4,
    format: Boolean = true
): String = CardValidation.maskCardNumber(cardNumber, maskChar, preserveLeading, preserveTrailing, format)
fun redactCvc(cvc: String, maskChar: Char? = '•'): String = CardValidation.redactCvc(cvc, maskChar)

/**
 * Platform-specific date/time provider for getting current year/month.
 * Used for expiry date validation.
 */
expect fun currentYearTwoDigit(): Int
expect fun currentMonth(): Int