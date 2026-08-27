package com.landoulsi.payment.ui.card

import androidx.compose.ui.text.AnnotatedString
import com.landoulsi.payment.shared.model.CardNetwork
import org.junit.Assert.assertEquals
import org.junit.Test

class CardVisualTransformationTest {

    @Test
    fun testStandardCardNumberVisualTransformation() {
        val transformation = CardNumberVisualTransformation(CardNetwork.VISA)
        val input = AnnotatedString("4242424242424242")
        val transformed = transformation.filter(input)

        assertEquals("4242 4242 4242 4242", transformed.text.text)

        val mapping = transformed.offsetMapping

        // originalToTransformed
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(3, mapping.originalToTransformed(3))
        assertEquals(5, mapping.originalToTransformed(4))
        assertEquals(10, mapping.originalToTransformed(8))
        assertEquals(15, mapping.originalToTransformed(12))
        assertEquals(19, mapping.originalToTransformed(16))

        // transformedToOriginal
        assertEquals(0, mapping.transformedToOriginal(0))
        assertEquals(3, mapping.transformedToOriginal(3))
        assertEquals(4, mapping.transformedToOriginal(4))
        assertEquals(4, mapping.transformedToOriginal(5))
        assertEquals(8, mapping.transformedToOriginal(9))
        assertEquals(8, mapping.transformedToOriginal(10))
        assertEquals(12, mapping.transformedToOriginal(14))
        assertEquals(12, mapping.transformedToOriginal(15))
        assertEquals(16, mapping.transformedToOriginal(19))
    }

    @Test
    fun testAmexCardNumberVisualTransformation() {
        val transformation = CardNumberVisualTransformation(CardNetwork.AMEX)
        val input = AnnotatedString("378282246310005")
        val transformed = transformation.filter(input)

        assertEquals("3782 822463 10005", transformed.text.text)

        val mapping = transformed.offsetMapping

        // originalToTransformed
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(3, mapping.originalToTransformed(3))
        assertEquals(5, mapping.originalToTransformed(4))
        assertEquals(12, mapping.originalToTransformed(10))
        assertEquals(17, mapping.originalToTransformed(15))

        // transformedToOriginal
        assertEquals(0, mapping.transformedToOriginal(0))
        assertEquals(4, mapping.transformedToOriginal(4))
        assertEquals(4, mapping.transformedToOriginal(5))
        assertEquals(10, mapping.transformedToOriginal(11))
        assertEquals(10, mapping.transformedToOriginal(12))
        assertEquals(15, mapping.transformedToOriginal(17))
    }

    @Test
    fun testExpiryVisualTransformation() {
        val transformation = ExpiryVisualTransformation()
        val input = AnnotatedString("1228")
        val transformed = transformation.filter(input)

        assertEquals("12/28", transformed.text.text)

        val mapping = transformed.offsetMapping

        // originalToTransformed
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(1, mapping.originalToTransformed(1))
        assertEquals(2, mapping.originalToTransformed(2))
        assertEquals(4, mapping.originalToTransformed(3))
        assertEquals(5, mapping.originalToTransformed(4))

        // transformedToOriginal
        assertEquals(0, mapping.transformedToOriginal(0))
        assertEquals(1, mapping.transformedToOriginal(1))
        assertEquals(2, mapping.transformedToOriginal(2))
        assertEquals(2, mapping.transformedToOriginal(3))
        assertEquals(3, mapping.transformedToOriginal(4))
        assertEquals(4, mapping.transformedToOriginal(5))
    }

    @Test
    fun testNullNetworkStandardTransformation() {
        val transformation = CardNumberVisualTransformation(null)
        val input = AnnotatedString("4111222233334444")
        val transformed = transformation.filter(input)

        assertEquals("4111 2222 3333 4444", transformed.text.text)
        val mapping = transformed.offsetMapping
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(5, mapping.originalToTransformed(4))
        assertEquals(10, mapping.originalToTransformed(8))
        assertEquals(15, mapping.originalToTransformed(12))
        assertEquals(19, mapping.originalToTransformed(16))
    }

    @Test
    fun testMastercardVisualTransformation() {
        val transformation = CardNumberVisualTransformation(CardNetwork.MASTERCARD)
        val input = AnnotatedString("5555444433332222")
        val transformed = transformation.filter(input)

        assertEquals("5555 4444 3333 2222", transformed.text.text)
    }

