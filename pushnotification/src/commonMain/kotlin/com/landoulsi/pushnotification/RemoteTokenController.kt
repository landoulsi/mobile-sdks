package com.landoulsi.pushnotification

/**
 * Manages the remote-notification device token lifecycle.
 *
 * Token registration is asynchronous (FCM/APNs), so the operations are
 * suspend functions. [getToken] returns the most recently obtained token,
 * or null if none has been received yet.
 */
interface RemoteTokenController {

    suspend fun registerForRemoteNotifications()

    suspend fun unregisterForRemoteNotifications()

    fun getToken(): String?

    val tokenFlow: kotlinx.coroutines.flow.StateFlow<String?>

    fun onNewToken(token: String)
}
