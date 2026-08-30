package com.landoulsi.remoteconfig

/**
 * Generic interface for handling remote configuration, synchronization, and local overrides.
 */
interface RemoteConfigManager {

    /**
     * Fetches parameter values from the remote configuration service and activates them.
     *
     * @return [Result] containing `true` if previously un-activated fetched values were activated,
     *         `false` if already up-to-date, or a failure if an error occurred.
     */
    suspend fun fetchAndActivate(): Result<Boolean>

    /**
     * Fetches parameter values from the remote server without activating them.
     *
     * @param minimumFetchIntervalInSeconds Optional minimum fetch interval in seconds.
     */
    suspend fun fetch(minimumFetchIntervalInSeconds: Long? = null): Result<Unit>

    /**
     * Activates the most recently fetched configuration.
     *
     * @return [Result] containing `true` if new fetched values were activated, `false` otherwise.
     */
    suspend fun activate(): Result<Boolean>

    /**
     * Sets in-app default parameter values using a map of key-value pairs.
     */
    suspend fun setDefaults(defaults: Map<String, Any?>): Result<Unit>

    // Generic read operations
    suspend fun getString(key: String): Result<String?>
    suspend fun getBoolean(key: String): Result<Boolean?>
    suspend fun getInt(key: String): Result<Int?>
    suspend fun getLong(key: String): Result<Long?>
    suspend fun getDouble(key: String): Result<Double?>

    // Generic write operations (for user state / local developer overrides)
    suspend fun putString(key: String, value: String): Result<Unit>
    suspend fun putBoolean(key: String, value: Boolean): Result<Unit>
    suspend fun putInt(key: String, value: Int): Result<Unit>
    suspend fun putLong(key: String, value: Long): Result<Unit>
    suspend fun putDouble(key: String, value: Double): Result<Unit>
}
