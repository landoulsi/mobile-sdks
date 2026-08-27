package com.landoulsi.update

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.landoulsi.update.shared.manager.UpdateManager
import com.landoulsi.update.shared.model.UpdateConfig
import com.landoulsi.update.shared.model.UpdateState
import com.landoulsi.update.ui.UpdateScreen
import com.landoulsi.update.ui.theme.UpdatesdkTheme

class MainActivity : ComponentActivity() {
    private val updateManager = UpdateManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UpdatesdkTheme {
                var state by remember { 
                    val config = UpdateConfig("2.0.0", "1.0.0", false)
                    mutableStateOf(updateManager.checkUpdate("1.5.0", config))
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UpdateScreen(
                        state = state,
                        onUpdateClicked = {
                            // Launch native or custom update URL
                            println("Update clicked")
                        },
                        onDismissClicked = {
                            state = UpdateState.NoUpdate
                        }
                    )
                }
            }
        }
    }
}
