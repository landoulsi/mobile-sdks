package com.landoulsi.integrity.mocklocation

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.SignalSeverity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MockLocationDetectionEvaluatorTest {

    private class FakeMockLocationCheckContext(
        private val mockLocationAppSet: Boolean = false,
        private val mockProviderActive: Boolean = false,
        private val developerMockSettingEnabled: Boolean = false,
        private val installedPackages: Set<String> = emptySet(),
        private val recentLocations: List<LocationSample> = emptyList(),
        private val shouldThrowOnMockLocationAppSet: Boolean = false,
        private val shouldThrowOnMockProviderActive: Boolean = false,
        private val shouldThrowOnDeveloperMockSetting: Boolean = false,
        private val shouldThrowOnPackageInstalled: Boolean = false,
        private val shouldThrowOnRecentLocations: Boolean = false,
    ) : MockLocationCheckContext {

        override fun isMockLocationAppSet(): Boolean {
            if (shouldThrowOnMockLocationAppSet) throw IllegalStateException("Simulated isMockLocationAppSet error")
            return mockLocationAppSet
        }

        override fun isMockProviderActive(): Boolean {
            if (shouldThrowOnMockProviderActive) throw IllegalStateException("Simulated isMockProviderActive error")
            return mockProviderActive
        }

        override fun isDeveloperMockSettingEnabled(): Boolean {
            if (shouldThrowOnDeveloperMockSetting) throw IllegalStateException("Simulated isDeveloperMockSettingEnabled error")
            return developerMockSettingEnabled
        }

        override fun isPackageInstalled(packageName: String): Boolean {
            if (shouldThrowOnPackageInstalled) throw IllegalStateException("Simulated isPackageInstalled error")
            return packageName in installedPackages
        }

        override fun getRecentLocations(): List<LocationSample> {
            if (shouldThrowOnRecentLocations) throw IllegalStateException("Simulated getRecentLocations error")
            return recentLocations
        }
    }

    @Test
    fun cleanDeviceProducesNoSignals() = runTest {
        val context = FakeMockLocationCheckContext(
            recentLocations = listOf(
                LocationSample(
                    latitude = 37.7749,
                    longitude = -122.4194,
                    speed = 1.2f,
                    accuracy = 5.0f,
                    timestampMs = 1000L,
                    isMock = false,
                ),
            ),
        )
        val evaluator = MockLocationDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
        assertEquals(IntegrityCategory.MOCK_LOCATION, evaluator.category)
    }

    @Test
    fun mockLocationFlagDetectedWhenSampleIsMock() = runTest {
        val context = FakeMockLocationCheckContext(
            recentLocations = listOf(
                LocationSample(
                    latitude = 37.7749,
                    longitude = -122.4194,
                    timestampMs = 1000L,
                    isMock = true,
                ),
            ),
        )
        val evaluator = MockLocationDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val mockFlagSignals = signals.filter { it.id == MockLocationSignal.MOCK_FLAG_ACTIVE }
        assertEquals(1, mockFlagSignals.size)
        val signal = mockFlagSignals.first()
        assertEquals(IntegrityCategory.MOCK_LOCATION, signal.category)
        assertEquals(SignalSeverity.HIGH, signal.severity)
        assertEquals(1.0, signal.confidence)
        assertEquals(MockLocationSignal.Check.MOCK_FLAG, signal.metadata["check"])
    }

    @Test
    fun mockProviderActiveDetection() = runTest {
        val context = FakeMockLocationCheckContext(mockProviderActive = true)
        val evaluator = MockLocationDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val providerSignals = signals.filter { it.id == MockLocationSignal.MOCK_PROVIDER_ACTIVE }
        assertEquals(1, providerSignals.size)
        val signal = providerSignals.first()
        assertEquals(SignalSeverity.HIGH, signal.severity)
        assertEquals(MockLocationSignal.Check.MOCK_PROVIDER, signal.metadata["check"])
    }

    @Test
    fun developerMockSettingDetection() = runTest {
        val context = FakeMockLocationCheckContext(developerMockSettingEnabled = true)
        val evaluator = MockLocationDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val settingSignals = signals.filter { it.id == MockLocationSignal.DEVELOPER_MOCK_SETTING }
        assertEquals(1, settingSignals.size)
        val signal = settingSignals.first()
        assertEquals(SignalSeverity.MEDIUM, signal.severity)
        assertEquals(MockLocationSignal.Check.DEVELOPER_SETTING, signal.metadata["check"])
    }

    @Test
    fun mockAppSetDetection() = runTest {
        val context = FakeMockLocationCheckContext(mockLocationAppSet = true)
        val evaluator = MockLocationDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val settingSignals = signals.filter { it.id == MockLocationSignal.DEVELOPER_MOCK_SETTING }
        assertEquals(1, settingSignals.size)
        val signal = settingSignals.first()
        assertEquals(SignalSeverity.MEDIUM, signal.severity)
        assertEquals(MockLocationSignal.Check.DEVELOPER_SETTING, signal.metadata["check"])
    }

    @Test
    fun knownMockAppDetection() = runTest {
        val context = FakeMockLocationCheckContext(
            installedPackages = setOf("com.lexa.fakegps", "com.fly.gps"),
        )
        val evaluator = MockLocationDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val appSignals = signals.filter { it.id == MockLocationSignal.MOCK_APP_INSTALLED }
        assertEquals(2, appSignals.size)
        assertTrue(appSignals.all { it.severity == SignalSeverity.HIGH })
        assertTrue(appSignals.any { it.metadata["package"] == "com.lexa.fakegps" })
        assertTrue(appSignals.any { it.metadata["package"] == "com.fly.gps" })
    }

    @Test
    fun velocityAnomalyDetectionOnSupersonicMovement() = runTest {
        // From San Francisco (37.7749, -122.4194) to San Jose (37.3382, -121.8863): ~67 km apart
        // Within 20 seconds -> ~3350 m/s (well above 350 m/s threshold)
        val context = FakeMockLocationCheckContext(
            recentLocations = listOf(
                LocationSample(latitude = 37.7749, longitude = -122.4194, timestampMs = 10000L),
                LocationSample(latitude = 37.3382, longitude = -121.8863, timestampMs = 30000L),
            ),
        )
        val evaluator = MockLocationDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val velocitySignals = signals.filter { it.id == MockLocationSignal.LOCATION_ANOMALY_VELOCITY }
        assertEquals(1, velocitySignals.size)
        val signal = velocitySignals.first()
        assertEquals(SignalSeverity.HIGH, signal.severity)
        assertEquals(MockLocationSignal.Check.VELOCITY_ANOMALY, signal.metadata["check"])
        assertNotNull(signal.metadata["speed_mps"])
    }

    @Test
    fun jumpAnomalyDetectionOnInstantaneousTeleportation() = runTest {
        // Jump from New York (40.7128, -74.0060) to London (51.5074, -0.1278): ~5500 km in 2 seconds
        val context = FakeMockLocationCheckContext(
            recentLocations = listOf(
                LocationSample(latitude = 40.7128, longitude = -74.0060, timestampMs = 1000L),
                LocationSample(latitude = 51.5074, longitude = -0.1278, timestampMs = 3000L),
            ),
        )
        val evaluator = MockLocationDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val jumpSignals = signals.filter { it.id == MockLocationSignal.LOCATION_ANOMALY_JUMP }
        assertEquals(1, jumpSignals.size)
        val signal = jumpSignals.first()
        assertEquals(SignalSeverity.CRITICAL, signal.severity)
        assertEquals(MockLocationSignal.Check.JUMP_ANOMALY, signal.metadata["check"])
    }

    @Test
    fun frozenLocationAnomalyDetection() = runTest {
        val context = FakeMockLocationCheckContext(
            recentLocations = listOf(
                LocationSample(latitude = 37.774900, longitude = -122.419400, speed = 0.0f, accuracy = 0.0f, timestampMs = 1000L),
                LocationSample(latitude = 37.774900, longitude = -122.419400, speed = 0.0f, accuracy = 0.0f, timestampMs = 2000L),
                LocationSample(latitude = 37.774900, longitude = -122.419400, speed = 0.0f, accuracy = 0.0f, timestampMs = 3000L),
                LocationSample(latitude = 37.774900, longitude = -122.419400, speed = 0.0f, accuracy = 0.0f, timestampMs = 4000L),
                LocationSample(latitude = 37.774900, longitude = -122.419400, speed = 0.0f, accuracy = 0.0f, timestampMs = 5000L),
            ),
        )
        val evaluator = MockLocationDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val frozenSignals = signals.filter { it.id == MockLocationSignal.LOCATION_ANOMALY_FROZEN }
        assertEquals(1, frozenSignals.size)
        val signal = frozenSignals.first()
        assertEquals(SignalSeverity.LOW, signal.severity)
        assertEquals(MockLocationSignal.Check.FROZEN_ANOMALY, signal.metadata["check"])
    }

    @Test
    fun frozenLocationNotDetectedUnderThresholdCount() = runTest {
        val context = FakeMockLocationCheckContext(
            recentLocations = listOf(
                LocationSample(latitude = 37.7749, longitude = -122.4194, speed = 0.0f, accuracy = 0.0f, timestampMs = 1000L),
                LocationSample(latitude = 37.7749, longitude = -122.4194, speed = 0.0f, accuracy = 0.0f, timestampMs = 2000L),
                LocationSample(latitude = 37.7749, longitude = -122.4194, speed = 0.0f, accuracy = 0.0f, timestampMs = 3000L),
            ),
        )
        val evaluator = MockLocationDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == MockLocationSignal.LOCATION_ANOMALY_FROZEN })
    }

    @Test
    fun normalPlausibleMovementProducesNoAnomalies() = runTest {
        // Walking movement: ~1.4 m/s over 5 seconds (7 meters)
        val context = FakeMockLocationCheckContext(
            recentLocations = listOf(
                LocationSample(latitude = 37.774900, longitude = -122.419400, speed = 1.4f, accuracy = 4.0f, timestampMs = 1000L),
                LocationSample(latitude = 37.774950, longitude = -122.419420, speed = 1.4f, accuracy = 4.5f, timestampMs = 6000L),
            ),
        )
        val evaluator = MockLocationDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
    }

    @Test
    fun singleLocationFixProducesNoAnomalies() = runTest {
        val context = FakeMockLocationCheckContext(
            recentLocations = listOf(
                LocationSample(latitude = 37.7749, longitude = -122.4194, timestampMs = 1000L),
            ),
        )
        val evaluator = MockLocationDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
    }

    @Test
    fun emptyLocationsProducesNoAnomalies() = runTest {
        val context = FakeMockLocationCheckContext(recentLocations = emptyList())
        val evaluator = MockLocationDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
    }

    @Test
    fun evaluatorFaultToleranceWhenContextOperationsThrow() = runTest {
        val context = FakeMockLocationCheckContext(
            shouldThrowOnMockLocationAppSet = true,
            shouldThrowOnMockProviderActive = true,
            shouldThrowOnDeveloperMockSetting = true,
            shouldThrowOnPackageInstalled = true,
            shouldThrowOnRecentLocations = true,
        )
        val evaluator = MockLocationDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
    }
}