    @Test
    fun testPartialCardNumberVisualTransformation() {
        val transformation = CardNumberVisualTransformation(CardNetwork.VISA)

        // Empty
        val emptyResult = transformation.filter(AnnotatedString(""))
        assertEquals("", emptyResult.text.text)
        assertEquals(0, emptyResult.offsetMapping.originalToTransformed(0))
        assertEquals(0, emptyResult.offsetMapping.transformedToOriginal(0))

        // 1 digit
        val oneDigit = transformation.filter(AnnotatedString("4"))
        assertEquals("4", oneDigit.text.text)
        assertEquals(1, oneDigit.offsetMapping.originalToTransformed(1))
        assertEquals(1, oneDigit.offsetMapping.transformedToOriginal(1))

        // 4 digits
        val fourDigits = transformation.filter(AnnotatedString("4242"))
        assertEquals("4242", fourDigits.text.text)
        assertEquals(4, fourDigits.offsetMapping.originalToTransformed(4))

        // 5 digits
        val fiveDigits = transformation.filter(AnnotatedString("42424"))
        assertEquals("4242 4", fiveDigits.text.text)
        assertEquals(5, fiveDigits.offsetMapping.originalToTransformed(4))
        assertEquals(6, fiveDigits.offsetMapping.originalToTransformed(5))
    }

    @Test
    fun testAmexPartialVisualTransformation() {
        val transformation = CardNumberVisualTransformation(CardNetwork.AMEX)

        // 4 digits (no spaces yet)
        val fourDigits = transformation.filter(AnnotatedString("3782"))
        assertEquals("3782", fourDigits.text.text)

        // 5 digits (1 space after 4th digit)
        val fiveDigits = transformation.filter(AnnotatedString("37828"))
        assertEquals("3782 8", fiveDigits.text.text)

        // 10 digits (space at 4, no second space until 11th digit)
        val tenDigits = transformation.filter(AnnotatedString("3782822463"))
        assertEquals("3782 822463", tenDigits.text.text)

        // 11 digits (second space added)
        val elevenDigits = transformation.filter(AnnotatedString("37828224631"))
        assertEquals("3782 822463 1", elevenDigits.text.text)
    }

    @Test
    fun testCardNumberVisualTransformationOutOfBoundsOffsets() {
        val transformation = CardNumberVisualTransformation(CardNetwork.VISA)
        val input = AnnotatedString("4242424242424242")
        val transformed = transformation.filter(input)
        val mapping = transformed.offsetMapping

        // Negative or out of bounds offsets should be handled gracefully without crashing
        assertEquals(0, mapping.originalToTransformed(-5))
        assertEquals(19, mapping.originalToTransformed(100))
        assertEquals(0, mapping.transformedToOriginal(-5))
        assertEquals(16, mapping.transformedToOriginal(100))
    }

    @Test
    fun testExpiryVisualTransformationPartialAndEdgeCases() {
        val transformation = ExpiryVisualTransformation()

        // Empty
        val empty = transformation.filter(AnnotatedString(""))
        assertEquals("", empty.text.text)
        assertEquals(0, empty.offsetMapping.originalToTransformed(0))
        assertEquals(0, empty.offsetMapping.transformedToOriginal(0))

        // 1 digit
        val oneDigit = transformation.filter(AnnotatedString("1"))
        assertEquals("1", oneDigit.text.text)
        assertEquals(1, oneDigit.offsetMapping.originalToTransformed(1))
        assertEquals(1, oneDigit.offsetMapping.transformedToOriginal(1))

        // 2 digits
        val twoDigits = transformation.filter(AnnotatedString("12"))
        assertEquals("12", twoDigits.text.text)
        assertEquals(2, twoDigits.offsetMapping.originalToTransformed(2))

        // 3 digits
        val threeDigits = transformation.filter(AnnotatedString("123"))
        assertEquals("12/3", threeDigits.text.text)
        assertEquals(4, threeDigits.offsetMapping.originalToTransformed(3))

        // Out of bounds offsets
        val full = transformation.filter(AnnotatedString("1228"))
        assertEquals(0, full.offsetMapping.originalToTransformed(-1))
        assertEquals(5, full.offsetMapping.originalToTransformed(50))
        assertEquals(0, full.offsetMapping.transformedToOriginal(-1))
        assertEquals(4, full.offsetMapping.transformedToOriginal(50))
    }

    @Test
    fun testCvcVisualTransformationEdgeCases() {
        val transformation = CvcVisualTransformation()

        // Empty
        val empty = transformation.filter(AnnotatedString(""))
        assertEquals("", empty.text.text)
        assertEquals(0, empty.offsetMapping.originalToTransformed(0))
        assertEquals(0, empty.offsetMapping.transformedToOriginal(0))

        // 4 digits (Amex)
        val amexCvc = transformation.filter(AnnotatedString("1234"))
        assertEquals("••••", amexCvc.text.text)
        assertEquals(4, amexCvc.offsetMapping.originalToTransformed(4))
        assertEquals(4, amexCvc.offsetMapping.transformedToOriginal(4))
    }
}
