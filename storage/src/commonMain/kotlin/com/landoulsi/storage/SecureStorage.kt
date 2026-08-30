package com.landoulsi.storage

/**
 * Encrypted-backing counterpart to [StorageManager], for values that must not sit in plaintext
 * on disk (auth tokens). Non-sensitive settings should keep using [StorageManager].
 */
interface SecureStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun remove(key: String)
}
