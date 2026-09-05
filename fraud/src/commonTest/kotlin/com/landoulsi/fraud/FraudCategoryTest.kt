package com.landoulsi.fraud

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for FraudCategory enum.
 */
class FraudCategoryTest {

    @Test
    fun testFraudCategoryValues() {
        // Assert
        assertEquals(16, FraudCategory.values().size)
        assertTrue(FraudCategory.values().contains(FraudCategory.ROOT_DETECTION))
        assertTrue(FraudCategory.values().contains(FraudCategory.JAILBREAK_DETECTION))
        assertTrue(FraudCategory.values().contains(FraudCategory.EMULATOR_DETECTION))
        assertTrue(FraudCategory.values().contains(FraudCategory.FRIDA_HOOKING))
        assertTrue(FraudCategory.values().contains(FraudCategory.XPOSED_HOOKING))
        assertTrue(FraudCategory.values().contains(FraudCategory.SUBSTRATE_HOOKING))
    }

    @Test
    fun testFraudCategoryName() {
        // Act & Assert
        assertEquals("ROOT_DETECTION", FraudCategory.ROOT_DETECTION.name)
        assertEquals("JAILBREAK_DETECTION", FraudCategory.JAILBREAK_DETECTION.name)
        assertEquals("EMULATOR_DETECTION", FraudCategory.EMULATOR_DETECTION.name)
        assertEquals("FRIDA_HOOKING", FraudCategory.FRIDA_HOOKING.name)
    }
}
