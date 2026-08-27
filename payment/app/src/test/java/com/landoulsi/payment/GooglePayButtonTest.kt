package com.landoulsi.payment

import com.landoulsi.payment.ui.GooglePayButtonTheme
import com.landoulsi.payment.ui.GooglePayButtonType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GooglePayButtonTest {

    @Test
    fun testButtonTypeResourceMappings() {
        assertEquals(R.string.gpay_buy_with, GooglePayButtonType.BUY.labelResId)
        assertEquals(R.string.gpay_pay_with, GooglePayButtonType.PAY.labelResId)
        assertEquals(R.string.gpay_checkout_with, GooglePayButtonType.CHECKOUT.labelResId)
        assertEquals(R.string.gpay_order_with, GooglePayButtonType.ORDER.labelResId)
        assertEquals(R.string.gpay_subscribe_with, GooglePayButtonType.SUBSCRIBE.labelResId)
        assertNull(GooglePayButtonType.PLAIN.labelResId)
    }

    @Test
    fun testButtonThemes() {
        val themes = GooglePayButtonTheme.values()
        assertEquals(3, themes.size)
        assertNotNull(GooglePayButtonTheme.DARK)
        assertNotNull(GooglePayButtonTheme.LIGHT)
        assertNotNull(GooglePayButtonTheme.DYNAMIC)
    }

    @Test
    fun testButtonTypeCount() {
        assertEquals(6, GooglePayButtonType.values().size)
    }

    @Test
    fun testButtonTypeValueOf() {
        assertEquals(GooglePayButtonType.BUY, GooglePayButtonType.valueOf("BUY"))
        assertEquals(GooglePayButtonType.PAY, GooglePayButtonType.valueOf("PAY"))
        assertEquals(GooglePayButtonType.CHECKOUT, GooglePayButtonType.valueOf("CHECKOUT"))
        assertEquals(GooglePayButtonType.ORDER, GooglePayButtonType.valueOf("ORDER"))
        assertEquals(GooglePayButtonType.SUBSCRIBE, GooglePayButtonType.valueOf("SUBSCRIBE"))
        assertEquals(GooglePayButtonType.PLAIN, GooglePayButtonType.valueOf("PLAIN"))
    }

    @Test
    fun testButtonThemeValueOf() {
        assertEquals(GooglePayButtonTheme.DARK, GooglePayButtonTheme.valueOf("DARK"))
        assertEquals(GooglePayButtonTheme.LIGHT, GooglePayButtonTheme.valueOf("LIGHT"))
        assertEquals(GooglePayButtonTheme.DYNAMIC, GooglePayButtonTheme.valueOf("DYNAMIC"))
    }

    @Test
    fun testButtonTypeEntries() {
        val entries = GooglePayButtonType.entries
        assertEquals(6, entries.size)
        assertTrue(entries.contains(GooglePayButtonType.BUY))
        assertTrue(entries.contains(GooglePayButtonType.PAY))
        assertTrue(entries.contains(GooglePayButtonType.CHECKOUT))
        assertTrue(entries.contains(GooglePayButtonType.ORDER))
        assertTrue(entries.contains(GooglePayButtonType.SUBSCRIBE))
        assertTrue(entries.contains(GooglePayButtonType.PLAIN))
    }

    @Test
    fun testButtonThemeEntries() {
        val entries = GooglePayButtonTheme.entries
        assertEquals(3, entries.size)
        assertTrue(entries.contains(GooglePayButtonTheme.DARK))
        assertTrue(entries.contains(GooglePayButtonTheme.LIGHT))
        assertTrue(entries.contains(GooglePayButtonTheme.DYNAMIC))
    }

    @Test
    fun testButtonTypeNames() {
        assertEquals("BUY", GooglePayButtonType.BUY.name)
        assertEquals("PAY", GooglePayButtonType.PAY.name)
        assertEquals("CHECKOUT", GooglePayButtonType.CHECKOUT.name)
        assertEquals("ORDER", GooglePayButtonType.ORDER.name)
        assertEquals("SUBSCRIBE", GooglePayButtonType.SUBSCRIBE.name)
        assertEquals("PLAIN", GooglePayButtonType.PLAIN.name)
    }

    @Test
    fun testButtonThemeNames() {
        assertEquals("DARK", GooglePayButtonTheme.DARK.name)
        assertEquals("LIGHT", GooglePayButtonTheme.LIGHT.name)
        assertEquals("DYNAMIC", GooglePayButtonTheme.DYNAMIC.name)
    }

    @Test
    fun testButtonTypeOrdinal() {
        assertEquals(0, GooglePayButtonType.BUY.ordinal)
        assertEquals(1, GooglePayButtonType.PAY.ordinal)
        assertEquals(2, GooglePayButtonType.CHECKOUT.ordinal)
        assertEquals(3, GooglePayButtonType.ORDER.ordinal)
        assertEquals(4, GooglePayButtonType.SUBSCRIBE.ordinal)
        assertEquals(5, GooglePayButtonType.PLAIN.ordinal)
    }

    @Test
    fun testButtonThemeOrdinal() {
        assertEquals(0, GooglePayButtonTheme.DARK.ordinal)
        assertEquals(1, GooglePayButtonTheme.LIGHT.ordinal)
        assertEquals(2, GooglePayButtonTheme.DYNAMIC.ordinal)
    }
}
