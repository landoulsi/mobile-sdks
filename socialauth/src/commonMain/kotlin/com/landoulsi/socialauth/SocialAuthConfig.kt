package com.landoulsi.socialauth

import com.landoulsi.socialauth.internal.DEFAULT_OIDC_SCOPES
import com.landoulsi.socialauth.model.SocialProvider
import io.ktor.http.URLProtocol
import io.ktor.http.Url

/**
 * Everything the SDK needs to run an OAuth 2.0 Authorization Code flow against a provider.
 *
 * Defaults target Google. For other providers, override [authorizationEndpoint] /
 * [tokenEndpoint] / [scopes] / [additionalAuthParams] accordingly.
 *
 * @property provider identity provider this config describes.
 * @property clientId OAuth client id. For mobile this must be a **public** client
 *   (no secret shipped in the app); PKCE ([usePkce]) replaces the secret.
 * @property redirectUri redirect URI registered with the provider. On Android this is
 *   typically a custom scheme (`com.example.app:/oauth2redirect`); it must match the
 *   value the platform authorizer listens for.
 * @property scopes scopes to request.
 * @property authorizationEndpoint provider authorization URL.
 * @property tokenEndpoint provider token URL.
 * @property clientSecret optional secret for confidential clients (server-side / desktop).
 *   Leave null for mobile public clients — when null it is simply omitted from token requests.
 * @property usePkce whether to use PKCE (RFC 7636, `S256`). Strongly recommended; required
 *   for public clients.
 * @property additionalAuthParams extra query params appended to the authorization URL.
 *   The Google defaults request a refresh token on first consent.
 * @property issuer expected OpenID Connect `iss` claim. When set, an `id_token` whose
 *   `iss` differs is rejected. Null disables the issuer check (the `aud` and `exp`
 *   checks always run when an id_token is present).
 */
