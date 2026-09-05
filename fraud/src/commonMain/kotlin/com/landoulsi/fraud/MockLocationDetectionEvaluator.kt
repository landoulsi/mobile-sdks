package com.landoulsi.fraud

import com.landoulsi.fraud.category.FraudCategory
import com.landoulsi.fraud.severity.SignalSeverity
import com.landoulsi.fraud.model.FraudSignal

/**
 * Evaluates mock location detection on the device.
 * Checks for GPS spoofing, mock location settings, and location anomalies.
 */
class MockLocationDetectionEvaluator {

    /**
     * Evaluates whether mock location is active on the device.
     */
    suspend fun evaluate(): FraudSignal {
        return withContext(Dispatchers.Default) {
            // Check for mock location settings enabled
            val mockLocationEnabled = checkMockLocationSettings()
            
            // Check for GPS spoofing indicators
            val gpsSpoofing = checkGpsSpoofing()
            
            // Check for location anomalies (impossible speeds, etc.)
            val locationAnomalies = checkLocationAnomalies()
            
            // Check for mock GPS apps installed
            val mockGpsApps = checkMockGpsApps()
            
            val isSuspicious = mockLocationEnabled || gpsSpoofing || locationAnomalies || mockGpsApps
            
            val severity = if (isSuspicious) SignalSeverity.MEDIUM else SignalSeverity.LOW
            
            val description = when {
                mockLocationEnabled -> "Mock location settings are enabled"
                gpsSpoofing -> "GPS spoofing indicators detected"
                locationAnomalies -> "Location anomalies (impossible speeds/directions)"
                mockGpsApps -> "Mock GPS applications detected"
                else -> "No mock location detection"
            }
            
            FraudSignal(
                category = FraudCategory.MOCK_LOCATION_DETECTION,
                severity = severity,
                isSuspicious = isSuspicious,
                description = description
            )
        }
    }

    /**
     * Checks for mock location settings enabled.
     */
    private fun checkMockLocationSettings(): Boolean {
        // Check Android settings: ALLOW_MOCK_LOCATION
        // Check iOS location preferences
        return false // Stub
    }

    /**
     * Checks for GPS spoofing indicators.
     */
    private fun checkGpsSpoofing(): Boolean {
        // Check for GPS coordinate patterns
        // Check velocity/accuracy anomalies
        return false // Stub
    }

    /**
     * Checks for location anomalies.
     */
    private fun checkLocationAnomalies(): Boolean {
        // Check for impossible speeds or routes
        return false // Stub
    }

    /**
     * Checks for mock GPS apps installed.
     */
    private fun checkMockGpsApps(): Boolean {
        // Check for fake gps, GPS spoofing apps
        return false // Stub
    }
}
