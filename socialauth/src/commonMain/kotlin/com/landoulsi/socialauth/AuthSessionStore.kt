package com.landoulsi.socialauth

import com.landoulsi.logger.Logger
import com.landoulsi.socialauth.internal.socialAuthJson
import com.landoulsi.socialauth.internal.SOCIAL_AUTH_LOG_TAG
import com.landoulsi.socialauth.model.AuthSession
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.concurrent.Volatile

/**
 * Persistence for the active [AuthSession].
 *
 * The SDK deliberately does not depend on a concrete storage module: an
 * [AuthSession] contains refresh tokens, so the host app should back this with
 * its own encrypted store (e.g. the `:storage` `SecureStorage`). [DelegatingAuthSessionStore]
 * adapts any string key-value store; [InMemoryAuthSessionStore] is for tests and previews.
 */
interface AuthSessionStore {
    fun load(): AuthSession?
    fun save(session: AuthSession)
    fun clear()
}

/**
 * Non-persistent store. Fine for tests and throwaway previews; loses the session
 * when the process dies.
 */
class InMemoryAuthSessionStore(initial: AuthSession? = null) : AuthSessionStore {
    @Volatile
    private var session: AuthSession? = initial
    override fun load(): AuthSession? = session
    override fun save(session: AuthSession) {
        this.session = session
    }
    override fun clear() {
        session = null
    }
}

/**
 * Serializes the [AuthSession] to/from a JSON string held in a caller-supplied
 * key-value store. Wire [read] / [write] to an encrypted store:
 *
 * ```
 * DelegatingAuthSessionStore(
 *     read = { secureStorage.getString("social_auth_session") },
 *     write = { json -> if (json == null) secureStorage.remove("social_auth_session")
 *                       else secureStorage.putString("social_auth_session", json) },
 * )
 * ```
 */
class DelegatingAuthSessionStore(
    private val read: () -> String?,
    private val write: (String?) -> Unit,
) : AuthSessionStore {

    override fun load(): AuthSession? {
        return try {
            val serialized = read() ?: return null
            socialAuthJson.decodeFromString<AuthSession>(serialized)
        } catch (e: Exception) {
            // A throwing backing store (e.g. an invalidated keystore key) or an unreadable
            // blob is treated as "no session" rather than propagating out of a constructor,
            // but it is logged so the regression is not silent.
            Logger.w(SOCIAL_AUTH_LOG_TAG, "Failed to load a cached session; treating as signed out: ${e.message}")
            null
        }
    }

    override fun save(session: AuthSession) = write(socialAuthJson.encodeToString(session))

    override fun clear() = write(null)
}
