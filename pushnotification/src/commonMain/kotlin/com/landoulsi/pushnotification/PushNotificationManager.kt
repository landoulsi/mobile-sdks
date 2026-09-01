package com.landoulsi.pushnotification

/**
 * Facade over the focused push-notification capabilities.
 *
 * Implementations may not support every capability (e.g. iOS has no
 * Android-style channels); the focused sub-interfaces ([NotificationPermissionController],
 * [RemoteTokenController], [LocalNotificationPresenter], [NotificationChannelManager])
 * let callers depend on only what they need and give a compile-time signal
 * of the supported surface.
 */
interface PushNotificationManager :
    NotificationPermissionController,
    RemoteTokenController,
    LocalNotificationPresenter,
    NotificationChannelManager
