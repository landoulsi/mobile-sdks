package com.landoulsi.survey.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.landoulsi.schemaui.compose.SchemaUI
import com.landoulsi.survey.SurveyController
import com.landoulsi.survey.SurveyState

/**
 * Renders whatever [controller] currently holds: a spinner while a server fetch runs, an
 * error line if loading failed, the interactive `:schemaui` survey while [SurveyState.Ready],
 * or [submittedContent] once the response is accepted.
 *
 * The survey submits itself through the button in its schema; the host does not call
 * [SurveyController.submit] directly unless it wants a custom submit affordance.
 *
 * @param submittedContent shown after a successful submit. Defaults to a short thank-you.
 */
@Composable
fun Survey(
    controller: SurveyController,
    modifier: Modifier = Modifier,
    submittedContent: @Composable (SurveyState.Submitted) -> Unit = { DefaultSubmitted() },
) {
    val state by controller.state.collectAsState()

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val s = state) {
            SurveyState.Idle -> Unit

            SurveyState.Loading -> CircularProgressIndicator()

            is SurveyState.LoadError -> Text(
                text = s.message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp),
            )

            is SurveyState.Ready -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (s.submitting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                SchemaUI(
                    node = s.node,
                    engine = controller.engine,
                    modifier = Modifier.fillMaxWidth(),
                )
                s.submitError?.let { err ->
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            is SurveyState.Submitted -> submittedContent(s)
        }
    }
}

@Composable
private fun DefaultSubmitted() {
    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Thanks!", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Your response has been recorded.", style = MaterialTheme.typography.bodyMedium)
    }
}
