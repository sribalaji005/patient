package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.iot.Esp32Client
import com.example.ui.theme.EcgNeonGreen
import com.example.ui.theme.FallAlertRed
import com.example.ui.theme.HealthNavyBorder
import com.example.ui.theme.HealthNavyDark
import com.example.ui.theme.HealthNavySurface
import com.example.ui.theme.Spo2Cyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun Esp32ConnectDialog(
    currentUrl: String,
    isConnected: Boolean,
    isSimulating: Boolean,
    pollingIntervalMs: Long,
    lastPingStatus: String?,
    onSaveConfig: (url: String, intervalMs: Long, enableSimulation: Boolean) -> Unit,
    onTestPing: (url: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf(currentUrl) }
    var selectedInterval by remember { mutableStateOf(pollingIntervalMs) }
    var useSimulation by remember { mutableStateOf(isSimulating) }
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, HealthNavyBorder, RoundedCornerShape(24.dp))
                .testTag("dialog_esp32_connect"),
            color = HealthNavyDark,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                // Dialog Title Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Spo2Cyan.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = Spo2Cyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ESP32 IoT Node Setup",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (useSimulation) "Simulator Mode Active" else if (isConnected) "ESP32 Connected" else "Hardware Disconnected",
                                fontSize = 12.sp,
                                color = if (useSimulation) Spo2Cyan else if (isConnected) EcgNeonGreen else FallAlertRed
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("button_close_esp32_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs: Setup vs Arduino Code
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = HealthNavySurface,
                    contentColor = Spo2Cyan,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Connection", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("ESP32 Arduino Code", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == 0) {
                        // MODE SELECTOR: Simulation vs Real Hardware
                        Text(
                            text = "DATA SOURCE MODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                onClick = { useSimulation = false },
                                shape = RoundedCornerShape(12.dp),
                                color = if (!useSimulation) Spo2Cyan.copy(alpha = 0.2f) else HealthNavySurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (!useSimulation) Spo2Cyan else HealthNavyBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mode_real_esp32")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Sensors,
                                            contentDescription = null,
                                            tint = if (!useSimulation) Spo2Cyan else TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Real ESP32",
                                            fontWeight = FontWeight.Bold,
                                            color = if (!useSimulation) Spo2Cyan else TextPrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Wi-Fi / LAN IP stream",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Surface(
                                onClick = { useSimulation = true },
                                shape = RoundedCornerShape(12.dp),
                                color = if (useSimulation) Spo2Cyan.copy(alpha = 0.2f) else HealthNavySurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (useSimulation) Spo2Cyan else HealthNavyBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mode_simulator")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PlayCircle,
                                            contentDescription = null,
                                            tint = if (useSimulation) Spo2Cyan else TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Interactive Demo",
                                            fontWeight = FontWeight.Bold,
                                            color = if (useSimulation) Spo2Cyan else TextPrimary,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Simulated biometrics",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // IP Address / URL Field
                        Text(
                            text = "ESP32 ENDPOINT URL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_esp32_url"),
                            placeholder = { Text("http://192.168.4.1/data", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Spo2Cyan,
                                unfocusedBorderColor = HealthNavyBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = HealthNavySurface,
                                unfocusedContainerColor = HealthNavySurface
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { urlInput = "http://192.168.4.1/data" },
                                shape = RoundedCornerShape(8.dp),
                                color = HealthNavySurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, HealthNavyBorder)
                            ) {
                                Text(
                                    text = "SoftAP: 192.168.4.1",
                                    fontSize = 10.sp,
                                    color = Spo2Cyan,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }

                            Surface(
                                onClick = { urlInput = "http://192.168.1.150:80/telemetry" },
                                shape = RoundedCornerShape(8.dp),
                                color = HealthNavySurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, HealthNavyBorder)
                            ) {
                                Text(
                                    text = "LAN: 192.168.1.X",
                                    fontSize = 10.sp,
                                    color = Spo2Cyan,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Polling Rate
                        Text(
                            text = "POLLING INTERVAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(500L to "0.5s", 1000L to "1.0s", 2000L to "2.0s", 5000L to "5.0s").forEach { (ms, label) ->
                                Surface(
                                    onClick = { selectedInterval = ms },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selectedInterval == ms) Spo2Cyan.copy(alpha = 0.2f) else HealthNavySurface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (selectedInterval == ms) Spo2Cyan else HealthNavyBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedInterval == ms) Spo2Cyan else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        if (lastPingStatus != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isConnected) EcgNeonGreen.copy(alpha = 0.15f) else FallAlertRed.copy(alpha = 0.15f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = lastPingStatus,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isConnected) EcgNeonGreen else FallAlertRed,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    } else {
                        // Tab 1: Arduino ESP32 C++ Code
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ARDUINO / ESP32 SKETCH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )

                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("ESP32 Health Sketch", Esp32Client.getSampleArduinoCode())
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Arduino Code Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Spo2Cyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Code", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF070B10),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HealthNavyBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = Esp32Client.getSampleArduinoCode(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF7DD3FC),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Dialog Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (selectedTab == 0 && !useSimulation) {
                        Button(
                            onClick = { onTestPing(urlInput) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HealthNavySurface,
                                contentColor = Spo2Cyan
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("button_ping_esp32")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Ping", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Button(
                        onClick = {
                            onSaveConfig(urlInput, selectedInterval, useSimulation)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Spo2Cyan,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("button_save_esp32_config")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Apply", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
