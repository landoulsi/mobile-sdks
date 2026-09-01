package com.landoulsi.pushnotification

/**
 * Controls notification permission / user consent.
 *
 * Permission requests are inherently asynchronous on both platforms
 * (Android needs an Activity + ActivityResultLauncher; iOS has a completion
 * handler), so [requestPermission] is a suspend function.
 */
interface NotificationPermissionController {

    suspend fun requestPermission(): PermissionStatus

    suspend fun getPermissionStatus(): PermissionStatus

    enum class PermissionStatus {
        GRANTED,
        DENIED,
        NOT_DETERMINED,
    }
}
