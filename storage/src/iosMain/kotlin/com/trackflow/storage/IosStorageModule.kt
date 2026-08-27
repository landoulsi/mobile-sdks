package com.trackflow.storage

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import platform.Foundation.NSUserDefaults

@ContributesTo(StorageScope::class)
interface IosStorageModule {
    @Provides
    fun provideSettings(): Settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
}
