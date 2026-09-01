# Landoulsi Social Auth Module (`:socialauth`)

A Kotlin Multiplatform (KMP) social sign-in module implementing the OAuth 2.0
**Authorization Code flow with PKCE** (RFC 6749 + RFC 7636). Almost everything —
URL building, PKCE, `state`, token exchange, refresh, session persistence — lives
in `commonMain`; the only per-platform piece is the interactive browser step.

---

## 1. Architecture

```
+---------------------------------------------------------------+
|                        host app                               |
|   (binds an authorizer, calls SocialAuthClient)               |
+---------------------------------------------------------------+
                               |
                               v
+---------------------------------------------------------------+
|                        :socialauth                            |
|                                                               |
|  [commonMain]                                                  |
|  - SocialAuthClient (interface) / DefaultSocialAuthClient      |
|  - SocialAuthConfig, AuthUser, AuthTokens, AuthSession         |
|  - AuthResult / AuthState / AuthError                          |
|  - AuthorizationCodeProvider  <-- the platform seam            |
|  - AuthSessionStore (InMemory / Delegating)                    |
|  - oauth/: Pkce, Sha256, Jwt, AuthorizationUrl,                |
|            OAuthTokenParser, OAuthTokenClient (Ktor),          |
|            GoogleCredentialsParser                             |
|  - testing/: FakeSocialAuthClient, FakeAuthorizationCodeProvider|
|                                                               |
|  [androidMain]                                                 |
|  - SocialAuthClientFactory (Ktor OkHttp engine)               |
|  - RedirectAuthorizer (Chrome Custom Tabs + app redirect)      |
|                                                               |
|  [iosMain]                                                     |
|  - SocialAuthClientFactory (Ktor Darwin engine)               |
|  - WebAuthenticationAuthorizer (ASWebAuthenticationSession)    |
+---------------------------------------------------------------+
```

The module declares **no `<activity>` and no manifest placeholders**. The browser
round-trip is abstracted behind `AuthorizationCodeProvider`; each platform binds
one into `SocialAuthClientFactory` before the first sign-in.

---

## 2. Security design

- **Public clients, no shipped secret.** `SocialAuthConfig.clientSecret` defaults
  to `null` and is omitted from token requests. PKCE `S256` is what secures the
  code exchange for a mobile client. `GoogleCredentialsParser.toConfig(...)` drops
  the secret from `client_secret.json` unless `includeClientSecret = true` (for
  confidential server/desktop clients only).
- **PKCE everywhere.** A 64-char `code_verifier` is drawn per sign-in from the
  platform CSPRNG (`java.security.SecureRandom` / `arc4random_buf`); the `S256`
  challenge uses the platform's audited SHA-256 (`MessageDigest` / CommonCrypto
  `CC_SHA256`) via `expect`/`actual`.
- **CSRF protection.** A 128-bit `state` from the same CSPRNG is sent on every
  authorization request and checked on the redirect; a mismatch aborts before any
  token call.
- **Local sign-out only.** `signOut()` clears the local session and store; it does
  not hit a provider revocation endpoint, so an outstanding access token remains
  valid until expiry.
- **`id_token` is not signature-verified on device.** The SDK trusts it because it
  comes straight from the token endpoint over TLS and checks `aud`/`azp`/`iss`/`exp`/`nonce`.
  If you forward the `id_token` to your own backend, that backend **must** verify the
  JWT signature against the provider's JWKS itself.
- **Token hygiene.** Access/refresh/id tokens never appear in log lines. Errors
  are reported as a small `AuthError` enum plus a log-safe message.
- **Refresh-token durability.** Google issues a refresh token only on first
  consent; a missing one is tolerated (not a parse failure) and any previously
  held refresh token is carried across refreshes.
- **Storage is the host's choice.** The module does not persist tokens itself.
  Back `AuthSessionStore` with your own encrypted store (e.g. the `:storage`
  `SecureStorage`) via `DelegatingAuthSessionStore`.

---

## 3. Usage

### 3.1 Configure

```kotlin
val config = SocialAuthConfig(
    clientId = "YOUR_CLIENT_ID.apps.googleusercontent.com",
    redirectUri = "com.example.app:/oauth2redirect",
    scopes = listOf("openid", "email", "profile"),
)
```

or from a Google `client_secret.json`:

