package com.landoulsi.storage

import com.russhwolf.settings.Settings
import dev.zacsweers.metro.Inject

@Inject
class StorageManager(private val settings: Settings) {
    fun getString(key: String, defaultValue: String? = null): String? = settings.getStringOrNull(key) ?: defaultValue

    fun putString(key: String, value: String) {
        settings.putString(key, value)
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean = settings.getBoolean(key, defaultValue)

    fun putBoolean(key: String, value: Boolean) {
        settings.putBoolean(key, value)
    }

    fun getInt(key: String, defaultValue: Int = 0): Int = settings.getInt(key, defaultValue)

    fun putInt(key: String, value: Int) {
        settings.putInt(key, value)
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long = settings.getLong(key, defaultValue)

    fun putLong(key: String, value: Long) {
        settings.putLong(key, value)
    }

    fun remove(key: String) {
        settings.remove(key)
    }

    fun clear() {
        settings.clear()
    }
}
