package com.landoulsi.socialauth.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Identity provider a session was obtained from.
 *
 * An open value type rather than an enum so consumers can name their own OpenID
 * Connect providers (Auth0, Keycloak, Okta, an enterprise IdP…) without modifying
 * the library. The well-known ones are [GOOGLE], [APPLE], [MICROSOFT]; the SDK
 * derives identity from the `id_token` `sub` claim, so any OIDC provider that
 * returns one works once [com.landoulsi.socialauth.SocialAuthConfig] points at its
 * endpoints.
 *
 * @property id lowercase provider identifier, e.g. `"google"`.
 */
@Serializable
@JvmInline
value class SocialProvider(val id: String) {
    companion object {
        val GOOGLE = SocialProvider("google")
        val APPLE = SocialProvider("apple")
        val MICROSOFT = SocialProvider("microsoft")
    }
}
