package com.landoulsi.pushnotification

/**
 * Platform-specific factory for creating a [PushNotificationManager].
 *
 * Uses `expect object` to provide a singleton factory on each platform.
 * On Android, [initialize] must be called before [create] to supply the
 * application configuration (deep link schemes, permission requester).
 * On iOS, no initialization is required.
 */
expect object PushNotificationManagerFactory {
    fun create(): PushNotificationManager
}