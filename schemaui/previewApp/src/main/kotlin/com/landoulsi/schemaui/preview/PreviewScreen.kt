package com.landoulsi.schemaui.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.landoulsi.schemaui.compose.SchemaUI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(viewModel: PreviewViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val schemaInput by viewModel.schemaInput.collectAsState()

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "SchemaUI Preview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                            )
                            Text(
                                "JSON → Native UI",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF6366F1),
                        titleContentColor = Color.White,
                    ),
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // ── Schema Input ──────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F9FF))
                        .padding(16.dp),
                ) {
                    Text(
                        "JSON Schema",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color(0xFF374151),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = schemaInput,
                        onValueChange = viewModel::updateInput,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = {
                            Text(
                                """{"type":"text","text":"Hello SchemaUI!"}""",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFF9CA3AF),
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (schemaInput.isNotEmpty()) {
                                IconButton(onClick = viewModel::clear) {
                                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                    )
                    Spacer(Modifier.height(12.dp))

                    // ── Preset Buttons ────────────────────────────────────────
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Presets:",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280),
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                        PresetButton("Form") {
                            viewModel.loadPreset(R.raw.sample_form)
                        }
                        PresetButton("List") {
                            viewModel.loadPreset(R.raw.sample_list)
                        }
                        PresetButton("Card") {
                            viewModel.loadPreset(R.raw.sample_card)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // ── Render Button ─────────────────────────────────────────
                    Button(
                        onClick = viewModel::render,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Render", fontWeight = FontWeight.SemiBold)
                    }
                }

                // ── Live Preview Pane ─────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF1F5F9)),
                ) {
                    when (val state = uiState) {
                        is PreviewUiState.Idle -> IdlePlaceholder()
                        is PreviewUiState.Error -> ErrorCard(state.message)
                        is PreviewUiState.Rendered -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                SchemaUI(node = state.node, engine = viewModel.engine)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun IdlePlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✏️", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Paste a JSON schema above\nand tap Render",
                textAlign = TextAlign.Center,
                color = Color(0xFF9CA3AF),
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "⚠ Parse Error",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626),
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    message,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFF7F1D1D),
                )
            }
        }
    }
}
