package com.landoulsi.fraud

import com.landoulsi.fraud.category.FraudCategory
import com.landoulsi.fraud.severity.SignalSeverity
import com.landoulsi.fraud.model.FraudSignal
import com.landoulsi.fraud.risk.FraudRiskScore
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for FraudSignal data class.
 */
class FraudSignalTest {

    @Test
    fun testFraudSignalCreation() {
        // Arrange
        val category = FraudCategory.ROOT_DETECTION
        val severity = SignalSeverity.HIGH
        val description = "Root detected"

        // Act
        val signal = FraudSignal(
            category = category,
            severity = severity,
            isSuspicious = true,
            description = description
        )

        // Assert
        assertEquals(category, signal.category)
        assertEquals(severity, signal.severity)
        assertTrue(signal.isSuspicious)
        assertEquals(description, signal.description)
        assertNotNull(signal.timestamp)
    }

    @Test
    fun testFraudSignalDefaultTimestamp() {
        // Act
        val signal = FraudSignal(
            category = FraudCategory.EMULATOR_DETECTION,
            severity = SignalSeverity.LOW,
            isSuspicious = false,
            description = "Test"
        )

        // Assert
        assertTrue(signal.timestamp > 0L)
    }

    @Test
    fun testFraudSignalCopy() {
        // Arrange
        val original = FraudSignal(
            category = FraudCategory.ROOT_DETECTION,
            severity = SignalSeverity.HIGH,
            isSuspicious = true,
            description = "Original"
        )

        // Act
        val copy = original.copy(description = "Copy")

        // Assert
        assertEquals("Copy", copy.description)
        assertEquals(original.category, copy.category)
        assertEquals(original.severity, copy.severity)
        assertEquals(original.isSuspicious, copy.isSuspicious)
        assertEquals(original.timestamp, copy.timestamp)
    }
}
