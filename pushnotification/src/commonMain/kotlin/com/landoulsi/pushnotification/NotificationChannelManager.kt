package com.landoulsi.pushnotification

/**
 * Manages notification channels.
 *
 * Channels are a first-class concept on Android but not on iOS. Platforms
 * that do not support channels (iOS) may leave these as no-ops; callers
 * should guard with a capability check where behavior differs.
 */
interface NotificationChannelManager {

    val areChannelsSupported: Boolean

    fun createChannel(channel: NotificationChannel)

    fun deleteChannel(id: String)

    fun getChannels(): List<NotificationChannel>
}
