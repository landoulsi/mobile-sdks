package com.landoulsi.survey

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

internal actual fun defaultSurveyHttpClient(): HttpClient = HttpClient(Darwin) { surveyHttpDefaults() }
