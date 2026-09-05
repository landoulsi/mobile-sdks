package com.landoulsi.integrity.hooking.frida

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.SignalSeverity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FridaDetectionEvaluatorTest {

    private class FakeFridaCheckContext(
        private val existingFiles: Set<String> = emptySet(),
        private val fileContents: Map<String, List<String>> = emptyMap(),
        private val runningProcesses: Set<String> = emptySet(),
        private val openPorts: Set<Int> = emptySet(),
        private val installedPackages: Set<String> = emptySet(),
        private val shouldThrowOnFileExists: Boolean = false,
        private val shouldThrowOnReadFile: Boolean = false,
        private val shouldThrowOnProcess: Boolean = false,
        private val shouldThrowOnPort: Boolean = false,
        private val shouldThrowOnPackage: Boolean = false,
    ) : FridaCheckContext {
        override fun fileExists(path: String): Boolean {
            if (shouldThrowOnFileExists) throw IllegalStateException("Simulated fileExists error")
            return path in existingFiles
        }

        override fun readFileLines(path: String): List<String> {
            if (shouldThrowOnReadFile) throw IllegalStateException("Simulated readFileLines error")
            return fileContents[path] ?: emptyList()
        }

        override fun isProcessRunning(processName: String): Boolean {
            if (shouldThrowOnProcess) throw IllegalStateException("Simulated isProcessRunning error")
            return processName in runningProcesses
        }

        override fun isPortOpen(port: Int): Boolean {
            if (shouldThrowOnPort) throw IllegalStateException("Simulated isPortOpen error")
            return port in openPorts
        }

        override fun isPackageInstalled(packageName: String): Boolean {
            if (shouldThrowOnPackage) throw IllegalStateException("Simulated isPackageInstalled error")
            return packageName in installedPackages
        }
    }

    @Test
    fun cleanDeviceProducesNoSignals() = runTest {
        val context = FakeFridaCheckContext()
        val evaluator = FridaDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
        assertEquals(IntegrityCategory.HOOKING_OR_TAMPERING, evaluator.category)
        assertEquals(FridaSignal.all, evaluator.knownSignalIds)
    }

    @Test
    fun fridaServerProcessDetection() = runTest {
        val context = FakeFridaCheckContext(
            runningProcesses = setOf("frida-server"),
        )
        val evaluator = FridaDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val serverSignals = signals.filter { it.id == FridaSignal.FRIDA_SERVER_PROCESS }
        assertEquals(1, serverSignals.size)
        assertEquals(SignalSeverity.HIGH, serverSignals.first().severity)
        assertEquals(IntegrityCategory.HOOKING_OR_TAMPERING, serverSignals.first().category)
        assertEquals("frida-server", serverSignals.first().metadata["process"])
        assertEquals(FridaSignal.Check.FRIDA_SERVER_PROCESS, serverSignals.first().metadata["check"])
    }

    @Test
    fun fridaPortOpenDetection() = runTest {
        val context = FakeFridaCheckContext(
            openPorts = setOf(27042),
        )
        val evaluator = FridaDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val portSignals = signals.filter { it.id == FridaSignal.FRIDA_PORT_OPEN }
        assertEquals(1, portSignals.size)
        assertEquals(SignalSeverity.MEDIUM, portSignals.first().severity)
        assertEquals("27042", portSignals.first().metadata["port"])
        assertEquals(FridaSignal.Check.FRIDA_PORT_OPEN, portSignals.first().metadata["check"])
    }

    @Test
    fun fridaGadgetMapsDetection() = runTest {
        val context = FakeFridaCheckContext(
            fileContents = mapOf(
                "/proc/self/maps" to listOf(
                    "7f8a0000-7f8b0000 r-xp 00000000 08:01 12345 /data/local/tmp/libfrida-gadget.so",
                    "7f8b0000-7f8c0000 rw-p 00010000 08:01 12345 /data/local/tmp/libfrida-gadget.so",
                ),
            ),
        )
        val evaluator = FridaDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val mapsSignals = signals.filter { it.id == FridaSignal.FRIDA_GADGET_MAPS }
        assertEquals(1, mapsSignals.size)
        assertEquals(SignalSeverity.HIGH, mapsSignals.first().severity)
        assertEquals(FridaSignal.Check.FRIDA_GADGET_MAPS, mapsSignals.first().metadata["check"])
        assertTrue(mapsSignals.first().metadata["matches"]!!.contains("libfrida-gadget.so"))
    }

    @Test
    fun fridaGadgetFileDetection() = runTest {
        val context = FakeFridaCheckContext(
            existingFiles = setOf(
                "/system/lib/libfrida-gadget.so",
                "/data/local/tmp/frida-server",
            ),
        )
        val evaluator = FridaDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        val fileSignals = signals.filter { it.id == FridaSignal.FRIDA_GADGET_FILE }
        assertEquals(2, fileSignals.size)
        assertTrue(fileSignals.all { it.severity == SignalSeverity.HIGH })
        assertTrue(fileSignals.any { it.metadata["path"] == "/system/lib/libfrida-gadget.so" })
        assertTrue(fileSignals.any { it.metadata["path"] == "/data/local/tmp/frida-server" })
    }

    @Test
    fun multipleSignalsDetection() = runTest {
        val context = FakeFridaCheckContext(
            runningProcesses = setOf("frida-server"),
            openPorts = setOf(27042),
            existingFiles = setOf("/data/local/tmp/frida-server"),
            fileContents = mapOf(
                "/proc/self/maps" to listOf(
                    "7f8a0000-7f8b0000 r-xp 00000000 08:01 12345 /data/local/tmp/libfrida-agent.so",
                ),
            ),
        )
        val evaluator = FridaDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(4, signals.size)
        assertTrue(signals.any { it.id == FridaSignal.FRIDA_SERVER_PROCESS })
        assertTrue(signals.any { it.id == FridaSignal.FRIDA_PORT_OPEN })
        assertTrue(signals.any { it.id == FridaSignal.FRIDA_GADGET_MAPS })
        assertTrue(signals.any { it.id == FridaSignal.FRIDA_GADGET_FILE })
        assertTrue(signals.all { it.category == IntegrityCategory.HOOKING_OR_TAMPERING })
    }

    @Test
    fun evaluatorFaultToleranceWhenCheckThrows() = runTest {
        val context = FakeFridaCheckContext(
            runningProcesses = setOf("frida-server"),
            shouldThrowOnPort = true,
            shouldThrowOnReadFile = true,
            shouldThrowOnFileExists = true,
        )
        val evaluator = FridaDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        // Process check must still succeed even when port/maps/file checks throw
        assertEquals(1, signals.size)
        assertEquals(FridaSignal.FRIDA_SERVER_PROCESS, signals.first().id)
    }
}
