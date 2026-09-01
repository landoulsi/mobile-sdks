package com.landoulsi.fraud

import com.landoulsi.fraud.model.FraudCategory
import com.landoulsi.fraud.model.FraudConfig
import com.landoulsi.fraud.model.FraudMitigationAction
import com.landoulsi.fraud.model.FraudRiskScore
import com.landoulsi.fraud.model.FraudSignal
import com.landoulsi.fraud.model.RiskLevel
import com.landoulsi.fraud.model.SignalSeverity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FraudManagerTest {

    private fun signal(
        id: String,
        category: FraudCategory,
        severity: SignalSeverity = SignalSeverity.HIGH,
    ) = FraudSignal(
        id = id,
        name = id,
        category = category,
        severity = severity,
        confidence = 1.0,
        detectedAt = 1_000L,
    )

    private class FakeEvaluator(
        override val category: FraudCategory,
        override val knownSignalIds: Set<String>,
        private val produce: List<FraudSignal> = emptyList(),
        private val throwError: Boolean = false,
    ) : SignalEvaluator {
        override suspend fun evaluate(): List<FraudSignal> {
            if (throwError) throw IllegalStateException("simulated evaluator failure")
            return produce
        }
    }

    private class FakeFraudDetector(
        private val score: FraudRiskScore,
    ) : FraudDetector {
        override val currentConfig: FraudConfig = FraudConfig()
        override fun updateConfig(config: FraudConfig) = Unit
        override suspend fun detectSignals(): List<FraudSignal> = score.signals
        override suspend fun evaluateRisk(): FraudRiskScore = score
        override suspend fun evaluateCategory(category: FraudCategory): List<FraudSignal> =
            score.signals.filter { it.category == category }
        override fun observeSignals(pollIntervalMs: Long): Flow<List<FraudSignal>> =
            throw UnsupportedOperationException()
        override fun observeRisk(pollIntervalMs: Long): Flow<FraudRiskScore> =
            throw UnsupportedOperationException()
    }

    @Test
    fun cleanDeviceProducesExhaustiveFalseMapAndZeroScore() = runTest {
        val manager = FraudManager.from(
            evaluators = listOf(
                FakeEvaluator(FraudCategory.ROOT_OR_JAILBREAK, RootSignal.all),
                FakeEvaluator(FraudCategory.ROOT_OR_JAILBREAK, JailbreakSignal.all),
            ),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val result = manager.scan()

        assertEquals(RootSignal.all + JailbreakSignal.all, result.signals.keys)
        assertTrue(result.signals.values.none { it })
        assertEquals(0, result.fraudScore)
        assertEquals(FraudMitigationAction.ALLOW, result.action)
        assertEquals(RiskLevel.LOW, result.riskLevel)
        assertFalse(result.hasAnySignal)
        assertTrue(result.fired.isEmpty())
        assertTrue(FraudCategory.entries.all { result.categories[it] == false })
        assertFalse(result.isRootedOrJailbroken)
        assertFalse(result.isRooted)
        assertFalse(result.isJailbroken)
    }

    @Test
    fun firedSignalsMarkedTrueOthersFalse() = runTest {
        val manager = FraudManager.from(
            evaluators = listOf(
                FakeEvaluator(
                    category = FraudCategory.ROOT_OR_JAILBREAK,
                    knownSignalIds = RootSignal.all,
                    produce = listOf(
                        signal(RootSignal.SU_BINARY, FraudCategory.ROOT_OR_JAILBREAK),
                        signal(RootSignal.MAGISK_PACKAGE, FraudCategory.ROOT_OR_JAILBREAK),
                    ),
                ),
            ),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val result = manager.scan()

        assertEquals(true, result.signals[RootSignal.SU_BINARY])
        assertEquals(true, result.signals[RootSignal.MAGISK_PACKAGE])
        assertEquals(false, result.signals[RootSignal.KERNELSU_PACKAGE])
        assertEquals(false, result.signals[RootSignal.SUPERUSER_APP])
        assertTrue(result.hasSuBinary)
        assertTrue(result.hasMagisk)
        assertFalse(result.hasKernelSu)
        assertTrue(result.isRooted)
        assertFalse(result.isJailbroken)
        assertTrue(result.isRootedOrJailbroken)
        assertFalse(result.isEmulator)
        assertEquals(2, result.fired.size)
    }

    @Test
    fun categoriesMapReflectsFiredCategoriesOnly() = runTest {
        val manager = FraudManager.from(
            evaluators = listOf(
                FakeEvaluator(
                    category = FraudCategory.MOCK_LOCATION,
                    knownSignalIds = setOf("mock_provider_enabled"),
                    produce = listOf(signal("mock_provider_enabled", FraudCategory.MOCK_LOCATION)),
                ),
            ),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val result = manager.scan()

        assertEquals(true, result.categories[FraudCategory.MOCK_LOCATION])
        assertTrue(result.hasMockLocation)
        assertEquals(false, result.categories[FraudCategory.ROOT_OR_JAILBREAK])
        assertFalse(result.isRootedOrJailbroken)
    }

    @Test
    fun fraudScoreAndActionComeFromScoringEngine() = runTest {
        val manager = FraudManager.from(
            evaluators = listOf(
                FakeEvaluator(
                    category = FraudCategory.ROOT_OR_JAILBREAK,
                    knownSignalIds = RootSignal.all,
                    produce = listOf(
                        signal(
                            RootSignal.KERNELSU_PACKAGE,
                            FraudCategory.ROOT_OR_JAILBREAK,
                            SignalSeverity.CRITICAL,
                        ),
                    ),
                ),
            ),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val result = manager.scan()

        // One CRITICAL signal -> raw 40 -> ~49 -> WARN band under default thresholds.
        assertEquals(49, result.fraudScore)
        assertEquals(FraudMitigationAction.WARN, result.action)
        assertEquals(RiskLevel.MEDIUM, result.riskLevel)
    }

    @Test
    fun evaluatorFailureIsIsolatedAndOtherSignalsSurvive() = runTest {
        val manager = FraudManager.from(
            evaluators = listOf(
                FakeEvaluator(
                    category = FraudCategory.ROOT_OR_JAILBREAK,
                    knownSignalIds = RootSignal.all,
                    throwError = true,
                ),
                FakeEvaluator(
                    category = FraudCategory.ROOT_OR_JAILBREAK,
                    knownSignalIds = JailbreakSignal.all,
                    produce = listOf(
                        signal(JailbreakSignal.APP_BUNDLE, FraudCategory.ROOT_OR_JAILBREAK),
                    ),
                ),
            ),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val result = manager.scan()

        assertTrue(result.hasJailbreakApp)
        assertEquals(RootSignal.all + JailbreakSignal.all, result.signals.keys)
        assertTrue(result.signals.filterKeys { it in RootSignal.all }.values.none { it })
    }

    @Test
    fun fromDerivesCatalogFromEvaluators() = runTest {
        val emulatorIds = setOf("emu_qemu_files", "emu_fingerprint")
        val manager = FraudManager.from(
            evaluators = listOf(
                FakeEvaluator(FraudCategory.ROOT_OR_JAILBREAK, RootSignal.all),
                FakeEvaluator(FraudCategory.VIRTUAL_OS_OR_EMULATOR, emulatorIds),
            ),
            dispatcher = StandardTestDispatcher(testScheduler),
        )

        val result = manager.scan()

        assertEquals(RootSignal.all + emulatorIds, result.signals.keys)
    }

    @Test
    fun injectedDetectorResultIsReshapedFaithfully() = runTest {
        val score = FraudRiskScore(
            score = 73.4,
            riskLevel = RiskLevel.HIGH,
            action = FraudMitigationAction.CHALLENGE,
            signals = listOf(signal("hook_frida_port", FraudCategory.HOOKING_OR_TAMPERING)),
            evaluatedAt = 42L,
        )
        val manager = FraudManager(FakeFraudDetector(score), knownSignalIds = setOf("hook_frida_port", "hook_xposed"))

        val result = manager.scan()

        assertEquals(73, result.fraudScore)
        assertEquals(RiskLevel.HIGH, result.riskLevel)
        assertEquals(FraudMitigationAction.CHALLENGE, result.action)
        assertEquals(mapOf("hook_frida_port" to true, "hook_xposed" to false), result.signals)
        assertEquals(42L, result.evaluatedAt)
        assertTrue(result.isHooked)
        assertEquals(1, result.fired.size)
    }

    @Test
    fun undeclaredFiredSignalStillReportedAsTrue() = runTest {
        val score = FraudRiskScore(
            score = 10.0,
            riskLevel = RiskLevel.LOW,
            action = FraudMitigationAction.ALLOW,
            signals = listOf(signal("surprise_signal", FraudCategory.NETWORK_ANOMALY)),
            evaluatedAt = 0L,
        )
        val manager = FraudManager(FakeFraudDetector(score), knownSignalIds = setOf("known_signal"))

        val result = manager.scan()

        assertEquals(mapOf("known_signal" to false, "surprise_signal" to true), result.signals)
        assertTrue(result.hasNetworkAnomaly)
    }

    @Test
    fun realEvaluatorsExposeCatalogViaKnownSignalIds() {
        assertEquals(RootSignal.all, RootDetectionEvaluator(NoopRootContext).knownSignalIds)
        assertEquals(JailbreakSignal.all, JailbreakDetectionEvaluator(NoopJailbreakContext).knownSignalIds)
        assertEquals(7, RootSignal.all.size)
        assertEquals(5, JailbreakSignal.all.size)
    }

    private object NoopRootContext : RootCheckContext {
        override fun fileExists(path: String): Boolean = false
        override fun readFileLines(path: String): List<String> = emptyList()
        override fun getBuildTag(): String = "release-keys"
        override fun isPackageInstalled(packageName: String): Boolean = false
    }

    private object NoopJailbreakContext : JailbreakCheckContext {
        override fun fileExists(path: String): Boolean = false
        override fun directoryContents(path: String): List<String> = emptyList()
        override fun canFork(): Boolean = false
        override fun canWriteOutsideSandbox(path: String): Boolean = false
    }
}
