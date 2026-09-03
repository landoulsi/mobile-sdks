package com.landoulsi.diagnostic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.landoulsi.design.Spacing
import com.landoulsi.design.components.BadgeTone
import com.landoulsi.design.components.ButtonTone
import com.landoulsi.design.components.DesignButton
import com.landoulsi.design.components.DesignCard
import com.landoulsi.design.components.DesignStatusIcon
import com.landoulsi.design.components.StatusBadge
import com.landoulsi.design.components.StatusIconVariant
import com.landoulsi.diagnostic.DiagnosticEngine
import com.landoulsi.diagnostic.DiagnosticExecutionState
import com.landoulsi.diagnostic.DiagnosticResult
import com.landoulsi.diagnostic.DiagnosticState
import com.landoulsi.diagnostic.DiagnosticUiState
import kotlinx.coroutines.launch

private val SummaryStatusIconSize = 32.dp
private val ResultStatusIconSize = 28.dp

internal fun DiagnosticState.displayLabel(): String = when (this) {
    DiagnosticState.PASS -> "Pass"
    DiagnosticState.WARNING -> "Warning"
    DiagnosticState.ERROR -> "Error"
}

internal fun formatSummaryText(state: DiagnosticUiState): String = when (state.overallState) {
    DiagnosticState.PASS -> "All systems operational"
    DiagnosticState.WARNING -> if (state.warningCount == 1) "1 warning detected" else "${state.warningCount} warnings detected"
    DiagnosticState.ERROR -> if (state.errorCount == 1) "1 error detected" else "${state.errorCount} errors detected"
}

internal fun DiagnosticState.badgeTone(): BadgeTone = when (this) {
    DiagnosticState.PASS -> BadgeTone.Success
    DiagnosticState.WARNING -> BadgeTone.Tertiary
    DiagnosticState.ERROR -> BadgeTone.Error
}

internal fun DiagnosticState.statusIconVariant(): StatusIconVariant = when (this) {
    DiagnosticState.PASS -> StatusIconVariant.Success
    DiagnosticState.WARNING -> StatusIconVariant.Warning
    DiagnosticState.ERROR -> StatusIconVariant.Error
}

/**
 * Stateful Diagnostic View observing state from a [DiagnosticEngine].
 *
 * Uses [collectAsState] rather than Android-only lifecycle-aware collectors to maintain
 * full Kotlin Multiplatform (KMP) compatibility across Android, iOS, and desktop targets.
 */
@Composable
fun DiagnosticView(
    engine: DiagnosticEngine,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.md),
) {
    val uiState by engine.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    DiagnosticContent(
        state = uiState,
        onRunDiagnostics = {
            scope.launch {
                engine.runDiagnostics()
            }
        },
        modifier = modifier,
        contentPadding = contentPadding,
    )
}

/**
 * Stateless Diagnostic Content layout.
 */
@Composable
internal fun DiagnosticContent(
    state: DiagnosticUiState,
    onRunDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.md),
) {
    val isRunning = state.executionState == DiagnosticExecutionState.RUNNING

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        // Top action bar / Header
        DiagnosticHeader(
            isRunning = isRunning,
            onRunDiagnostics = onRunDiagnostics,
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        // Aggregate health summary indicator
        if (state.executionState == DiagnosticExecutionState.COMPLETED) {
            DiagnosticSummaryCard(state = state)
            Spacer(modifier = Modifier.height(Spacing.md))
        }

        // List of diagnostic results
        if (state.results.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(
                    items = state.results,
                    key = { it.id },
                ) { result ->
                    DiagnosticResultItem(result = result)
                }
            }
        }
    }
}

/**
 * Header with "Run Diagnostics" button and progress indicator.
 */
@Composable
internal fun DiagnosticHeader(
    isRunning: Boolean,
    onRunDiagnostics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DesignButton(
        text = if (isRunning) "Running Diagnostics..." else "Run Diagnostics",
        onClick = onRunDiagnostics,
        enabled = !isRunning,
        loading = isRunning,
        tone = ButtonTone.Primary,
        modifier = modifier.fillMaxWidth(),
    )
}

/**
 * Summary badge & card representing aggregate health state.
 */
@Composable
internal fun DiagnosticSummaryCard(
    state: DiagnosticUiState,
    modifier: Modifier = Modifier,
) {
    val summaryText = formatSummaryText(state)
    val badgeTone = state.overallState.badgeTone()
    val iconVariant = state.overallState.statusIconVariant()

    DesignCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            DesignStatusIcon(
                variant = iconVariant,
                size = SummaryStatusIconSize,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "System Health",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            StatusBadge(
                text = state.overallState.displayLabel(),
                tone = badgeTone,
            )
        }
    }
}

/**
 * Diagnostic result card displaying check title, status badge/icon (Pass, Warning, Error), and cause explanation.
 */
@Composable
internal fun DiagnosticResultItem(
    result: DiagnosticResult,
    modifier: Modifier = Modifier,
) {
    val badgeTone = result.state.badgeTone()
    val iconVariant = result.state.statusIconVariant()

    DesignCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            DesignStatusIcon(
                variant = iconVariant,
                size = ResultStatusIconSize,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (!result.cause.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = result.cause,
                        style = MaterialTheme.typography.bodySmall,
                        color = when (result.state) {
                            DiagnosticState.ERROR -> MaterialTheme.colorScheme.error
                            DiagnosticState.WARNING -> MaterialTheme.colorScheme.tertiary
                            DiagnosticState.PASS -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }

            StatusBadge(
                text = result.state.displayLabel(),
                tone = badgeTone,
            )
        }
    }
}
