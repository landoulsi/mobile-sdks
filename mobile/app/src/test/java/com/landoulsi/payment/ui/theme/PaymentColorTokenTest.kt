package com.landoulsi.payment.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentColorTokenTest {

    // ── helpers ──────────────────────────────────────────────

    private fun Color.redInt() = (red * 255).toInt()
    private fun Color.greenInt() = (green * 255).toInt()
    private fun Color.blueInt() = (blue * 255).toInt()
    private fun Color.alphaInt() = (alpha * 255).toInt()

    // ── Blue palette ────────────────────────────────────────

    @Test
    fun bluePalette_isAllOpaque() {
        listOf(Blue10, Blue20, Blue30, Blue40, Blue80, Blue90, Blue95).forEach { c ->
            assertEquals("Alpha of $c should be 255", 255, c.alphaInt())
        }
    }

    @Test
    fun blue40_isExpectedValue() {
        assertEquals(0x00, Blue40.redInt())
        assertEquals(0x78, Blue40.greenInt())
        assertEquals(0xA3, Blue40.blueInt())
    }

    @Test
    fun blue80_isExpectedValue() {
        assertEquals(0x8E, Blue80.redInt())
        assertEquals(0xCF, Blue80.greenInt())
        assertEquals(0xF6, Blue80.blueInt())
    }

    @Test
    fun blue10_isDarkest() {
        assertTrue(Blue10.greenInt() < Blue20.greenInt())
        assertTrue(Blue20.greenInt() < Blue30.greenInt())
        assertTrue(Blue10.blueInt() < Blue20.blueInt())
        assertTrue(Blue20.blueInt() < Blue30.blueInt())
    }

    @Test
    fun blue95_isLightest() {
        assertTrue(Blue95.redInt() > Blue90.redInt())
        assertTrue(Blue90.redInt() > Blue80.redInt())
    }

    // ── Teal palette ────────────────────────────────────────

    @Test
    fun tealPalette_isAllOpaque() {
        listOf(Teal10, Teal20, Teal30, Teal40, Teal80, Teal90).forEach { c ->
            assertEquals(255, c.alphaInt())
        }
    }

    @Test
    fun teal40_isExpectedValue() {
        assertEquals(0x00, Teal40.redInt())
        assertEquals(0x6A, Teal40.greenInt())
        assertEquals(0x6E, Teal40.blueInt())
    }

    @Test
    fun teal80_isExpectedValue() {
        assertEquals(0x4D, Teal80.redInt())
        assertEquals(0xD9, Teal80.greenInt())
        assertEquals(0xDE, Teal80.blueInt())
    }

    // ── Amber palette ───────────────────────────────────────

    @Test
    fun amberPalette_isAllOpaque() {
        listOf(Amber10, Amber20, Amber30, Amber40, Amber80, Amber90).forEach { c ->
            assertEquals(255, c.alphaInt())
        }
    }

    @Test
    fun amber40_isExpectedValue() {
        assertEquals(0x7A, Amber40.redInt())
        assertEquals(0x59, Amber40.greenInt())
        assertEquals(0x00, Amber40.blueInt())
    }

    @Test
    fun amber80_isExpectedValue() {
        assertEquals(0xF5, Amber80.redInt())
        assertEquals(0xBF, Amber80.greenInt())
        assertEquals(0x48, Amber80.blueInt())
    }

    // ── Red palette ─────────────────────────────────────────

    @Test
    fun redPalette_isAllOpaque() {
        listOf(Red10, Red20, Red30, Red40, Red80, Red90).forEach { c ->
            assertEquals(255, c.alphaInt())
        }
    }

    @Test
    fun red40_isExpectedValue() {
        assertEquals(0xBA, Red40.redInt())
        assertEquals(0x1A, Red40.greenInt())
        assertEquals(0x1A, Red40.blueInt())
    }

    @Test
    fun red80_isExpectedValue() {
        assertEquals(0xFF, Red80.redInt())
        assertEquals(0xB4, Red80.greenInt())
        assertEquals(0xAB, Red80.blueInt())
    }

    // ── Neutral palette ─────────────────────────────────────

    @Test
    fun neutralPalette_isAllOpaque() {
        val neutrals = listOf(
            Neutral4, Neutral6, Neutral10, Neutral12, Neutral17,
            Neutral20, Neutral22, Neutral24, Neutral87, Neutral90,
            Neutral92, Neutral94, Neutral95, Neutral96, Neutral98,
            Neutral99, Neutral100
        )
        neutrals.forEach { c ->
            assertEquals("Alpha of $c should be 255", 255, c.alphaInt())
        }
    }

    @Test
    fun neutral100_isWhite() {
        assertEquals(255, Neutral100.redInt())
        assertEquals(255, Neutral100.greenInt())
        assertEquals(255, Neutral100.blueInt())
    }

    @Test
    fun neutral4_isNearBlack() {
        assertEquals(0x0E, Neutral4.redInt())
        assertEquals(0x0E, Neutral4.greenInt())
        assertEquals(0x11, Neutral4.blueInt())
    }

    @Test
    fun neutrals_areMonotonicallyIncreasing() {
        val sorted = listOf(
            Neutral4, Neutral6, Neutral10, Neutral12, Neutral17,
            Neutral20, Neutral22, Neutral24, Neutral87, Neutral90,
            Neutral92, Neutral94, Neutral95, Neutral96, Neutral98,
            Neutral99, Neutral100
        )
        for (i in 1 until sorted.size) {
            assertTrue(
                "Neutral at index $i (${sorted[i].redInt()}) should be >= index ${i - 1} (${sorted[i - 1].redInt()})",
                sorted[i].redInt() >= sorted[i - 1].redInt()
            )
        }
    }

    // ── Neutral-variant palette ──────────────────────────────

    @Test
    fun neutralVariantPalette_isAllOpaque() {
        listOf(NeutralVar30, NeutralVar50, NeutralVar60, NeutralVar80, NeutralVar90).forEach { c ->
            assertEquals(255, c.alphaInt())
        }
    }

    @Test
    fun neutralVariant30_isExpectedValue() {
        assertEquals(0x41, NeutralVar30.redInt())
        assertEquals(0x47, NeutralVar30.greenInt())
        assertEquals(0x50, NeutralVar30.blueInt())
    }

    @Test
    fun neutralVariant90_isExpectedValue() {
        assertEquals(0xDE, NeutralVar90.redInt())
        assertEquals(0xE3, NeutralVar90.greenInt())
        assertEquals(0xEA, NeutralVar90.blueInt())
    }

    // ── Status & Card Brand Colors ──────────────────────────

    @Test
    fun statusAndCardBrandColors_areAllOpaque() {
        val colors = listOf(
            SuccessGreen,
            CardVisa,
            CardMastercard,
            CardAmex,
            CardDiscover,
            CardJcb,
            CardDinersClub,
            CardUnionPay,
            CardInterac
        )
        colors.forEach { c ->
            assertEquals("Alpha of $c should be 255", 255, c.alphaInt())
        }
    }

    @Test
    fun successGreen_isExpectedValue() {
        assertEquals(0x34, SuccessGreen.redInt())
        assertEquals(0xA8, SuccessGreen.greenInt())
        assertEquals(0x53, SuccessGreen.blueInt())
    }

    @Test
    fun cardBrandColors_haveExpectedValues() {
        assertEquals(0x1A, CardVisa.redInt())
        assertEquals(0x1F, CardVisa.greenInt())
        assertEquals(0x71, CardVisa.blueInt())

        assertEquals(0xEB, CardMastercard.redInt())
        assertEquals(0x00, CardMastercard.greenInt())
        assertEquals(0x1B, CardMastercard.blueInt())

        assertEquals(0x00, CardAmex.redInt())
        assertEquals(0x6F, CardAmex.greenInt())
        assertEquals(0xCF, CardAmex.blueInt())

        assertEquals(0xFF, CardDiscover.redInt())
        assertEquals(0x60, CardDiscover.greenInt())
        assertEquals(0x00, CardDiscover.blueInt())

        assertEquals(0x00, CardJcb.redInt())
        assertEquals(0x3B, CardJcb.greenInt())
        assertEquals(0x77, CardJcb.blueInt())

        assertEquals(0x00, CardDinersClub.redInt())
        assertEquals(0x4A, CardDinersClub.greenInt())
        assertEquals(0x97, CardDinersClub.blueInt())

        assertEquals(0x00, CardUnionPay.redInt())
        assertEquals(0x7B, CardUnionPay.greenInt())
        assertEquals(0x78, CardUnionPay.blueInt())

        assertEquals(0xFF, CardInterac.redInt())
        assertEquals(0xD1, CardInterac.greenInt())
        assertEquals(0x00, CardInterac.blueInt())
    }

    @Test
    fun paymentColorTokens_referenceCorrectConstants() {
        assertEquals(SuccessGreen, PaymentColorTokens.success)
        assertEquals(CardVisa, PaymentColorTokens.cardVisa)
        assertEquals(CardMastercard, PaymentColorTokens.cardMastercard)
        assertEquals(CardAmex, PaymentColorTokens.cardAmex)
        assertEquals(CardDiscover, PaymentColorTokens.cardDiscover)
        assertEquals(CardJcb, PaymentColorTokens.cardJcb)
        assertEquals(CardDinersClub, PaymentColorTokens.cardDinersClub)
        assertEquals(CardUnionPay, PaymentColorTokens.cardUnionPay)
        assertEquals(CardInterac, PaymentColorTokens.cardInterac)
    }

    @Test
    fun cardBrandColors_areMutuallyDistinct() {
        val brands = listOf(
            CardVisa,
            CardMastercard,
            CardAmex,
            CardDiscover,
            CardJcb,
            CardDinersClub,
            CardUnionPay,
            CardInterac
        )
        for (i in brands.indices) {
            for (j in i + 1 until brands.size) {
                assertTrue(
                    "Brand color at $i (${brands[i]}) should not equal brand color at $j (${brands[j]})",
                    brands[i] != brands[j]
                )
            }
        }
    }
}
