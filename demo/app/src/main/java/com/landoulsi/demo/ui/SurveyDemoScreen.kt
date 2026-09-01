package com.landoulsi.demo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.landoulsi.survey.SurveyController
import com.landoulsi.survey.SurveyState
import com.landoulsi.survey.compose.Survey
import com.landoulsi.survey.testing.FakeSurveyClient

/**
 * Dummy survey rendered from JSON by the `:survey` SDK (which builds a `:schemaui` tree and
 * collects answers). Submission goes to a [FakeSurveyClient] so it works offline — the
 * accepted payload is shown on the confirmation screen.
 */
private const val DEMO_SUBMIT_URL = "https://api.example.com/surveys/dev-experience/responses"

private val DEMO_SURVEY_JSON = """
{
  "id": "dev-experience-2026",
  "title": "Developer Experience Survey",
  "description": "Help us shape the SDK roadmap — about a minute.",
  "submitLabel": "Send feedback",
  "submitUrl": "$DEMO_SUBMIT_URL",
  "questions": [
    {
      "type": "rating",
      "id": "recommend",
      "title": "How likely are you to recommend these SDKs to a colleague?",
      "description": "1 = not at all, 10 = absolutely",
      "max": 10,
      "required": true
    },
    {
      "type": "singleChoice",
      "id": "role",
      "title": "What best describes your role?",
      "required": true,
      "options": [
        { "value": "android", "label": "Android engineer" },
        { "value": "ios", "label": "iOS engineer" },
        { "value": "fullstack", "label": "Full-stack / cross-platform" },
        { "value": "lead", "label": "Tech lead / manager" }
      ]
    },
    {
      "type": "multiChoice",
      "id": "modules",
      "title": "Which modules do you use today?",
      "options": [
        { "value": "payment", "label": "Payment" },
        { "value": "update", "label": "In-app Update" },
        { "value": "schemaui", "label": "SchemaUI" },
        { "value": "survey", "label": "Survey" },
        { "value": "socialauth", "label": "Social Auth" }
      ]
    },
    {
      "type": "boolean",
      "id": "docs_ok",
      "title": "Was the documentation enough to get started?",
      "trueLabel": "Yes",
      "falseLabel": "Not really"
    },
    {
      "type": "shortText",
      "id": "missing_module",
      "title": "One module you wish we shipped",
      "placeholder": "e.g. feature flags"
    },
    {
      "type": "longText",
      "id": "anything_else",
      "title": "Anything else on your mind?",
      "required": true
    }
  ]
}
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyDemoScreen(onBack: () -> Unit) {
    val fakeClient = remember { FakeSurveyClient() }
    val controller = remember { SurveyController(client = fakeClient, ownsClient = false) }

    LaunchedEffect(Unit) { controller.loadFromJson(DEMO_SURVEY_JSON) }
    DisposableEffect(Unit) { onDispose { controller.close() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Survey SDK Demo") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { paddingValues ->
        Survey(
            controller = controller,
            modifier = Modifier.padding(paddingValues),
            submittedContent = { submitted -> SubmittedPayload(submitted) },
        )
    }
}

@Composable
private fun SubmittedPayload(submitted: SurveyState.Submitted) {
    val response = submitted.response
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Submitted ✅", style = MaterialTheme.typography.headlineSmall)
        Text(
            "POST $DEMO_SUBMIT_URL",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("surveyId = ${response.surveyId}", fontFamily = FontFamily.Monospace)
        Text("submittedAtMillis = ${response.submittedAtMillis}", fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(4.dp))
        Text("answers:", style = MaterialTheme.typography.titleSmall)
        if (response.answers.isEmpty()) {
            Text("  (none)", fontFamily = FontFamily.Monospace)
        } else {
            response.answers.forEach { answer ->
                Text(
                    "  ${answer.questionId} = ${answer.values.joinToString(", ")}",
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}
