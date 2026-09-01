package com.landoulsi.pushnotification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel as AndroidNotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirebasePushNotificationManager(
    context: Context,
    private val allowedDeepLinkSchemes: Set<String> = emptySet(),
) : PushNotificationManager {

    var permissionRequester: (suspend () -> Boolean)? = null

    private val appContext: Context = context.applicationContext
    
    private val notificationManager: NotificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val _tokenFlow = MutableStateFlow<String?>(null)
    override val tokenFlow: StateFlow<String?> = _tokenFlow.asStateFlow()

    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .build()

    override suspend fun requestPermission(): NotificationPermissionController.PermissionStatus {
        val granted = permissionRequester?.invoke() ?: false
        return if (granted) {
            NotificationPermissionController.PermissionStatus.GRANTED
        } else {
            NotificationPermissionController.PermissionStatus.DENIED
        }
    }

    override suspend fun getPermissionStatus(): NotificationPermissionController.PermissionStatus {
        val areEnabled = NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        if (!areEnabled) {
            return NotificationPermissionController.PermissionStatus.DENIED
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    NotificationPermissionController.PermissionStatus.GRANTED
                }
                else -> {
                    NotificationPermissionController.PermissionStatus.DENIED
                }
            }
        } else {
            NotificationPermissionController.PermissionStatus.GRANTED
        }
    }

    override suspend fun registerForRemoteNotifications() {
        val token = suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    task.exception?.printStackTrace()
                    continuation.resume(null)
                }
            }
        }
        _tokenFlow.value = token
    }

    override suspend fun unregisterForRemoteNotifications() {
        suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        task.exception?.printStackTrace()
                    }
                    _tokenFlow.value = null
                    continuation.resume(Unit)
                }
        }
    }

    override fun getToken(): String? = _tokenFlow.value

    override fun onNewToken(token: String) {
        _tokenFlow.value = token
    }

    @android.annotation.SuppressLint("MissingPermission")
    override fun showLocalNotification(notification: PushNotification, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val builder = NotificationCompat.Builder(appContext, channelId)
            // Use a safe system icon rather than applicationInfo.icon which may be adaptive and crash
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setPriority(toAndroidPriority(notification.priority))
            .setAutoCancel(true)

        notification.deepLink?.let { deepLink ->
            buildDeepLinkPendingIntent(deepLink, notification.id)?.let { pendingIntent ->
                builder.setContentIntent(pendingIntent)
            }
        }

        val bundle = Bundle()
        notification.payload.forEach { (key, value) ->
            bundle.putString(key, value)
        }
        builder.addExtras(bundle)

        val whenTime = notification.sentAt ?: notification.createdAt
        if (whenTime != null) {
            builder.setWhen(whenTime)
        }

        val manager = NotificationManagerCompat.from(appContext)
        manager.notify(notification.id, notification.id.hashCode(), builder.build())
    }

    override fun cancelNotification(id: String) {
        NotificationManagerCompat.from(appContext).cancel(id, id.hashCode())
    }

    override fun cancelAllNotifications() {
        NotificationManagerCompat.from(appContext).cancelAll()
    }

    override val areChannelsSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    override fun createChannel(channel: NotificationChannel) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val androidChannel = AndroidNotificationChannel(
                channel.id,
                channel.name,
                toAndroidImportance(channel.importance),
            ).apply {
                description = channel.description
                lockscreenVisibility = when (channel.lockScreenVisibility) {
                    NotificationChannel.LockScreenVisibility.PUBLIC ->
                        Notification.VISIBILITY_PUBLIC
                    NotificationChannel.LockScreenVisibility.SECRET ->
                        Notification.VISIBILITY_SECRET
                    NotificationChannel.LockScreenVisibility.PRIVATE ->
                        Notification.VISIBILITY_PRIVATE
                }
                channel.sound?.let { setSound(Uri.parse(it), audioAttributes) }
                if (channel.vibrationPattern.isNotEmpty()) {
                    setVibrationPattern(channel.vibrationPattern.toLongArray())
                }
            }
            notificationManager.createNotificationChannel(androidChannel)
        }
    }

    override fun deleteChannel(id: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(id)
        }
    }

    override fun getChannels(): List<NotificationChannel> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.notificationChannels.map { channel ->
                NotificationChannel(
                    id = channel.id,
                    name = channel.name.toString(),
                    description = channel.description?.toString() ?: "",
                    importance = fromAndroidImportance(channel.importance),
                    sound = channel.sound?.toString(),
                    vibrationPattern = channel.vibrationPattern?.toList() ?: emptyList(),
                    lockScreenVisibility = when (channel.lockscreenVisibility) {
                        Notification.VISIBILITY_PUBLIC ->
                            NotificationChannel.LockScreenVisibility.PUBLIC
                        Notification.VISIBILITY_SECRET ->
                            NotificationChannel.LockScreenVisibility.SECRET
                        else ->
                            NotificationChannel.LockScreenVisibility.PRIVATE
                    },
                )
            }
        } else {
            emptyList()
        }
    }

    private fun buildDeepLinkPendingIntent(
        deepLink: String,
        notificationId: String,
    ): PendingIntent? {
        val uri = Uri.parse(deepLink)
        val scheme = uri.scheme?.lowercase() ?: return null

        if (allowedDeepLinkSchemes.isNotEmpty()) {
            val allowedLower = allowedDeepLinkSchemes.map { it.lowercase() }
            if (scheme !in allowedLower) {
                return null
            }
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(appContext.packageName)
        }
        val requestCode = notificationId.hashCode() and Int.MAX_VALUE
        return PendingIntent.getActivity(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun toAndroidPriority(priority: PushNotification.Priority): Int = when (priority) {
        PushNotification.Priority.MIN -> NotificationCompat.PRIORITY_MIN
        PushNotification.Priority.LOW -> NotificationCompat.PRIORITY_LOW
        PushNotification.Priority.DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
        PushNotification.Priority.HIGH -> NotificationCompat.PRIORITY_HIGH
        PushNotification.Priority.MAX -> NotificationCompat.PRIORITY_MAX
    }

    private fun toAndroidImportance(importance: NotificationChannel.Importance): Int =
        when (importance) {
            NotificationChannel.Importance.NONE -> NotificationManager.IMPORTANCE_NONE
            NotificationChannel.Importance.MIN -> NotificationManager.IMPORTANCE_MIN
            NotificationChannel.Importance.LOW -> NotificationManager.IMPORTANCE_LOW
            NotificationChannel.Importance.DEFAULT -> NotificationManager.IMPORTANCE_DEFAULT
            NotificationChannel.Importance.HIGH -> NotificationManager.IMPORTANCE_HIGH
            NotificationChannel.Importance.MAX -> NotificationManager.IMPORTANCE_MAX
        }

    private fun fromAndroidImportance(importance: Int): NotificationChannel.Importance =
        when (importance) {
            NotificationManager.IMPORTANCE_NONE -> NotificationChannel.Importance.NONE
            NotificationManager.IMPORTANCE_MIN -> NotificationChannel.Importance.MIN
            NotificationManager.IMPORTANCE_LOW -> NotificationChannel.Importance.LOW
            NotificationManager.IMPORTANCE_DEFAULT -> NotificationChannel.Importance.DEFAULT
            NotificationManager.IMPORTANCE_HIGH -> NotificationChannel.Importance.HIGH
            NotificationManager.IMPORTANCE_MAX -> NotificationChannel.Importance.MAX
            else -> NotificationChannel.Importance.DEFAULT
        }
}
