package com.landoulsi.remoteconfig

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * In-memory thread-safe implementation of [RemoteConfigManager] for testing and local development.
 */
class InMemoryRemoteConfigManager(
    initialValues: Map<String, Any?> = emptyMap(),
) : RemoteConfigManager {

    private val mutex = Mutex()
    private val defaults = mutableMapOf<String, Any?>()
    private val remoteStaging = mutableMapOf<String, Any?>()
    private val activeRemote = mutableMapOf<String, Any?>()
    private val localOverrides = mutableMapOf<String, Any?>()

    init {
        activeRemote.putAll(initialValues)
    }

    /**
     * Staging helper for tests to simulate remote changes before activation.
     */
    suspend fun stageRemoteConfig(values: Map<String, Any?>) = mutex.withLock {
        remoteStaging.clear()
        remoteStaging.putAll(values)
    }

    override suspend fun fetchAndActivate(): Result<Boolean> = mutex.withLock {
        runCatching {
            val hasChanges = remoteStaging.isNotEmpty()
            activeRemote.putAll(remoteStaging)
            remoteStaging.clear()
            hasChanges
        }
    }

    override suspend fun fetch(minimumFetchIntervalInSeconds: Long?): Result<Unit> = mutex.withLock {
        Result.success(Unit)
    }

    override suspend fun activate(): Result<Boolean> = mutex.withLock {
        runCatching {
            val hasChanges = remoteStaging.isNotEmpty()
            activeRemote.putAll(remoteStaging)
            remoteStaging.clear()
            hasChanges
        }
    }

    override suspend fun setDefaults(defaults: Map<String, Any?>): Result<Unit> = mutex.withLock {
        runCatching {
            this.defaults.clear()
            this.defaults.putAll(defaults)
        }
    }

    override suspend fun getString(key: String): Result<String?> = mutex.withLock {
        runCatching {
            findValue(key)?.toString()
        }
    }

    override suspend fun getBoolean(key: String): Result<Boolean?> = mutex.withLock {
        runCatching {
            when (val value = findValue(key)) {
                is Boolean -> value
                is String -> value.toBooleanStrictOrNull()
                is Number -> value.toInt() != 0
                null -> null
                else -> null
            }
        }
    }

    override suspend fun getInt(key: String): Result<Int?> = mutex.withLock {
        runCatching {
            when (val value = findValue(key)) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull()
                is Boolean -> if (value) 1 else 0
                null -> null
                else -> null
            }
        }
    }

    override suspend fun getLong(key: String): Result<Long?> = mutex.withLock {
        runCatching {
            when (val value = findValue(key)) {
                is Number -> value.toLong()
                is String -> value.toLongOrNull()
                is Boolean -> if (value) 1L else 0L
                null -> null
                else -> null
            }
        }
    }

    override suspend fun getDouble(key: String): Result<Double?> = mutex.withLock {
        runCatching {
            when (val value = findValue(key)) {
                is Number -> value.toDouble()
                is String -> value.toDoubleOrNull()
                null -> null
                else -> null
            }
        }
    }

    override suspend fun putString(key: String, value: String): Result<Unit> = mutex.withLock {
        runCatching { localOverrides[key] = value }
    }

    override suspend fun putBoolean(key: String, value: Boolean): Result<Unit> = mutex.withLock {
        runCatching { localOverrides[key] = value }
    }

    override suspend fun putInt(key: String, value: Int): Result<Unit> = mutex.withLock {
        runCatching { localOverrides[key] = value }
    }

    override suspend fun putLong(key: String, value: Long): Result<Unit> = mutex.withLock {
        runCatching { localOverrides[key] = value }
    }

    override suspend fun putDouble(key: String, value: Double): Result<Unit> = mutex.withLock {
        runCatching { localOverrides[key] = value }
    }

    private fun findValue(key: String): Any? {
        return localOverrides[key] ?: activeRemote[key] ?: defaults[key]
    }
}
