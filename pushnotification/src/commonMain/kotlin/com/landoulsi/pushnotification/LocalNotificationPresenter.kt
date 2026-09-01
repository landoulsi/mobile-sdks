package com.landoulsi.pushnotification

/**
 * Presents and cancels local notifications on the device.
 *
 * Remote delivery is a server-side responsibility; this interface only
 * handles on-device presentation of notifications.
 */
interface LocalNotificationPresenter {

    fun showLocalNotification(notification: PushNotification, channelId: String)

    fun cancelNotification(id: String)

    fun cancelAllNotifications()
}
