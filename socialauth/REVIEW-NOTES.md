# `:socialauth` — code-review disposition ledger

The module was hardened over **23 iterations** of
`review-code.sh --model antigravity/claude --diff-only` (rounds 18–40 of this effort).

That reviewer is an LLM and is **non-deterministic: it has never once produced an empty
run.** On a security-dense OAuth/OIDC surface it re-raises declined architectural
preferences every pass, and from ~round 34 on it began reporting "missing" test files that
demonstrably exist (`GoogleCredentialsParserTest`, `PkceTest`, `OAuthTokenParserTest`, …) —
its `--diff-only` view truncates the ~5,200-line diff. Its round 38–40 executive summaries
call the module *"exceptionally well-engineered, security-conscious, and thoroughly
tested… top-tier."*

This file is the honest answer to "has the review passed": **every finding it raised is
dispositioned below** — *fixed* (with the test that pins it) or *declined* (with the
reason). The loop was stopped after round 40 because the reviewer does not terminate and
every substantive item is resolved.

Final state: `./gradlew :socialauth:testAndroidHostTest :socialauth:iosSimulatorArm64Test
:socialauth:compileKotlinIosArm64 :socialauth:compileKotlinIosX64 :socialauth:assemble`
→ **BUILD SUCCESSFUL, 176 tests/platform, 0 failures, 0 warnings.** (146 at the start.)

---

## Fixed — security / correctness

| Finding | Fix | Pinned by (test) |
|---|---|---|
| `id_token` carried onto a refreshed session even when stale/expired | kept only if `idTokenValidator.isRetainable()` — strict, **no** clock-skew leeway | `refreshCarriesForwardAStillValidIdTokenWhenTheGrantReturnsNone`, `refreshDropsAnExpiredPriorIdTokenWhenTheGrantReturnsNone`, `IdTokenValidatorTest.isRetainableAppliesNoPastExpiryLeeway` |
| `sub` changed across refresh left the session in storage | dedicated `SubjectChangedException` → `refreshLocked` clears the session | `refreshWithAChangedSubjectIsRejectedAndClearsTheSession` |
| A substituted/invalid `id_token` on refresh (bad aud/azp/iss/sub) left the session intact | `refreshLocked` matches `IdTokenCheck.Rejected` → `clearSessionState()`; `IdTokenCheck.Expired` (stale, not an attack) keeps it | `refreshGrantIdTokenAudienceIsAlsoValidatedAndClearsTheSession`, `refreshWithAnExpiredIdTokenSurfacesFailureButKeepsTheSession` |
| `IdTokenValidator` never checked `sub` (OIDC Core: REQUIRED) | `sub` presence check, first in the chain | `IdTokenValidatorTest.rejectsMissingSub` |
| `IdTokenValidator` never checked `iat` | reject an `iat` in the future beyond clock skew (missing `iat` tolerated) | `IdTokenValidatorTest.rejectsIatInTheFutureButToleratesAMissingIat` |
| Oversized/malformed `exp`/`iat` could overflow `Long` when scaled to millis | `epochSecondsToMillis` clamps to a year-2100 ceiling | `IdTokenValidatorTest.oversizedExpDoesNotOverflow` |
| Unverified `email` surfaced from an `id_token` (account-linking / pre-hijack) | id_token `email` surfaced only when `email_verified` is explicitly `true` (absent ≠ verified, OIDC §5.1); with an id_token present, never falls back to a body `email` field | `OAuthTokenParserTest.idTokenEmailIsDroppedWhenEmailVerifiedIsFalse`, `aTopLevelEmailIsNotUsedWhenAnIdTokenIsPresentButOmitsEmail` |
| Blank `sub` / `user_id` accepted as the user id | `takeIf { it.isNotBlank() }` on both | (covered via `IdTokenValidatorTest.rejectsMissingSub` + parser tests) |
| `redirectUri` accepted cleartext `http://` on a non-loopback host (RFC 8252 §7.1/§8.1) | `http` allowed only for loopback, parsed via Ktor `Url` so `http://localhost:80@evil.com` can't spoof loopback | `SocialAuthConfigTest.cleartextHttpRedirectUriIsRejectedUnlessLoopback` |
| `redirectUri` accepted browser/OS pseudo-schemes (`javascript:`, `data:`, `file:`, `intent:`, …) | denylist check | `SocialAuthConfigTest.dangerousRedirectUriSchemesAreRejected` |
| `redirectUri` / endpoints accepted a URI fragment; https redirect accepted user-info; a malformed http(s) URL slipped past the user-info check | reject `#` on redirect **and** endpoints; `Url(...).getOrElse { throw }` (fail-closed) | `redirectUriWithAFragmentOrUserInfoIsRejected`, `endpointWithAFragmentIsRejected` |
| `redirectUri` with no scheme failed deep inside `authorize()` | `validate()` requires an RFC 3986 scheme | `redirectUriWithoutASchemeIsRejected` |
| OkHttp engine's `RetryAndFollowUpInterceptor` could follow a 3xx on a credential POST before Ktor's `followRedirects=false` applied | Android engine also sets `followRedirects(false)` / `followSslRedirects(false)` on the OkHttp builder | inspection (engine config) |
| OIDC `nonce` mismatch not covered end-to-end | test added | `idTokenWithMismatchedNonceIsRejected` |
| `clientSecret` redaction in `SocialAuthConfig` / `GoogleOAuthClient` `toString()` untested | test added | `RedactedToStringTest.configAndGoogleClientHideTheClientSecret` |
| PKCE not checked against the RFC 7636 worked example | test added | `PkceTest.matchesRfc7636AppendixBVector` |

