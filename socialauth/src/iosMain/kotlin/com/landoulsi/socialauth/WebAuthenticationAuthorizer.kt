package com.landoulsi.socialauth

import com.landoulsi.socialauth.internal.DEFAULT_INTERACTIVE_AUTH_TIMEOUT_MILLIS
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.AuthenticationServices.ASPresentationAnchor
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorCodeCanceledLogin
import platform.AuthenticationServices.ASWebAuthenticationSessionErrorDomain
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.Foundation.NSURLQueryItem
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.concurrent.Volatile

/**
 * iOS [AuthorizationCodeProvider] backed by `ASWebAuthenticationSession`, the
 * system-provided secure OAuth browser.
 *
 * Bind it once at startup:
 * ```
 * SocialAuthClientFactory.bindAuthorizer(WebAuthenticationAuthorizer())
 * ```
 *
 * The [SocialAuthConfig.redirectUri] scheme must be a custom scheme registered by
 * the app (it is passed as the session's `callbackURLScheme`).
 *
 * Session setup and `start()` run on [Dispatchers.Main] as UIKit requires, regardless
 * of the dispatcher the SDK calls in on.
 *
 * @param prefersEphemeralSession when true, no cookies/website data are shared with Safari.
 * @param anchorProvider supplies the window to present from; defaults to the foreground
 *   window scene's key window. `ASPresentationAnchor` is `UIWindow?` in the Kotlin/Native
 *   bindings, so this may yield null when there is no attached scene — [authorize] then
 *   fails cleanly rather than presenting from a detached window.
 * @param timeoutMillis backstop so a session that never fires its completion handler
 *   can't hold the internal lock forever (default 3 min).
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class WebAuthenticationAuthorizer(
    private val prefersEphemeralSession: Boolean = false,
    private val anchorProvider: () -> ASPresentationAnchor = ::defaultAnchor,
    private val timeoutMillis: Long = DEFAULT_INTERACTIVE_AUTH_TIMEOUT_MILLIS,
) : AuthorizationCodeProvider {

    // `ASWebAuthenticationSession.presentationContextProvider` is a `weak` property, and the
    // system keeps no strong reference to the session either — the SDK must retain both for
    // the session's lifetime or the flow is cancelled. [mutex] guarantees a single flow at a
    // time, so single slots are sufficient and can't be clobbered by a concurrent authorize().
    private val mutex = Mutex()

    @Volatile
    private var strongContextProvider: AnchorProvider? = null

    @Volatile
    private var activeSession: ASWebAuthenticationSession? = null

    override suspend fun authorize(request: AuthorizationRequest): AuthorizationResult = mutex.withLock {
        withContext(Dispatchers.Main) {
            val authorizationNsUrl = NSURL.URLWithString(request.authorizationUrl)
                ?: return@withContext AuthorizationResult.Failure(
                    AuthorizationError.InvalidConfiguration, "authorizationUrl is not a valid URL",
                )
            val callbackScheme = NSURL.URLWithString(request.redirectUri)?.scheme
            if (callbackScheme.isNullOrBlank()) {
                return@withContext AuthorizationResult.Failure(
                    AuthorizationError.InvalidConfiguration,
                    "redirectUri has no scheme for ASWebAuthenticationSession to intercept",
                )
            }
            val anchor = anchorProvider()
                ?: return@withContext AuthorizationResult.Failure(
                    AuthorizationError.InteractiveFlowStartFailed,
                    "no foreground window scene to present the sign-in from",
                )

            // A session that never calls its completion handler would otherwise hold the
            // lock forever; on timeout the coroutine cancellation cancels the session.
            withTimeoutOrNull(timeoutMillis) {
                startSession(authorizationNsUrl, callbackScheme, anchor)
            } ?: AuthorizationResult.Cancelled
        }
    }

    private suspend fun startSession(
        authorizationNsUrl: NSURL,
        callbackScheme: String,
        anchor: ASPresentationAnchor,
    ): AuthorizationResult = suspendCancellableCoroutine<AuthorizationResult> { continuation ->
        val onComplete: (NSURL?, NSError?) -> Unit = handler@{ callbackUrl, error ->
            strongContextProvider = null
            activeSession = null // `mutex` guarantees no newer flow exists yet here.
            if (!continuation.isActive) return@handler
            val result = when {
                error != null ->
                    if (error.domain == ASWebAuthenticationSessionErrorDomain &&
                        error.code == ASWebAuthenticationSessionErrorCodeCanceledLogin
                    ) {
                        AuthorizationResult.Cancelled
                    } else {
                        AuthorizationResult.Failure(
                            AuthorizationError.InteractiveFlowFailed, error.localizedDescription,
                        )
                    }
                callbackUrl != null -> parseCallback(callbackUrl)
                else -> AuthorizationResult.Failure(
                    AuthorizationError.InteractiveFlowFailed, "no callback URL returned",
                )
            }
            continuation.resumeWith(Result.success(result))
        }

        val session = ASWebAuthenticationSession(
            uRL = authorizationNsUrl,
            callbackURLScheme = callbackScheme,
            completionHandler = onComplete,
        )
        val contextProvider = AnchorProvider { anchor }
        strongContextProvider = contextProvider
        activeSession = session
        session.presentationContextProvider = contextProvider
        session.prefersEphemeralWebBrowserSession = prefersEphemeralSession

        continuation.invokeOnCancellation {
            // Cancellation may arrive on any thread; the UIKit call and the teardown
            // of the strong references both go on the main queue, in order. Identity
            // check: never clear a newer flow's session (belt-and-braces atop `mutex`).
            dispatch_async(dispatch_get_main_queue()) {
                session.cancel()
                if (activeSession === session) {
                    strongContextProvider = null
                    activeSession = null
                }
            }
        }

        if (!session.start() && continuation.isActive) {
            if (activeSession === session) {
                strongContextProvider = null
                activeSession = null
            }
            continuation.resumeWith(
                Result.success(AuthorizationResult.Failure(AuthorizationError.InteractiveFlowStartFailed)),
            )
        }
    }

    private fun parseCallback(url: NSURL): AuthorizationResult {
        val query = NSURLComponents.componentsWithURL(url, resolvingAgainstBaseURL = false)
            ?.queryItems.orEmpty()
            .mapNotNull { it as? NSURLQueryItem }
            .associate { it.name to it.value }
        return RedirectResult.from(
            code = query["code"],
            state = query["state"],
            error = query["error"],
            errorDescription = query["error_description"],
        )
    }

    private class AnchorProvider(
        private val provider: () -> ASPresentationAnchor,
    ) : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
        override fun presentationAnchorForWebAuthenticationSession(
            session: ASWebAuthenticationSession,
        ): ASPresentationAnchor = provider()
    }
}

@OptIn(ExperimentalForeignApi::class)
@Suppress("DEPRECATION") // UIApplication.keyWindow: last-resort fallback for non-scene apps
// Return type `ASPresentationAnchor` is `UIWindow?` in K/N — this legitimately returns null
// when nothing scene-attached is available; callers guard with `?:`.
private fun defaultAnchor(): ASPresentationAnchor {
    val app = UIApplication.sharedApplication
    val activeScene = app.connectedScenes
        .mapNotNull { it as? UIWindowScene }
        .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
    val sceneWindows = activeScene?.windows?.mapNotNull { it as? UIWindow }
    // A real, scene-attached window only; no detached UIWindow() fallback (that fails
    // ASWebAuthenticationSession presentation on iOS 13+).
    return sceneWindows?.firstOrNull { it.keyWindow }
        ?: sceneWindows?.firstOrNull()
        ?: app.keyWindow
}
