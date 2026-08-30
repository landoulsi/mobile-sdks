package com.landoulsi.remoteconfig

import cocoapods.FirebaseRemoteConfig.FIRRemoteConfig
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigFetchAndActivateStatus
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigFetchAndActivateStatusSuccessFetchedFromRemote
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigFetchAndActivateStatusSuccessUsingPreFetchedData
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigSettings
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigSourceStatic
import cocoapods.FirebaseRemoteConfig.FIRRemoteConfigValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

/**
 * iOS implementation of [RemoteConfigManager] backed by Firebase Remote Config CocoaPod SDK.
 *
 * Supports remote parameter fetching, activation, in-app defaults, and in-memory local developer overrides.
 */
@OptIn(ExperimentalForeignApi::class)
class FirebaseRemoteConfigManager(
    private val remoteConfig: FIRRemoteConfig = FIRRemoteConfig.remoteConfig(),
) : RemoteConfigManager {

    private val mutex = Mutex()
    private val localOverrides = mutableMapOf<String, Any>()

    /**
     * Applies the given [RemoteConfigSettings] to Firebase Remote Config.
     */
    suspend fun setConfigSettings(settings: RemoteConfigSettings): Result<Unit> = runCatchingCancelling {
        val configSettings = FIRRemoteConfigSettings().apply {
            minimumFetchInterval = settings.minimumFetchInterval.inWholeSeconds.toDouble()
            fetchTimeout = settings.fetchTimeout.inWholeSeconds.toDouble()
        }
        remoteConfig.configSettings = configSettings
        Unit
    }

    override suspend fun fetchAndActivate(): Result<Boolean> = runCatchingCancelling {
        suspendCancellableCoroutine { continuation ->
            remoteConfig.fetchAndActivateWithCompletionHandler { status, error ->
                if (error != null) {
                    continuation.resume(Result.failure(Exception(error.localizedDescription)))
                } else {
                    val activated = status == FIRRemoteConfigFetchAndActivateStatusSuccessFetchedFromRemote ||
                            status == FIRRemoteConfigFetchAndActivateStatusSuccessUsingPreFetchedData
                    continuation.resume(Result.success(activated))
                }
            }
        }.getOrThrow()
    }

    override suspend fun fetch(minimumFetchIntervalInSeconds: Long?): Result<Unit> = runCatchingCancelling {
        suspendCancellableCoroutine { continuation ->
            val completion: (FIRRemoteConfigFetchAndActivateStatus, platform.Foundation.NSError?) -> Unit = { _, error ->
                if (error != null) {
                    continuation.resume(Result.failure(Exception(error.localizedDescription)))
                } else {
                    continuation.resume(Result.success(Unit))
                }
            }

            if (minimumFetchIntervalInSeconds != null) {
                remoteConfig.fetchWithExpirationDuration(
                    minimumFetchIntervalInSeconds.toDouble(),
                    completionHandler = { _, error ->
                        if (error != null) {
                            continuation.resume(Result.failure(Exception(error.localizedDescription)))
                        } else {
                            continuation.resume(Result.success(Unit))
                        }
                    }
                )
            } else {
                remoteConfig.fetchWithCompletionHandler { _, error ->
                    if (error != null) {
                        continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    } else {
                        continuation.resume(Result.success(Unit))
                    }
                }
            }
        }.getOrThrow()
    }

    override suspend fun activate(): Result<Boolean> = runCatchingCancelling {
        suspendCancellableCoroutine { continuation ->
            remoteConfig.activateWithCompletion { changed, error ->
                if (error != null) {
                    continuation.resume(Result.failure(Exception(error.localizedDescription)))
                } else {
                    continuation.resume(Result.success(changed))
                }
            }
        }.getOrThrow()
    }

    override suspend fun setDefaults(defaults: Map<String, Any?>): Result<Unit> = runCatchingCancelling {
        @Suppress("UNCHECKED_CAST")
        val nonNullDefaults = defaults.filterValues { it != null } as Map<Any?, *>
        remoteConfig.setDefaults(nonNullDefaults)
        Unit
    }

    override suspend fun getString(key: String): Result<String?> = runCatchingCancelling {
        mutex.withLock { localOverrides[key] }?.toString() ?: run {
            val value = remoteConfig.configValueForKey(key)
            if (isStaticEmpty(value)) null else value.stringValue
        }
    }

    override suspend fun getBoolean(key: String): Result<Boolean?> = runCatchingCancelling {
        val local = mutex.withLock { localOverrides[key] }
        if (local != null) {
            when (local) {
                is Boolean -> local
                is String -> local.toBooleanStrictOrNull()
                is Number -> local.toInt() != 0
                else -> null
            }
        } else {
            val value = remoteConfig.configValueForKey(key)
            if (isStaticEmpty(value)) null else value.boolValue
        }
    }

    override suspend fun getInt(key: String): Result<Int?> = runCatchingCancelling {
        val local = mutex.withLock { localOverrides[key] }
        if (local != null) {
            when (local) {
                is Number -> local.toInt()
                is String -> local.toIntOrNull()
                is Boolean -> if (local) 1 else 0
                else -> null
            }
        } else {
            val value = remoteConfig.configValueForKey(key)
            if (isStaticEmpty(value)) null else value.numberValue.intValue
        }
    }

    override suspend fun getLong(key: String): Result<Long?> = runCatchingCancelling {
        val local = mutex.withLock { localOverrides[key] }
        if (local != null) {
            when (local) {
                is Number -> local.toLong()
                is String -> local.toLongOrNull()
                is Boolean -> if (local) 1L else 0L
                else -> null
            }
        } else {
            val value = remoteConfig.configValueForKey(key)
            if (isStaticEmpty(value)) null else value.numberValue.longLongValue
        }
    }

    override suspend fun getDouble(key: String): Result<Double?> = runCatchingCancelling {
        val local = mutex.withLock { localOverrides[key] }
        if (local != null) {
            when (local) {
                is Number -> local.toDouble()
                is String -> local.toDoubleOrNull()
                else -> null
            }
        } else {
            val value = remoteConfig.configValueForKey(key)
            if (isStaticEmpty(value)) null else value.numberValue.doubleValue
        }
    }

    override suspend fun putString(key: String, value: String): Result<Unit> = runCatchingCancelling {
        mutex.withLock { localOverrides[key] = value }
        Unit
    }

    override suspend fun putBoolean(key: String, value: Boolean): Result<Unit> = runCatchingCancelling {
        mutex.withLock { localOverrides[key] = value }
        Unit
    }

    override suspend fun putInt(key: String, value: Int): Result<Unit> = runCatchingCancelling {
        mutex.withLock { localOverrides[key] = value }
        Unit
    }

    override suspend fun putLong(key: String, value: Long): Result<Unit> = runCatchingCancelling {
        mutex.withLock { localOverrides[key] = value }
        Unit
    }

    override suspend fun putDouble(key: String, value: Double): Result<Unit> = runCatchingCancelling {
        mutex.withLock { localOverrides[key] = value }
        Unit
    }

    suspend fun clearLocalOverrides() = mutex.withLock {
        localOverrides.clear()
    }

    private fun isStaticEmpty(value: FIRRemoteConfigValue): Boolean {
        return value.source == FIRRemoteConfigSourceStatic && (value.stringValue.isNullOrEmpty())
    }

    private inline fun <T> runCatchingCancelling(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
}
