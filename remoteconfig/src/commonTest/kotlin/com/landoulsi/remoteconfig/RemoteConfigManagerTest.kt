package com.landoulsi.remoteconfig

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RemoteConfigManagerTest {

    @Test
    fun testDefaultValuesRetrieval() = runTest {
        val manager = InMemoryRemoteConfigManager()
        manager.setDefaults(
            mapOf(
                "stringKey" to "hello",
                "booleanKey" to true,
                "intKey" to 42,
                "longKey" to 100L,
                "doubleKey" to 3.14,
                "nullKey" to null,
            )
        )

        assertEquals("hello", manager.getString("stringKey").getOrNull())
        assertEquals(true, manager.getBoolean("booleanKey").getOrNull())
        assertEquals(42, manager.getInt("intKey").getOrNull())
        assertEquals(100L, manager.getLong("longKey").getOrNull())
        assertEquals(3.14, manager.getDouble("doubleKey").getOrNull())
        assertNull(manager.getString("nullKey").getOrNull())
    }

    @Test
    fun testMissingKeyReturnsNull() = runTest {
        val manager = InMemoryRemoteConfigManager()

        assertNull(manager.getString("unknown").getOrNull())
        assertNull(manager.getBoolean("unknown").getOrNull())
        assertNull(manager.getInt("unknown").getOrNull())
        assertNull(manager.getLong("unknown").getOrNull())
        assertNull(manager.getDouble("unknown").getOrNull())
    }

    @Test
    fun testRemoteStagingAndActivation() = runTest {
        val manager = InMemoryRemoteConfigManager(initialValues = mapOf("featureEnabled" to false))
        assertEquals(false, manager.getBoolean("featureEnabled").getOrNull())

        manager.stageRemoteConfig(mapOf("featureEnabled" to true, "newParam" to "value"))

        // Still old value before activation
        assertEquals(false, manager.getBoolean("featureEnabled").getOrNull())

        val activated = manager.activate().getOrThrow()
        assertTrue(activated)

        // New values after activation
        assertEquals(true, manager.getBoolean("featureEnabled").getOrNull())
        assertEquals("value", manager.getString("newParam").getOrNull())

        val secondActivate = manager.activate().getOrThrow()
        assertFalse(secondActivate)
    }

    @Test
    fun testFetchAndActivateFlow() = runTest {
        val manager = InMemoryRemoteConfigManager()
        manager.stageRemoteConfig(mapOf("promoBanner" to "50% OFF"))

        val changed = manager.fetchAndActivate().getOrThrow()
        assertTrue(changed)
        assertEquals("50% OFF", manager.getString("promoBanner").getOrNull())
    }

    @Test
    fun testLocalOverridesTakePrecedenceOverRemoteAndDefaults() = runTest {
        val manager = InMemoryRemoteConfigManager(initialValues = mapOf("serverUrl" to "https://prod.api.com"))
        manager.setDefaults(mapOf("serverUrl" to "https://default.api.com"))

        assertEquals("https://prod.api.com", manager.getString("serverUrl").getOrNull())

        manager.putString("serverUrl", "https://staging.api.com")
        assertEquals("https://staging.api.com", manager.getString("serverUrl").getOrNull())

        manager.putBoolean("isDebug", true)
        assertEquals(true, manager.getBoolean("isDebug").getOrNull())

        manager.putInt("retryCount", 5)
        assertEquals(5, manager.getInt("retryCount").getOrNull())

        manager.putLong("timeoutMs", 5000L)
        assertEquals(5000L, manager.getLong("timeoutMs").getOrNull())

        manager.putDouble("threshold", 0.85)
        assertEquals(0.85, manager.getDouble("threshold").getOrNull())
    }

    @Test
    fun testClearLocalOverridesRevertsToRemoteOrDefault() = runTest {
        val manager = InMemoryRemoteConfigManager(initialValues = mapOf("serverUrl" to "https://prod.api.com"))
        manager.putString("serverUrl", "https://override.api.com")
        assertEquals("https://override.api.com", manager.getString("serverUrl").getOrNull())

        manager.clearLocalOverrides().getOrThrow()
        assertEquals("https://prod.api.com", manager.getString("serverUrl").getOrNull())
    }

    @Test
    fun testCancellationExceptionIsRethrown() = runTest {
        val manager = object : RemoteConfigManager {
            override suspend fun fetchAndActivate(): Result<Boolean> = throw CancellationException("test cancel")
            override suspend fun fetch(minimumFetchIntervalInSeconds: Long?): Result<Unit> = Result.success(Unit)
            override suspend fun activate(): Result<Boolean> = Result.success(true)
            override suspend fun setDefaults(defaults: Map<String, Any?>): Result<Unit> = Result.success(Unit)
            override suspend fun getString(key: String): Result<String?> = Result.success(null)
            override suspend fun getBoolean(key: String): Result<Boolean?> = Result.success(null)
            override suspend fun getInt(key: String): Result<Int?> = Result.success(null)
            override suspend fun getLong(key: String): Result<Long?> = Result.success(null)
            override suspend fun getDouble(key: String): Result<Double?> = Result.success(null)
            override suspend fun putString(key: String, value: String): Result<Unit> = Result.success(Unit)
            override suspend fun putBoolean(key: String, value: Boolean): Result<Unit> = Result.success(Unit)
            override suspend fun putInt(key: String, value: Int): Result<Unit> = Result.success(Unit)
            override suspend fun putLong(key: String, value: Long): Result<Unit> = Result.success(Unit)
            override suspend fun putDouble(key: String, value: Double): Result<Unit> = Result.success(Unit)
            override suspend fun clearLocalOverrides(): Result<Unit> = Result.success(Unit)
        }

        assertFailsWith<CancellationException> {
            manager.fetchAndActivate()
        }
    }
}
