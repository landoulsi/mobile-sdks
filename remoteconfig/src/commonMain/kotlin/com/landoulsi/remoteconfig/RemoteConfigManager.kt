package com.landoulsi.remoteconfig

/**
 * Generic interface for handling remote configuration and user state.
 */
interface RemoteConfigManager {
    
    // Generic read operations
    suspend fun getString(key: String): Result<String?>
    suspend fun getBoolean(key: String): Result<Boolean?>
    suspend fun getInt(key: String): Result<Int?>
    
    // Generic write operations (for user state)
    suspend fun putString(key: String, value: String): Result<Unit>
    suspend fun putBoolean(key: String, value: Boolean): Result<Unit>
    suspend fun putInt(key: String, value: Int): Result<Unit>
}
