package com.landoulsi.permission

/**
 * Represents the current state of a permission.
 */
enum class PermissionState {
    /**
     * Permission has been granted by the user.
     */
    GRANTED,

    /**
     * Permission has been denied by the user.
     */
    DENIED,

    /**
     * Permission has not been requested yet or its status is undetermined.
     */
    NOT_DETERMINED
}
