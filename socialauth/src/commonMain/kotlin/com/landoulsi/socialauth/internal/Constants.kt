package com.landoulsi.socialauth.internal

/** Single log tag for the whole module. */
internal const val SOCIAL_AUTH_LOG_TAG = "SocialAuth"

internal const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L

/** Clock-drift tolerance for token/id_token expiry checks (1 minute). */
internal const val DEFAULT_CLOCK_SKEW_LEEWAY_MILLIS = SECONDS_PER_MINUTE * MILLIS_PER_SECOND

/**
 * Backstop for an interactive browser round-trip that never reports a result, so a
 * platform authorizer can't hold its lock forever (3 minutes). Shared by both platform
 * authorizers. `const` so it inlines and can seed a `const` alias on either authorizer.
 */
internal const val DEFAULT_INTERACTIVE_AUTH_TIMEOUT_MILLIS = 3 * SECONDS_PER_MINUTE * MILLIS_PER_SECOND

/** Ceiling for a provider's `expires_in` (~10 years) so `now + seconds*1000` can't overflow `Long`. */
internal const val MAX_EXPIRES_IN_SECONDS = 3650L * 24 * 60 * 60

/** The scopes an OpenID Connect sign-in requests unless the caller overrides them. */
internal val DEFAULT_OIDC_SCOPES = listOf("openid", "email", "profile")
