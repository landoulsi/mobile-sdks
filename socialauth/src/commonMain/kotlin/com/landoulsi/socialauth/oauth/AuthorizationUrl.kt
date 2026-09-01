package com.landoulsi.socialauth.oauth

import com.landoulsi.socialauth.SocialAuthConfig
import io.ktor.http.URLBuilder

/**
 * Builds the provider authorization URL for an Authorization Code flow.
 */
internal object AuthorizationUrl {

    fun build(
        config: SocialAuthConfig,
        state: String,
        pkce: PkceCodes?,
        nonce: String?,
    ): String {
        val builder = URLBuilder(config.authorizationEndpoint)
        with(builder.parameters) {
            // set(), not append(): if the endpoint already carries one of these it is
            // replaced, not duplicated.
            set("client_id", config.clientId)
            set("redirect_uri", config.redirectUri)
            set("response_type", "code")
            set("scope", config.scopes.joinToString(" "))
            set("state", state)
            nonce?.let { set("nonce", it) }
            if (pkce != null) {
                set("code_challenge", pkce.codeChallenge)
                set("code_challenge_method", PkceCodes.CHALLENGE_METHOD)
            }
            config.additionalAuthParams.forEach { (key, value) ->
                if (!contains(key)) append(key, value)
            }
        }
        return builder.buildString()
    }
}
