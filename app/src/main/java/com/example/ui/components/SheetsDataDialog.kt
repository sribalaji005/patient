package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.PatientRecord
import com.example.ui.theme.EcgNeonGreen
import com.example.ui.theme.FallAlertRed
import com.example.ui.theme.HealthNavyBorder
import com.example.ui.theme.HealthNavyDark
import com.example.ui.theme.HealthNavySurface
import com.example.ui.theme.HeartRatePink
import com.example.ui.theme.Spo2Cyan
import com.example.ui.theme.TempAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SheetsDataDialog(
    records: List<PatientRecord>,
    onImportCsv: (String) -> Unit,
    onClearLogs: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var importCsvInput by remember { mutableStateOf("") }

    val sampleCsvTemplate = remember {
        """
            Timestamp,Formatted Date,Patient Name,Patient ID,Heart Rate (BPM),SpO2 (%),Temperature (°C),Fall Detected,Fall Severity,ECG Rhythm,Source,Notes
            ${System.currentTimeMillis() - 7200000},"2026-09-22 09:30:00","John Doe","PT-8092",74,98,36.7,false,"NONE","Normal Sinus Rhythm","EXCEL_IMPORT","Morning routine check"
            ${System.currentTimeMillis() - 5400000},"2026-09-22 10:00:00","John Doe","PT-8092",82,97,36.9,false,"NONE","Normal Sinus Rhythm","EXCEL_IMPORT","Post-breakfast walk"
            ${System.currentTimeMillis() - 3600000},"2026-09-22 10:30:00","John Doe","PT-8092",110,94,37.2,true,"IMPACT","Tachycardia Trigger","EXCEL_IMPORT","Fall near bedside detected"
            ${System.currentTimeMillis() - 1800000},"2026-09-22 11:00:00","John Doe","PT-8092",78,98,36.8,false,"NONE","Normal Sinus Rhythm","EXCEL_IMPORT","Stabilized by nurse"
        """.trimIndent()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, HealthNavyBorder, RoundedCornerShape(24.dp))
                .testTag("dialog_sheets_sync"),
            color = HealthNavyDark,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(EcgNeonGreen.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TableChart,
                                contentDescription = null,
                                tint = EcgNeonGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sheets & Excel Sync",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${records.size} Telemetry Records Logged",
                                fontSize = 12.sp,
                                color = Spo2Cyan
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("button_close_sheets_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = HealthNavySurface,
                    contentColor = EcgNeonGreen,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Export to Sheets", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Import & Merge", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Logs (${records.size})", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> {
                        // EXPORT TO SHEETS
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = HealthNavySurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, HealthNavyBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Export Patient Health Log to Sheets / Excel",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Generates a clean CSV file with all IoT vitals (Heart rate, SpO2, Temp, ECG rhythm, and Fall detection events) formatted for Google Sheets and Excel.",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val csvData = buildCsvString(records)
                                                val sendIntent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_TEXT, csvData)
                                                    putExtra(Intent.EXTRA_SUBJECT, "Patient_Health_Monitor_IoT_Report.csv")
                                                    type = "text/csv"
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, "Open in Google Sheets / Excel / Share")
                                                context.startActivity(shareIntent)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = EcgNeonGreen,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("button_share_csv")
                                        ) {
                                            Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Share to Sheets / Excel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val csvData = buildCsvString(records)
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Patient Health CSV", csvData)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Spo2Cyan),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Spo2Cyan),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("button_copy_csv")
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copy CSV Text", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "CSV PREVIEW (FIRST 5 ROWS)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF070B10),
                                border = androidx.compose.foundation.BorderStroke(1.dp, HealthNavyBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                val previewText = buildCsvString(records.take(5))
                                Text(
                                    text = previewText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFA7F3D0),
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                    1 -> {
                        // IMPORT & MERGE EXCEL / CSV
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "PASTE CSV OR MERGE EXCEL FILE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = importCsvInput,
                                onValueChange = { importCsvInput = it },
                                placeholder = {
                                    Text(
                                        "Paste CSV from Google Sheets / Excel here...\nFormat: Timestamp, Date, Name, ID, HeartRate, SpO2, Temp, FallDetected, Severity, ECG, Source, Notes",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .testTag("input_csv_merge"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EcgNeonGreen,
                                    unfocusedBorderColor = HealthNavyBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = HealthNavySurface,
                                    unfocusedContainerColor = HealthNavySurface
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        importCsvInput = sampleCsvTemplate
                                        Toast.makeText(context, "Loaded Sample Patient Excel Data!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Spo2Cyan),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Spo2Cyan),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("button_load_sample_csv")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Load Sample Excel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (importCsvInput.isNotBlank()) {
                                            onImportCsv(importCsvInput)
                                            Toast.makeText(context, "Imported and merged patient records!", Toast.LENGTH_SHORT).show()
                                            selectedTab = 2 // Switch to logs view
                                        } else {
                                            Toast.makeText(context, "Please paste or load CSV first", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EcgNeonGreen,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("button_merge_csv")
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Merge to Database", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    2 -> {
                        // RECENT LOGS LIST
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RECORDED VITALS & FALL ALERTS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted
                                )

                                if (records.isNotEmpty()) {
                                    Surface(
                                        onClick = {
                                            onClearLogs()
                                            Toast.makeText(context, "All logs cleared", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = FallAlertRed.copy(alpha = 0.2f),
                                        modifier = Modifier.testTag("button_clear_logs")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = FallAlertRed, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Clear Logs", fontSize = 11.sp, color = FallAlertRed, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (records.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(HealthNavySurface, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No records stored yet. Live ESP32 logs or import Excel rows.", color = TextMuted, fontSize = 13.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(records, key = { it.id }) { record ->
                                        RecordRowItem(record)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordRowItem(record: PatientRecord) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val formattedTime = dateFormat.format(Date(record.timestamp))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = HealthNavySurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (record.fallDetected) FallAlertRed.copy(alpha = 0.7f) else HealthNavyBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formattedTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (record.fallDetected) FallAlertRed.copy(alpha = 0.2f) else EcgNeonGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (record.fallDetected) "FALL ALERT" else "NORMAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (record.fallDetected) FallAlertRed else EcgNeonGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = record.source,
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "HR: ${record.heartRate} BPM",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HeartRatePink
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SpO2: ${record.spo2}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Spo2Cyan
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${String.format("%.1f", record.temperature)}°C",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TempAmber
                    )
                }
            }

            if (record.notes.isNotBlank()) {
                Text(
                    text = record.notes,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

private fun buildCsvString(records: List<PatientRecord>): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val sb = StringBuilder()
    sb.append("Timestamp,Formatted Date,Patient Name,Patient ID,Heart Rate (BPM),SpO2 (%),Temperature (°C),Fall Detected,Fall Severity,ECG Rhythm,Source,Notes\n")
    records.forEach { r ->
        val dateStr = dateFormat.format(Date(r.timestamp))
        val cleanNotes = r.notes.replace("\"", "\"\"")
        val cleanRhythm = r.ecgRhythm.replace("\"", "\"\"")
        sb.append("${r.timestamp},\"$dateStr\",\"${r.patientName}\",\"${r.patientId}\",${r.heartRate},${r.spo2},${r.temperature},${r.fallDetected},\"${r.fallSeverity}\",\"$cleanRhythm\",\"${r.source}\",\"$cleanNotes\"\n")
    }
    return sb.toString()
}
