package com.landoulsi.integrity.simulator

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.SignalSeverity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SimulatorDetectionEvaluatorTest {

    private class FakeSimulatorCheckContext(
        private val environment: Map<String, String> = emptyMap(),
        private val bundlePathIsSimulator: Boolean = false,
        private val shouldThrowOnEnvironment: Boolean = false,
        private val shouldThrowOnBundlePath: Boolean = false,
    ) : SimulatorCheckContext {
        override fun getEnvironmentVariable(name: String): String? {
            if (shouldThrowOnEnvironment) throw IllegalStateException("Simulated environment error")
            return environment[name]
        }

        override fun isBundlePathWithinCoreSimulator(): Boolean {
            if (shouldThrowOnBundlePath) throw IllegalStateException("Simulated bundle path error")
            return bundlePathIsSimulator
        }
    }

    @Test
    fun realDeviceProducesNoSignals() = runTest {
        val context = FakeSimulatorCheckContext()
        val evaluator = SimulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
        assertEquals(IntegrityCategory.VIRTUAL_OS_OR_EMULATOR, evaluator.category)
    }

    @Test
    fun simulatorEnvironmentDetection() = runTest {
        val context = FakeSimulatorCheckContext(
            environment = mapOf("SIMULATOR_DEVICE_NAME" to "iPhone 15 Pro"),
        )
        val evaluator = SimulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val envSignals = signals.filter { it.id == SimulatorSignal.ENVIRONMENT }
        assertEquals(1, envSignals.size)
        assertEquals(SignalSeverity.CRITICAL, envSignals.first().severity)
    }

    @Test
    fun blankEnvironmentValueIsIgnored() = runTest {
        val context = FakeSimulatorCheckContext(
            environment = mapOf("SIMULATOR_DEVICE_NAME" to ""),
        )
        val evaluator = SimulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == SimulatorSignal.ENVIRONMENT })
    }

    @Test
    fun bundlePathDetection() = runTest {
        val context = FakeSimulatorCheckContext(bundlePathIsSimulator = true)
        val evaluator = SimulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val bundleSignals = signals.filter { it.id == SimulatorSignal.BUNDLE_PATH }
        assertEquals(1, bundleSignals.size)
        assertEquals(SignalSeverity.HIGH, bundleSignals.first().severity)
    }

    @Test
    fun metadataAndDetailsNeverContainRawPath() = runTest {
        val context = FakeSimulatorCheckContext(bundlePathIsSimulator = true)
        val evaluator = SimulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val bundleSignal = signals.first { it.id == SimulatorSignal.BUNDLE_PATH }
        assertTrue(bundleSignal.metadata.values.none { it.contains("/Users/") })
        assertFalse(bundleSignal.details.contains("/Users/"))
    }

    @Test
    fun mixedSignalsDetection() = runTest {
        val context = FakeSimulatorCheckContext(
            environment = mapOf("SIMULATOR_UDID" to "ABC-123"),
            bundlePathIsSimulator = true,
        )
        val evaluator = SimulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(2, signals.size)
        assertTrue(signals.all { it.category == IntegrityCategory.VIRTUAL_OS_OR_EMULATOR })
    }

    @Test
    fun evaluatorFaultToleranceWhenCheckThrows() = runTest {
        val context = FakeSimulatorCheckContext(
            environment = mapOf("SIMULATOR_ROOT" to "/some/path"),
            shouldThrowOnBundlePath = true,
        )
        val evaluator = SimulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        // Environment signal should still be returned even when the bundle path check throws
        assertEquals(1, signals.size)
        assertEquals(SimulatorSignal.ENVIRONMENT, signals.first().id)
    }

    @Test
    fun environmentThrowsHandledSafely() = runTest {
        val context = FakeSimulatorCheckContext(
            shouldThrowOnEnvironment = true,
            bundlePathIsSimulator = true,
        )
        val evaluator = SimulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        // Bundle path signal should still be returned even when environment lookup throws
        assertEquals(1, signals.size)
        assertEquals(SimulatorSignal.BUNDLE_PATH, signals.first().id)
    }
}
