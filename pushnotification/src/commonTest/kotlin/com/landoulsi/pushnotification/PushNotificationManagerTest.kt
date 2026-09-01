package com.landoulsi.pushnotification

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies the [PushNotificationManager] interface contract using an
 * in-memory fake. The fake records and reflects operations so the contract
 * (method signatures, return types, and round-trip behavior) is exercised
 * without platform frameworks.
 */
class PushNotificationManagerTest {

    private val manager = InMemoryPushNotificationManager()

    @Test
    fun testPermissionStatusRoundTrip() = runTest {
        assertEquals(
            NotificationPermissionController.PermissionStatus.GRANTED,
            manager.requestPermission(),
        )
        assertEquals(
            NotificationPermissionController.PermissionStatus.GRANTED,
            manager.getPermissionStatus(),
        )
    }

    @Test
    fun testTokenRegistrationAndRetrieval() = runTest {
        assertNull(manager.getToken())
        manager.registerForRemoteNotifications()
        assertEquals("fake-token", manager.getToken())
        manager.unregisterForRemoteNotifications()
        assertNull(manager.getToken())
    }

    @Test
    fun testShowAndCancelLocalNotification() {
        val notification = PushNotification(
            id = "test-1",
            title = "Title",
            body = "Body",
        )
        manager.showLocalNotification(notification, "default")
        assertEquals(listOf(notification), manager.scheduledNotifications)

        manager.cancelNotification("test-1")
        assertTrue(manager.cancelledIds.contains("test-1"))
    }

    @Test
    fun testCancelAllNotifications() {
        manager.showLocalNotification(
            PushNotification(id = "a", title = "A", body = "A"),
            "default",
        )
        manager.showLocalNotification(
            PushNotification(id = "b", title = "B", body = "B"),
            "default",
        )
        manager.cancelAllNotifications()
        assertTrue(manager.cancelAllCalled)
    }

    @Test
    fun testChannelCreateAndGetRoundTrip() {
        val channel = NotificationChannel(
            id = "test-channel",
            name = "Test",
        )
        manager.createChannel(channel)
        assertEquals(listOf(channel), manager.getChannels())

        manager.deleteChannel("test-channel")
        assertTrue(manager.getChannels().isEmpty())
    }
}

/**
 * In-memory [PushNotificationManager] used to exercise the interface contract.
 */
private class InMemoryPushNotificationManager : PushNotificationManager {

    private val _tokenFlow = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    override val tokenFlow: kotlinx.coroutines.flow.StateFlow<String?> = _tokenFlow

    private var token: String? = null
    private val channels = mutableListOf<NotificationChannel>()
    val scheduledNotifications = mutableListOf<PushNotification>()
    val cancelledIds = mutableListOf<String>()
    var cancelAllCalled = false

    override suspend fun requestPermission(): NotificationPermissionController.PermissionStatus =
        NotificationPermissionController.PermissionStatus.GRANTED

    override suspend fun getPermissionStatus(): NotificationPermissionController.PermissionStatus =
        NotificationPermissionController.PermissionStatus.GRANTED

    override suspend fun registerForRemoteNotifications() {
        token = "fake-token"
        _tokenFlow.value = "fake-token"
    }

    override suspend fun unregisterForRemoteNotifications() {
        token = null
        _tokenFlow.value = null
    }

    override fun getToken(): String? = token

    override fun onNewToken(token: String) {
        this.token = token
        _tokenFlow.value = token
    }

    override fun showLocalNotification(notification: PushNotification, channelId: String) {
        scheduledNotifications.add(notification)
    }

    override fun cancelNotification(id: String) {
        cancelledIds.add(id)
    }

    override fun cancelAllNotifications() {
        cancelAllCalled = true
    }

    override fun createChannel(channel: NotificationChannel) {
        channels.add(channel)
    }

    override fun deleteChannel(id: String) {
        channels.removeAll { it.id == id }
    }

    override fun getChannels(): List<NotificationChannel> = channels.toList()
}
