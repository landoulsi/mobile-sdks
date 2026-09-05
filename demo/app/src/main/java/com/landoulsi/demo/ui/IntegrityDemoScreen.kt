package com.landoulsi.demo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.landoulsi.design.DesignIcons
import com.landoulsi.design.components.BadgeTone
import com.landoulsi.design.components.DesignCard
import com.landoulsi.design.components.DesignChip
import com.landoulsi.design.components.DesignOutlinedCard
import com.landoulsi.design.components.StatusBadge
import com.landoulsi.integrity.IntegrityResult
import com.landoulsi.integrity.model.IntegrityCategory
import com.landoulsi.integrity.model.IntegrityMitigationAction
import com.landoulsi.integrity.model.IntegrityRiskScore
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.RiskLevel
import com.landoulsi.integrity.model.SignalSeverity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Shared color design tokens for integrity detection status and risk visualizations.
 */
object IntegrityColorTokens {
    val LowRisk = Color(0xFF10B981) // Emerald Green
    val MediumRisk = Color(0xFFF59E0B) // Amber
    val HighRisk = Color(0xFFF97316) // Orange
    val CriticalRisk = Color(0xFFEF4444) // Crimson Red
    val Info = Color(0xFF3B82F6) // Accent Blue

    fun forRiskLevel(riskLevel: RiskLevel): Color = when (riskLevel) {
        RiskLevel.LOW -> LowRisk
        RiskLevel.MEDIUM -> MediumRisk
        RiskLevel.HIGH -> HighRisk
        RiskLevel.CRITICAL -> CriticalRisk
    }

    fun forSeverity(severity: SignalSeverity): Color = when (severity) {
        SignalSeverity.INFO -> Info
        SignalSeverity.LOW -> LowRisk
        SignalSeverity.MEDIUM -> MediumRisk
        SignalSeverity.HIGH -> HighRisk
        SignalSeverity.CRITICAL -> CriticalRisk
    }
}

private fun RiskLevel.toBadgeTone(): BadgeTone = when (this) {
    RiskLevel.LOW -> BadgeTone.Success
    RiskLevel.MEDIUM -> BadgeTone.Tertiary
    RiskLevel.HIGH, RiskLevel.CRITICAL -> BadgeTone.Error
}

private fun IntegrityMitigationAction.toBadgeTone(): BadgeTone = when (this) {
    IntegrityMitigationAction.ALLOW -> BadgeTone.Success
    IntegrityMitigationAction.WARN -> BadgeTone.Tertiary
    IntegrityMitigationAction.CHALLENGE -> BadgeTone.Primary
    IntegrityMitigationAction.BLOCK -> BadgeTone.Error
}

