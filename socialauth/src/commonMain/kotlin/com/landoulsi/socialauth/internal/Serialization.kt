package com.landoulsi.socialauth.internal

import kotlinx.serialization.json.Json

/**
 * Shared JSON configuration for the module. Lenient and forgiving of unknown keys
 * because provider responses evolve and carry fields we do not model.
 */
internal val socialAuthJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}
