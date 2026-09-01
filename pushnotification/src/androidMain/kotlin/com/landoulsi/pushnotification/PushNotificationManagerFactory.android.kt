package com.landoulsi.pushnotification

import android.content.Context

actual object PushNotificationManagerFactory {

    private var instance: FirebasePushNotificationManager? = null

    /**
     * Initializes the factory with an application configuration. Must be called
     * before [create] on Android.
     */
    fun initialize(
        context: Context,
        allowedDeepLinkSchemes: Set<String> = emptySet(),
    ) {
        if (instance == null) {
            instance = FirebasePushNotificationManager(context, allowedDeepLinkSchemes)
        }
    }

    /**
     * Binds a permission requester lambda to the singleton manager.
     * The caller (typically an Activity) is responsible for calling [unbindPermissionRequester]
     * when destroyed to avoid leaking the Activity context.
     */
    fun bindPermissionRequester(permissionRequester: suspend () -> Boolean) {
        instance?.permissionRequester = permissionRequester
    }

    /**
     * Unbinds the permission requester to prevent memory leaks.
     */
    fun unbindPermissionRequester() {
        instance?.permissionRequester = null
    }

    actual fun create(): PushNotificationManager {
        return instance ?: throw IllegalStateException(
            "PushNotificationManagerFactory.initialize(context, ...) must be called before create()"
        )
    }
}
