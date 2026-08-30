package com.trackmit.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dev.zacsweers.metro.Inject

@Inject
class AndroidSecureStorage constructor(context: Context) : SecureStorage {

    private val encryptedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFERENCES_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun getString(key: String): String? = encryptedPreferences.getString(key, null)

    override fun putString(key: String, value: String) {
        encryptedPreferences.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        encryptedPreferences.edit().remove(key).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "trackmit_secure_settings"
    }
}
