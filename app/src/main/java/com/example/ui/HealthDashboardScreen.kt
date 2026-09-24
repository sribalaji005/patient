package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.EcgMonitorCanvas
import com.example.ui.components.Esp32ConnectDialog
import com.example.ui.components.FallAlertCard
import com.example.ui.components.HeartRateCard
import com.example.ui.components.SheetsDataDialog
import com.example.ui.components.Spo2Card
import com.example.ui.components.TemperatureCard
import com.example.ui.theme.EcgNeonGreen
import com.example.ui.theme.FallAlertRed
import com.example.ui.theme.HealthNavyBorder
import com.example.ui.theme.HealthNavyDark
import com.example.ui.theme.HealthNavySurface
import com.example.ui.theme.HeartRateRed
import com.example.ui.theme.Spo2Cyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDashboardScreen(
    viewModel: HealthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val recentHrs by viewModel.recentHeartRates.collectAsStateWithLifecycle()
    val ecgSamples by viewModel.ecgSamples.collectAsStateWithLifecycle()
    val sweepIndex by viewModel.sweepIndex.collectAsStateWithLifecycle()
    val isSimulating by viewModel.isSimulating.collectAsStateWithLifecycle()
    val isEsp32Connected by viewModel.isEsp32Connected.collectAsStateWithLifecycle()
    val esp32Url by viewModel.esp32Url.collectAsStateWithLifecycle()
    val pollingIntervalMs by viewModel.pollingIntervalMs.collectAsStateWithLifecycle()
    val lastPingStatus by viewModel.lastPingStatus.collectAsStateWithLifecycle()
    val tempUnit by viewModel.tempUnit.collectAsStateWithLifecycle()
    val lastFallTimestamp by viewModel.lastFallTimestamp.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val patientName by viewModel.patientName.collectAsStateWithLifecycle()
    val patientId by viewModel.patientId.collectAsStateWithLifecycle()

    var showEsp32Dialog by remember { mutableStateOf(false) }
    var showSheetsDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("health_dashboard_scaffold"),
        containerColor = HealthNavyDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(HeartRateRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Health Monitor",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    // Patient chip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = HealthNavySurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, HealthNavyBorder),
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .testTag("patient_info_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Spo2Cyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$patientName ($patientId)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                },
                actions = {
                    // Sheets / Excel sync button
                    IconButton(
                        onClick = { showSheetsDialog = true },
                        modifier = Modifier.testTag("action_sheets_sync")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Sheets / Excel Sync",
                            tint = EcgNeonGreen
                        )
                    }

                    // ESP32 IoT settings button
                    IconButton(
                        onClick = { showEsp32Dialog = true },
                        modifier = Modifier.testTag("action_esp32_setup")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "ESP32 Setup",
                            tint = Spo2Cyan
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = HealthNavyDark
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // IoT Wearable Hardware Status Banner
            Surface(
                onClick = { showEsp32Dialog = true },
                shape = RoundedCornerShape(12.dp),
                color = HealthNavySurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, HealthNavyBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("iot_status_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isSimulating || isEsp32Connected) EcgNeonGreen else FallAlertRed,
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isSimulating || isEsp32Connected) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = if (isSimulating || isEsp32Connected) EcgNeonGreen else FallAlertRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSimulating) "ESP32 Simulation (Active)" else if (isEsp32Connected) "ESP32 Node (Connected)" else "ESP32 Node (Offline)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = Spo2Cyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${telemetry.batteryLevel}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Spo2Cyan
                        )
                    }
                }
            }

            // 1. FALL DETECTION ALERT CARD (High Priority)
            FallAlertCard(
                fallDetected = telemetry.fallDetected,
                fallSeverity = telemetry.fallSeverity,
                lastFallTimestamp = lastFallTimestamp,
                onDismissAlert = { viewModel.dismissFallAlert() },
                onSimulateFall = { viewModel.simulateFallAlert() },
                onCallEmergency = {
                    val callIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:911")
                    }
                    try {
                        context.startActivity(callIntent)
                    } catch (_: Exception) {}
                }
            )

            // 2. BIOMETRIC METRICS GRID (Heart Rate & SpO2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeartRateCard(
                    heartRate = telemetry.heartRate,
                    recentHistory = recentHrs,
                    modifier = Modifier.weight(1f)
                )

                Spo2Card(
                    spo2 = telemetry.spo2,
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. BODY TEMPERATURE CARD
            TemperatureCard(
                tempCelsius = telemetry.temperature,
                tempUnit = tempUnit,
                onToggleUnit = { viewModel.toggleTempUnit() },
                modifier = Modifier.fillMaxWidth()
            )

            // 4. LIVE ECG OSCILLOSCOPE MONITOR
            EcgMonitorCanvas(
                samples = ecgSamples,
                heartRate = telemetry.heartRate,
                sweepIndex = sweepIndex,
                rhythmName = if (telemetry.fallDetected) "⚠️ Impact Disruption • Lead II" else "Lead II • Normal Sinus Rhythm"
            )

            // 5. SHEETS & EXCEL EXPORT SUMMARY CARD
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HealthNavySurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, HealthNavyBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_sheets_summary")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = EcgNeonGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Patient Logs & Sheets Sync",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${records.size} vitals recorded • Ready to merge or export to Excel/Sheets",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Surface(
                        onClick = { showSheetsDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = EcgNeonGreen.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EcgNeonGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("button_open_sheets_sync")
                    ) {
                        Text(
                            text = "Open Sheets",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcgNeonGreen,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ESP32 Config & Arduino Code Dialog
    if (showEsp32Dialog) {
        Esp32ConnectDialog(
            currentUrl = esp32Url,
            isConnected = isEsp32Connected,
            isSimulating = isSimulating,
            pollingIntervalMs = pollingIntervalMs,
            lastPingStatus = lastPingStatus,
            onSaveConfig = { url, interval, sim ->
                viewModel.setEsp32Config(url, interval, sim)
            },
            onTestPing = { url -> viewModel.testPing(url) },
            onDismiss = { showEsp32Dialog = false }
        )
    }

    // Google Sheets & Excel Export / Import Dialog
    if (showSheetsDialog) {
        SheetsDataDialog(
            records = records,
            onImportCsv = { csv -> viewModel.importCsvData(csv) },
            onClearLogs = { viewModel.clearAllLogs() },
            onDismiss = { showSheetsDialog = false }
        )
    }
}
