package com.landoulsi.fraud.emulator

import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.SignalSeverity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmulatorDetectionEvaluatorTest {

    private class FakeEmulatorCheckContext(
        private val existingFiles: Set<String> = emptySet(),
        private val installedPackages: Set<String> = emptySet(),
        private val fingerprint: String = "samsung/a53xxx/a53x:13/TP1A.220624.014/N123:user/release-keys",
        private val model: String = "SM-A536B",
        private val manufacturer: String = "samsung",
        private val brand: String = "samsung",
        private val device: String = "a53x",
        private val product: String = "a53xxx",
        private val hardware: String = "qcom",
        private val sensorCount: Int? = 12,
        private val shouldThrowOnFileExists: Boolean = false,
        private val shouldThrowOnPackageInstalled: Boolean = false,
        private val shouldThrowOnSensorCount: Boolean = false,
    ) : EmulatorCheckContext {
        override fun fileExists(path: String): Boolean {
            if (shouldThrowOnFileExists) throw IllegalStateException("Simulated fileExists error")
            return path in existingFiles
        }

        override fun isPackageInstalled(packageName: String): Boolean {
            if (shouldThrowOnPackageInstalled) throw IllegalStateException("Simulated isPackageInstalled error")
            return packageName in installedPackages
        }

        override fun getBuildFingerprint(): String = fingerprint
        override fun getBuildModel(): String = model
        override fun getBuildManufacturer(): String = manufacturer
        override fun getBuildBrand(): String = brand
        override fun getBuildDevice(): String = device
        override fun getBuildProduct(): String = product
        override fun getBuildHardware(): String = hardware

        override fun getSensorCount(): Int? {
            if (shouldThrowOnSensorCount) throw IllegalStateException("Simulated getSensorCount error")
            return sensorCount
        }
    }

    @Test
    fun realDeviceProducesNoSignals() = runTest {
        val context = FakeEmulatorCheckContext()
        val evaluator = EmulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
        assertEquals(FraudCategory.VIRTUAL_OS_OR_EMULATOR, evaluator.category)
    }

    @Test
    fun genericAvdFingerprintDetection() = runTest {
        val context = FakeEmulatorCheckContext(
            fingerprint = "google/sdk_gphone64_x86_64/emu64x:14/UE1A.230829.036/1234567:userdebug/test-keys",
            model = "sdk_gphone64_x86_64",
            manufacturer = "Google",
            brand = "google",
            device = "emu64x",
            product = "sdk_gphone64_x86_64",
        )
        val evaluator = EmulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val buildSignals = signals.filter { it.id == EmulatorSignal.BUILD_GENERIC }
        assertEquals(1, buildSignals.size)
        assertEquals(SignalSeverity.MEDIUM, buildSignals.first().severity)
    }

    @Test
    fun genericBrandAndDeviceDetection() = runTest {
        val context = FakeEmulatorCheckContext(brand = "generic", device = "generic_x86")
        val evaluator = EmulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.any { it.id == EmulatorSignal.BUILD_GENERIC })
    }

    @Test
    fun singleWeakIndicatorYieldsLowerConfidenceThanMultiple() = runTest {
        val weakContext = FakeEmulatorCheckContext(manufacturer = "unknown")
        val strongContext = FakeEmulatorCheckContext(
            manufacturer = "unknown",
            brand = "generic",
            device = "generic_x86",
            product = "sdk_x86",
        )

        val weakSignal = EmulatorDetectionEvaluator(weakContext).evaluate()
            .first { it.id == EmulatorSignal.BUILD_GENERIC }
        val strongSignal = EmulatorDetectionEvaluator(strongContext).evaluate()
            .first { it.id == EmulatorSignal.BUILD_GENERIC }

        assertTrue(weakSignal.confidence < strongSignal.confidence)
    }

    @Test
    fun goldfishHardwareDetection() = runTest {
        val context = FakeEmulatorCheckContext(hardware = "goldfish")
        val evaluator = EmulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val hardwareSignals = signals.filter { it.id == EmulatorSignal.BUILD_HARDWARE }
        assertEquals(1, hardwareSignals.size)
        assertEquals(SignalSeverity.HIGH, hardwareSignals.first().severity)
    }

    @Test
    fun qemuFileDetection() = runTest {
        val context = FakeEmulatorCheckContext(
            existingFiles = setOf("/dev/qemu_pipe", "/system/bin/qemu-props"),
        )
        val evaluator = EmulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val qemuSignals = signals.filter { it.id == EmulatorSignal.QEMU_FILE }
        assertEquals(2, qemuSignals.size)
        assertTrue(qemuSignals.all { it.severity == SignalSeverity.HIGH })
    }

    @Test
    fun emulatorManagementAppDetection() = runTest {
        val context = FakeEmulatorCheckContext(
            installedPackages = setOf("com.bignox.app"),
        )
        val evaluator = EmulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val appSignals = signals.filter { it.id == EmulatorSignal.MANAGEMENT_APP }
        assertEquals(1, appSignals.size)
        assertEquals("com.bignox.app", appSignals.first().metadata["package"])
    }

    @Test
    fun zeroSensorsProducesSignal() = runTest {
        val context = FakeEmulatorCheckContext(sensorCount = 0)
        val evaluator = EmulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.any { it.id == EmulatorSignal.SENSOR_DEFICIT })
    }

    @Test
    fun nullSensorCountProducesNoSignal() = runTest {
        val context = FakeEmulatorCheckContext(sensorCount = null)
        val evaluator = EmulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == EmulatorSignal.SENSOR_DEFICIT })
    }

    @Test
    fun fewSensorsProducesNoSignal() = runTest {
        val context = FakeEmulatorCheckContext(sensorCount = 3)
        val evaluator = EmulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == EmulatorSignal.SENSOR_DEFICIT })
    }

    @Test
    fun mixedSignalsDetection() = runTest {
        val context = FakeEmulatorCheckContext(
            fingerprint = "generic/sdk_gphone_x86/generic_x86:13/TE1A.220922.010/1234:userdebug/test-keys",
            brand = "generic",
            device = "generic_x86",
            product = "sdk_gphone_x86",
            hardware = "ranchu",
            existingFiles = setOf("/dev/qemu_pipe"),
            sensorCount = 0,
        )
        val evaluator = EmulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.any { it.id == EmulatorSignal.BUILD_GENERIC })
        assertTrue(signals.any { it.id == EmulatorSignal.BUILD_HARDWARE })
        assertTrue(signals.any { it.id == EmulatorSignal.QEMU_FILE })
        assertTrue(signals.any { it.id == EmulatorSignal.SENSOR_DEFICIT })
        assertTrue(signals.all { it.category == FraudCategory.VIRTUAL_OS_OR_EMULATOR })
    }

    @Test
    fun signalMetadataContainsCheckIdentifier() = runTest {
        val context = FakeEmulatorCheckContext(
            existingFiles = setOf("/dev/qemu_pipe"),
        )
        val evaluator = EmulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val qemuSignal = signals.first { it.id == EmulatorSignal.QEMU_FILE }
        assertEquals(EmulatorSignal.Check.QEMU_FILE, qemuSignal.metadata["check"])
        assertEquals("/dev/qemu_pipe", qemuSignal.metadata["path"])
    }

    @Test
    fun evaluatorFaultToleranceWhenCheckThrows() = runTest {
        val context = FakeEmulatorCheckContext(
            existingFiles = setOf("/dev/qemu_pipe"),
            shouldThrowOnPackageInstalled = true,
            shouldThrowOnSensorCount = true,
        )
        val evaluator = EmulatorDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        // QEMU file signal should still be returned even when other checks throw
        assertTrue(signals.any { it.id == EmulatorSignal.QEMU_FILE })
        assertTrue(signals.none { it.id == EmulatorSignal.MANAGEMENT_APP })
        assertTrue(signals.none { it.id == EmulatorSignal.SENSOR_DEFICIT })
    }
}
