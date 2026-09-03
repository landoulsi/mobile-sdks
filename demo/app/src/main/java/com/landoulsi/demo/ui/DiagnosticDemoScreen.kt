package com.landoulsi.demo.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.landoulsi.diagnostic.AndroidLocationDiagnosticsProvider
import com.landoulsi.diagnostic.AndroidNetworkDiagnosticsProvider
import com.landoulsi.diagnostic.DiagnosticCheckConfig
import com.landoulsi.diagnostic.DiagnosticEngine
import com.landoulsi.diagnostic.location.GpsStatusDiagnosticCheck
import com.landoulsi.diagnostic.location.LocationAccuracyDiagnosticCheck
import com.landoulsi.diagnostic.network.NetworkSignalDiagnosticCheck
import com.landoulsi.diagnostic.network.VpnDiagnosticCheck
import com.landoulsi.diagnostic.ui.DiagnosticView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticDemoScreen(onBack: () -> Unit) {
    val appContext = LocalContext.current.applicationContext

    var vpnEnabled by remember { mutableStateOf(true) }
    var networkSignalEnabled by remember { mutableStateOf(true) }
    var gpsEnabled by remember { mutableStateOf(true) }
    var locationAccuracyEnabled by remember { mutableStateOf(true) }
    var signalThreshold by remember { mutableFloatStateOf(25f) }
    var accuracyThreshold by remember { mutableFloatStateOf(100f) }

    val networkProvider = remember { AndroidNetworkDiagnosticsProvider(appContext) }
    val locationProvider = remember { AndroidLocationDiagnosticsProvider(appContext) }

    val configKey = listOf(
        vpnEnabled, networkSignalEnabled, gpsEnabled, locationAccuracyEnabled,
        signalThreshold, accuracyThreshold,
    ).joinToString()

    val engine = remember(configKey) {
        val availableChecks = listOf(
            VpnDiagnosticCheck(networkProvider),
            NetworkSignalDiagnosticCheck(networkProvider, signalThreshold.toInt()),
            GpsStatusDiagnosticCheck(locationProvider),
            LocationAccuracyDiagnosticCheck(locationProvider, accuracyThreshold),
        )
        val configs = listOf(
            DiagnosticCheckConfig(VpnDiagnosticCheck.CHECK_ID, vpnEnabled),
            DiagnosticCheckConfig(NetworkSignalDiagnosticCheck.CHECK_ID, networkSignalEnabled),
            DiagnosticCheckConfig(GpsStatusDiagnosticCheck.CHECK_ID, gpsEnabled),
            DiagnosticCheckConfig(LocationAccuracyDiagnosticCheck.CHECK_ID, locationAccuracyEnabled),
        )
        DiagnosticEngine.fromConfigs(availableChecks, configs)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostic SDK Demo") },
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
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "Configure Checks",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Toggle checks and adjust thresholds, then tap Run Diagnostics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))

                CheckToggleRow(
                    label = "VPN & Proxy Detection",
                    checked = vpnEnabled,
                    onCheckedChange = { vpnEnabled = it },
                )
                CheckToggleRow(
                    label = "Network Signal Strength",
                    checked = networkSignalEnabled,
                    onCheckedChange = { networkSignalEnabled = it },
                )
                if (networkSignalEnabled) {
                    ThresholdSlider(
                        label = "Low signal threshold",
                        value = signalThreshold,
                        valueRange = 0f..100f,
                        onValueChange = { signalThreshold = it },
                        valueLabel = "${signalThreshold.toInt()}%",
                    )
                }
                CheckToggleRow(
                    label = "GPS & Location Services",
                    checked = gpsEnabled,
                    onCheckedChange = { gpsEnabled = it },
                )
                CheckToggleRow(
                    label = "Location Accuracy",
                    checked = locationAccuracyEnabled,
                    onCheckedChange = { locationAccuracyEnabled = it },
                )
                if (locationAccuracyEnabled) {
                    ThresholdSlider(
                        label = "Accuracy threshold",
                        value = accuracyThreshold,
                        valueRange = 10f..500f,
                        onValueChange = { accuracyThreshold = it },
                        valueLabel = "${accuracyThreshold.toInt()}m",
                    )
                }
            }

            HorizontalDivider()

            DiagnosticView(
                engine = engine,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CheckToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun ThresholdSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueLabel: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
        )
    }
}
