package com.landoulsi.pushnotification

import android.content.Context
import java.lang.ref.WeakReference

actual object PushNotificationManagerFactory {

    private var appContext: Context? = null
    private var allowedDeepLinkSchemes: Set<String> = emptySet()
    
    // Use a weak reference or nullable var that gets cleared, but since it's a lambda,
    // we just use a nullable var and expect the caller to clear it or we just don't store it statically.
    // However, create() is called once to create the manager. If we pass the lambda at creation,
    // the manager holds it. We can just pass the lambda to initialize and then clear it.
    private var pendingPermissionRequester: (suspend () -> Boolean)? = null

    /**
     * Initializes the factory with an application configuration. Must be called
     * before [create] on Android.
     */
    fun initialize(
        context: Context,
        allowedDeepLinkSchemes: Set<String> = emptySet(),
        permissionRequester: suspend () -> Boolean = { false },
    ) {
        this.appContext = context.applicationContext
        this.allowedDeepLinkSchemes = allowedDeepLinkSchemes
        this.pendingPermissionRequester = permissionRequester
    }

    actual fun create(): PushNotificationManager {
        val ctx = appContext
            ?: throw IllegalStateException(
                "PushNotificationManagerFactory.initialize(context, ...) must be called before create()"
            )
        val requester = pendingPermissionRequester ?: { false }
        
        // Clear the pending requester so we don't hold a static reference to an Activity lambda
        pendingPermissionRequester = null
        
        return FirebasePushNotificationManager(ctx, allowedDeepLinkSchemes, requester)
    }
}
