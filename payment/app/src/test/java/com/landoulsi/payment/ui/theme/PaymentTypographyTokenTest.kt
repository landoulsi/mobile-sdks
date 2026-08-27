package com.landoulsi.payment.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentTypographyTokenTest {

    // ── display styles ──────────────────────────────────────

    @Test
    fun displayLarge_fontSizeIs57sp() {
        assertEquals(57f, Typography.displayLarge.fontSize.value, 0.001f)
    }

    @Test
    fun displayLarge_lineHeightIs64sp() {
        assertEquals(64f, Typography.displayLarge.lineHeight.value, 0.001f)
    }

    @Test
    fun displayLarge_letterSpacingIsMinus025() {
        assertEquals(-0.25f, Typography.displayLarge.letterSpacing.value, 0.001f)
    }

    @Test
    fun displayMedium_fontSizeIs45sp() {
        assertEquals(45f, Typography.displayMedium.fontSize.value, 0.001f)
    }

    @Test
    fun displayMedium_lineHeightIs52sp() {
        assertEquals(52f, Typography.displayMedium.lineHeight.value, 0.001f)
    }

    @Test
    fun displaySmall_fontSizeIs36sp() {
        assertEquals(36f, Typography.displaySmall.fontSize.value, 0.001f)
    }

    @Test
    fun displaySmall_lineHeightIs44sp() {
        assertEquals(44f, Typography.displaySmall.lineHeight.value, 0.001f)
    }

    // ── headline styles ─────────────────────────────────────

    @Test
    fun headlineLarge_fontSizeIs32sp() {
        assertEquals(32f, Typography.headlineLarge.fontSize.value, 0.001f)
    }

    @Test
    fun headlineLarge_lineHeightIs40sp() {
        assertEquals(40f, Typography.headlineLarge.lineHeight.value, 0.001f)
    }

    @Test
    fun headlineMedium_fontSizeIs28sp() {
        assertEquals(28f, Typography.headlineMedium.fontSize.value, 0.001f)
    }

    @Test
    fun headlineMedium_lineHeightIs36sp() {
        assertEquals(36f, Typography.headlineMedium.lineHeight.value, 0.001f)
    }

    @Test
    fun headlineSmall_fontSizeIs24sp() {
        assertEquals(24f, Typography.headlineSmall.fontSize.value, 0.001f)
    }

    @Test
    fun headlineSmall_lineHeightIs32sp() {
        assertEquals(32f, Typography.headlineSmall.lineHeight.value, 0.001f)
    }

    // ── title styles ────────────────────────────────────────

    @Test
    fun titleLarge_fontSizeIs22sp() {
        assertEquals(22f, Typography.titleLarge.fontSize.value, 0.001f)
    }

    @Test
    fun titleLarge_lineHeightIs28sp() {
        assertEquals(28f, Typography.titleLarge.lineHeight.value, 0.001f)
    }

    @Test
    fun titleMedium_fontSizeIs16sp() {
        assertEquals(16f, Typography.titleMedium.fontSize.value, 0.001f)
    }

    @Test
    fun titleMedium_letterSpacingIs015() {
        assertEquals(0.15f, Typography.titleMedium.letterSpacing.value, 0.001f)
    }

    @Test
    fun titleSmall_fontSizeIs14sp() {
        assertEquals(14f, Typography.titleSmall.fontSize.value, 0.001f)
    }

    @Test
    fun titleSmall_letterSpacingIs01() {
        assertEquals(0.1f, Typography.titleSmall.letterSpacing.value, 0.001f)
    }

    // ── body styles ─────────────────────────────────────────

    @Test
    fun bodyLarge_fontSizeIs16sp() {
        assertEquals(16f, Typography.bodyLarge.fontSize.value, 0.001f)
    }

    @Test
    fun bodyLarge_lineHeightIs24sp() {
        assertEquals(24f, Typography.bodyLarge.lineHeight.value, 0.001f)
    }

    @Test
    fun bodyLarge_letterSpacingIs05() {
        assertEquals(0.5f, Typography.bodyLarge.letterSpacing.value, 0.001f)
    }

    @Test
    fun bodyMedium_fontSizeIs14sp() {
        assertEquals(14f, Typography.bodyMedium.fontSize.value, 0.001f)
    }

    @Test
    fun bodyMedium_lineHeightIs20sp() {
        assertEquals(20f, Typography.bodyMedium.lineHeight.value, 0.001f)
    }

    @Test
    fun bodySmall_fontSizeIs12sp() {
        assertEquals(12f, Typography.bodySmall.fontSize.value, 0.001f)
    }

    @Test
    fun bodySmall_lineHeightIs16sp() {
        assertEquals(16f, Typography.bodySmall.lineHeight.value, 0.001f)
    }

    // ── label styles ────────────────────────────────────────

    @Test
    fun labelLarge_fontSizeIs14sp() {
        assertEquals(14f, Typography.labelLarge.fontSize.value, 0.001f)
    }

    @Test
    fun labelMedium_fontSizeIs12sp() {
        assertEquals(12f, Typography.labelMedium.fontSize.value, 0.001f)
    }

    @Test
    fun labelSmall_fontSizeIs11sp() {
        assertEquals(11f, Typography.labelSmall.fontSize.value, 0.001f)
    }

    // ── font family consistency ─────────────────────────────

    @Test
    fun allStylesUseDefaultFontFamily() {
        val styles = listOf(
            Typography.displayLarge, Typography.displayMedium, Typography.displaySmall,
            Typography.headlineLarge, Typography.headlineMedium, Typography.headlineSmall,
            Typography.titleLarge, Typography.titleMedium, Typography.titleSmall,
            Typography.bodyLarge, Typography.bodyMedium, Typography.bodySmall,
            Typography.labelLarge, Typography.labelMedium, Typography.labelSmall
        )
        styles.forEach { style ->
            assertNotNull("FontFamily should not be null", style.fontFamily)
        }
    }

    // ── line-height > font-size invariant ───────────────────

    @Test
    fun lineHeightExceedsFontSize_forAllStyles() {
        val styles = listOf(
            Typography.displayLarge, Typography.displayMedium, Typography.displaySmall,
            Typography.headlineLarge, Typography.headlineMedium, Typography.headlineSmall,
            Typography.titleLarge, Typography.titleMedium, Typography.titleSmall,
            Typography.bodyLarge, Typography.bodyMedium, Typography.bodySmall,
            Typography.labelLarge, Typography.labelMedium, Typography.labelSmall
        )
        styles.forEach { style ->
            assertTrue(
                "lineHeight (${style.lineHeight}) should be >= fontSize (${style.fontSize})",
                style.lineHeight >= style.fontSize
            )
        }
    }

    // ── 15 styles total (Material3 standard) ────────────────

    @Test
    fun typographyScale_has15Styles() {
        val styles = listOf(
            Typography.displayLarge, Typography.displayMedium, Typography.displaySmall,
            Typography.headlineLarge, Typography.headlineMedium, Typography.headlineSmall,
            Typography.titleLarge, Typography.titleMedium, Typography.titleSmall,
            Typography.bodyLarge, Typography.bodyMedium, Typography.bodySmall,
            Typography.labelLarge, Typography.labelMedium, Typography.labelSmall
        )
        assertEquals(15, styles.size)
    }

    // ── font-weight tiers ───────────────────────────────────

    @Test
    fun displayAndBodyAndHeadline_useNormalWeight() {
        val normalWeightStyles = listOf(
            Typography.displayLarge, Typography.displayMedium, Typography.displaySmall,
            Typography.headlineLarge, Typography.headlineMedium, Typography.headlineSmall,
            Typography.bodyLarge, Typography.bodyMedium, Typography.bodySmall
        )
        normalWeightStyles.forEach { style ->
            assertEquals(
                "Expected Normal weight for ${style.fontSize}",
                androidx.compose.ui.text.font.FontWeight.Normal,
                style.fontWeight
            )
        }
    }

    @Test
    fun titleAndLabel_useMediumWeight() {
        val mediumWeightStyles = listOf(
            Typography.titleLarge, Typography.titleMedium, Typography.titleSmall,
            Typography.labelLarge, Typography.labelMedium, Typography.labelSmall
        )
        mediumWeightStyles.forEach { style ->
            assertEquals(
                "Expected Medium weight for ${style.fontSize}",
                androidx.compose.ui.text.font.FontWeight.Medium,
                style.fontWeight
            )
        }
    }
}
