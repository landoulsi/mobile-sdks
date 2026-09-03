package com.landoulsi.demo.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.landoulsi.diagnostic.AndroidLocationDiagnosticsProvider
import com.landoulsi.diagnostic.AndroidNetworkDiagnosticsProvider
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

    val engine = remember {
        val networkProvider = AndroidNetworkDiagnosticsProvider(appContext)
        val locationProvider = AndroidLocationDiagnosticsProvider(appContext)

        val checks = listOf(
            VpnDiagnosticCheck(networkProvider),
            NetworkSignalDiagnosticCheck(networkProvider),
            GpsStatusDiagnosticCheck(locationProvider),
            LocationAccuracyDiagnosticCheck(locationProvider),
        )
        DiagnosticEngine(checks)
    }

    LaunchedEffect(engine) {
        engine.runDiagnostics()
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
        DiagnosticView(
            engine = engine,
            modifier = Modifier.padding(paddingValues),
        )
    }
}
