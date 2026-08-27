package com.landoulsi.payment.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentDimensionTokenTest {

    // ── PaymentSpacing ──────────────────────────────────────

    @Test
    fun spacingValues_areMonotonicallyIncreasing() {
        val values = listOf(
            PaymentSpacing.xxs, PaymentSpacing.xs, PaymentSpacing.sm,
            PaymentSpacing.md, PaymentSpacing.lg, PaymentSpacing.xl,
            PaymentSpacing.xxl, PaymentSpacing.xxxl
        )
        for (i in 1 until values.size) {
            assertTrue(
                "Spacing[$i] (${values[i]}) should be > Spacing[${i - 1}] (${values[i - 1]})",
                values[i] > values[i - 1]
            )
        }
    }

    @Test
    fun spacingXxs_is2dp() {
        assertEquals(2f, PaymentSpacing.xxs.value, 0.001f)
    }

    @Test
    fun spacingXs_is4dp() {
        assertEquals(4f, PaymentSpacing.xs.value, 0.001f)
    }

    @Test
    fun spacingSm_is8dp() {
        assertEquals(8f, PaymentSpacing.sm.value, 0.001f)
    }

    @Test
    fun spacingMd_is12dp() {
        assertEquals(12f, PaymentSpacing.md.value, 0.001f)
    }

    @Test
    fun spacingLg_is16dp() {
        assertEquals(16f, PaymentSpacing.lg.value, 0.001f)
    }

    @Test
    fun spacingXl_is24dp() {
        assertEquals(24f, PaymentSpacing.xl.value, 0.001f)
    }

    @Test
    fun spacingXxl_is32dp() {
        assertEquals(32f, PaymentSpacing.xxl.value, 0.001f)
    }

    @Test
    fun spacingXxxl_is48dp() {
        assertEquals(48f, PaymentSpacing.xxxl.value, 0.001f)
    }

    @Test
    fun spacingScale_hasEightSteps() {
        val getterCount = PaymentSpacing::class.java.declaredMethods.count { it.name.startsWith("get") }
        assertEquals(8, getterCount)
    }

    // ── PaymentRadius ───────────────────────────────────────

    @Test
    fun radiusXs_is4dp() {
        assertEquals(4f, PaymentRadius.xs.value, 0.001f)
    }

    @Test
    fun radiusSm_is8dp() {
        assertEquals(8f, PaymentRadius.sm.value, 0.001f)
    }

    @Test
    fun radiusMd_is12dp() {
        assertEquals(12f, PaymentRadius.md.value, 0.001f)
    }

    @Test
    fun radiusLg_is16dp() {
        assertEquals(16f, PaymentRadius.lg.value, 0.001f)
    }

    @Test
    fun radiusXl_is24dp() {
        assertEquals(24f, PaymentRadius.xl.value, 0.001f)
    }

    @Test
    fun radiusFull_is999dp() {
        assertEquals(999f, PaymentRadius.full.value, 0.001f)
    }

    @Test
    fun radiusScale_hasSixSteps() {
        val getterCount = PaymentRadius::class.java.declaredMethods.count { it.name.startsWith("get") }
        assertEquals(6, getterCount)
    }

    @Test
    fun radiusValues_areMonotonicallyIncreasing() {
        val values = listOf(
            PaymentRadius.xs, PaymentRadius.sm, PaymentRadius.md,
            PaymentRadius.lg, PaymentRadius.xl, PaymentRadius.full
        )
        for (i in 1 until values.size) {
            assertTrue(
                "Radius[$i] (${values[i]}) should be > Radius[${i - 1}] (${values[i - 1]})",
                values[i] > values[i - 1]
            )
        }
    }

    // ── PaymentElevation ────────────────────────────────────

    @Test
    fun elevationNone_is0dp() {
        assertEquals(0f, PaymentElevation.none.value, 0.001f)
    }

    @Test
    fun elevationXs_is1dp() {
        assertEquals(1f, PaymentElevation.xs.value, 0.001f)
    }

    @Test
    fun elevationSm_is2dp() {
        assertEquals(2f, PaymentElevation.sm.value, 0.001f)
    }

    @Test
    fun elevationMd_is4dp() {
        assertEquals(4f, PaymentElevation.md.value, 0.001f)
    }

    @Test
    fun elevationLg_is8dp() {
        assertEquals(8f, PaymentElevation.lg.value, 0.001f)
    }

    @Test
    fun elevationXl_is16dp() {
        assertEquals(16f, PaymentElevation.xl.value, 0.001f)
    }

    @Test
    fun elevationScale_hasSixSteps() {
        val getterCount = PaymentElevation::class.java.declaredMethods.count { it.name.startsWith("get") }
        assertEquals(6, getterCount)
    }

    @Test
    fun elevationValues_areMonotonicallyIncreasing() {
        val values = listOf(
            PaymentElevation.none, PaymentElevation.xs, PaymentElevation.sm,
            PaymentElevation.md, PaymentElevation.lg, PaymentElevation.xl
        )
        for (i in 1 until values.size) {
            assertTrue(
                "Elevation[$i] (${values[i]}) should be >= Elevation[${i - 1}] (${values[i - 1]})",
                values[i] >= values[i - 1]
            )
        }
    }

    // ── PaymentTypeSize ─────────────────────────────────────

    @Test
    fun typeSizeDisplay_is32sp() {
        assertEquals(32f, PaymentTypeSize.display.value, 0.001f)
    }

    @Test
    fun typeSizeHeadline_is24sp() {
        assertEquals(24f, PaymentTypeSize.headline.value, 0.001f)
    }

    @Test
    fun typeSizeTitle_is20sp() {
        assertEquals(20f, PaymentTypeSize.title.value, 0.001f)
    }

    @Test
    fun typeSizeBody_is16sp() {
        assertEquals(16f, PaymentTypeSize.body.value, 0.001f)
    }

    @Test
    fun typeSizeLabel_is14sp() {
        assertEquals(14f, PaymentTypeSize.label.value, 0.001f)
    }

    @Test
    fun typeSizeCaption_is12sp() {
        assertEquals(12f, PaymentTypeSize.caption.value, 0.001f)
    }

    @Test
    fun typeSizeScale_hasSixSteps() {
        val getterCount = PaymentTypeSize::class.java.declaredMethods.count { it.name.startsWith("get") }
        assertEquals(6, getterCount)
    }

    @Test
    fun typeSizeValues_areMonotonicallyDecreasing() {
        val values = listOf(
            PaymentTypeSize.display, PaymentTypeSize.headline, PaymentTypeSize.title,
            PaymentTypeSize.body, PaymentTypeSize.label, PaymentTypeSize.caption
        )
        for (i in 1 until values.size) {
            assertTrue(
                "TypeSize[$i] (${values[i]}) should be < TypeSize[${i - 1}] (${values[i - 1]})",
                values[i] < values[i - 1]
            )
        }
    }

    // ── Cross-token consistency ─────────────────────────────

    @Test
    fun spacingSmMatchesRadiusSm() {
        assertEquals(PaymentSpacing.sm.value, PaymentRadius.sm.value, 0.001f)
    }

    @Test
    fun spacingLgMatchesRadiusLg() {
        assertEquals(PaymentSpacing.lg.value, PaymentRadius.lg.value, 0.001f)
    }

    @Test
    fun spacingXlMatchesRadiusXl() {
        assertEquals(PaymentSpacing.xl.value, PaymentRadius.xl.value, 0.001f)
    }
}
