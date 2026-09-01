package com.landoulsi.socialauth

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun defaultTokenHttpClient(): HttpClient = HttpClient(OkHttp) {
    socialAuthHttpDefaults()
    // Belt-and-braces: also forbid redirects on the OkHttp client itself so its
    // RetryAndFollowUpInterceptor can't follow a 3xx on a credential POST before
    // Ktor's pipeline sees it.
    engine {
        config {
            followRedirects(false)
            followSslRedirects(false)
        }
    }
}
