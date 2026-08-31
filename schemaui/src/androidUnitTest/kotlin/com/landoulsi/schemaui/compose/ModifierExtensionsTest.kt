package com.landoulsi.schemaui.compose

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ModifierExtensionsTest {

    @Test
    fun hexToComposeColorParsesValid6DigitHex() {
        val color = "FF0000".hexToComposeColor()
        assertNotNull(color)
        assertEquals(Color(0xFFFF0000), color)
    }

    @Test
    fun hexToComposeColorParsesValid8DigitHex() {
        val color = "8000FF00".hexToComposeColor()
        assertNotNull(color)
        assertEquals(Color(0x8000FF00), color)
    }

    @Test
    fun hexToComposeColorReturnsNullForInvalidHex() {
        assertNull("".hexToComposeColor())
        assertNull("XYZ123".hexToComposeColor())
        assertNull("12345".hexToComposeColor())
    }
}
