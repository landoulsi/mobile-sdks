package com.landoulsi.timeprovider

import com.google.android.gms.time.TrustedTimeClient
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidTimeProviderTest {

    private fun createFakeTrustedTimeClient(
        epochMillis: Long?,
        shouldThrow: Boolean = false,
    ): TrustedTimeClient {
        return Proxy.newProxyInstance(
            TrustedTimeClient::class.java.classLoader,
            arrayOf(TrustedTimeClient::class.java),
        ) { _, method, _ ->
            if (method.name == "computeCurrentUnixEpochMillis") {
                if (shouldThrow) {
                    throw IllegalStateException("Simulated Play Services TrustedTime error")
                }
                epochMillis
            } else {
                null
            }
        } as TrustedTimeClient
    }

    @Test
    fun testAndroidTimeProviderWithNullClientFallsBackToSystemClock() {
        val provider = AndroidTimeProvider(initialClient = null)
        val time = provider.currentTimeMillis()
        assertTrue(time > 0L)
    }

    @Test
    fun testAndroidTimeProviderWithValidTrustedTimeClient() {
        val expectedEpoch = 1_725_000_000_000L
        val fakeClient = createFakeTrustedTimeClient(expectedEpoch)
        val provider = AndroidTimeProvider(initialClient = fakeClient)

        assertEquals(expectedEpoch, provider.currentTimeMillis())
    }

    @Test
    fun testAndroidTimeProviderWithUnsynchronizedTrustedTimeClientFallsBack() {
        val fakeClient = createFakeTrustedTimeClient(null)
        val provider = AndroidTimeProvider(initialClient = fakeClient)

        val time = provider.currentTimeMillis()
        assertTrue(time > 0L)
    }

    @Test
    fun testAndroidTimeProviderWithZeroOrNegativeEpochFallsBack() {
        val fakeClient = createFakeTrustedTimeClient(0L)
        val provider = AndroidTimeProvider(initialClient = fakeClient)

        val time = provider.currentTimeMillis()
        assertTrue(time > 0L)
    }

    @Test
    fun testAndroidTimeProviderWithThrowingClientGracefullyFallsBack() {
        val fakeClient = createFakeTrustedTimeClient(null, shouldThrow = true)
        val provider = AndroidTimeProvider(initialClient = fakeClient)

        val time = provider.currentTimeMillis()
        assertTrue(time > 0L)
    }
}
