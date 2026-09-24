package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EcgGridColor
import com.example.ui.theme.EcgMonitorBg
import com.example.ui.theme.EcgNeonGreen

@Composable
fun EcgMonitorCanvas(
    samples: List<Float>,
    heartRate: Int,
    sweepIndex: Int,
    rhythmName: String = "Lead II • Sinus Rhythm",
    modifier: Modifier = Modifier
) {
    var isPaused by remember { mutableStateOf(false) }
    var gainMultiplier by remember { mutableFloatStateOf(1.0f) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF1E3A2B), RoundedCornerShape(16.dp))
            .testTag("ecg_monitor_card"),
        color = EcgMonitorBg,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (isPaused) Color(0xFFFFB703) else EcgNeonGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ECG LIVE MONITOR",
                        color = EcgNeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Gain adjustment chip
                    Surface(
                        onClick = {
                            gainMultiplier = if (gainMultiplier == 1.0f) 1.5f else if (gainMultiplier == 1.5f) 2.0f else 1.0f
                        },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0x3300E676),
                        modifier = Modifier.testTag("ecg_gain_button")
                    ) {
                        Text(
                            text = "${gainMultiplier}x GAIN",
                            color = EcgNeonGreen,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("ecg_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (isPaused) "Resume ECG" else "Pause ECG",
                            tint = EcgNeonGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF040A06))
                    .testTag("ecg_monitor_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val centerY = height * 0.58f

                    // 1. Draw Grid Lines (oscilloscope medical grid)
                    val gridSpacing = 20.dp.toPx()
                    var x = 0f
                    while (x < width) {
                        drawLine(
                            color = EcgGridColor,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = 0.8f
                        )
                        x += gridSpacing
                    }

                    var y = 0f
                    while (y < height) {
                        drawLine(
                            color = EcgGridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 0.8f
                        )
                        y += gridSpacing
                    }

                    // 2. Draw Center Baseline
                    drawLine(
                        color = Color(0x4400E676),
                        start = Offset(0f, centerY),
                        end = Offset(width, centerY),
                        strokeWidth = 1f
                    )

                    // 3. Draw ECG waveform if samples exist
                    if (samples.size > 1) {
                        val path = Path()
                        val stepX = width / (samples.size - 1)
                        val maxAmplitude = height * 0.42f * gainMultiplier

                        var firstPoint = true
                        for (i in samples.indices) {
                            val px = i * stepX
                            val py = centerY - (samples[i] * maxAmplitude)
                            if (firstPoint) {
                                path.moveTo(px, py.coerceIn(4f, height - 4f))
                                firstPoint = false
                            } else {
                                path.lineTo(px, py.coerceIn(4f, height - 4f))
                            }
                        }

                        // Glow trace
                        drawPath(
                            path = path,
                            color = Color(0x5500E676),
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Sharp line trace
                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF00B4D8),
                                    EcgNeonGreen,
                                    Color(0xFF69F0AE)
                                )
                            ),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // 4. Draw sweep cursor if actively sweeping
                        if (!isPaused && samples.isNotEmpty()) {
                            val cursorX = (sweepIndex % samples.size) * stepX
                            drawLine(
                                color = Color(0xFFFFFFFF),
                                start = Offset(cursorX, 0f),
                                end = Offset(cursorX, height),
                                strokeWidth = 2f
                            )
                            drawCircle(
                                color = Color(0xFF69F0AE),
                                radius = 4.dp.toPx(),
                                center = Offset(
                                    cursorX,
                                    centerY - (samples.getOrElse(sweepIndex % samples.size) { 0f } * maxAmplitude)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = EcgNeonGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = rhythmName,
                        color = Color(0xFF81C784),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = "25mm/s • 10mm/mV • $heartRate BPM",
                    color = Color(0xFF4CAF50),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
