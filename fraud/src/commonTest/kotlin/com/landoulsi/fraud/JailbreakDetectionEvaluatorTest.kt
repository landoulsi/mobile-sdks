package com.landoulsi.fraud

import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudSignal
import com.landoulsi.fraud.model.SignalSeverity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JailbreakDetectionEvaluatorTest {

    private class FakeJailbreakCheckContext(
        private val existingFiles: Set<String> = emptySet(),
        private val directoryContents: Map<String, List<String>> = emptyMap(),
        private val canForkResult: Boolean = false,
        private val canWriteResult: Boolean = false,
        private val shouldThrowOnFork: Boolean = false,
        private val shouldThrowOnFileExists: Boolean = false,
        private val shouldThrowOnDirectoryContents: Boolean = false,
    ) : JailbreakCheckContext {
        override fun fileExists(path: String): Boolean {
            if (shouldThrowOnFileExists) throw IllegalStateException("Simulated fileExists failure")
            return path in existingFiles
        }

        override fun directoryContents(path: String): List<String> {
            if (shouldThrowOnDirectoryContents) throw IllegalStateException("Simulated directoryContents failure")
            return directoryContents[path] ?: emptyList()
        }

        override fun canFork(): Boolean {
            if (shouldThrowOnFork) throw IllegalStateException("Simulated fork failure")
            return canForkResult
        }

        override fun canWriteOutsideSandbox(path: String): Boolean = canWriteResult
    }

    @Test
    fun cleanDeviceProducesNoSignals() = runTest {
        val context = FakeJailbreakCheckContext()
        val evaluator = JailbreakDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
        assertEquals(FraudCategory.ROOT_OR_JAILBREAK, evaluator.category)
    }

    @Test
    fun jailbreakAppDetection() = runTest {
        val context = FakeJailbreakCheckContext(
            existingFiles = setOf("/Applications/Cydia.app", "/Applications/Sileo.app"),
        )
        val evaluator = JailbreakDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val appSignals = signals.filter { it.id == JailbreakSignal.APP_BUNDLE }
        assertEquals(2, appSignals.size)
        assertTrue(appSignals.all { it.severity == SignalSeverity.HIGH })
        assertTrue(appSignals.all { it.category == FraudCategory.ROOT_OR_JAILBREAK })
    }

    @Test
    fun systemBinaryDetection() = runTest {
        val context = FakeJailbreakCheckContext(
            existingFiles = setOf("/bin/sh", "/usr/sbin/sshd"),
        )
        val evaluator = JailbreakDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val binarySignals = signals.filter { it.id == JailbreakSignal.SYSTEM_BINARY }
        assertEquals(2, binarySignals.size)
        assertTrue(binarySignals.all { it.severity == SignalSeverity.MEDIUM })
    }

    @Test
    fun forkCapabilityDetection() = runTest {
        val context = FakeJailbreakCheckContext(canForkResult = true)
        val evaluator = JailbreakDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val forkSignals = signals.filter { it.id == JailbreakSignal.FORK_CAPABILITY }
        assertEquals(1, forkSignals.size)
        assertEquals(SignalSeverity.CRITICAL, forkSignals.first().severity)
    }

    @Test
    fun sandboxEscapeDetection() = runTest {
        val context = FakeJailbreakCheckContext(canWriteResult = true)
        val evaluator = JailbreakDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val sandboxSignals = signals.filter { it.id == JailbreakSignal.SANDBOX_ESCAPE }
        assertEquals(1, sandboxSignals.size)
        assertEquals(SignalSeverity.HIGH, sandboxSignals.first().severity)
    }

    @Test
    fun dylibInjectionDetection() = runTest {
        val context = FakeJailbreakCheckContext(
            existingFiles = setOf("/Library/MobileSubstrate/DynamicLibraries"),
            directoryContents = mapOf(
                "/Library/MobileSubstrate/DynamicLibraries" to listOf("Tweak.dylib", "SubstrateLoader.dylib"),
            ),
        )
        val evaluator = JailbreakDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val dylibSignals = signals.filter { it.id == JailbreakSignal.DYLIB_INJECTION }
        assertEquals(1, dylibSignals.size)
        assertEquals(SignalSeverity.MEDIUM, dylibSignals.first().severity)
    }

    @Test
    fun mixedSignalsDetection() = runTest {
        val context = FakeJailbreakCheckContext(
            existingFiles = setOf(
                "/Applications/Cydia.app",
                "/bin/sh",
                "/Library/MobileSubstrate/DynamicLibraries",
            ),
            directoryContents = mapOf(
                "/Library/MobileSubstrate/DynamicLibraries" to listOf("Tweak.dylib"),
            ),
            canForkResult = true,
        )
        val evaluator = JailbreakDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.size >= 4)
        assertTrue(signals.any { it.id == JailbreakSignal.APP_BUNDLE })
        assertTrue(signals.any { it.id == JailbreakSignal.SYSTEM_BINARY })
        assertTrue(signals.any { it.id == JailbreakSignal.FORK_CAPABILITY })
        assertTrue(signals.any { it.id == JailbreakSignal.DYLIB_INJECTION })
        assertTrue(signals.all { it.category == FraudCategory.ROOT_OR_JAILBREAK })
    }

    @Test
    fun forkFailureProducesNoSignal() = runTest {
        val context = FakeJailbreakCheckContext(canForkResult = false)
        val evaluator = JailbreakDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == JailbreakSignal.FORK_CAPABILITY })
    }

    @Test
    fun sandboxWriteFailureProducesNoSignal() = runTest {
        val context = FakeJailbreakCheckContext(canWriteResult = false)
        val evaluator = JailbreakDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == JailbreakSignal.SANDBOX_ESCAPE })
    }

    @Test
    fun signalMetadataContainsPathInfo() = runTest {
        val context = FakeJailbreakCheckContext(
            existingFiles = setOf("/Applications/Cydia.app"),
        )
        val evaluator = JailbreakDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val appSignal = signals.first { it.id == JailbreakSignal.APP_BUNDLE }
        assertEquals(JailbreakSignal.Check.APP_BUNDLE, appSignal.metadata["check"])
        assertEquals("/Applications/Cydia.app", appSignal.metadata["path"])
    }

    @Test
    fun emptyDylibDirectoryProducesNoSignal() = runTest {
        val context = FakeJailbreakCheckContext(
            existingFiles = setOf("/Library/MobileSubstrate/DynamicLibraries"),
            directoryContents = mapOf("/Library/MobileSubstrate/DynamicLibraries" to emptyList()),
        )
        val evaluator = JailbreakDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == JailbreakSignal.DYLIB_INJECTION })
    }

    @Test
    fun directoryContentsThrowsHandledGracefully() = runTest {
        val context = FakeJailbreakCheckContext(
            existingFiles = setOf("/Library/MobileSubstrate/DynamicLibraries"),
            shouldThrowOnDirectoryContents = true,
        )
        val evaluator = JailbreakDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == JailbreakSignal.DYLIB_INJECTION })
    }

    @Test
    fun fileExistsThrowsHandledGracefully() = runTest {
        val context = FakeJailbreakCheckContext(
            shouldThrowOnFileExists = true,
        )
        val evaluator = JailbreakDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.none { it.id == JailbreakSignal.APP_BUNDLE })
        assertTrue(signals.none { it.id == JailbreakSignal.SYSTEM_BINARY })
    }

    @Test
    fun evaluatorFaultToleranceWhenCheckThrows() = runTest {
        val context = FakeJailbreakCheckContext(
            existingFiles = setOf("/Applications/Cydia.app"),
            shouldThrowOnFork = true,
        )
        val evaluator = JailbreakDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        // The app bundle signal should still be returned even if fork check throws
        assertEquals(1, signals.size)
        assertEquals(JailbreakSignal.APP_BUNDLE, signals.first().id)
    }
}

