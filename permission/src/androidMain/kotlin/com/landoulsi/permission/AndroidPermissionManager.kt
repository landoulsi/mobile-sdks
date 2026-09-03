package com.landoulsi.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class AndroidPermissionManager(private val context: Context) : PermissionManager {

    override fun checkPermission(permission: Permission): PermissionState {
        val androidPermission = permission.toAndroidPermission() ?: return PermissionState.NOT_DETERMINED
        
        return if (ContextCompat.checkSelfPermission(context, androidPermission) == PackageManager.PERMISSION_GRANTED) {
            PermissionState.GRANTED
        } else {
            PermissionState.DENIED
        }
    }

    override suspend fun requestPermission(permission: Permission): PermissionState {
        // Requesting permissions in Android from a generic context requires starting an Activity
        // or using an ActivityResultLauncher. This is typically implemented using a headless 
        // fragment or an invisible activity to handle the callback.
        // For architectural design purposes, this is left as a placeholder or to be handled 
        // by the calling Activity/Fragment using standard Android APIs, while checking remains synchronous.
        TODO("Implementing full suspendable permission request requires an Activity context or a transparent Activity to wrap ActivityResult API")
    }

    private fun Permission.toAndroidPermission(): String? {
        return when (this) {
            Permission.CAMERA -> Manifest.permission.CAMERA
            Permission.LOCATION_FOREGROUND -> Manifest.permission.ACCESS_FINE_LOCATION
            Permission.LOCATION_BACKGROUND -> Manifest.permission.ACCESS_BACKGROUND_LOCATION
            Permission.MICROPHONE -> Manifest.permission.RECORD_AUDIO
            Permission.BLUETOOTH -> Manifest.permission.BLUETOOTH_CONNECT
            Permission.NOTIFICATIONS -> "android.permission.POST_NOTIFICATIONS" // Manifest.permission.POST_NOTIFICATIONS for API 33+
            Permission.STORAGE_READ -> Manifest.permission.READ_EXTERNAL_STORAGE
            Permission.STORAGE_WRITE -> Manifest.permission.WRITE_EXTERNAL_STORAGE
            Permission.CONTACTS -> Manifest.permission.READ_CONTACTS
        }
    }
}
