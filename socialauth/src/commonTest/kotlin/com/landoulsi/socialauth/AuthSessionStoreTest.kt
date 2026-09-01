package com.landoulsi.socialauth

import com.landoulsi.socialauth.model.AuthSession
import com.landoulsi.socialauth.model.AuthTokens
import com.landoulsi.socialauth.model.AuthUser
import com.landoulsi.socialauth.model.SocialProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthSessionStoreTest {

    private val session = AuthSession(
        user = AuthUser(uid = "u1", email = "e@e.com", provider = SocialProvider.GOOGLE),
        tokens = AuthTokens(accessToken = "at", refreshToken = "rt", expiresAtEpochMillis = 123L, scopes = listOf("openid")),
    )

    @Test
    fun inMemoryRoundTrips() {
        val store = InMemoryAuthSessionStore()
        assertNull(store.load())
        store.save(session)
        assertEquals(session, store.load())
        store.clear()
        assertNull(store.load())
    }

    @Test
    fun delegatingStoreSerializesThroughBackingMap() {
        val backing = HashMap<String, String>()
        val store = DelegatingAuthSessionStore(
            read = { backing["k"] },
            write = { json -> if (json == null) backing.remove("k") else backing["k"] = json },
        )

        store.save(session)
        assertEquals(1, backing.size)
        assertEquals(session, store.load())

        store.clear()
        assertNull(store.load())
        assertEquals(0, backing.size)
    }

    @Test
    fun delegatingStoreReturnsNullOnCorruptData() {
        val store = DelegatingAuthSessionStore(read = { "{ broken" }, write = {})
        assertNull(store.load())
    }
}
