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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.landoulsi.integrity.IntegrityManager
import com.landoulsi.integrity.IntegrityResult
import com.landoulsi.integrity.SignalEvaluator
import com.landoulsi.integrity.emulator.AndroidEmulatorCheckContext
import com.landoulsi.integrity.emulator.EmulatorDetectionEvaluator
import com.landoulsi.integrity.hooking.frida.AndroidFridaCheckContext
import com.landoulsi.integrity.hooking.frida.FridaCheckContext
import com.landoulsi.integrity.hooking.frida.FridaDetectionEvaluator
import com.landoulsi.integrity.hooking.substrate.AndroidSubstrateCheckContext
import com.landoulsi.integrity.hooking.substrate.SubstrateCheckContext
import com.landoulsi.integrity.hooking.substrate.SubstrateDetectionEvaluator
import com.landoulsi.integrity.hooking.xposed.AndroidXposedCheckContext
import com.landoulsi.integrity.hooking.xposed.XposedCheckContext
import com.landoulsi.integrity.hooking.xposed.XposedDetectionEvaluator
import com.landoulsi.integrity.mocklocation.AndroidMockLocationCheckContext
import com.landoulsi.integrity.mocklocation.LocationSample
import com.landoulsi.integrity.mocklocation.MockLocationDetectionEvaluator
import com.landoulsi.integrity.model.IntegritySignal
import com.landoulsi.integrity.model.RiskLevel
import com.landoulsi.integrity.root.AndroidRootCheckContext
import com.landoulsi.integrity.root.RootDetectionEvaluator
import com.landoulsi.integrity.virtualos.AndroidVirtualOsCheckContext
import com.landoulsi.integrity.virtualos.VirtualOsDetectionEvaluator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrityDemoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var scanResult by remember { mutableStateOf<IntegrityResult?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var simulatedScenario by remember { mutableStateOf("Live Device Scan") }

    fun runScan(
        customEvaluators: List<SignalEvaluator>? = null,
        scenarioName: String = "Live Device Scan",
    ) {
        coroutineScope.launch {
            isScanning = true
            simulatedScenario = scenarioName

            val evaluators = customEvaluators ?: listOf(
                FridaDetectionEvaluator(AndroidFridaCheckContext(context)),
                XposedDetectionEvaluator(AndroidXposedCheckContext(context)),
                SubstrateDetectionEvaluator(AndroidSubstrateCheckContext(context)),
                MockLocationDetectionEvaluator(AndroidMockLocationCheckContext(context)),
                RootDetectionEvaluator(AndroidRootCheckContext(context)),
                EmulatorDetectionEvaluator(AndroidEmulatorCheckContext(context)),
                VirtualOsDetectionEvaluator(AndroidVirtualOsCheckContext(context)),
            )

            val manager = IntegrityManager.from(evaluators)
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
                title = { Text("Device Integrity & Tampering") },
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
                text = "Detection Sweeps & Hooking Scenarios",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { runScan(null, "Live Device Scan") },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Live Scan")
                }
                Button(
                    onClick = {
                        val fakeFridaContext = object : FridaCheckContext {
                            override fun fileExists(path: String): Boolean = path == "/data/local/tmp/frida-server"
                            override fun readFileLines(path: String): List<String> =
                                if (path == "/proc/self/maps") listOf("7f8a0000-7f8b0000 r-xp 00000000 08:01 12345 /data/local/tmp/libfrida-gadget.so") else emptyList()
                            override fun isPortOpen(port: Int): Boolean = port == 27042
                            override fun isProcessRunning(processName: String): Boolean = processName == "frida-server"
                            override fun isPackageInstalled(packageName: String): Boolean = false
                        }
                        runScan(listOf(FridaDetectionEvaluator(fakeFridaContext)), "Simulated Frida Hooking")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Frida Hook")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        val fakeXposedContext = object : XposedCheckContext {
                            override fun fileExists(path: String): Boolean = path == "/system/framework/XposedBridge.jar"
                            override fun readFileLines(path: String): List<String> = emptyList()
                            override fun isPackageInstalled(packageName: String): Boolean = packageName == "org.lsposed.manager"
                            override fun isClassLoadable(className: String): Boolean = className == "de.robv.android.xposed.XposedBridge"
                        }
                        runScan(listOf(XposedDetectionEvaluator(fakeXposedContext)), "Simulated Xposed Framework")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Xposed Hook")
                }
                Button(
                    onClick = {
                        val fakeSubstrateContext = object : SubstrateCheckContext {
                            override fun fileExists(path: String): Boolean = path == "/Library/MobileSubstrate/MobileSubstrate.dylib"
                            override fun directoryContents(path: String): List<String> =
                                if (path == "/Library/MobileSubstrate/DynamicLibraries") listOf("Tweak1.dylib", "Tweak2.dylib") else emptyList()
                        }
                        runScan(listOf(SubstrateDetectionEvaluator(fakeSubstrateContext)), "Simulated Substrate Dylib")
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Substrate Hook")
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
                                text = "Hooking / Tampering: ${result.isHooked} (Frida: ${result.hasFrida}, Xposed: ${result.hasXposed}, Substrate: ${result.hasSubstrate})",
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
                                    text = "No hooking, tampering, or spoofing signals detected. Environment clean.",
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
