package com.landoulsi.payment.shared.validation

import com.landoulsi.payment.shared.model.CardNetwork
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CardValidationTest {

    // ─────────────────────────────────────────────────────────
    //  Luhn Algorithm Tests
    // ─────────────────────────────────────────────────────────

    @Test
    fun testLuhnValidVisa() {
        assertTrue(CardValidation.luhnCheck("4242424242424242"))
        assertTrue(CardValidation.luhnCheck("4000000000000002"))
        assertTrue(CardValidation.luhnCheck("4111 1111 1111 1111"))
    }

    @Test
    fun testLuhnValidMastercard() {
        assertTrue(CardValidation.luhnCheck("5555555555554444"))
        assertTrue(CardValidation.luhnCheck("2221000000000009"))
        assertTrue(CardValidation.luhnCheck("5105 1051 0510 5100"))
    }

    @Test
    fun testLuhnValidAmex() {
        assertTrue(CardValidation.luhnCheck("378282246310005"))
        assertTrue(CardValidation.luhnCheck("340000000000009"))
        assertTrue(CardValidation.luhnCheck("3782 822463 10005"))
    }

    @Test
    fun testLuhnValidDiscover() {
        assertTrue(CardValidation.luhnCheck("6011000000000004"))
        assertTrue(CardValidation.luhnCheck("6500000000000002"))
    }

    @Test
    fun testLuhnValidDinersClub() {
        assertTrue(CardValidation.luhnCheck("30000000000004"))
        assertTrue(CardValidation.luhnCheck("36000000000008"))
    }

    @Test
    fun testLuhnValidJcb() {
        assertTrue(CardValidation.luhnCheck("3528000000000007"))
        assertTrue(CardValidation.luhnCheck("3589000000000003"))
    }

    @Test
    fun testLuhnValidUnionPay() {
        assertTrue(CardValidation.luhnCheck("6200000000000005"))
    }

    @Test
    fun testLuhnInvalidCards() {
        // Altering checksum digit
        assertFalse(CardValidation.luhnCheck("4242424242424243"))
        assertFalse(CardValidation.luhnCheck("5555555555554445"))
        assertFalse(CardValidation.luhnCheck("378282246310006"))
        assertFalse(CardValidation.luhnCheck("6011111111111118"))

        // Too short / empty / non-digits
        assertFalse(CardValidation.luhnCheck(""))
        assertFalse(CardValidation.luhnCheck("4"))
        assertFalse(CardValidation.luhnCheck("abcdef"))
    }

    // ─────────────────────────────────────────────────────────
    //  Card Network Detection Tests
    // ─────────────────────────────────────────────────────────

    @Test
    fun testDetectNetworkVisa() {
        assertEquals(CardNetwork.VISA, CardValidation.detectNetwork("4"))
        assertEquals(CardNetwork.VISA, CardValidation.detectNetwork("4242"))
        assertEquals(CardNetwork.VISA, CardValidation.detectNetwork("4000 1234 5678 9010"))
    }

    @Test
    fun testDetectNetworkMastercard() {
        assertEquals(CardNetwork.MASTERCARD, CardValidation.detectNetwork("51"))
        assertEquals(CardNetwork.MASTERCARD, CardValidation.detectNetwork("55"))
        assertEquals(CardNetwork.MASTERCARD, CardValidation.detectNetwork("2221"))
        assertEquals(CardNetwork.MASTERCARD, CardValidation.detectNetwork("2720"))
        assertEquals(CardNetwork.MASTERCARD, CardValidation.detectNetwork("5555 5555 5555 4444"))
    }

    @Test
    fun testDetectNetworkAmex() {
        assertEquals(CardNetwork.AMEX, CardValidation.detectNetwork("34"))
        assertEquals(CardNetwork.AMEX, CardValidation.detectNetwork("37"))
        assertEquals(CardNetwork.AMEX, CardValidation.detectNetwork("378282246310005"))
    }

    @Test
    fun testDetectNetworkDiscover() {
        assertEquals(CardNetwork.DISCOVER, CardValidation.detectNetwork("6011"))
        assertEquals(CardNetwork.DISCOVER, CardValidation.detectNetwork("644"))
        assertEquals(CardNetwork.DISCOVER, CardValidation.detectNetwork("649"))
        assertEquals(CardNetwork.DISCOVER, CardValidation.detectNetwork("65"))
        assertEquals(CardNetwork.DISCOVER, CardValidation.detectNetwork("6011 1111 1111 1117"))
    }

    @Test
    fun testDetectNetworkDinersClub() {
        assertEquals(CardNetwork.DINERS_CLUB, CardValidation.detectNetwork("300"))
        assertEquals(CardNetwork.DINERS_CLUB, CardValidation.detectNetwork("305"))
        assertEquals(CardNetwork.DINERS_CLUB, CardValidation.detectNetwork("3095"))
        assertEquals(CardNetwork.DINERS_CLUB, CardValidation.detectNetwork("36"))
        assertEquals(CardNetwork.DINERS_CLUB, CardValidation.detectNetwork("38"))
        assertEquals(CardNetwork.DINERS_CLUB, CardValidation.detectNetwork("39"))
    }

    @Test
    fun testDetectNetworkJcb() {
        assertEquals(CardNetwork.JCB, CardValidation.detectNetwork("35"))
        assertEquals(CardNetwork.JCB, CardValidation.detectNetwork("3528"))
        assertEquals(CardNetwork.JCB, CardValidation.detectNetwork("3589"))
    }

    @Test
    fun testDetectNetworkUnionPay() {
        assertEquals(CardNetwork.UNION_PAY, CardValidation.detectNetwork("62"))
        assertEquals(CardNetwork.UNION_PAY, CardValidation.detectNetwork("6200000000000000"))
    }

    @Test
    fun testDetectNetworkInterac() {
        assertEquals(CardNetwork.INTERAC, CardValidation.detectNetwork("60"))
        assertEquals(CardNetwork.INTERAC, CardValidation.detectNetwork("6000000000000008"))
    }

    @Test
    fun testDetectNetworkUnknownOrEmpty() {
        assertNull(CardValidation.detectNetwork(""))
        assertNull(CardValidation.detectNetwork("99"))
        assertNull(CardValidation.detectNetwork("1234"))
    }

    // ─────────────────────────────────────────────────────────
    //  Card Length and CVC Length Specifications
    // ─────────────────────────────────────────────────────────

    @Test
    fun testMaxCardLengthPerNetwork() {
        assertEquals(15, CardValidation.maxCardLength(CardNetwork.AMEX))
        assertEquals(14, CardValidation.maxCardLength(CardNetwork.DINERS_CLUB))
        assertEquals(19, CardValidation.maxCardLength(CardNetwork.UNION_PAY))
        assertEquals(16, CardValidation.maxCardLength(CardNetwork.VISA))
        assertEquals(16, CardValidation.maxCardLength(CardNetwork.MASTERCARD))
        assertEquals(16, CardValidation.maxCardLength(CardNetwork.DISCOVER))
        assertEquals(16, CardValidation.maxCardLength(CardNetwork.JCB))
        assertEquals(16, CardValidation.maxCardLength(CardNetwork.INTERAC))
        assertEquals(16, CardValidation.maxCardLength(null))
    }

    @Test
    fun testCvcLengthPerNetwork() {
        assertEquals(4, CardValidation.cvcLength(CardNetwork.AMEX))
        assertEquals(3, CardValidation.cvcLength(CardNetwork.VISA))
        assertEquals(3, CardValidation.cvcLength(CardNetwork.MASTERCARD))
        assertEquals(3, CardValidation.cvcLength(CardNetwork.DISCOVER))
        assertEquals(3, CardValidation.cvcLength(CardNetwork.JCB))
        assertEquals(3, CardValidation.cvcLength(CardNetwork.DINERS_CLUB))
        assertEquals(3, CardValidation.cvcLength(CardNetwork.UNION_PAY))
        assertEquals(3, CardValidation.cvcLength(CardNetwork.INTERAC))
        assertEquals(3, CardValidation.cvcLength(null))
    }

    // ─────────────────────────────────────────────────────────
    //  Card Number Live Formatting Tests
    // ─────────────────────────────────────────────────────────

    @Test
    fun testFormatCardNumberStandard() {
        assertEquals("4242", CardValidation.formatCardNumber("4242", CardNetwork.VISA))
        assertEquals("4242 42", CardValidation.formatCardNumber("424242", CardNetwork.VISA))
        assertEquals("4242 4242 4242 4242", CardValidation.formatCardNumber("4242424242424242", CardNetwork.VISA))
        // Truncation at 16 digits
        assertEquals("4242 4242 4242 4242", CardValidation.formatCardNumber("4242424242424242999", CardNetwork.VISA))
    }

    @Test
    fun testFormatCardNumberAmex() {
        assertEquals("3782", CardValidation.formatCardNumber("3782", CardNetwork.AMEX))
        assertEquals("3782 822463", CardValidation.formatCardNumber("3782822463", CardNetwork.AMEX))
        assertEquals("3782 822463 10005", CardValidation.formatCardNumber("378282246310005", CardNetwork.AMEX))
        // Truncation at 15 digits
        assertEquals("3782 822463 10005", CardValidation.formatCardNumber("37828224631000599", CardNetwork.AMEX))
    }

    // ─────────────────────────────────────────────────────────
    //  Expiry Formatting, Parsing & Validation Tests
    // ─────────────────────────────────────────────────────────

    @Test
    fun testFormatExpiry() {
        assertEquals("1", CardValidation.formatExpiry("1"))
        assertEquals("12", CardValidation.formatExpiry("12"))
        assertEquals("12/3", CardValidation.formatExpiry("123"))
        assertEquals("12/34", CardValidation.formatExpiry("1234"))
        assertEquals("12/34", CardValidation.formatExpiry("123456"))
    }

    @Test
    fun testParseExpiry() {
        val parsed = CardValidation.parseExpiry("12/28")
        assertNotNull(parsed)
        assertEquals(12, parsed.first)
        assertEquals(28, parsed.second)

        assertNull(CardValidation.parseExpiry("12"))
        assertNull(CardValidation.parseExpiry("invalid"))
    }

    @Test
    fun testExpiryCompletion() {
        assertTrue(CardValidation.isExpiryComplete("12/28"))
        assertTrue(CardValidation.isExpiryComplete("01/30"))
        assertFalse(CardValidation.isExpiryComplete("12/"))
        assertFalse(CardValidation.isExpiryComplete("12"))
        assertFalse(CardValidation.isExpiryComplete("1"))
        assertFalse(CardValidation.isExpiryComplete(""))
    }

    @Test
    fun testExpiryValidation() {
        val curYear = currentYearTwoDigit()
        val curMonth = currentMonth()

        // Future year is valid
        assertTrue(CardValidation.isExpiryValid("12/${curYear + 5}"))

        // Current year with future/current month is valid
        val validMonthStr = curMonth.toString().padStart(2, '0')
        assertTrue(CardValidation.isExpiryValid("$validMonthStr/$curYear"))

        // Invalid month numbers (00, 13)
        assertFalse(CardValidation.isExpiryValid("00/${curYear + 1}"))
        assertFalse(CardValidation.isExpiryValid("13/${curYear + 1}"))

        // Past year is invalid
        val pastYearStr = (curYear - 1).toString().padStart(2, '0')
        assertFalse(CardValidation.isExpiryValid("12/$pastYearStr"))

        // Incomplete / unparseable strings
        assertFalse(CardValidation.isExpiryValid("12"))
        assertFalse(CardValidation.isExpiryValid(""))
    }

    // ─────────────────────────────────────────────────────────
    //  CVC Formatting & Validation Tests
    // ─────────────────────────────────────────────────────────

    @Test
    fun testFormatCvc() {
        assertEquals("123", CardValidation.formatCvc("123", CardNetwork.VISA))
        assertEquals("123", CardValidation.formatCvc("1234", CardNetwork.VISA))
        assertEquals("1234", CardValidation.formatCvc("1234", CardNetwork.AMEX))
        assertEquals("1234", CardValidation.formatCvc("12345", CardNetwork.AMEX))
    }

    @Test
    fun testIsCvcValidAndComplete() {
        // Standard networks (3 digits)
        assertTrue(CardValidation.isCvcValid("123", CardNetwork.VISA))
        assertTrue(CardValidation.isCvcComplete("123", CardNetwork.VISA))
        assertFalse(CardValidation.isCvcValid("12", CardNetwork.VISA))
        assertFalse(CardValidation.isCvcComplete("12", CardNetwork.VISA))
        assertFalse(CardValidation.isCvcValid("1234", CardNetwork.VISA))

        // Amex (4 digits)
        assertTrue(CardValidation.isCvcValid("1234", CardNetwork.AMEX))
        assertTrue(CardValidation.isCvcComplete("1234", CardNetwork.AMEX))
        assertFalse(CardValidation.isCvcValid("123", CardNetwork.AMEX))
        assertFalse(CardValidation.isCvcComplete("123", CardNetwork.AMEX))
    }

    // ─────────────────────────────────────────────────────────
    //  Full Card Number Completeness & Validation
    // ─────────────────────────────────────────────────────────

    @Test
    fun testIsCardNumberComplete() {
        assertTrue(CardValidation.isCardNumberComplete("4242424242424242"))
        assertFalse(CardValidation.isCardNumberComplete("424242424242424"))

        assertTrue(CardValidation.isCardNumberComplete("378282246310005"))
        assertFalse(CardValidation.isCardNumberComplete("37828224631000"))
    }

    @Test
    fun testIsCardNumberValid() {
        assertTrue(CardValidation.isCardNumberValid("4242424242424242"))
        assertTrue(CardValidation.isCardNumberValid("378282246310005"))
        assertFalse(CardValidation.isCardNumberValid("4242424242424243")) // bad luhn
        assertFalse(CardValidation.isCardNumberValid("424242424242424")) // incomplete
    }

    // ─────────────────────────────────────────────────────────
    //  CardFormState Lifecycle & PCI-DSS Clearing Tests
    // ─────────────────────────────────────────────────────────

    @Test
    fun testCardFormStateClearSensitiveData() {
        val form = CardFormState(
            number = CardNumberState(rawValue = "4242424242424242", isValid = true, isComplete = true),
            expiry = ExpiryState(rawValue = "1228", month = 12, year = 28, isValid = true, isComplete = true),
            cvc = CvcState(rawValue = "123", isValid = true, isComplete = true),
            cardholderName = "Jane Doe"
        )

        val sanitized = form.clearSensitiveData()
        assertEquals("4242424242424242", sanitized.number.rawValue)
        assertEquals(12, sanitized.expiry.month)
        assertEquals("", sanitized.cvc.rawValue)
        assertFalse(sanitized.cvc.isValid)
        assertFalse(sanitized.cvc.isComplete)
        assertEquals("Jane Doe", sanitized.cardholderName)
    }

    @Test
    fun testCardFormStateClearCvc() {
        val form = CardFormState(
            number = CardNumberState(rawValue = "4242424242424242", isValid = true, isComplete = true),
            expiry = ExpiryState(rawValue = "1228", month = 12, year = 28, isValid = true, isComplete = true),
            cvc = CvcState(rawValue = "123", isValid = true, isComplete = true),
            cardholderName = "Jane Doe"
        )

        val cvcCleared = form.clearCvc()
        assertEquals("4242424242424242", cvcCleared.number.rawValue)
        assertEquals(12, cvcCleared.expiry.month)
        assertEquals(28, cvcCleared.expiry.year)
        assertEquals("", cvcCleared.cvc.rawValue)
        assertFalse(cvcCleared.cvc.isValid)
        assertFalse(cvcCleared.cvc.isComplete)
        assertEquals("Jane Doe", cvcCleared.cardholderName)
    }

    @Test
    fun testCardFormStateIsFormValidAndComplete() {
        val validNumber = CardNumberState(rawValue = "4242424242424242", isValid = true, isComplete = true)
        val validExpiry = ExpiryState(rawValue = "1228", month = 12, year = 28, isValid = true, isComplete = true)
        val validCvc = CvcState(rawValue = "123", isValid = true, isComplete = true)

        // All valid
        val validForm = CardFormState(number = validNumber, expiry = validExpiry, cvc = validCvc)
        assertTrue(validForm.isFormValid)
        assertTrue(validForm.isFormComplete)

        // Invalid number
        assertFalse(validForm.copy(number = validNumber.copy(isValid = false)).isFormValid)
        assertFalse(validForm.copy(number = validNumber.copy(isComplete = false)).isFormComplete)

        // Invalid expiry
        assertFalse(validForm.copy(expiry = validExpiry.copy(isValid = false)).isFormValid)
        assertFalse(validForm.copy(expiry = validExpiry.copy(month = null)).isFormValid)
        assertFalse(validForm.copy(expiry = validExpiry.copy(year = null)).isFormValid)

        // Invalid CVC
        assertFalse(validForm.copy(cvc = validCvc.copy(isValid = false)).isFormValid)
        assertFalse(validForm.copy(cvc = validCvc.copy(isComplete = false)).isFormComplete)
    }

    @Test
    fun testCardInputStateFormattedValues() {
        val numberStateVisa = CardNumberState(rawValue = "4242424242424242", network = CardNetwork.VISA)
        assertEquals("4242 4242 4242 4242", numberStateVisa.formattedValue)

        val numberStateAmex = CardNumberState(rawValue = "378282246310005", network = CardNetwork.AMEX)
        assertEquals("3782 822463 10005", numberStateAmex.formattedValue)

        val expiryState = ExpiryState(rawValue = "1228")
        assertEquals("12/28", expiryState.formattedValue)

        val cvcState = CvcState(rawValue = "123")
        assertEquals("123", cvcState.formattedValue)
    }

    @Test
    fun testCardFormStateClearAll() {
        val form = CardFormState(
            number = CardNumberState(rawValue = "4242424242424242", isValid = true, isComplete = true),
            expiry = ExpiryState(rawValue = "1228", month = 12, year = 28, isValid = true, isComplete = true),
            cvc = CvcState(rawValue = "123", isValid = true, isComplete = true),
            cardholderName = "Jane Doe"
        )

        val reset = form.clearAll()
        assertEquals("", reset.number.rawValue)
        assertEquals("", reset.expiry.rawValue)
        assertEquals("", reset.cvc.rawValue)
        assertEquals("", reset.cardholderName)
        assertFalse(reset.isFormValid)
        assertFalse(reset.isFormComplete)
    }

    @Test
    fun testInitialStates() {
        val initialNumber = CardNumberState.initial()
        assertEquals("", initialNumber.rawValue)
        assertNull(initialNumber.network)
        assertFalse(initialNumber.isValid)
        assertFalse(initialNumber.isComplete)
        assertNull(initialNumber.error)

        val initialExpiry = ExpiryState.initial()
        assertEquals("", initialExpiry.rawValue)
        assertNull(initialExpiry.month)
        assertNull(initialExpiry.year)
        assertFalse(initialExpiry.isValid)
        assertFalse(initialExpiry.isComplete)
        assertNull(initialExpiry.error)

        val initialCvc = CvcState.initial()
        assertEquals("", initialCvc.rawValue)
        assertFalse(initialCvc.isValid)
        assertFalse(initialCvc.isComplete)
        assertNull(initialCvc.error)

        val initialForm = CardFormState.initial()
        assertEquals(initialNumber, initialForm.number)
        assertEquals(initialExpiry, initialForm.expiry)
        assertEquals(initialCvc, initialForm.cvc)
        assertEquals("", initialForm.cardholderName)
        assertFalse(initialForm.isFormValid)
        assertFalse(initialForm.isFormComplete)
    }
}
