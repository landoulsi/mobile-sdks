package com.landoulsi.demo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.landoulsi.update.shared.manager.UpdateManager
import com.landoulsi.update.shared.model.UpdateConfig
import com.landoulsi.update.shared.model.UpdateState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDemoScreen(onBack: () -> Unit) {
    var state by remember { mutableStateOf<UpdateState>(UpdateState.NoUpdate) }
    val updateManager = remember { UpdateManager() }

    // Mock config to force an update recommended state for demo purposes
    LaunchedEffect(Unit) {
        val config = UpdateConfig(
            latestVersion = "2.0.0",
            minRequiredVersion = "1.0.0",
            isUpdateRequired = false
        )
        state = updateManager.checkUpdate("1.5.0", config)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update SDK Demo") }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            UpdateScreen(
                state = state,
                onUpdateClicked = {
                    println("Update flow started")
                },
                onDismissClicked = {
                    state = UpdateState.NoUpdate
                }
            )
        }
    }
}
