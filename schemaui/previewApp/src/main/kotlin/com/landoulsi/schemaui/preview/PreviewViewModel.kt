package com.landoulsi.schemaui.preview

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.landoulsi.schemaui.SchemaUIEngine
import com.landoulsi.schemaui.ir.UINode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "SchemaUIPreview"

/**
 * UI state for the SchemaUI preview screen.
 */
sealed class PreviewUiState {
    data object Idle : PreviewUiState()
    data class Rendered(val node: UINode) : PreviewUiState()
    data class Error(val message: String) : PreviewUiState()
}

/**
 * ViewModel for the SchemaUI preview screen.
 * Owns the [SchemaUIEngine], registers demo action handlers, and drives [uiState].
 */
class PreviewViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<PreviewUiState>(PreviewUiState.Idle)
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    private val _schemaInput = MutableStateFlow("")
    val schemaInput: StateFlow<String> = _schemaInput.asStateFlow()

    val engine = SchemaUIEngine()

    init {
        // Register demo action handlers
        engine.registerActionWithState("register") { state ->
            val name = state["name"] ?: ""
            val email = state["email"] ?: ""
            Log.d(TAG, "Register: name=$name, email=$email")
        }
        engine.registerAction("navigate_login") {
            Log.d(TAG, "Navigate to login")
        }
        listOf("add_item_1", "add_item_2", "add_item_3").forEach { action ->
            engine.registerAction(action) {
                Log.d(TAG, "Action: $action")
            }
        }
        listOf("book_mountain_retreat", "learn_more").forEach { action ->
            engine.registerAction(action) {
                Log.d(TAG, "Action: $action")
            }
        }
    }

    fun updateInput(text: String) {
        _schemaInput.value = text
    }

    fun render() {
        val input = _schemaInput.value.trim()
        if (input.isEmpty()) {
            _uiState.value = PreviewUiState.Error("Schema input is empty.")
            return
        }
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                engine.parseFromString(input)
            }
            _uiState.value = result.fold(
                onSuccess = { node -> PreviewUiState.Rendered(node) },
                onFailure = { e -> PreviewUiState.Error(e.message ?: "Unknown error") },
            )
        }
    }

    fun loadPreset(resourceId: Int) {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    getApplication<Application>().resources.openRawResource(resourceId)
                        .bufferedReader()
                        .use { it.readText() }
                }
                _schemaInput.value = json
                render()
            } catch (e: Exception) {
                _uiState.value = PreviewUiState.Error("Failed to load preset: ${e.localizedMessage}")
            }
        }
    }

    fun clear() {
        _schemaInput.value = ""
        _uiState.value = PreviewUiState.Idle
        engine.stateStore.clear()
    }
}
