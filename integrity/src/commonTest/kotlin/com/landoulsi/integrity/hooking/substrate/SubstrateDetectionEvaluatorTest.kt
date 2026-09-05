package com.landoulsi.integrity.hooking.substrate

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.SignalSeverity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubstrateDetectionEvaluatorTest {

    private class FakeSubstrateCheckContext(
        private val existingFiles: Set<String> = emptySet(),
        private val directoryContentsMap: Map<String, List<String>> = emptyMap(),
        private val shouldThrowOnFileExists: Boolean = false,
        private val shouldThrowOnDirectoryContents: Boolean = false,
    ) : SubstrateCheckContext {
        override fun fileExists(path: String): Boolean {
            if (shouldThrowOnFileExists) throw IllegalStateException("Simulated fileExists error")
            return path in existingFiles
        }

        override fun directoryContents(path: String): List<String> {
            if (shouldThrowOnDirectoryContents) throw IllegalStateException("Simulated directoryContents error")
            return directoryContentsMap[path] ?: emptyList()
        }
    }

    @Test
    fun cleanDeviceProducesNoSignals() = runTest {
        val context = FakeSubstrateCheckContext()
        val evaluator = SubstrateDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
        assertEquals(IntegrityCategory.HOOKING_OR_TAMPERING, evaluator.category)
        assertEquals(SubstrateSignal.all, evaluator.knownSignalIds)
    }

    @Test
    fun substrateDylibInjectionDetection() = runTest {
        val context = FakeSubstrateCheckContext(
            directoryContentsMap = mapOf(
                "/Library/MobileSubstrate/DynamicLibraries" to listOf("Tweak1.dylib", "Tweak2.dylib"),
            ),
        )
        val evaluator = SubstrateDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val dylibSignals = signals.filter { it.id == SubstrateSignal.SUBSTRATE_DYLIB_INJECTION }
        assertEquals(1, dylibSignals.size)
        assertEquals(SignalSeverity.MEDIUM, dylibSignals.first().severity)
        assertEquals(0.85, dylibSignals.first().confidence)
        assertEquals("2", dylibSignals.first().metadata["file_count"])
        assertEquals(SubstrateSignal.Check.SUBSTRATE_DYLIB_INJECTION, dylibSignals.first().metadata["check"])
    }

    @Test
    fun substrateFrameworkDetection() = runTest {
        val context = FakeSubstrateCheckContext(
            existingFiles = setOf("/Library/MobileSubstrate/MobileSubstrate.dylib"),
        )
        val evaluator = SubstrateDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val frameworkSignals = signals.filter { it.id == SubstrateSignal.SUBSTRATE_FRAMEWORK }
        assertEquals(1, frameworkSignals.size)
        assertEquals(SignalSeverity.MEDIUM, frameworkSignals.first().severity)
        assertEquals(0.9, frameworkSignals.first().confidence)
        assertEquals("/Library/MobileSubstrate/MobileSubstrate.dylib", frameworkSignals.first().metadata["path"])
        assertEquals(SubstrateSignal.Check.SUBSTRATE_FRAMEWORK, frameworkSignals.first().metadata["check"])
    }

    @Test
    fun substrateTweakInjectDetection() = runTest {
        val context = FakeSubstrateCheckContext(
            existingFiles = setOf("/usr/lib/TweakInject"),
        )
        val evaluator = SubstrateDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val tweakSignals = signals.filter { it.id == SubstrateSignal.SUBSTRATE_TWEAK_INJECT }
        assertEquals(1, tweakSignals.size)
        assertEquals(SignalSeverity.LOW, tweakSignals.first().severity)
        assertEquals(0.6, tweakSignals.first().confidence)
        assertEquals("/usr/lib/TweakInject", tweakSignals.first().metadata["path"])
        assertEquals(SubstrateSignal.Check.SUBSTRATE_TWEAK_INJECT, tweakSignals.first().metadata["check"])
    }

    @Test
    fun substrateLoadedDetection() = runTest {
        val context = FakeSubstrateCheckContext(
            existingFiles = setOf("/usr/lib/substrate/SubstrateLoader.dylib"),
        )
        val evaluator = SubstrateDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val loadedSignals = signals.filter { it.id == SubstrateSignal.SUBSTRATE_LOADED }
        assertEquals(1, loadedSignals.size)
        assertEquals(SignalSeverity.HIGH, loadedSignals.first().severity)
        assertEquals(0.9, loadedSignals.first().confidence)
        assertEquals("/usr/lib/substrate/SubstrateLoader.dylib", loadedSignals.first().metadata["path"])
        assertEquals(SubstrateSignal.Check.SUBSTRATE_LOADED, loadedSignals.first().metadata["check"])
    }

    @Test
    fun multipleSignalsDetection() = runTest {
        val context = FakeSubstrateCheckContext(
            existingFiles = setOf(
                "/Library/MobileSubstrate/MobileSubstrate.dylib",
                "/usr/lib/TweakInject",
                "/usr/lib/substrate/SubstrateLoader.dylib",
            ),
            directoryContentsMap = mapOf(
                "/Library/MobileSubstrate/DynamicLibraries" to listOf("Tweak.dylib"),
            ),
        )
        val evaluator = SubstrateDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(4, signals.size)
        assertTrue(signals.any { it.id == SubstrateSignal.SUBSTRATE_DYLIB_INJECTION })
        assertTrue(signals.any { it.id == SubstrateSignal.SUBSTRATE_FRAMEWORK })
        assertTrue(signals.any { it.id == SubstrateSignal.SUBSTRATE_TWEAK_INJECT })
        assertTrue(signals.any { it.id == SubstrateSignal.SUBSTRATE_LOADED })
        assertTrue(signals.all { it.category == IntegrityCategory.HOOKING_OR_TAMPERING })
    }

    @Test
    fun evaluatorFaultToleranceWhenCheckThrows() = runTest {
        val context = FakeSubstrateCheckContext(
            directoryContentsMap = mapOf(
                "/Library/MobileSubstrate/DynamicLibraries" to listOf("Tweak.dylib"),
            ),
            shouldThrowOnFileExists = true,
        )
        val evaluator = SubstrateDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        // Directory injection check must still succeed even when file checks throw
        assertEquals(1, signals.size)
        assertEquals(SubstrateSignal.SUBSTRATE_DYLIB_INJECTION, signals.first().id)
    }
}
