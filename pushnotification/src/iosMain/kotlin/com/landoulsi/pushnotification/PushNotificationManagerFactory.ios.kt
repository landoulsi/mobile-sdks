package com.landoulsi.pushnotification

actual object PushNotificationManagerFactory {
    actual fun create(): PushNotificationManager {
        return IosPushNotificationManager()
    }
}