## Fixed — lifecycle / concurrency

| Finding | Fix | Pinned by |
|---|---|---|
| `close()` did not stop an in-flight sign-in: burned the auth code, hit the closed HTTP client, could persist / report `Success` | `isClosed` (`@Volatile`) checked at the `signIn` fast-path, inside `interactiveMutex` before `runInteractiveSignIn`, before the token exchange, and in the persist gate; **also in `refreshLocked` before `persist`** — a closed client returns `AuthResult.Failure(UNKNOWN, "…closed")` | `signInOnAClosedClientFailsWithoutOpeningTheBrowser`, `signInThatCompletesAfterCloseDoesNotPersistOrReportSuccess`, `signInQueuedBehindAnotherFailsWithoutOpeningABrowserWhenClosedWhileWaiting`, `refreshThatCompletesAfterCloseDoesNotPersist` |
| A store that throws on `clear()` left the client stuck "signed in" (only `signOut()` was guarded) | single `clearSessionState()` helper (`runCatching` + log + `SignedOut`) at all four call sites | `signOutMovesToSignedOutEvenIfClearingThrows`, `expiredUnrefreshableSessionClearsToSignedOutEvenIfClearingThrows` |
| A custom store that throws on `load()` crashed the constructor | `restoreInitialAuthState()` wraps `load()` in `runCatching { }.getOrNull()` | `constructionStartsSignedOutWhenTheStoreThrowsOnLoad` |
| Restore / `currentAccessToken` wiped a session that was only *inside the leeway window* (still usable) | restore uses `leewayMillis = 0` (hard-expiry only); `currentAccessToken` returns the token while it is genuinely unexpired — on no-refresh-token, on a transient refresh failure (re-sampling the clock after the network call), never on `invalid_grant` | `startupDiscardsAnExpiredUnrefreshableSession`, `currentAccessTokenReturnsAnUnrefreshableTokenThatIsOnlyInsideTheLeewayWindow`, `transientRefreshFailureFallsBackToTheStillUsableCurrentToken`, `transientRefreshFailureKeepsTheStoredSession` |
| A sign-in queued behind an abandoned one opened a needless browser instead of refreshing | queued path re-runs `validateAndReuseExisting()` under `interactiveMutex` | `queuedSignInRefreshesInsteadOfOpeningASecondBrowserWhenTheSessionExpiredWhileWaiting` |
| `RedirectAuthorizer` timeout race discarded a redirect that completed at the same instant | on timeout, `complete(Cancelled)` returning `false` ⇒ `await()` the real result | (mechanism; `PendingAuthorizationTest` covers the CAS) |
| Blocking store/HTTP work ran on `Dispatchers.Default` | `internal expect val socialAuthIoDispatcher` → `Dispatchers.IO` (Android), `Dispatchers.Default` (iOS — K/N has no public `Dispatchers.IO`; verified: `internal` in kotlinx-coroutines 1.11.0) | compiles on all targets |

## Fixed — architecture / cleanup

