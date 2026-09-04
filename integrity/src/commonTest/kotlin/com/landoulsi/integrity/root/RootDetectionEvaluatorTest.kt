package com.landoulsi.integrity.root

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.SignalSeverity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RootDetectionEvaluatorTest {

    private class FakeRootCheckContext(
        private val existingFiles: Set<String> = emptySet(),
        private val fileContents: Map<String, List<String>> = emptyMap(),
        private val buildTag: String = "release-keys",
        private val installedPackages: Set<String> = emptySet(),
        private val shouldThrowOnFileExists: Boolean = false,
        private val shouldThrowOnReadFile: Boolean = false,
        private val shouldThrowOnBuildTag: Boolean = false,
        private val shouldThrowOnPackageInstalled: Boolean = false,
    ) : RootCheckContext {
        override fun fileExists(path: String): Boolean {
            if (shouldThrowOnFileExists) throw IllegalStateException("Simulated fileExists error")
            return path in existingFiles
        }

        override fun readFileLines(path: String): List<String> {
            if (shouldThrowOnReadFile) throw IllegalStateException("Simulated readFileLines error")
            return fileContents[path] ?: emptyList()
        }

        override fun getBuildTag(): String {
            if (shouldThrowOnBuildTag) throw IllegalStateException("Simulated getBuildTag error")
            return buildTag
        }

        override fun isPackageInstalled(packageName: String): Boolean {
            if (shouldThrowOnPackageInstalled) throw IllegalStateException("Simulated isPackageInstalled error")
            return packageName in installedPackages
        }
    }

    @Test
    fun cleanDeviceProducesNoSignals() = runTest {
        val context = FakeRootCheckContext()
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
        assertEquals(IntegrityCategory.ROOT_OR_JAILBREAK, evaluator.category)
    }

    @Test
    fun suBinaryDetection() = runTest {
        val context = FakeRootCheckContext(
            existingFiles = setOf("/system/bin/su", "/system/xbin/su"),
        )
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val suSignals = signals.filter { it.id == RootSignal.SU_BINARY }
        assertEquals(2, suSignals.size)
        assertTrue(suSignals.all { it.severity == SignalSeverity.HIGH })
        assertTrue(suSignals.all { it.category == IntegrityCategory.ROOT_OR_JAILBREAK })
        assertTrue(suSignals.any { it.details.contains("/system/bin/su") })
        assertTrue(suSignals.any { it.details.contains("/system/xbin/su") })
    }

    @Test
    fun magiskMountDetection() = runTest {
        val context = FakeRootCheckContext(
            fileContents = mapOf(
                "/proc/mounts" to listOf(
                    "none /magiskMagisk:/magisk rw,relatime ...",
                    "/dev/block/dm-0 /system ext4 ro ...",
                ),
            ),
        )
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val magiskSignals = signals.filter { it.id == RootSignal.MAGISK_MOUNT }
        assertEquals(1, magiskSignals.size)
        assertEquals(SignalSeverity.HIGH, magiskSignals.first().severity)
    }

    @Test
    fun kernelSuMountDetection() = runTest {
        val context = FakeRootCheckContext(
            fileContents = mapOf(
                "/proc/mounts" to listOf(
                    "none /data/adb/ksu/modules kernelsu rw,relatime 0 0",
                ),
            ),
        )
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val magiskSignals = signals.filter { it.id == RootSignal.MAGISK_MOUNT }
        assertEquals(1, magiskSignals.size)
        assertEquals(SignalSeverity.HIGH, magiskSignals.first().severity)
    }

    @Test
    fun magiskPackageDetection() = runTest {
        val context = FakeRootCheckContext(
            installedPackages = setOf("com.topjohnwu.magisk"),
        )
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val magiskSignals = signals.filter { it.id == RootSignal.MAGISK_PACKAGE }
        assertEquals(1, magiskSignals.size)
        assertEquals(SignalSeverity.HIGH, magiskSignals.first().severity)
    }

    @Test
    fun kernelSuDetection() = runTest {
        val context = FakeRootCheckContext(
            installedPackages = setOf("me.weishu.kernelsu"),
        )
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val kernelSuSignals = signals.filter { it.id == RootSignal.KERNELSU_PACKAGE }
        assertEquals(1, kernelSuSignals.size)
        assertEquals(SignalSeverity.CRITICAL, kernelSuSignals.first().severity)
    }

    @Test
    fun testKeysBuildTag() = runTest {
        val context = FakeRootCheckContext(buildTag = "test-keys")
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val buildTagSignals = signals.filter { it.id == RootSignal.TEST_KEYS }
        assertEquals(1, buildTagSignals.size)
        assertEquals(SignalSeverity.MEDIUM, buildTagSignals.first().severity)
    }

    @Test
    fun writableSystemDetection() = runTest {
        val context = FakeRootCheckContext(
            fileContents = mapOf(
                "/proc/mounts" to listOf(
                    "/dev/block/system /system ext4 rw,seclabel,relatime 0 0",
                ),
            ),
        )
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val writableSignals = signals.filter { it.id == RootSignal.WRITABLE_SYSTEM }
        assertEquals(1, writableSignals.size)
        assertEquals(SignalSeverity.MEDIUM, writableSignals.first().severity)
    }

    @Test
    fun whitespacePaddedMountLineHandledCorrectly() = runTest {
        val context = FakeRootCheckContext(
            fileContents = mapOf(
                "/proc/mounts" to listOf(
                    "   /dev/block/system   /system   ext4   rw,seclabel   0   0   ",
                ),
            ),
        )
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val writableSignals = signals.filter { it.id == RootSignal.WRITABLE_SYSTEM }
        assertEquals(1, writableSignals.size)
    }

    @Test
    fun malformedMountLineHandledSafely() = runTest {
        val context = FakeRootCheckContext(
            fileContents = mapOf(
                "/proc/mounts" to listOf(
                    "corrupted line",
                    "",
                ),
            ),
        )
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == RootSignal.WRITABLE_SYSTEM })
    }

    @Test
    fun superuserAppDetection() = runTest {
        val context = FakeRootCheckContext(
            existingFiles = setOf("/system/app/Superuser.apk", "/system/app/SuperSU.apk"),
        )
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val suAppSignals = signals.filter { it.id == RootSignal.SUPERUSER_APP }
        assertEquals(2, suAppSignals.size)
        assertTrue(suAppSignals.all { it.severity == SignalSeverity.LOW })
    }

    @Test
    fun mixedSignalsDetection() = runTest {
        val context = FakeRootCheckContext(
            existingFiles = setOf("/system/bin/su", "/system/app/Superuser.apk"),
            buildTag = "test-keys",
            fileContents = mapOf(
                "/proc/mounts" to listOf(
                    "/dev/block/system /system ext4 rw,seclabel ...",
                ),
            ),
        )
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.size >= 4)
        assertTrue(signals.any { it.id == RootSignal.SU_BINARY })
        assertTrue(signals.any { it.id == RootSignal.TEST_KEYS })
        assertTrue(signals.any { it.id == RootSignal.WRITABLE_SYSTEM })
        assertTrue(signals.any { it.id == RootSignal.SUPERUSER_APP })
        assertTrue(signals.all { it.category == IntegrityCategory.ROOT_OR_JAILBREAK })
    }

    @Test
    fun readSystemTagReturnsNoBuildTagSignal() = runTest {
        val context = FakeRootCheckContext(buildTag = "release-keys")
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == RootSignal.TEST_KEYS })
    }

    @Test
    fun noMountsReturnsNoWritableSignal() = runTest {
        val context = FakeRootCheckContext(
            fileContents = mapOf("/proc/mounts" to emptyList()),
        )
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == RootSignal.WRITABLE_SYSTEM })
    }

    @Test
    fun signalMetadataContainsCheckIdentifier() = runTest {
        val context = FakeRootCheckContext(
            existingFiles = setOf("/system/bin/su"),
        )
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val suSignal = signals.first { it.id == RootSignal.SU_BINARY }
        assertEquals(RootSignal.Check.SU_BINARY, suSignal.metadata["check"])
        assertEquals("/system/bin/su", suSignal.metadata["path"])
    }

    @Test
    fun fileExistsThrowsHandledSafely() = runTest {
        val context = FakeRootCheckContext(shouldThrowOnFileExists = true)
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == RootSignal.SU_BINARY })
        assertTrue(signals.none { it.id == RootSignal.SUPERUSER_APP })
    }

    @Test
    fun isPackageInstalledThrowsHandledSafely() = runTest {
        val context = FakeRootCheckContext(shouldThrowOnPackageInstalled = true)
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == RootSignal.MAGISK_PACKAGE })
        assertTrue(signals.none { it.id == RootSignal.KERNELSU_PACKAGE })
    }

    @Test
    fun evaluatorFaultToleranceWhenCheckThrows() = runTest {
        val context = FakeRootCheckContext(
            existingFiles = setOf("/system/bin/su"),
            shouldThrowOnBuildTag = true,
            shouldThrowOnReadFile = true,
        )
        val evaluator = RootDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        // SU binary check must still succeed and be returned even when other checks fail
        assertEquals(1, signals.size)
        assertEquals(RootSignal.SU_BINARY, signals.first().id)
    }
}

