package com.trackmit.storage

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(StorageScope::class)
interface AndroidStorageModule {
    @Provides
    fun provideSettings(context: Context): Settings {
        val sharedPreferences = context.getSharedPreferences("trackmit_settings", Context.MODE_PRIVATE)
        return SharedPreferencesSettings(sharedPreferences)
    }
}
