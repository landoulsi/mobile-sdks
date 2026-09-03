package com.landoulsi.permission

/**
 * Public interface to check and request permissions in a cross-platform way.
 */
interface PermissionManager {
    
    /**
     * Checks the current state of a given permission.
     * 
     * @param permission The [Permission] to check.
     * @return The current [PermissionState].
     */
    fun checkPermission(permission: Permission): PermissionState

    /**
     * Requests the given permission from the user.
     * This will typically trigger a system prompt if the permission is not yet determined.
     * 
     * @param permission The [Permission] to request.
     * @return The updated [PermissionState] after the user interacts with the prompt.
     */
    suspend fun requestPermission(permission: Permission): PermissionState
}
