package com.landoulsi.socialauth.model

import kotlinx.serialization.Serializable

/**
 * The authenticated end user, normalized across providers.
 *
 * @property uid stable, provider-scoped user identifier (the OpenID `sub` claim for Google).
 * @property email verified email address, when the granted scopes expose it.
 * @property displayName human-readable name, when available.
 * @property photoUrl avatar URL, when available.
 * @property provider the identity provider that vouched for this user.
 */
@Serializable
data class AuthUser(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val provider: SocialProvider,
)
