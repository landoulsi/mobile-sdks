package com.landoulsi.update.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.landoulsi.update.shared.model.UpdateState

@Composable
fun UpdateScreen(
    state: UpdateState,
    onUpdateClicked: () -> Unit,
    onDismissClicked: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                is UpdateState.UpdateRequired -> {
                    Text(
                        text = "Update Required",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "A new version of the app is required to continue. Please update to the latest version.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onUpdateClicked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update Now")
                    }
                }
                is UpdateState.UpdateRecommended -> {
                    Text(
                        text = "Update Available",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "A new version is available with new features and improvements.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onUpdateClicked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Update")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismissClicked,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Later")
                    }
                }
                else -> {
                    // Empty or NoUpdate state
                }
            }
        }
    }
}
