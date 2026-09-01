package com.landoulsi.socialauth

import com.landoulsi.socialauth.oauth.OAuthTokenClient
import com.landoulsi.timeprovider.SystemTimeProvider
import com.landoulsi.timeprovider.TimeProvider
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import kotlin.concurrent.Volatile

/**
 * Factory for [SocialAuthClient].
 *
 * The interactive browser step differs per platform, so each target supplies it —
 * either process-wide via [bindAuthorizer], or per-client via the `authorizer`
 * argument of [createWith]:
 * - **Android**: a [com.landoulsi.socialauth.RedirectAuthorizer] (Custom Tabs).
 * - **iOS**: a [com.landoulsi.socialauth.WebAuthenticationAuthorizer] (`ASWebAuthenticationSession`).
 *
 * When no authorizer is available, sign-in fails uniformly with
 * [com.landoulsi.socialauth.model.AuthError.PROVIDER_UNAVAILABLE].
 *
 * The returned client owns its HTTP engine unless one is passed to [createWith];
 * [SocialAuthClient.close] releases whatever it holds.
 */
object SocialAuthClientFactory {

    @Volatile
    private var boundAuthorizer: AuthorizationCodeProvider? = null

    /**
     * Registers a process-wide interactive authorization step. Safe to call again
     * to replace it (e.g. on Activity recreation).
     */
    fun bindAuthorizer(authorizer: AuthorizationCodeProvider) {
        boundAuthorizer = authorizer
    }

    /** Removes the process-wide authorizer to avoid leaking platform context. */
    fun unbindAuthorizer() {
        boundAuthorizer = null
    }

    /** Creates a client with an in-memory session store and the system clock. */
    fun create(config: SocialAuthConfig): SocialAuthClient = createWith(config)

    /**
     * Creates a client.
     *
     * @param sessionStore where the session is persisted (default: in-memory).
     * @param timeProvider clock for token-expiry math; pass `:timeprovider`'s
     *   `AndroidTimeProvider` for trusted-time-backed expiry.
     * @param authorizer explicit authorization step for this client; when null the
     *   process-wide one from [bindAuthorizer] is used.
     * @param httpClient an HTTP client to use for token calls. When null a platform
     *   default engine is created and owned by the returned client (and released by
     *   [SocialAuthClient.close]). A client you pass in is left untouched by `close()` —
     *   you retain its lifecycle. **It MUST be configured with `followRedirects = false`**:
     *   a 3xx on a credential POST could forward the auth code / PKCE verifier to another
     *   host. The built-in engines set this; a client you supply is used as-is.
     */
    fun createWith(
        config: SocialAuthConfig,
        sessionStore: AuthSessionStore = InMemoryAuthSessionStore(),
        timeProvider: TimeProvider = SystemTimeProvider(),
        authorizer: AuthorizationCodeProvider? = null,
        httpClient: HttpClient? = null,
    ): SocialAuthClient = DefaultSocialAuthClient(
        config = config,
        authorizationCodeProvider = resolveAuthorizer(authorizer) { boundAuthorizer },
        tokenClient = OAuthTokenClient(
            httpClient = httpClient ?: defaultTokenHttpClient(),
            ownsHttpClient = httpClient == null,
        ),
        sessionStore = sessionStore,
        timeProvider = timeProvider,
    )
}

/** Platform-default Ktor client (OkHttp on Android, Darwin on iOS) for token-endpoint calls. */
internal expect fun defaultTokenHttpClient(): HttpClient

private const val CONNECT_TIMEOUT_MILLIS = 15_000L
private const val REQUEST_TIMEOUT_MILLIS = 30_000L
private const val SOCKET_TIMEOUT_MILLIS = 30_000L

/** Shared client config applied to the platform-default engines. */
internal fun HttpClientConfig<*>.socialAuthHttpDefaults() {
    // Never follow a redirect on a credential POST — a 3xx could forward the auth code,
    // client secret or PKCE verifier to an unintended host.
    followRedirects = false
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
    }
}

/**
 * Wraps [explicit] (or the process-wide [bound] one) so that a missing authorizer
 * degrades to a uniform [AuthorizationResult.Failure] instead of a platform-specific
 * crash.
 */
internal fun resolveAuthorizer(
    explicit: AuthorizationCodeProvider?,
    bound: () -> AuthorizationCodeProvider?,
): AuthorizationCodeProvider = AuthorizationCodeProvider { request ->
    (explicit ?: bound())?.authorize(request)
        ?: AuthorizationResult.Failure(
            AuthorizationError.ProviderUnavailable,
            "No authorizer bound; call SocialAuthClientFactory.bindAuthorizer(...) " +
                "or pass one to createWith(...)",
        )
}
