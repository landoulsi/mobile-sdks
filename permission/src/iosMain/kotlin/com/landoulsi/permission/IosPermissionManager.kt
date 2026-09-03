package com.landoulsi.permission

// Basic implementation structure for iOS
class IosPermissionManager : PermissionManager {

    override fun checkPermission(permission: Permission): PermissionState {
        // Here you would use platform-specific code, e.g.:
        // when (permission) {
        //     Permission.LOCATION_FOREGROUND -> {
        //         val status = CLLocationManager.authorizationStatus()
        //         ... map status to PermissionState ...
        //     }
        //     ...
        // }
        // For the sake of the architectural design:
        return PermissionState.NOT_DETERMINED
    }

    override suspend fun requestPermission(permission: Permission): PermissionState {
        // Here you would use platform-specific APIs to request permission, e.g.:
        // AVBase.requestAccessForMediaType(AVMediaTypeVideo) { granted -> ... }
        // For the sake of architectural design:
        TODO("Implementing full suspendable permission request requires wrapping iOS completion handlers into suspend coroutines using suspendCoroutine or suspendCancellableCoroutine")
    }
}
