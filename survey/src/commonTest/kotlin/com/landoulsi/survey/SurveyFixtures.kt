package com.landoulsi.survey

/** Shared survey JSON used across the module's tests. Exercises every question type. */
internal object SurveyFixtures {

    const val SUBMIT_URL = "https://api.example.com/surveys/demo/responses"

    val FULL_SURVEY_JSON = """
        {
          "id": "demo",
          "title": "Product feedback",
          "description": "Two minutes, tops.",
          "submitLabel": "Send it",
          "submitUrl": "$SUBMIT_URL",
          "questions": [
            {
              "type": "rating",
              "id": "nps",
              "title": "How likely are you to recommend us?",
              "max": 10,
              "required": true
            },
            {
              "type": "singleChoice",
              "id": "role",
              "title": "What best describes you?",
              "required": true,
              "options": [
                { "value": "dev", "label": "Developer" },
                { "value": "pm", "label": "Product Manager" },
                { "value": "design", "label": "Designer" }
              ]
            },
            {
              "type": "multiChoice",
              "id": "channels",
              "title": "Where do you use the app?",
              "options": [
                { "value": "ios" },
                { "value": "android" },
                { "value": "web" }
              ]
            },
            {
              "type": "boolean",
              "id": "beta",
              "title": "Join the beta programme?"
            },
            {
              "type": "shortText",
              "id": "name",
              "title": "Your name",
              "placeholder": "Optional"
            },
            {
              "type": "longText",
              "id": "why",
              "title": "Anything else?",
              "required": true
            }
          ]
        }
    """.trimIndent()

    val UNKNOWN_TYPE_JSON = """
        {
          "id": "fwd",
          "title": "Forward compatible",
          "questions": [
            { "type": "hologram", "id": "q1", "title": "Rate the hologram" },
            { "type": "shortText", "id": "q2", "title": "Fallback still works" }
          ]
        }
    """.trimIndent()

    val MINIMAL_JSON = """
        { "id": "m", "title": "Minimal", "questions": [] }
    """.trimIndent()
}
