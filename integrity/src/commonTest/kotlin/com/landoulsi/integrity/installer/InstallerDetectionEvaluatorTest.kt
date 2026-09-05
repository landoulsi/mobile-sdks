package com.landoulsi.integrity.installer

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.SignalSeverity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InstallerDetectionEvaluatorTest {

    private class FakeInstallerCheckContext(
        private val installingPackageName: String? = "com.android.vending",
        private val initiatingPackageName: String? = "com.android.vending",
        private val shouldThrow: Boolean = false,
    ) : InstallerCheckContext {
        override fun getInstallSourceInfo(): InstallSourceInfo {
            if (shouldThrow) throw IllegalStateException("Simulated getInstallSourceInfo error")
            return InstallSourceInfo(
                installingPackageName = installingPackageName,
                initiatingPackageName = initiatingPackageName,
            )
        }
    }

    @Test
    fun playStoreInstallProducesNoSignal() = runTest {
        val context = FakeInstallerCheckContext(
            installingPackageName = "com.android.vending",
            initiatingPackageName = "com.android.vending",
        )
        val evaluator = InstallerDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
        assertEquals(IntegrityCategory.UNTRUSTED_INSTALLER, evaluator.category)
    }

    @Test
    fun modernAdbInstallDetectedViaInitiatingPackage() = runTest {
        // API 30+: installingPackageName is null of record, initiatingPackageName is the shell.
        val context = FakeInstallerCheckContext(
            installingPackageName = null,
            initiatingPackageName = "com.android.shell",
        )
        val evaluator = InstallerDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(1, signals.size)
        assertEquals(InstallerSignal.SHELL, signals.first().id)
        assertEquals(SignalSeverity.MEDIUM, signals.first().severity)
    }

    @Test
    fun legacyAdbInstallDetectedViaInstallingPackage() = runTest {
        // Pre-API 30: initiatingPackageName is always null; installer of record is the shell.
        val context = FakeInstallerCheckContext(
            installingPackageName = "com.android.shell",
            initiatingPackageName = null,
        )
        val evaluator = InstallerDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(1, signals.size)
        assertEquals(InstallerSignal.SHELL, signals.first().id)
    }

    @Test
    fun unknownInstallSourceWhenBothFieldsAreNull() = runTest {
        val context = FakeInstallerCheckContext(installingPackageName = null, initiatingPackageName = null)
        val evaluator = InstallerDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(1, signals.size)
        assertEquals(InstallerSignal.UNKNOWN, signals.first().id)
        assertEquals(SignalSeverity.LOW, signals.first().severity)
    }

    @Test
    fun untrustedInstallingPackageDetected() = runTest {
        val context = FakeInstallerCheckContext(
            installingPackageName = "com.malicious.dropper",
            initiatingPackageName = "com.malicious.dropper",
        )
        val evaluator = InstallerDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(1, signals.size)
        assertEquals(InstallerSignal.UNTRUSTED, signals.first().id)
        assertEquals("com.malicious.dropper", signals.first().metadata["installer"])
    }

    @Test
    fun untrustedInitiatingPackageDetectedWhenInstallingPackageIsNull() = runTest {
        // API 30+ sideload via a browser/dropper: installer of record is cleared, but the
        // initiating package still names the real, untrusted origin.
        val context = FakeInstallerCheckContext(
            installingPackageName = null,
            initiatingPackageName = "com.android.chrome",
        )
        val evaluator = InstallerDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(1, signals.size)
        assertEquals(InstallerSignal.UNTRUSTED, signals.first().id)
        assertEquals("com.android.chrome", signals.first().metadata["installer"])
    }

    @Test
    fun trustedInitiatingPackageProducesNoSignalWhenInstallingPackageIsNull() = runTest {
        val context = FakeInstallerCheckContext(
            installingPackageName = null,
            initiatingPackageName = "com.android.vending",
        )
        val evaluator = InstallerDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
    }

    @Test
    fun customTrustedInstallersSetIsRespected() = runTest {
        val context = FakeInstallerCheckContext(
            installingPackageName = "com.sec.android.app.samsungapps",
            initiatingPackageName = "com.sec.android.app.samsungapps",
        )
        val evaluator = InstallerDetectionEvaluator(
            context = context,
            trustedInstallers = setOf("com.android.vending", "com.sec.android.app.samsungapps"),
        )

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
    }

    @Test
    fun evaluatorFaultToleranceWhenContextThrows() = runTest {
        val context = FakeInstallerCheckContext(shouldThrow = true)
        val evaluator = InstallerDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        // A platform error must never be reported as an active threat signal.
        assertTrue(signals.isEmpty())
    }

    @Test
    fun signalMetadataContainsCheckIdentifier() = runTest {
        val context = FakeInstallerCheckContext(
            installingPackageName = "com.malicious.dropper",
            initiatingPackageName = "com.malicious.dropper",
        )
        val evaluator = InstallerDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(InstallerSignal.Check.INSTALL_SOURCE, signals.first().metadata["check"])
    }
}
