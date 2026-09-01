package com.landoulsi.pushnotification

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UIKit.UIApplication
import platform.UIKit.registerForRemoteNotifications
import platform.UIKit.unregisterForRemoteNotifications
import kotlin.coroutines.resume

class IosPushNotificationManager : PushNotificationManager {

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    private val _tokenFlow = MutableStateFlow<String?>(null)
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    override suspend fun requestPermission(): NotificationPermissionController.PermissionStatus {
        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                notificationCenter.requestAuthorizationWithOptions(
                    options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
                    completionHandler = { granted, _ ->
                        continuation.resume(
                            if (granted) {
                                NotificationPermissionController.PermissionStatus.GRANTED
                            } else {
                                NotificationPermissionController.PermissionStatus.DENIED
                            }
                        )
                    }
                )
            }
        }
    }

    override suspend fun getPermissionStatus(): NotificationPermissionController.PermissionStatus {
        return suspendCancellableCoroutine { continuation ->
            notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                val status = when (settings?.authorizationStatus) {
                    UNAuthorizationStatusAuthorized -> NotificationPermissionController.PermissionStatus.GRANTED
                    UNAuthorizationStatusDenied -> NotificationPermissionController.PermissionStatus.DENIED
                    UNAuthorizationStatusProvisional -> NotificationPermissionController.PermissionStatus.NOT_DETERMINED
                    UNAuthorizationStatusEphemeral -> NotificationPermissionController.PermissionStatus.NOT_DETERMINED
                    UNAuthorizationStatusNotDetermined -> NotificationPermissionController.PermissionStatus.NOT_DETERMINED
                    else -> NotificationPermissionController.PermissionStatus.NOT_DETERMINED
                }
                continuation.resume(status)
            }
        }
    }

    override suspend fun registerForRemoteNotifications() {
        withContext(Dispatchers.Main) {
            val granted = suspendCancellableCoroutine { continuation ->
                notificationCenter.requestAuthorizationWithOptions(
                    options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound,
                    completionHandler = { granted, _ -> continuation.resume(granted) }
                )
            }
            if (granted) {
                UIApplication.sharedApplication.registerForRemoteNotifications()
            }
        }
    }

    override suspend fun unregisterForRemoteNotifications() {
        withContext(Dispatchers.Main) {
            UIApplication.sharedApplication.unregisterForRemoteNotifications()
        }
        _tokenFlow.value = null
    }

    override fun getToken(): String? = _tokenFlow.value

    override fun onNewToken(token: String) {
        _tokenFlow.value = token
    }

    /**
     * Called by the application delegate when APNs delivers the device token.
     * Converts the raw `NSData` token bytes into a hex string for use as an
     * FCM/APNs registration token.
     */
    fun setDeviceToken(token: NSData) {
        _tokenFlow.value = token.toHexRepresentation()
    }

    override fun showLocalNotification(notification: PushNotification, channelId: String) {
        val content = UNMutableNotificationContent()
        content.setTitle(notification.title)
        content.setBody(notification.body)
        content.setSound(UNNotificationSound.defaultSound)

        val userInfoMap = mutableMapOf<Any?, Any?>()
        notification.deepLink?.let { userInfoMap["deepLink"] = it }
        notification.payload.forEach { (key, value) -> userInfoMap[key] = value }
        if (userInfoMap.isNotEmpty()) {
            content.setUserInfo(userInfoMap)
        }

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = notification.id,
            content = content,
            trigger = null,
        )

        notificationCenter.addNotificationRequest(request) { }
    }

    override fun cancelNotification(id: String) {
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(id))
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(listOf(id))
    }

    override fun cancelAllNotifications() {
        notificationCenter.removeAllPendingNotificationRequests()
        notificationCenter.removeAllDeliveredNotifications()
    }

    override fun createChannel(channel: NotificationChannel) {
        // iOS does not have Android-style notification channels.
    }

    override fun deleteChannel(id: String) {
        // No-op on iOS
    }

    override fun getChannels(): List<NotificationChannel> {
        return emptyList()
    }
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toHexRepresentation(): String {
    val size = length.toInt()
    if (size == 0) return ""
    val dataBytes = this.bytes?.reinterpret<ByteVar>() ?: return ""
    val builder = StringBuilder(size * 2)
    for (i in 0 until size) {
        val unsigned = dataBytes[i].toInt() and 0xFF
        val hex = unsigned.toString(16)
        if (hex.length == 1) builder.append("0")
        builder.append(hex)
    }
    return builder.toString()
}