```kotlin
val client = GoogleCredentialsParser.parse(jsonString)!!
val config = GoogleCredentialsParser.toConfig(client, redirectUri = "com.example.app:/oauth2redirect")
```

### 3.2 Android

```kotlin
// startup
val authorizer = RedirectAuthorizer(applicationContext)
SocialAuthClientFactory.bindAuthorizer(authorizer)

val secure = /* :storage SecureStorage */
val client = SocialAuthClientFactory.createWith(
    config,
    DelegatingAuthSessionStore(
        read = { secure.getString("social_auth_session") },
        write = { json ->
            if (json == null) secure.remove("social_auth_session")
            else secure.putString("social_auth_session", json)
        },
    ),
)
```

Declare **your own** redirect Activity and forward the URI:

```xml
<activity android:name=".OAuthRedirectActivity" android:exported="true"
          android:launchMode="singleTask">
  <intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="com.example.app" android:path="/oauth2redirect" />
  </intent-filter>
</activity>
```

```kotlin
class OAuthRedirectActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handle(intent)
    }

    // singleTask reuses a live instance — the redirect then arrives via onNewIntent.
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.data?.let { authorizer.onRedirect(it) }
        finish()
    }
}
```

**Redirect scheme.** A private-use scheme (`com.example.app:/oauth2redirect`) is simplest, but
any app on the device can register the same scheme. Where the provider supports it, prefer an
**Android App Link** (`https://` `<data>` with a verified Digital Asset Links file), per RFC 8252 §8.1.

**Abandoned flows.** Custom Tabs gives no "dismissed" callback, so `RedirectAuthorizer` falls back
to a 3-minute timeout (configurable via its constructor). Call `authorizer.onCancelled()` from the
screen that launched sign-in, in `onResume`, to end an abandoned flow immediately — but guard it so
a user returning mid-flow from a 2FA / authenticator app doesn't cancel a still-valid attempt
(e.g. only when your own "signing in" UI is showing).

**Process death.** The in-flight `state`/PKCE verifier live only in memory. If Android kills the
app process while the Custom Tab is open (common on low-memory devices during a slow browser
sign-in or 2FA), the redirect arrives after a cold start with no pending round and
`onRedirect(...)` returns `false` — the sign-in is silently lost and the user must start over.
Treat a sign-in that never produces an `AuthResult` as "retry from the sign-in screen".

### 3.3 iOS

```kotlin
SocialAuthClientFactory.bindAuthorizer(WebAuthenticationAuthorizer())
val client = SocialAuthClientFactory.create(config)
```

The redirect scheme is passed to `ASWebAuthenticationSession` as its
`callbackURLScheme`; register it in the app's URL types.

### 3.4 Sign in / use tokens

```kotlin
when (val result = client.signIn()) {
    is AuthResult.Success   -> show(result.session.user)
    is AuthResult.Cancelled -> { /* user backed out */ }
    is AuthResult.Failure   -> report(result.error, result.message)
}

// always-valid access token (auto-refreshes when expired)
val token = client.currentAccessToken()

// observe
client.authState.collect { state -> /* SignedOut | SignedIn */ }

client.signOut()

// releases the HTTP engine when the client is no longer needed
client.close()
```

`AuthResult.Failure.message` is diagnostic text for logs — branch on `AuthResult.Failure.error`
(an `AuthError`) for anything user-facing.

---

## 4. Testing

`commonMain/.../testing/` ships fakes usable from any target:

- `FakeSocialAuthClient` — scriptable `SocialAuthClient`, no browser/network.
- `FakeAuthorizationCodeProvider` — scriptable authorization step for driving
  `DefaultSocialAuthClient` in tests (pair with a Ktor `MockEngine`).

```bash
./gradlew :socialauth:testAndroidHostTest
./gradlew :socialauth:iosSimulatorArm64Test
./gradlew :socialauth:compileKotlinIosSimulatorArm64
```

Coverage: SHA-256 vectors, PKCE, JWT claim extraction, token-response/error
parsing, `client_secret.json` parsing, authorization-URL construction, the Ktor
token client (`MockEngine`), and the full `DefaultSocialAuthClient` orchestration
(success, cancel, provider error, `state` mismatch, exchange failure, silent
re-use of a valid session, expiry-triggered refresh, sign-out, store restore).
