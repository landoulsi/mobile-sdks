package com.landoulsi.remoteconfig

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

/**
 * Android implementation of [RemoteConfigManager] backed by Firebase Remote Config (`play-services-config`).
 *
 * Supports remote parameter fetching, activation, in-app defaults, and in-memory local developer overrides.
 */
class FirebaseRemoteConfigManager(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
) : RemoteConfigManager {

    private val localOverrides = ConcurrentHashMap<String, Any>()

    /**
     * Applies the given [RemoteConfigSettings] to Firebase Remote Config.
     */
    suspend fun setConfigSettings(settings: RemoteConfigSettings): Result<Unit> = runCatchingCancelling {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(settings.minimumFetchInterval.inWholeSeconds)
            .setFetchTimeoutInSeconds(settings.fetchTimeout.inWholeSeconds)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings).await()
        Unit
    }

    override suspend fun fetchAndActivate(): Result<Boolean> = runCatchingCancelling {
        remoteConfig.fetchAndActivate().await()
    }

    override suspend fun fetch(minimumFetchIntervalInSeconds: Long?): Result<Unit> = runCatchingCancelling {
        if (minimumFetchIntervalInSeconds != null) {
            remoteConfig.fetch(minimumFetchIntervalInSeconds).await()
        } else {
            remoteConfig.fetch().await()
        }
        Unit
    }

    override suspend fun activate(): Result<Boolean> = runCatchingCancelling {
        remoteConfig.activate().await()
    }

    override suspend fun setDefaults(defaults: Map<String, Any?>): Result<Unit> = runCatchingCancelling {
        @Suppress("UNCHECKED_CAST")
        val nonNullDefaults = defaults.filterValues { it != null } as Map<String, Any>
        remoteConfig.setDefaultsAsync(nonNullDefaults).await()
        Unit
    }

    override suspend fun getString(key: String): Result<String?> = runCatchingCancelling {
        localOverrides[key]?.toString() ?: run {
            val value = remoteConfig.getValue(key)
            if (isStaticEmpty(value)) null else value.asString()
        }
    }

    override suspend fun getBoolean(key: String): Result<Boolean?> = runCatchingCancelling {
        val local = localOverrides[key]
        if (local != null) {
            when (local) {
                is Boolean -> local
                is String -> local.toBooleanStrictOrNull()
                is Number -> local.toInt() != 0
                else -> null
            }
        } else {
            val value = remoteConfig.getValue(key)
            if (isStaticEmpty(value)) null else value.asBoolean()
        }
    }

    override suspend fun getInt(key: String): Result<Int?> = runCatchingCancelling {
        val local = localOverrides[key]
        if (local != null) {
            when (local) {
                is Number -> local.toInt()
                is String -> local.toIntOrNull()
                is Boolean -> if (local) 1 else 0
                else -> null
            }
        } else {
            val value = remoteConfig.getValue(key)
            if (isStaticEmpty(value)) null else value.asLong().toInt()
        }
    }

    override suspend fun getLong(key: String): Result<Long?> = runCatchingCancelling {
        val local = localOverrides[key]
        if (local != null) {
            when (local) {
                is Number -> local.toLong()
                is String -> local.toLongOrNull()
                is Boolean -> if (local) 1L else 0L
                else -> null
            }
        } else {
            val value = remoteConfig.getValue(key)
            if (isStaticEmpty(value)) null else value.asLong()
        }
    }

    override suspend fun getDouble(key: String): Result<Double?> = runCatchingCancelling {
        val local = localOverrides[key]
        if (local != null) {
            when (local) {
                is Number -> local.toDouble()
                is String -> local.toDoubleOrNull()
                else -> null
            }
        } else {
            val value = remoteConfig.getValue(key)
            if (isStaticEmpty(value)) null else value.asDouble()
        }
    }

    override suspend fun putString(key: String, value: String): Result<Unit> = runCatchingCancelling {
        localOverrides[key] = value
        Unit
    }

    override suspend fun putBoolean(key: String, value: Boolean): Result<Unit> = runCatchingCancelling {
        localOverrides[key] = value
        Unit
    }

    override suspend fun putInt(key: String, value: Int): Result<Unit> = runCatchingCancelling {
        localOverrides[key] = value
        Unit
    }

    override suspend fun putLong(key: String, value: Long): Result<Unit> = runCatchingCancelling {
        localOverrides[key] = value
        Unit
    }

    override suspend fun putDouble(key: String, value: Double): Result<Unit> = runCatchingCancelling {
        localOverrides[key] = value
        Unit
    }

    /**
     * Clears any active local overrides.
     */
    override suspend fun clearLocalOverrides(): Result<Unit> = runCatchingCancelling {
        localOverrides.clear()
    }

    private fun isStaticEmpty(value: FirebaseRemoteConfigValue): Boolean =
        value.source == FirebaseRemoteConfig.VALUE_SOURCE_STATIC && value.asString().isEmpty()

    private inline fun <T> runCatchingCancelling(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