- `AuthorizationResult.Failure(error: String)` → **sealed `AuthorizationError`** (`LaunchFailed`, `InvalidRedirect`, `ProviderUnavailable`, `InvalidConfiguration`, `InteractiveFlowFailed`/`InteractiveFlowStartFailed` — platform-neutral, not iOS-named — and `ProviderReported(code)` for open-ended provider codes). Mapping in `DefaultSocialAuthClient` is now exhaustive.
- `IdTokenValidator.validate(): String?` → **sealed `IdTokenCheck`** (`Valid` / `Expired` / `Rejected(reason)`); `Expired` is split from `Rejected` so the caller wipes the session for substitution-class failures only. String-matching (`REASON_EXPIRED`) removed.
- Synthetic `oauthErrorCode = "socialauth_subject_changed"` (a fake RFC field) → dedicated `SubjectChangedException : OAuthException`.
- All timing constants moved to `internal/Constants.kt` as real `const val` (`DEFAULT_INTERACTIVE_AUTH_TIMEOUT_MILLIS`, `MAX_EXPIRES_IN_SECONDS`, `DEFAULT_CLOCK_SKEW_LEEWAY_MILLIS` as arithmetic); `PkceCodes.method` → `PkceCodes.CHALLENGE_METHOD` const; `DEFAULT_OIDC_SCOPES` de-duplicated; `"openid"` → `OPENID_SCOPE`; `JWT_SEGMENT_COUNT`/`JWT_PAYLOAD_INDEX` named.
- `restoreInitialAuthState()` extracted from the property initializer; `_authState` seed logic readable.
- Dead `Jwt.audiences(jwt)` / `issuer(jwt)` / `expiresAtEpochSeconds(jwt)` single-arg wrappers removed (production uses `claims()` + the `*Of(JsonObject)` readers).
- `GoogleCredentialsParser.isValid()` was unused → wired in as a `toConfig()` precondition (`toConfigWithNoClientIdThrows`).
- Dead `AuthResult.Cancelled` branch removed from `validateAndReuseExisting()`; vestigial `failure` param removed from `validateIdToken`.
- `Jwt` parses the payload once per call; member ordering, `// region` comments, `signOut(): Unit`, import order, semicolon-chained test statements all cleaned to the Kotlin style guide.
- README: added the "process death drops an in-flight sign-in" limitation and a stronger `onCancelled()`/`onResume` 2FA caveat; `createWith(httpClient=)` now documents the `followRedirects = false` requirement.

---

## Declined (deliberate — recurring every round)