private fun SignalSeverity.toBadgeTone(): BadgeTone = when (this) {
    SignalSeverity.CRITICAL, SignalSeverity.HIGH -> BadgeTone.Error
    SignalSeverity.MEDIUM -> BadgeTone.Tertiary
    SignalSeverity.LOW, SignalSeverity.INFO -> BadgeTone.Primary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrityDemoScreen(
    onBack: () -> Unit,
    viewModel: IntegrityShowcaseViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (uiState.scanResult == null) {
            viewModel.runSweep(context = context, scenario = IntegrityScenario.LIVE_DEVICE)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Device Integrity & Security",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Threat detection, risk scoring & runtime defense",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.runSweep(context = context) },
                        enabled = !uiState.isScanning,
                    ) {
                        if (uiState.isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Scan",
                            )
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Scenario Simulation Playground Selector
            item {
                ScenarioSelectorSection(
                    selectedScenario = uiState.selectedScenario,
                    isScanning = uiState.isScanning,
                    onScenarioSelected = { scenario ->
                        viewModel.selectScenario(scenario, context)
                    },
                )
            }

            // Real-time Speedometer Risk Gauge & Attribution
            item {
                RiskGaugeCard(
                    isScanning = uiState.isScanning,
                    riskScore = uiState.riskScore,
                    scanResult = uiState.scanResult,
                    selectedScenario = uiState.selectedScenario,
                    lastScanTimestamp = uiState.lastScanTimestamp,
                    onRunSweep = { viewModel.runSweep(context) },
                )
            }

            // Categorized Signal Breakdown Accordion
            item {
                SignalBreakdownHeader(
                    totalFired = uiState.scanResult?.fired?.size ?: 0,
                    onExpandAll = viewModel::expandAllCategories,
                    onCollapseAll = viewModel::collapseAllCategories,
                )
            }

            // Derived directly from IntegrityCategory.entries to prevent desynchronization
            val categoriesToDisplay = IntegrityCategory.entries

            items(categoriesToDisplay) { category ->
                val isExpanded = category in uiState.expandedCategories
                val firedInCategory = uiState.scanResult?.fired?.filter { it.category == category } ?: emptyList()
                val isCategoryActive = uiState.scanResult?.categories?.get(category) == true

                CategoryAccordionCard(
                    category = category,
                    isExpanded = isExpanded,
                    firedSignals = firedInCategory,
                    isCategoryActive = isCategoryActive,
                    onToggleExpand = { viewModel.toggleCategory(category) },
                    onInspectSignal = viewModel::selectSignalForInspection,
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Threat Inspection & Remediation Modal
    uiState.selectedSignal?.let { signal ->
        ThreatInspectionModal(
            signal = signal,
            onDismiss = { viewModel.selectSignalForInspection(null) },
        )
    }
}

/**
 * Horizontal Carousel / Filter Chips for threat simulation scenarios.
 */
@Composable
private fun ScenarioSelectorSection(
    selectedScenario: IntegrityScenario,
    isScanning: Boolean,
    onScenarioSelected: (IntegrityScenario) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Simulation Playground",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Tap to switch vectors",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            items(IntegrityScenario.entries) { scenario ->
                val isSelected = scenario == selectedScenario
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (!isScanning) {
                            onScenarioSelected(scenario)
                        }
                    },
                    label = {
                        Text(
                            text = scenario.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                )
            }
        }
    }
}

/**
 * Speedometer Risk Gauge with animated needle/arc and qualitative indicators.
 */
@Composable
private fun RiskGaugeCard(
    isScanning: Boolean,
    riskScore: IntegrityRiskScore?,
    scanResult: IntegrityResult?,
    selectedScenario: IntegrityScenario,
    lastScanTimestamp: Long,
    onRunSweep: () -> Unit,
) {
    val rawScore = (riskScore?.score ?: scanResult?.integrityScore?.toDouble() ?: 0.0).toFloat()
    val animatedScore by animateFloatAsState(
        targetValue = rawScore,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "RiskScoreAnimation",
    )

    val riskLevel = riskScore?.riskLevel ?: scanResult?.riskLevel ?: RiskLevel.LOW
    val mitigationAction = riskScore?.action ?: scanResult?.action ?: IntegrityMitigationAction.ALLOW
    val gaugeColor = IntegrityColorTokens.forRiskLevel(riskLevel)

    DesignCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Composite Integrity Risk",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = selectedScenario.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (lastScanTimestamp > 0L) {
                    val timeString = remember(lastScanTimestamp) {
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastScanTimestamp))
                    }
                    Text(
                        text = "Last: $timeString",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gauge Drawing Arc
            Box(
                modifier = Modifier.size(240.dp, 150.dp),
                contentAlignment = Alignment.Center,
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 16.dp.toPx()
                    val arcSize = size.width - strokeWidth
                    val arcTop = strokeWidth / 2
                    val arcLeft = strokeWidth / 2

                    // Background Track Arc (240 degrees from 150° to 390°)
                    drawArc(
                        color = trackColor,
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = Size(arcSize, arcSize),
                        topLeft = Offset(arcLeft, arcTop),
                    )

                    // Foreground Progress Arc
                    val progressFraction = (animatedScore / 100f).coerceIn(0f, 1f)
                    val sweep = 240f * progressFraction
                    if (sweep > 0.5f) {
                        drawArc(
                            color = gaugeColor,
                            startAngle = 150f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            size = Size(arcSize, arcSize),
                            topLeft = Offset(arcLeft, arcTop),
                        )
                    }
                }

                // Gauge Center Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(
                        text = "${animatedScore.roundToInt()}",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 44.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = gaugeColor,
                    )
                    Text(
                        text = "RISK SCORE (0-100)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Risk Level & Mitigation Action Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusBadge(
                    text = "LEVEL: ${riskLevel.name}",
                    tone = riskLevel.toBadgeTone(),
                )
                StatusBadge(
                    text = "ACTION: ${mitigationAction.name}",
                    tone = mitigationAction.toBadgeTone(),
                )
            }

            // Category Attribution breakdown pills
            val attribution = riskScore?.categoryAttribution ?: emptyMap()
            val contributingVectors = attribution.filterValues { it > 0.0 }
            if (contributingVectors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Attribution Breakdown:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    contributingVectors.forEach { (category, points) ->
                        DesignChip(
                            text = "${formatCategoryShort(category)}: +${points.roundToInt()} pts",
                            tone = BadgeTone.Error,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRunSweep,
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Executing Detection Sweep...")
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Sweep")
                }
            }
        }
    }
}

