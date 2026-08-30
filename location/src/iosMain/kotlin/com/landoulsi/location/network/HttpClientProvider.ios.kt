package com.landoulsi.location.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS actual for [createLocationHttpClient] — Darwin (URLSession) engine.
 */
actual fun createLocationHttpClient(): HttpClient = locationHttpClient(Darwin)
