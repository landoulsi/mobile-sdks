package com.landoulsi.socialauth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.landoulsi.logger.Logger
import com.landoulsi.socialauth.internal.DEFAULT_INTERACTIVE_AUTH_TIMEOUT_MILLIS
import com.landoulsi.socialauth.internal.SOCIAL_AUTH_LOG_TAG
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Android [AuthorizationCodeProvider] backed by Chrome Custom Tabs.
 *
 * This class owns **no Activity and declares no manifest entry** — the host app
 * declares its own redirect `<activity>` with an `<intent-filter>` matching
 * [SocialAuthConfig.redirectUri]'s scheme, and forwards the incoming URI here:
 *
 * ```
 * // AndroidManifest.xml (host app)
 * <activity android:name=".OAuthRedirectActivity" android:exported="true"
 *           android:launchMode="singleTask">
 *   <intent-filter>
 *     <action android:name="android.intent.action.VIEW" />
 *     <category android:name="android.intent.category.DEFAULT" />
 *     <category android:name="android.intent.category.BROWSABLE" />
 *     <data android:scheme="com.example.app" android:path="/oauth2redirect" />
 *   </intent-filter>
 * </activity>
 *
 * // OAuthRedirectActivity — handle from BOTH onCreate and onNewIntent (singleTask)
 * intent?.data?.let { authorizer.onRedirect(it) }
 * finish()
 * ```
 *
 * Custom Tabs does not notify the app when the user dismisses the tab (back / X),
 * so a sign-in the user walks away from is resolved as [AuthorizationResult.Cancelled]
 * after [timeoutMillis] — a generous backstop, since 2FA / account recovery in the
 * browser can be slow. The host may end an abandoned flow sooner by calling
 * [onCancelled] from the launching screen's `onResume` — but see the warning on
 * [onCancelled] about doing this unconditionally.
 *
 * @param context any context; the application context is retained.
 * @param timeoutMillis how long to wait for a redirect before giving up (default 3 min).
 * @param configureCustomTab hook to style the Custom Tab (toolbar color, animations, …)
 *   before it is launched. Do not capture an `Activity` or `View` in this lambda — a
 *   [RedirectAuthorizer] is typically process-scoped and would leak it.
 */
class RedirectAuthorizer(
    context: Context,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val configureCustomTab: (CustomTabsIntent.Builder) -> Unit = {},
) : AuthorizationCodeProvider {

    private val applicationContext: Context = context.applicationContext
    private val pending = PendingAuthorization()

    override suspend fun authorize(request: AuthorizationRequest): AuthorizationResult {
        val round = pending.begin(request.state)
        return try {
            withContext(Dispatchers.Main) {
                val intent = CustomTabsIntent.Builder().apply(configureCustomTab).build()
                intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.launchUrl(applicationContext, Uri.parse(request.authorizationUrl))
            }
            withTimeoutOrNull(timeoutMillis) { round.deferred.await() } ?: run {
                // complete() returns false if a redirect completed the round at the same
                // instant the timeout fired — take that real result rather than dropping it.
                if (round.deferred.complete(AuthorizationResult.Cancelled)) {
                    AuthorizationResult.Cancelled
                } else {
                    round.deferred.await() // already completed; returns immediately
                }
            }
        } catch (e: CancellationException) {
            // Resolve the round so a redirect that lands afterwards is a no-op, not a match.
            round.deferred.complete(AuthorizationResult.Cancelled)
            throw e
        } catch (e: ActivityNotFoundException) {
            Logger.e(TAG, "No browser available to handle the Custom Tab", e)
            AuthorizationResult.Failure(AuthorizationError.ProviderUnavailable, "no browser installed")
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to launch Custom Tab", e)
            AuthorizationResult.Failure(AuthorizationError.LaunchFailed, e.message)
        } finally {
            pending.clear(round)
        }
    }

    /**
     * Feed the redirect URI captured by the host's redirect Activity. Returns true if
     * it matched the in-flight request and completed it.
     *
     * A redirect is ignored (returns false, the sign-in keeps waiting) when it is opaque,
     * has malformed percent-encoding, or its `state` does not match the in-flight request.
     * The redirect Activity is exported, so a hostile app can deliver a spoofed `?error=…`
     * or a `?state=%ZZ` that makes [Uri.getQueryParameter] throw; neither must abort a
     * legitimate sign-in (RFC 6749 §4.1.2.1 requires `state` on error responses too).
     */
    fun onRedirect(uri: Uri): Boolean {
        if (!uri.isHierarchical) return false
        return try {
            val state = uri.getQueryParameter("state") ?: return false
            // deliver() re-checks `state` atomically, so a concurrent begin() can't
            // hand this redirect to a different round.
            pending.deliver(
                receivedState = state,
                result = RedirectResult.from(
                    code = uri.getQueryParameter("code"),
                    state = state,
                    error = uri.getQueryParameter("error"),
                    errorDescription = uri.getQueryParameter("error_description"),
                ),
            )
        } catch (e: Exception) {
            Logger.w(TAG, "Ignoring an unparseable redirect URI: ${e.message}")
            false
        }
    }

    /**
     * Call when the host detects the user returned without completing (e.g. the launching
     * screen resumed and no redirect arrived).
     *
     * **Do not call this unconditionally from `onResume`.** The launching Activity also
     * resumes when the browser tab merely goes to the background — e.g. the user tapped
     * an "open your authenticator app" link and switched to a TOTP app mid-flow. Calling
     * [onCancelled] there aborts a sign-in that is still in progress. Gate it on a signal
     * that the user really backed out (e.g. a short delay after `onResume` with still no
     * redirect, or your own "sign-in screen is foreground again with no result" flag).
     */
    fun onCancelled(): Boolean = pending.cancelCurrent()

    companion object {
        /**
         * Backstop timeout for a sign-in the user abandoned in the browser (Custom Tabs
         * gives no dismissal callback). Generous on purpose; the host should call
         * [onCancelled] from its launching screen's `onResume` for a snappier UX, or
         * pass a shorter value to the constructor.
         */
        const val DEFAULT_TIMEOUT_MILLIS: Long = DEFAULT_INTERACTIVE_AUTH_TIMEOUT_MILLIS

        private const val TAG = SOCIAL_AUTH_LOG_TAG
    }
}
