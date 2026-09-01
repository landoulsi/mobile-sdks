package com.landoulsi.survey

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun defaultSurveyHttpClient(): HttpClient = HttpClient(OkHttp) { surveyHttpDefaults() }
