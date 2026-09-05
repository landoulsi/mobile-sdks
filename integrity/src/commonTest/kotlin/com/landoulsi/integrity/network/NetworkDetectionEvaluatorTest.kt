package com.landoulsi.integrity.network

import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.SignalSeverity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkDetectionEvaluatorTest {

    private class FakeNetworkCheckContext(
        private val vpnActive: Boolean = false,
        private val systemProxyConfigured: Boolean = false,
        private val adbEnabled: Boolean = false,
        private val shouldThrowOnVpn: Boolean = false,
        private val shouldThrowOnProxy: Boolean = false,
        private val shouldThrowOnAdb: Boolean = false,
    ) : NetworkCheckContext {
        override fun isVpnActive(): Boolean {
            if (shouldThrowOnVpn) throw IllegalStateException("Simulated VPN error")
            return vpnActive
        }

        override fun isSystemProxyConfigured(): Boolean {
            if (shouldThrowOnProxy) throw IllegalStateException("Simulated proxy error")
            return systemProxyConfigured
        }

        override fun isAdbEnabled(): Boolean {
            if (shouldThrowOnAdb) throw IllegalStateException("Simulated ADB error")
            return adbEnabled
        }
    }

    @Test
    fun cleanEnvironmentProducesNoSignals() = runTest {
        val context = FakeNetworkCheckContext()
        val evaluator = NetworkDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
        assertEquals(IntegrityCategory.NETWORK_ANOMALY, evaluator.category)
        assertEquals(NetworkSignal.all, evaluator.knownSignalIds)
    }

    @Test
    fun vpnActiveDetection() = runTest {
        val context = FakeNetworkCheckContext(vpnActive = true)
        val evaluator = NetworkDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(1, signals.size)
        val signal = signals.first()
        assertEquals(NetworkSignal.VPN_ACTIVE, signal.id)
        assertEquals(IntegrityCategory.NETWORK_ANOMALY, signal.category)
        assertEquals(SignalSeverity.MEDIUM, signal.severity)
        assertEquals(0.95, signal.confidence)
        assertEquals(NetworkSignal.Check.VPN_ACTIVE, signal.metadata["check"])
    }

    @Test
    fun systemProxyDetection() = runTest {
        val context = FakeNetworkCheckContext(systemProxyConfigured = true)
        val evaluator = NetworkDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(1, signals.size)
        val signal = signals.first()
        assertEquals(NetworkSignal.SYSTEM_PROXY_ACTIVE, signal.id)
        assertEquals(IntegrityCategory.NETWORK_ANOMALY, signal.category)
        assertEquals(SignalSeverity.HIGH, signal.severity)
        assertEquals(0.9, signal.confidence)
        assertEquals(NetworkSignal.Check.SYSTEM_PROXY, signal.metadata["check"])
    }

    @Test
    fun adbEnabledDetection() = runTest {
        val context = FakeNetworkCheckContext(adbEnabled = true)
        val evaluator = NetworkDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(1, signals.size)
        val signal = signals.first()
        assertEquals(NetworkSignal.DEVELOPER_ADB_ENABLED, signal.id)
        assertEquals(IntegrityCategory.NETWORK_ANOMALY, signal.category)
        assertEquals(SignalSeverity.MEDIUM, signal.severity)
        assertEquals(1.0, signal.confidence)
        assertEquals(NetworkSignal.Check.DEVELOPER_ADB, signal.metadata["check"])
    }

    @Test
    fun compoundNetworkAnomaliesDetection() = runTest {
        val context = FakeNetworkCheckContext(
            vpnActive = true,
            systemProxyConfigured = true,
            adbEnabled = true,
        )
        val evaluator = NetworkDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(3, signals.size)
        assertTrue(signals.any { it.id == NetworkSignal.VPN_ACTIVE })
        assertTrue(signals.any { it.id == NetworkSignal.SYSTEM_PROXY_ACTIVE })
        assertTrue(signals.any { it.id == NetworkSignal.DEVELOPER_ADB_ENABLED })
    }

    @Test
    fun evaluatorFaultToleranceWhenCheckThrows() = runTest {
        val context = FakeNetworkCheckContext(
            vpnActive = true,
            shouldThrowOnProxy = true,
            shouldThrowOnAdb = true,
        )
        val evaluator = NetworkDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertEquals(1, signals.size)
        assertEquals(NetworkSignal.VPN_ACTIVE, signals.first().id)
    }

    @Test
    fun evaluatorFaultToleranceWhenAllChecksThrow() = runTest {
        val context = FakeNetworkCheckContext(
            shouldThrowOnVpn = true,
            shouldThrowOnProxy = true,
            shouldThrowOnAdb = true,
        )
        val evaluator = NetworkDetectionEvaluator(context)

        val signals = evaluator.evaluate()

        assertTrue(signals.isEmpty())
    }
}

