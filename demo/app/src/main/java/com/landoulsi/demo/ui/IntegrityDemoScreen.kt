package com.landoulsi.demo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.landoulsi.integrity.IntegrityManager
import com.landoulsi.integrity.IntegrityResult
import com.landoulsi.integrity.mocklocation.AndroidMockLocationCheckContext
import com.landoulsi.integrity.mocklocation.LocationSample
import com.landoulsi.integrity.mocklocation.MockLocationDetectionEvaluator
import com.landoulsi.integrity.mocklocation.MockLocationSignal
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.RiskLevel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrityDemoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var scanResult by remember { mutableStateOf<IntegrityResult?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var simulatedScenario by remember { mutableStateOf("Live Device State") }

    fun runScan(samples: List<LocationSample> = emptyList(), scenarioName: String = "Live Device State") {
        coroutineScope.launch {
            isScanning = true
            simulatedScenario = scenarioName
            val mockContext = AndroidMockLocationCheckContext(
                context = context,
                recentLocationSupplier = { samples },
            )
            val evaluator = MockLocationDetectionEvaluator(mockContext)
            val manager = IntegrityManager.from(listOf(evaluator))
            scanResult = manager.scan()
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        runScan()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mock Location & GPS Integrity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Text(
                text = "Scenarios & Anomaly Simulations",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { runScan(emptyList(), "Live Device Scan") },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Device Scan")
                }
                Button(
                    onClick = {
                        val jumpSamples = listOf(
                            LocationSample(latitude = 37.7749, longitude = -122.4194, timestampMs = 1000L),
                            LocationSample(latitude = 51.5074, longitude = -0.1278, timestampMs = 3000L),
                        )
                        runScan(jumpSamples, "Simulated 5500km Jump Anomaly")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Simulate Jump")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        val velocitySamples = listOf(
                            LocationSample(latitude = 37.7749, longitude = -122.4194, timestampMs = 10000L),
                            LocationSample(latitude = 37.3382, longitude = -121.8863, timestampMs = 30000L),
                        )
                        runScan(velocitySamples, "Simulated Supersonic Speed")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Velocity Spoof")
                }
                Button(
                    onClick = {
                        val mockFlagSamples = listOf(
                            LocationSample(
                                latitude = 48.8566,
                                longitude = 2.3522,
                                timestampMs = System.currentTimeMillis(),
                                isMock = true,
                            ),
                        )
                        runScan(mockFlagSamples, "Simulated Mock Flag Active")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Mock Flag")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            if (isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                scanResult?.let { result ->
                    Text(
                        text = "Current Scenario: $simulatedScenario",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (result.riskLevel) {
                                RiskLevel.LOW -> MaterialTheme.colorScheme.surfaceVariant
                                RiskLevel.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer
                                RiskLevel.HIGH -> MaterialTheme.colorScheme.errorContainer
                                RiskLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                            },
                        ),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Integrity Score: ${result.integrityScore} / 100",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = "Risk Level: ${result.riskLevel} | Action: ${result.action}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "Mock Location Vector Flagged: ${result.hasMockLocation}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Evaluated Signals (${result.fired.size} active)",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (result.fired.isEmpty()) {
                            item {
                                Text(
                                    text = "No spoofing or mock location signals detected. Environment clean.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            items(result.fired) { signal ->
                                SignalItemCard(signal)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignalItemCard(signal: IntegritySignal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = signal.name,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = signal.severity.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = signal.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (signal.metadata.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Metadata: ${signal.metadata}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
