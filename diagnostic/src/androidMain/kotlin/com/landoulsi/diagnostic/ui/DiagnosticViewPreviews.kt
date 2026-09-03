package com.landoulsi.diagnostic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.landoulsi.design.Spacing
import com.landoulsi.diagnostic.DiagnosticExecutionState
import com.landoulsi.diagnostic.DiagnosticResult
import com.landoulsi.diagnostic.DiagnosticState
import com.landoulsi.diagnostic.DiagnosticUiState
import com.landoulsi.diagnostic.platformTimeMillis

@Preview(name = "DiagnosticView - Idle", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DiagnosticViewIdlePreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            DiagnosticContent(
                state = DiagnosticUiState(),
                onRunDiagnostics = {},
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
            )
        }
    }
}

@Preview(name = "DiagnosticView - Running", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DiagnosticViewRunningPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            DiagnosticContent(
                state = DiagnosticUiState(
                    executionState = DiagnosticExecutionState.RUNNING,
                ),
                onRunDiagnostics = {},
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
            )
        }
    }
}

@Preview(name = "DiagnosticView - All Pass", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DiagnosticViewAllPassPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            DiagnosticContent(
                state = DiagnosticUiState(
                    executionState = DiagnosticExecutionState.COMPLETED,
                    results = listOf(
                        DiagnosticResult("1", "Network Connectivity", DiagnosticState.PASS, null, platformTimeMillis()),
                        DiagnosticResult("2", "VPN Detection", DiagnosticState.PASS, null, platformTimeMillis()),
                        DiagnosticResult("3", "GPS Status", DiagnosticState.PASS, null, platformTimeMillis()),
                        DiagnosticResult("4", "Location Accuracy", DiagnosticState.PASS, null, platformTimeMillis()),
                    ),
                    overallState = DiagnosticState.PASS,
                    passCount = 4,
                    warningCount = 0,
                    errorCount = 0,
                ),
                onRunDiagnostics = {},
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
            )
        }
    }
}

@Preview(name = "DiagnosticView - With Warnings", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DiagnosticViewWithWarningsPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            DiagnosticContent(
                state = DiagnosticUiState(
                    executionState = DiagnosticExecutionState.COMPLETED,
                    results = listOf(
                        DiagnosticResult("1", "Network Connectivity", DiagnosticState.PASS, null, platformTimeMillis()),
                        DiagnosticResult("2", "VPN Detection", DiagnosticState.WARNING, "VPN or proxy detected", platformTimeMillis()),
                        DiagnosticResult("3", "GPS Status", DiagnosticState.PASS, null, platformTimeMillis()),
                        DiagnosticResult("4", "Location Accuracy", DiagnosticState.WARNING, "Accuracy below threshold: 50m", platformTimeMillis()),
                    ),
                    overallState = DiagnosticState.WARNING,
                    passCount = 2,
                    warningCount = 2,
                    errorCount = 0,
                ),
                onRunDiagnostics = {},
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
            )
        }
    }
}

@Preview(name = "DiagnosticView - With Errors", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DiagnosticViewWithErrorsPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            DiagnosticContent(
                state = DiagnosticUiState(
                    executionState = DiagnosticExecutionState.COMPLETED,
                    results = listOf(
                        DiagnosticResult("1", "Network Connectivity", DiagnosticState.ERROR, "No internet connection", platformTimeMillis()),
                        DiagnosticResult("2", "VPN Detection", DiagnosticState.PASS, null, platformTimeMillis()),
                        DiagnosticResult("3", "GPS Status", DiagnosticState.ERROR, "Location services disabled", platformTimeMillis()),
                        DiagnosticResult("4", "Location Accuracy", DiagnosticState.PASS, null, platformTimeMillis()),
                    ),
                    overallState = DiagnosticState.ERROR,
                    passCount = 2,
                    warningCount = 0,
                    errorCount = 2,
                ),
                onRunDiagnostics = {},
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
            )
        }
    }
}

@Preview(name = "DiagnosticView - Mixed States", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DiagnosticViewMixedStatesPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            DiagnosticContent(
                state = DiagnosticUiState(
                    executionState = DiagnosticExecutionState.COMPLETED,
                    results = listOf(
                        DiagnosticResult("1", "Network Connectivity", DiagnosticState.PASS, null, platformTimeMillis()),
                        DiagnosticResult("2", "VPN Detection", DiagnosticState.WARNING, "VPN or proxy detected", platformTimeMillis()),
                        DiagnosticResult("3", "GPS Status", DiagnosticState.ERROR, "Location services disabled", platformTimeMillis()),
                        DiagnosticResult("4", "Location Accuracy", DiagnosticState.PASS, null, platformTimeMillis()),
                    ),
                    overallState = DiagnosticState.ERROR,
                    passCount = 2,
                    warningCount = 1,
                    errorCount = 1,
                ),
                onRunDiagnostics = {},
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
            )
        }
    }
}

@Preview(name = "DiagnosticResultItem - Pass", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DiagnosticResultItemPassPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            DiagnosticResultItem(
                result = DiagnosticResult("1", "Network Connectivity", DiagnosticState.PASS, null, platformTimeMillis()),
            )
        }
    }
}

@Preview(name = "DiagnosticResultItem - Warning", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DiagnosticResultItemWarningPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            DiagnosticResultItem(
                result = DiagnosticResult("2", "VPN Detection", DiagnosticState.WARNING, "VPN or proxy detected", platformTimeMillis()),
            )
        }
    }
}

@Preview(name = "DiagnosticResultItem - Error", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DiagnosticResultItemErrorPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            DiagnosticResultItem(
                result = DiagnosticResult("3", "GPS Status", DiagnosticState.ERROR, "Location services disabled", platformTimeMillis()),
            )
        }
    }
}

@Preview(name = "DiagnosticSummaryCard - Pass", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DiagnosticSummaryCardPassPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            DiagnosticSummaryCard(
                state = DiagnosticUiState(
                    executionState = DiagnosticExecutionState.COMPLETED,
                    overallState = DiagnosticState.PASS,
                    passCount = 4,
                ),
            )
        }
    }
}

@Preview(name = "DiagnosticSummaryCard - Warning", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DiagnosticSummaryCardWarningPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            DiagnosticSummaryCard(
                state = DiagnosticUiState(
                    executionState = DiagnosticExecutionState.COMPLETED,
                    overallState = DiagnosticState.WARNING,
                    warningCount = 2,
                ),
            )
        }
    }
}

@Preview(name = "DiagnosticSummaryCard - Error", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DiagnosticSummaryCardErrorPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            DiagnosticSummaryCard(
                state = DiagnosticUiState(
                    executionState = DiagnosticExecutionState.COMPLETED,
                    overallState = DiagnosticState.ERROR,
                    errorCount = 2,
                ),
            )
        }
    }
}

@Preview(name = "DiagnosticHeader - Idle", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DiagnosticHeaderIdlePreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            DiagnosticHeader(isRunning = false, onRunDiagnostics = {})
        }
    }
}

@Preview(name = "DiagnosticHeader - Running", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun DiagnosticHeaderRunningPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            DiagnosticHeader(isRunning = true, onRunDiagnostics = {})
        }
    }
}