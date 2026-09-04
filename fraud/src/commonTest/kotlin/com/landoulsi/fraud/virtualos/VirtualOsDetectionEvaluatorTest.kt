package com.landoulsi.fraud.virtualos

import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.SignalSeverity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VirtualOsDetectionEvaluatorTest {

    private class FakeVirtualOsCheckContext(
        private val installedPackages: Set<String> = emptySet(),
        private val ownPackageKnown: Boolean = true,
        private val selfReportedUid: Int = 10123,
        private val packageManagerUid: Int? = 10123,
        private val dataDirPath: String = "/data/user/0/com.example.app",
        private val ownPackageName: String = "com.example.app",
        private val shouldThrowOnPackageInstalled: Boolean = false,
        private val shouldThrowOnOwnPackageKnown: Boolean = false,
        private val shouldThrowOnSelfReportedUid: Boolean = false,
        private val shouldThrowOnPackageManagerUid: Boolean = false,
    ) : VirtualOsCheckContext {
        override fun isPackageInstalled(packageName: String): Boolean {
            if (shouldThrowOnPackageInstalled) throw IllegalStateException("Simulated isPackageInstalled error")
            return packageName in installedPackages
        }

        override fun isOwnPackageKnownToPackageManager(): Boolean {
            if (shouldThrowOnOwnPackageKnown) throw IllegalStateException("Simulated ownPackageKnown error")
            return ownPackageKnown
        }

        override fun getSelfReportedUid(): Int {
            if (shouldThrowOnSelfReportedUid) throw IllegalStateException("Simulated selfReportedUid error")
            return selfReportedUid
        }

        override fun getPackageManagerUid(): Int? {
            if (shouldThrowOnPackageManagerUid) throw IllegalStateException("Simulated packageManagerUid error")
            return packageManagerUid
        }

        override fun getDataDirPath(): String = dataDirPath

        override fun getOwnPackageName(): String = ownPackageName
    }

    @Test
    fun realAppProducesNoSignals() = runTest {
        val context = FakeVirtualOsCheckContext()
        val evaluator = VirtualOsDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
        assertEquals(FraudCategory.VIRTUAL_OS_OR_EMULATOR, evaluator.category)
    }

    @Test
    fun packageUnresolvableDetection() = runTest {
        val context = FakeVirtualOsCheckContext(ownPackageKnown = false, packageManagerUid = null)
        val evaluator = VirtualOsDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val packageSignals = signals.filter { it.id == VirtualOsSignal.PACKAGE_UNRESOLVABLE }
        assertEquals(1, packageSignals.size)
        assertEquals(SignalSeverity.CRITICAL, packageSignals.first().severity)
    }

    @Test
    fun uidMismatchDetection() = runTest {
        val context = FakeVirtualOsCheckContext(packageManagerUid = 10123, selfReportedUid = 10500)
        val evaluator = VirtualOsDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val uidSignals = signals.filter { it.id == VirtualOsSignal.UID_MISMATCH }
        assertEquals(1, uidSignals.size)
        assertEquals(SignalSeverity.HIGH, uidSignals.first().severity)
    }

    @Test
    fun uidMismatchSkippedWhenPackageManagerUidUnavailable() = runTest {
        val context = FakeVirtualOsCheckContext(packageManagerUid = null, ownPackageKnown = true)
        val evaluator = VirtualOsDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == VirtualOsSignal.UID_MISMATCH })
    }

    @Test
    fun nestedContainerDataDirDetection() = runTest {
        // Real VirtualApp-style shape: the guest's data dir is nested under the container's own
        // sandbox and *ends with* the guest package name, so a naive suffix check would miss it.
        val context = FakeVirtualOsCheckContext(
            dataDirPath = "/data/data/com.pspace.vandroid/virtual/data/user/0/com.example.app",
            ownPackageName = "com.example.app",
        )
        val evaluator = VirtualOsDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val dataDirSignals = signals.filter { it.id == VirtualOsSignal.DATA_DIR_ANOMALY }
        assertEquals(1, dataDirSignals.size)
        assertEquals(SignalSeverity.HIGH, dataDirSignals.first().severity)
    }

    @Test
    fun unrelatedDataDirDetection() = runTest {
        val context = FakeVirtualOsCheckContext(
            dataDirPath = "/data/user/0/com.pspace.vandroid",
            ownPackageName = "com.example.app",
        )
        val evaluator = VirtualOsDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.any { it.id == VirtualOsSignal.DATA_DIR_ANOMALY })
    }

    @Test
    fun legacyDataDataShapeProducesNoSignal() = runTest {
        val context = FakeVirtualOsCheckContext(
            dataDirPath = "/data/data/com.example.app",
            ownPackageName = "com.example.app",
        )
        val evaluator = VirtualOsDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == VirtualOsSignal.DATA_DIR_ANOMALY })
    }

    @Test
    fun dataDirMatchingOwnPackageProducesNoSignal() = runTest {
        val context = FakeVirtualOsCheckContext(
            dataDirPath = "/data/user/0/com.example.app",
            ownPackageName = "com.example.app",
        )
        val evaluator = VirtualOsDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == VirtualOsSignal.DATA_DIR_ANOMALY })
    }

    @Test
    fun metadataAndDetailsNeverContainRawDataDirPath() = runTest {
        val context = FakeVirtualOsCheckContext(
            dataDirPath = "/data/data/com.pspace.vandroid/virtual/0/com.example.app-private",
            ownPackageName = "com.example.app",
        )
        val evaluator = VirtualOsDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val dataDirSignal = signals.first { it.id == VirtualOsSignal.DATA_DIR_ANOMALY }
        assertTrue(dataDirSignal.metadata.values.none { it.contains("/data/") })
        assertFalse(dataDirSignal.details.contains("/data/"))
    }

    @Test
    fun knownContainerAppDetection() = runTest {
        val context = FakeVirtualOsCheckContext(
            installedPackages = setOf("com.pspace.vandroid"),
        )
        val evaluator = VirtualOsDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val appSignals = signals.filter { it.id == VirtualOsSignal.KNOWN_CONTAINER_APP }
        assertEquals(1, appSignals.size)
        assertEquals("com.pspace.vandroid", appSignals.first().metadata["package"])
    }

    @Test
    fun mixedSignalsDetection() = runTest {
        val context = FakeVirtualOsCheckContext(
            ownPackageKnown = false,
            packageManagerUid = null,
            dataDirPath = "/data/data/com.pspace.vandroid/virtual/data/user/0/com.example.app",
            ownPackageName = "com.example.app",
            installedPackages = setOf("com.pspace.vandroid"),
        )
        val evaluator = VirtualOsDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.any { it.id == VirtualOsSignal.PACKAGE_UNRESOLVABLE })
        assertTrue(signals.any { it.id == VirtualOsSignal.DATA_DIR_ANOMALY })
        assertTrue(signals.any { it.id == VirtualOsSignal.KNOWN_CONTAINER_APP })
        assertTrue(signals.all { it.category == FraudCategory.VIRTUAL_OS_OR_EMULATOR })
    }

    @Test
    fun evaluatorFaultToleranceWhenCheckThrows() = runTest {
        val context = FakeVirtualOsCheckContext(
            installedPackages = setOf("com.pspace.vandroid"),
            shouldThrowOnOwnPackageKnown = true,
        )
        val evaluator = VirtualOsDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        // Known container app signal should still be returned even when other checks throw
        assertTrue(signals.any { it.id == VirtualOsSignal.KNOWN_CONTAINER_APP })
        assertTrue(signals.none { it.id == VirtualOsSignal.PACKAGE_UNRESOLVABLE })
    }
}