| Finding | Why it stands |
|---|---|
| Global `SocialAuthClientFactory.boundAuthorizer` mutable state | Matches the repo's `PushNotificationManagerFactory.bindPermissionRequester` idiom; `unbindAuthorizer()` exists; `createWith(authorizer = …)` is the DI path. Documented in `[[socialauth-design]]`. |
| Synchronous `sessionStore.load()` in the constructor | `StateFlow` needs a seed and a constructor can't suspend. Documented ("keep it cheap / pre-warm off the main thread") and now crash-safe. A `suspend initialize()` / `AuthState.Loading` changes the contract for every consumer. |
| `SocialAuthConfig` primary constructor carries Google defaults | The bare constructor is documented as Google; `SocialAuthConfig.google(...)` is the named factory; other providers override the four endpoint/param fields. |
| `configureCustomTab` lambda could capture an `Activity` | KDoc explicitly warns; a data-class config loses legitimate flexibility (animations, theme colors). |
| `AuthError` coarse enum (not a sealed hierarchy); grant-type / claim-name string literals; hex formatting in `randomState()` | `AuthError`'s own KDoc: "kept deliberately small so callers can branch on it exhaustively". Grant types are OAuth wire values used once each. `0xff`/`16`/`2` is the universal byte-to-hex idiom (`ByteArray.toHexString` is still `@ExperimentalStdlibApi` here). |
| `IdTokenCheck.Rejected(reason: String)` — reason should be an enum | `IdTokenCheck` is already typed where control flow branches (`Valid`/`Expired`/`Rejected`); `reason` is a leaf log diagnostic asserted verbatim by ~20 tests. |
| Mutex held across `refreshLocked` (network) — `signOut()` can wait up to 30 s | Deliberate: dedupes concurrent refreshes; `signOut` is documented local-only; the true fix is an in-flight-`Deferred` refactor, out of scope for a review-fix pass. |
| `interactiveMutex` held across the browser round-trip — a re-tapped sign-in waits for the 3-min timeout | The host calls `onCancelled()` from `onResume` to end an abandoned flow promptly (documented). A cancellable-interactive-`Job` redesign is out of scope. |
| Zero clock-skew leeway on the *code-exchange* id_token (like `isRetainable`) | Clock-skew tolerance on a *received* token's `exp` is standard OIDC and guards a fast client clock; `isRetainable`'s zero leeway is specifically for "carry a stored token onward", where a backend rejects anything past `exp`. |
| Hierarchical-`redirectUri` (`scheme:/…`) hard requirement | Added round 29, **reverted**: iOS `WebAuthenticationAuthorizer` accepts `scheme:path`; a breaking config constraint in the final phase with no consumer to validate against was the wrong trade. |
| Empty-signature / `alg:none` JWT rejection in `Jwt.claims` | `Jwt` never verifies signatures (documented — TLS-authenticated channel); enforcing it means reworking every `testJwtRaw` fixture for no threat-model gain. |
| `127.0.0.0/8` loopback range vs just `127.0.0.1` | Loopback-http is a desktop/test convenience; `localhost` + `127.0.0.1` covers real use. |
| `issuer` URL not run through `requireSecureEndpoint` | `issuer` is never dereferenced (no JWKS fetch here) — it's a string compared against the id_token `iss`. A cleartext issuer is an odd config value, not a network path; and Google's schemeless `iss` form is tolerated on purpose. |
| Custom `HttpClient` isn't validated for `followRedirects` | Documented as a hard requirement on `createWith(httpClient=)`; the built-in engines set it. |
| `FakeSocialAuthClient.currentAccessToken()` doesn't evaluate expiry | It is a hand fake for consumer tests; duplicating refresh logic defeats its purpose. `refreshSession()` on it does mirror the real `NO_ACTIVE_SESSION` contract. |
| SRP: extract `AuthSessionMapper` / `OAuthParamGenerator` / `TokenRefreshPolicy` | `DefaultSocialAuthClient` already delegates token exchange, id_token validation, URL building and persistence; further splitting `sessionFrom` / `randomState` is ceremony for a ~470-line orchestrator. |
| `uid` → `userId`, `toConfig` → `toSocialAuthConfig`, test-var names (`cid`/`at`/`rt`), `aud`/`iss` params, FQN in KDoc, `restoreInitialAuthState` placement | `uid` mirrors the OIDC `sub` / Firebase-Auth convention. Cross-source-set KDoc links (`[com.landoulsi.socialauth.RedirectAuthorizer]` from `commonMain`) are **load-bearing** — the short form doesn't resolve. Helper-placement guidance contradicted itself across rounds. |
| No unit tests for `RedirectAuthorizer` / `WebAuthenticationAuthorizer` | Needs Robolectric or a simulator; the repo has no Robolectric in its version catalog and a prior attempt failed (`UncompletedCoroutinesError`). All platform-agnostic logic is factored into `PendingAuthorization` / `RedirectResult`, covered in `commonTest`. This is a known tooling gap, not an oversight. |

## Recurring false positives (recorded so they aren't re-investigated)

- **"Missing iOS `actual` implementations / iOS build breakage"** (rounds 26–28). All five `iosMain` files are `git ls-files`-tracked; `:socialauth:compileKotlinIosArm64` / `compileKotlinIosX64` / `iosSimulatorArm64Test` (176 tests) were green **every** round. The reviewer's diff view truncates before the `iosMain/` paths.
- **"`GoogleCredentialsParser` / `OAuthTokenParser` / `Pkce` are untested"** (rounds 34–40). `GoogleCredentialsParserTest` (11 tests), `OAuthTokenParserTest` (13), `PkceTest` (incl. the RFC 7636 Appendix B vector), `Sha256Test` (FIPS vectors) all exist. Same diff-truncation artifact.
- **`const val DEFAULT_TIMEOUT_MILLIS` "should be const"** — it *is* `const` now (the shared value became a `const` in round 36; earlier rounds flagged it while it aliased a non-`const` `val`).
- **"`Dispatchers.IO` is available on Apple targets in coroutines 1.7+"** — not in this project's 1.11.0: `actual val = Dispatchers.IO` in `iosMain` fails to compile with *"it is internal in 'kotlinx.coroutines.Dispatchers'"*. The `Dispatchers.ios.kt` comment is correct.
