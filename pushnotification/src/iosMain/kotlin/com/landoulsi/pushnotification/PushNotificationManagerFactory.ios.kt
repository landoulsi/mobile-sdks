package com.landoulsi.pushnotification

actual object PushNotificationManagerFactory {

    private val instance = IosPushNotificationManager()

    actual fun create(): PushNotificationManager {
        return instance
    }
}
