package com.landoulsi.socialauth

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun defaultTokenHttpClient(): HttpClient =
    HttpClient(Darwin) { socialAuthHttpDefaults() }
