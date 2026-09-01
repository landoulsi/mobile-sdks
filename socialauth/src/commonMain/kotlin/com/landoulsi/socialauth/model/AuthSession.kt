package com.landoulsi.socialauth.model

import kotlinx.serialization.Serializable

/**
 * A complete authenticated session: who the user is plus the tokens that prove it.
 *
 * This is the unit that [com.landoulsi.socialauth.AuthSessionStore] persists.
 */
@Serializable
data class AuthSession(
    val user: AuthUser,
    val tokens: AuthTokens,
)
