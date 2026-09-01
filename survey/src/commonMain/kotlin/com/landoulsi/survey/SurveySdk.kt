package com.landoulsi.survey

/**
 * Entry-point metadata for the `:survey` module.
 *
 * The functional surface is [SurveyController]: load a survey from JSON or a URL, render the
 * `:schemaui` tree it exposes, and submit the collected [com.landoulsi.survey.model.SurveyResponse].
 */
object SurveySdk {
    const val VERSION = "1.0.0"
}
