package com.landoulsi.remoteconfig

/**
 * Multiplatform contract for fetching, activating, and retrieving remote configuration parameters.
 *
 * Supports typed getters, in-app defaults, local developer overrides, and remote activation lifecycle.
 */
interface RemoteConfigManager {

    /**
     * Asynchronously fetches parameters and activates them immediately.
     *
     * @return [Result] containing `true` if parameter values were updated/activated, `false` otherwise.
     */
    suspend fun fetchAndActivate(): Result<Boolean>

    /**
     * Asynchronously fetches remote configuration data from the server.
     *
     * @param minimumFetchIntervalInSeconds optional cache expiration in seconds for this fetch.
     */
    suspend fun fetch(minimumFetchIntervalInSeconds: Long? = null): Result<Unit>

    /**
     * Asynchronously activates previously fetched parameters.
     *
     * @return [Result] containing `true` if newly fetched parameters were activated.
     */
    suspend fun activate(): Result<Boolean>

    /**
     * Sets in-app default parameter values.
     *
     * @param defaults map of parameter names to their default values.
     */
    suspend fun setDefaults(defaults: Map<String, Any?>): Result<Unit>

    /**
     * Retrieves the String value for the given [key], or null if absent or invalid.
     */
    suspend fun getString(key: String): Result<String?>

    /**
     * Retrieves the Boolean value for the given [key], or null if absent or invalid.
     */
    suspend fun getBoolean(key: String): Result<Boolean?>

    /**
     * Retrieves the Int value for the given [key], or null if absent or invalid.
     */
    suspend fun getInt(key: String): Result<Int?>

    /**
     * Retrieves the Long value for the given [key], or null if absent or invalid.
     */
    suspend fun getLong(key: String): Result<Long?>

    /**
     * Retrieves the Double value for the given [key], or null if absent or invalid.
     */
    suspend fun getDouble(key: String): Result<Double?>

    /**
     * Stores a local in-memory override for the given [key].
     */
    suspend fun putString(key: String, value: String): Result<Unit>

    /**
     * Stores a local in-memory override for the given [key].
     */
    suspend fun putBoolean(key: String, value: Boolean): Result<Unit>

    /**
     * Stores a local in-memory override for the given [key].
     */
    suspend fun putInt(key: String, value: Int): Result<Unit>

    /**
     * Stores a local in-memory override for the given [key].
     */
    suspend fun putLong(key: String, value: Long): Result<Unit>

    /**
     * Stores a local in-memory override for the given [key].
     */
    suspend fun putDouble(key: String, value: Double): Result<Unit>

    /**
     * Clears any active local in-memory overrides.
     */
    suspend fun clearLocalOverrides(): Result<Unit>
}
