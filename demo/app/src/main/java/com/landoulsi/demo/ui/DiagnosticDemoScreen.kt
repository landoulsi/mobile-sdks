package com.landoulsi.demo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.landoulsi.diagnostic.AndroidLocationDiagnosticsProvider
import com.landoulsi.diagnostic.AndroidNetworkDiagnosticsProvider
import com.landoulsi.diagnostic.DiagnosticResult
import com.landoulsi.diagnostic.DiagnosticState
import com.landoulsi.diagnostic.location.GpsStatusDiagnosticCheck
import com.landoulsi.diagnostic.location.LocationAccuracyDiagnosticCheck
import com.landoulsi.diagnostic.network.NetworkSignalDiagnosticCheck
import com.landoulsi.diagnostic.network.VpnDiagnosticCheck
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticDemoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var results by remember { mutableStateOf<List<DiagnosticResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val runDiagnostics = {
        coroutineScope.launch {
            isLoading = true
            val networkProvider = AndroidNetworkDiagnosticsProvider(context)
            val locationProvider = AndroidLocationDiagnosticsProvider(context)

            val checks = listOf(
                VpnDiagnosticCheck(networkProvider),
                NetworkSignalDiagnosticCheck(networkProvider),
                GpsStatusDiagnosticCheck(locationProvider),
                LocationAccuracyDiagnosticCheck(locationProvider)
            )

            results = checks.map { it.run() }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        runDiagnostics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostic SDK Demo") },
                navigationIcon = {
                    Button(onClick = onBack, modifier = Modifier.padding(start = 8.dp)) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Button(
                onClick = { runDiagnostics() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Running Checks..." else "Re-run Diagnostics")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (results.isEmpty() && isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(results) { result ->
                        DiagnosticResultCard(result)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticResultCard(result: DiagnosticResult) {
    val badgeColor = when (result.state) {
        DiagnosticState.PASS -> Color(0xFF2E7D32)
        DiagnosticState.WARNING -> Color(0xFFED6C02)
        DiagnosticState.ERROR -> Color(0xFFD32F2F)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = result.state.name,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (result.cause != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cause: ${result.cause}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (result.metadata.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Metadata: ${result.metadata.entries.joinToString { "${it.key}=${it.value}" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