/**
 * Section header with Expand/Collapse buttons.
 */
@Composable
private fun SignalBreakdownHeader(
    totalFired: Int,
    onExpandAll: () -> Unit,
    onCollapseAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "Evaluated Threat Vectors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (totalFired > 0) "$totalFired threats active" else "All checks clean",
                style = MaterialTheme.typography.bodySmall,
                color = if (totalFired > 0) MaterialTheme.colorScheme.error else IntegrityColorTokens.LowRisk,
                fontWeight = FontWeight.Medium,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onExpandAll) {
                Text("Expand", style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = onCollapseAll) {
                Text("Collapse", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Accordion Card representing an individual IntegrityCategory.
 */
@Composable
private fun CategoryAccordionCard(
    category: IntegrityCategory,
    isExpanded: Boolean,
    firedSignals: List<IntegritySignal>,
    isCategoryActive: Boolean,
    onToggleExpand: () -> Unit,
    onInspectSignal: (IntegritySignal) -> Unit,
) {
    val isImplemented = IntegrityScenarioFixtures.isCategoryImplemented(category)
    val hasThreat = firedSignals.isNotEmpty() || isCategoryActive
    val headerTone = when {
        !isImplemented -> BadgeTone.Neutral
        hasThreat -> BadgeTone.Error
        else -> BadgeTone.Success
    }

    DesignOutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Accordion Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !isImplemented -> MaterialTheme.colorScheme.surfaceVariant
                                    hasThreat -> MaterialTheme.colorScheme.errorContainer
                                    else -> IntegrityColorTokens.LowRisk.copy(alpha = 0.15f)
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            !isImplemented -> {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            hasThreat -> {
                                Icon(
                                    imageVector = DesignIcons.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = DesignIcons.Check,
                                    contentDescription = null,
                                    tint = IntegrityColorTokens.LowRisk,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }

                    Column {
                        Text(
                            text = formatCategoryTitle(category),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = when {
                                !isImplemented -> "Evaluator not yet implemented (on roadmap)"
                                hasThreat -> "${firedSignals.size} active threat signal(s)"
                                else -> "No anomalies detected"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusBadge(
                        text = when {
                            !isImplemented -> "NOT IMPLEMENTED"
                            hasThreat -> "THREAT"
                            else -> "SECURE"
                        },
                        tone = headerTone,
                    )
                    Text(
                        text = if (isExpanded) "▲" else "▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Accordion Content Body
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(2.dp))

                    if (!isImplemented) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Detection for this threat vector is planned on the roadmap and is not yet active in this build.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else if (firedSignals.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = DesignIcons.Check,
                                contentDescription = null,
                                tint = IntegrityColorTokens.LowRisk,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "All evaluated checks in this category passed securely.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        firedSignals.forEach { signal ->
                            ThreatSignalItemRow(
                                signal = signal,
                                onInspect = { onInspectSignal(signal) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

/**
 * Individual threat signal row with severity badge and inspect button.
 */
@Composable
private fun ThreatSignalItemRow(
    signal: IntegritySignal,
    onInspect: () -> Unit,
) {
    val severityColor = IntegrityColorTokens.forSeverity(signal.severity)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInspect),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(severityColor),
                    )
                    Text(
                        text = signal.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = signal.details.ifEmpty { "Threat signal ID: ${signal.id}" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                DesignChip(
                    text = signal.severity.name,
                    tone = signal.severity.toBadgeTone(),
                )
                Text(
                    text = "Inspect →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * Threat Inspection & Remediation Modal UI (ModalBottomSheet).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreatInspectionModal(
    signal: IntegritySignal,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = signal.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Signal ID: ${signal.id}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            // Badges Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DesignChip(
                    text = "Severity: ${signal.severity.name}",
                    tone = signal.severity.toBadgeTone(),
                )
                DesignChip(
                    text = "Confidence: ${(signal.confidence * 100).roundToInt()}%",
                    tone = BadgeTone.Secondary,
                )
                DesignChip(
                    text = formatCategoryShort(signal.category),
                    tone = BadgeTone.Neutral,
                )
            }

            // Evidence & Diagnostic Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Diagnostic Evidence",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = signal.details.ifEmpty { "No detailed diagnostic payload recorded." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (signal.detectedAt > 0L) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val formattedTime = remember(signal.detectedAt) {
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(signal.detectedAt))
                        }
                        Text(
                            text = "Detected at: $formattedTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            // Metadata key-value table
            if (signal.metadata.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Telemetry Feature Metadata",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder(),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            signal.metadata.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = value,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Remediation Advice Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Developer Remediation Guidance",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = getRemediationAdvice(signal),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text("Close")
            }
        }
    }
}

private fun formatCategoryTitle(category: IntegrityCategory): String = when (category) {
    IntegrityCategory.ROOT_OR_JAILBREAK -> "Root & Jailbreak"
    IntegrityCategory.HOOKING_OR_TAMPERING -> "Hooking & Tampering (Frida / Xposed)"
    IntegrityCategory.MOCK_LOCATION -> "Mock Location & Spoofing"
    IntegrityCategory.NETWORK_ANOMALY -> "Network & Proxy Anomaly"
    IntegrityCategory.VIRTUAL_OS_OR_EMULATOR -> "Emulator & Virtual OS"
    IntegrityCategory.APP_CLONING -> "App Cloning & Dual Space"
    IntegrityCategory.DEBUGGER_ATTACHED -> "Debugger Attached"
    IntegrityCategory.UNTRUSTED_INSTALLER -> "Untrusted Installer"
}

private fun formatCategoryShort(category: IntegrityCategory): String = when (category) {
    IntegrityCategory.ROOT_OR_JAILBREAK -> "Root"
    IntegrityCategory.HOOKING_OR_TAMPERING -> "Hooking"
    IntegrityCategory.MOCK_LOCATION -> "Mock GPS"
    IntegrityCategory.NETWORK_ANOMALY -> "Network"
    IntegrityCategory.VIRTUAL_OS_OR_EMULATOR -> "Emulator"
    IntegrityCategory.APP_CLONING -> "Clone"
    IntegrityCategory.DEBUGGER_ATTACHED -> "Debugger"
    IntegrityCategory.UNTRUSTED_INSTALLER -> "Installer"
}

private fun getRemediationAdvice(signal: IntegritySignal): String = when (signal.category) {
    IntegrityCategory.ROOT_OR_JAILBREAK ->
        "Restricted operations advised. The device has superuser or root privileges enabled (e.g. su binary or Magisk). Sensitive cryptographic keys, secure enclave, or high-value transactions should be safeguarded with step-up authentication or halted."
    IntegrityCategory.HOOKING_OR_TAMPERING ->
        "Critical tampering detected. Dynamic instrumentation tools (Frida, Xposed, Substrate) can intercept API calls, manipulate return values, and bypass authentication. Terminate session immediately and ensure binary code obfuscation and native integrity verification are active."
    IntegrityCategory.MOCK_LOCATION ->
        "Location spoofing detected. Mock GPS provider, mock flag, or velocity anomalies detected. Reject geo-fenced approvals or enforce backend IP geolocation cross-verification."
    IntegrityCategory.NETWORK_ANOMALY ->
        "Suspicious network environment. Active VPN, system MITM HTTP proxy, or ADB debugging detected. Enforce strict TLS certificate pinning and restrict sensitive network traffic."
    IntegrityCategory.VIRTUAL_OS_OR_EMULATOR ->
        "Emulated hardware or sandbox container. Running inside QEMU, Android Emulator, or container space. Verify device attestation (Play Integrity API) before issuing sensitive authorization tokens."
    IntegrityCategory.APP_CLONING ->
        "Multi-instance or cloned app detected. UID or file directory isolation anomalies indicate the app is running in a dual-space container. Prevent token sharing across cloned instances."
    IntegrityCategory.DEBUGGER_ATTACHED ->
        "Debugger attached. Ptrace or JDWP debugger is attached. Disable debugging in production builds and detect ptrace tracing."
    IntegrityCategory.UNTRUSTED_INSTALLER ->
        "Untrusted installation source. App was sideloaded or installed from an unrecognized package installer."
}
