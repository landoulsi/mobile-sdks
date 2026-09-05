package com.landoulsi.integrity.hooking.xposed

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.SignalSeverity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XposedDetectionEvaluatorTest {

    private class FakeXposedCheckContext(
        private val existingFiles: Set<String> = emptySet(),
        private val fileContents: Map<String, List<String>> = emptyMap(),
        private val installedPackages: Set<String> = emptySet(),
        private val loadableClasses: Set<String> = emptySet(),
        private val shouldThrowOnFileExists: Boolean = false,
        private val shouldThrowOnReadFile: Boolean = false,
        private val shouldThrowOnPackageInstalled: Boolean = false,
        private val shouldThrowOnClassLoadable: Boolean = false,
    ) : XposedCheckContext {
        override fun fileExists(path: String): Boolean {
            if (shouldThrowOnFileExists) throw IllegalStateException("Simulated fileExists error")
            return path in existingFiles
        }

        override fun readFileLines(path: String): List<String> {
            if (shouldThrowOnReadFile) throw IllegalStateException("Simulated readFileLines error")
            return fileContents[path] ?: emptyList()
        }

        override fun isPackageInstalled(packageName: String): Boolean {
            if (shouldThrowOnPackageInstalled) throw IllegalStateException("Simulated isPackageInstalled error")
            return packageName in installedPackages
        }

        override fun isClassLoadable(className: String): Boolean {
            if (shouldThrowOnClassLoadable) throw IllegalStateException("Simulated isClassLoadable error")
            return className in loadableClasses
        }
    }

    @Test
    fun cleanDeviceProducesNoSignals() = runTest {
        val context = FakeXposedCheckContext()
        val evaluator = XposedDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
        assertEquals(IntegrityCategory.HOOKING_OR_TAMPERING, evaluator.category)
        assertEquals(XposedSignal.all, evaluator.knownSignalIds)
    }

    @Test
    fun xposedFrameworkInstalledDetection() = runTest {
        val context = FakeXposedCheckContext(
            existingFiles = setOf("/system/framework/XposedBridge.jar"),
        )
        val evaluator = XposedDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val frameworkSignals = signals.filter { it.id == XposedSignal.XPOSED_FRAMEWORK_INSTALLED }
        assertEquals(1, frameworkSignals.size)
        assertEquals(SignalSeverity.HIGH, frameworkSignals.first().severity)
        assertEquals(1.0, frameworkSignals.first().confidence)
        assertEquals("/system/framework/XposedBridge.jar", frameworkSignals.first().metadata["path"])
        assertEquals(XposedSignal.Check.XPOSED_FRAMEWORK_INSTALLED, frameworkSignals.first().metadata["check"])
    }

    @Test
    fun xposedBridgeClassDetection() = runTest {
        val context = FakeXposedCheckContext(
            loadableClasses = setOf("de.robv.android.xposed.XposedBridge"),
        )
        val evaluator = XposedDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val bridgeSignals = signals.filter { it.id == XposedSignal.XPOSED_BRIDGE_CLASS }
        assertEquals(1, bridgeSignals.size)
        assertEquals(SignalSeverity.HIGH, bridgeSignals.first().severity)
        assertEquals("de.robv.android.xposed.XposedBridge", bridgeSignals.first().metadata["class"])
        assertEquals(XposedSignal.Check.XPOSED_BRIDGE_CLASS, bridgeSignals.first().metadata["check"])
    }

    @Test
    fun xposedInstallerAppDetection() = runTest {
        val context = FakeXposedCheckContext(
            installedPackages = setOf("org.lsposed.manager"),
        )
        val evaluator = XposedDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val installerSignals = signals.filter { it.id == XposedSignal.XPOSED_INSTALLER_APP }
        assertEquals(1, installerSignals.size)
        assertEquals(SignalSeverity.HIGH, installerSignals.first().severity)
        assertEquals("org.lsposed.manager", installerSignals.first().metadata["package"])
        assertEquals(XposedSignal.Check.XPOSED_INSTALLER_APP, installerSignals.first().metadata["check"])
    }

    @Test
    fun xposedModuleInstalledDetection() = runTest {
        val context = FakeXposedCheckContext(
            installedPackages = setOf("com.ceco.marshmallow.gravitybox"),
        )
        val evaluator = XposedDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val moduleSignals = signals.filter { it.id == XposedSignal.XPOSED_MODULE_INSTALLED }
        assertEquals(1, moduleSignals.size)
        assertEquals(SignalSeverity.MEDIUM, moduleSignals.first().severity)
        assertEquals("com.ceco.marshmallow.gravitybox", moduleSignals.first().metadata["package"])
        assertEquals(XposedSignal.Check.XPOSED_MODULE_INSTALLED, moduleSignals.first().metadata["check"])
    }

    @Test
    fun multipleSignalsDetection() = runTest {
        val context = FakeXposedCheckContext(
            existingFiles = setOf("/system/framework/XposedBridge.jar"),
            loadableClasses = setOf("de.robv.android.xposed.XposedBridge"),
            installedPackages = setOf(
                "org.lsposed.manager",
                "com.ceco.marshmallow.gravitybox",
            ),
        )
        val evaluator = XposedDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(4, signals.size)
        assertTrue(signals.any { it.id == XposedSignal.XPOSED_FRAMEWORK_INSTALLED })
        assertTrue(signals.any { it.id == XposedSignal.XPOSED_BRIDGE_CLASS })
        assertTrue(signals.any { it.id == XposedSignal.XPOSED_INSTALLER_APP })
        assertTrue(signals.any { it.id == XposedSignal.XPOSED_MODULE_INSTALLED })
        assertTrue(signals.all { it.category == IntegrityCategory.HOOKING_OR_TAMPERING })
    }

    @Test
    fun evaluatorFaultToleranceWhenCheckThrows() = runTest {
        val context = FakeXposedCheckContext(
            existingFiles = setOf("/system/framework/XposedBridge.jar"),
            shouldThrowOnClassLoadable = true,
            shouldThrowOnPackageInstalled = true,
            shouldThrowOnReadFile = true,
        )
        val evaluator = XposedDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        // File check must still succeed even when reflection or package manager checks throw
        assertEquals(1, signals.size)
        assertEquals(XposedSignal.XPOSED_FRAMEWORK_INSTALLED, signals.first().id)
    }
}
