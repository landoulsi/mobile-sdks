package com.trackmit.location.network

import io.ktor.client.HttpClient

/**
 * Platform-specific factory for the Ktor [HttpClient] backing the IP-based location provider.
 *
 * Each target source set supplies an `actual`:
 * - **androidMain** → OkHttp engine
 * - **iosMain** → Darwin (URLSession) engine
 */
expect fun createLocationHttpClient(): HttpClient