data class SocialAuthConfig(
    val clientId: String,
    val redirectUri: String,
    val provider: SocialProvider = SocialProvider.GOOGLE,
    val scopes: List<String> = DEFAULT_OIDC_SCOPES,
    val authorizationEndpoint: String = "https://accounts.google.com/o/oauth2/v2/auth",
    val tokenEndpoint: String = "https://oauth2.googleapis.com/token",
    val clientSecret: String? = null,
    val usePkce: Boolean = true,
    val issuer: String? = "https://accounts.google.com",
    val additionalAuthParams: Map<String, String> = mapOf(
        "access_type" to "offline",
        "prompt" to "consent",
    ),
) {
    /** Redacted — [clientSecret] must not reach logs. */
    override fun toString(): String =
        "SocialAuthConfig(clientId=$clientId, redirectUri=$redirectUri, provider=$provider, " +
            "scopes=$scopes, authorizationEndpoint=$authorizationEndpoint, tokenEndpoint=$tokenEndpoint, " +
            "clientSecret=${if (clientSecret != null) "***" else "null"}, usePkce=$usePkce, issuer=$issuer, " +
            "additionalAuthParams=$additionalAuthParams)"

    /** @throws IllegalArgumentException if a required field is blank, an endpoint is not HTTPS, or a public client disables PKCE. */
    fun validate() {
        require(clientId.isNotBlank()) { "SocialAuthConfig.clientId must not be blank" }
        require(redirectUri.isNotBlank()) { "SocialAuthConfig.redirectUri must not be blank" }
        // Both platform authorizers need a scheme to intercept the redirect (Android
        // intent-filter, iOS `callbackURLScheme`). Fail here instead of deep inside authorize().
        val schemeEnd = redirectUri.indexOf(':')
        val scheme = if (schemeEnd > 0) redirectUri.substring(0, schemeEnd) else ""
        require(scheme.matches(URI_SCHEME)) {
            "SocialAuthConfig.redirectUri must start with a URI scheme " +
                "(e.g. \"com.example.app:/oauth2redirect\" or \"https://example.com/oauth2redirect\")"
        }
        // RFC 6749 §3.1.2: a redirect URI MUST NOT carry a fragment.
        require('#' !in redirectUri) { "SocialAuthConfig.redirectUri must not contain a URI fragment" }
        // RFC 6749 §3.1.2 also forbids user-info in the redirect URI (checked here for
        // https/http; a private-use scheme has no authority component to carry it).
        if (scheme.equals("https", ignoreCase = true) || scheme.equals("http", ignoreCase = true)) {
            val parsedRedirect = runCatching { Url(redirectUri) }.getOrElse {
                throw IllegalArgumentException("SocialAuthConfig.redirectUri is not a valid URL", it)
            }
            require(parsedRedirect.user == null && parsedRedirect.password == null) {
                "SocialAuthConfig.redirectUri must not contain user-info"
            }
        }
        // Reject browser/URL pseudo-schemes: a redirect the OS could hand to a script or
        // file handler is never a legitimate OAuth callback (RFC 8252 §7.1 expects https
        // App Links or private-use application schemes).
        require(scheme.lowercase() !in DANGEROUS_REDIRECT_SCHEMES) {
            "SocialAuthConfig.redirectUri scheme \"$scheme\" is not a valid OAuth callback scheme"
        }
        // RFC 8252 §7.1 / §8.1: an https App/Universal Link is fine, a private-use scheme
        // is fine, but a cleartext http:// redirect on a non-loopback host lets the auth
        // code be intercepted. (Loopback http is allowed for desktop/testing.) Parse with
        // Ktor's Url so `http://localhost:80@evil.com/cb` can't spoof a loopback host.
        if (scheme.equals("http", ignoreCase = true)) {
            val parsed = runCatching { Url(redirectUri) }.getOrNull()
            require(parsed != null && parsed.user == null && parsed.password == null && parsed.host in LOOPBACK_HOSTS) {
                "SocialAuthConfig.redirectUri must not use cleartext http:// on a non-loopback host " +
                    "(use an https:// App Link or a private-use scheme)"
            }
        }
        require(scopes.isNotEmpty()) { "SocialAuthConfig.scopes must not be empty" }
        require(usePkce || !clientSecret.isNullOrBlank()) {
            "A public client (no clientSecret) must keep usePkce = true (RFC 7636 / OAuth 2.1)"
        }
        requireSecureEndpoint("authorizationEndpoint", authorizationEndpoint)
        requireSecureEndpoint("tokenEndpoint", tokenEndpoint)
    }

    private fun requireSecureEndpoint(name: String, value: String) {
        require(value.isNotBlank()) { "SocialAuthConfig.$name must not be blank" }
        // RFC 6749 §3.1 / §3.2: neither endpoint may carry a fragment (URLBuilder would
        // otherwise append query params after the '#' and break the request).
        require('#' !in value) { "SocialAuthConfig.$name must not contain a URL fragment" }
        val url = try {
            Url(value)
        } catch (e: Exception) {
            throw IllegalArgumentException("SocialAuthConfig.$name is not a valid URL", e)
        }
        require(url.user == null && url.password == null) {
            "SocialAuthConfig.$name must not contain URL user-info"
        }
        val loopback = url.host in LOOPBACK_HOSTS
        val secure = url.protocol == URLProtocol.HTTPS || (url.protocol == URLProtocol.HTTP && loopback)
        require(secure) { "SocialAuthConfig.$name must use https:// (http is only allowed for loopback hosts)" }
    }

    companion object {
        /**
         * A [SocialAuthConfig] wired for Google (endpoints, issuer, `openid email profile`
         * scopes, and `access_type=offline` + `prompt=consent` so the first sign-in yields
         * a refresh token). This is what the bare constructor defaults to, named explicitly.
         */
        fun google(
            clientId: String,
            redirectUri: String,
            scopes: List<String> = DEFAULT_OIDC_SCOPES,
        ): SocialAuthConfig = SocialAuthConfig(
            clientId = clientId,
            redirectUri = redirectUri,
            scopes = scopes,
            issuer = "https://accounts.google.com",
        )

        /** RFC 3986 scheme: ALPHA *( ALPHA / DIGIT / "+" / "-" / "." ). */
        private val URI_SCHEME = Regex("[a-zA-Z][a-zA-Z0-9+.-]*")

        private val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1", "::1", "[::1]")

        /**
         * Schemes the OS may route to a script, file handler, or (Android) an arbitrary
         * internal component — never a valid OAuth callback.
         */
        private val DANGEROUS_REDIRECT_SCHEMES = setOf(
            "javascript", "data", "vbscript", "file", "filesystem", "content", "blob", "about", "intent",
        )
    }
}
