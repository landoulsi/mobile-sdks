package com.trackflow.location.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/**
 * Android actual for [createLocationHttpClient] — OkHttp engine, Ktor's recommended Android engine.
 */
actual fun createLocationHttpClient(): HttpClient = locationHttpClient(OkHttp)
