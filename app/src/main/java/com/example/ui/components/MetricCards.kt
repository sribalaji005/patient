package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HeartRateBg
import com.example.ui.theme.HeartRatePink
import com.example.ui.theme.HeartRateRed
import com.example.ui.theme.Spo2Bg
import com.example.ui.theme.Spo2Cyan
import com.example.ui.theme.Spo2DeepCyan
import com.example.ui.theme.TempAmber
import com.example.ui.theme.TempBg
import com.example.ui.theme.TempOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HeartRateCard(
    heartRate: Int,
    recentHistory: List<Int>,
    modifier: Modifier = Modifier
) {
    // Pulse animation scaled by heart rate
    val pulseDuration = (60000 / heartRate.coerceIn(40, 180)).coerceIn(350, 1500)
    val infiniteTransition = rememberInfiniteTransition(label = "heartPulse")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDuration / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )

    val (statusText, statusColor) = when {
        heartRate < 60 -> "Bradycardia (Low)" to Color(0xFFFFB703)
        heartRate in 60..100 -> "Normal Resting" to Color(0xFF00E676)
        heartRate in 101..120 -> "Elevated Pulse" to Color(0xFFFF9800)
        else -> "Tachycardia (High)" to Color(0xFFFF1744)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, HeartRateRed.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .testTag("metric_card_heart_rate"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(HeartRateBg, Color.Transparent)
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(HeartRateRed.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Heart Rate",
                            tint = HeartRateRed,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(heartScale)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HEART RATE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$heartRate",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = HeartRatePink,
                        modifier = Modifier.testTag("text_heart_rate_value")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "BPM",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Mini Sparkline of beats
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(36.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (recentHistory.size >= 2) {
                            val w = size.width
                            val h = size.height
                            val minVal = (recentHistory.minOrNull() ?: 60).toFloat() - 5f
                            val maxVal = ((recentHistory.maxOrNull() ?: 100).toFloat() + 5f).coerceAtLeast(minVal + 10f)
                            val step = w / (recentHistory.size - 1)

                            val path = Path()
                            recentHistory.forEachIndexed { idx, bpm ->
                                val x = idx * step
                                val norm = (bpm - minVal) / (maxVal - minVal)
                                val y = h - (norm.coerceIn(0f, 1f) * h)
                                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = HeartRateRed,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Spo2Card(
    spo2: Int,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when {
        spo2 >= 95 -> "Optimal" to Color(0xFF00E676)
        spo2 in 90..94 -> "Moderate Hypoxia" to Color(0xFFFFB703)
        else -> "Critical Low" to Color(0xFFFF1744)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Spo2Cyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .testTag("metric_card_spo2"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Spo2Bg, Color.Transparent)
                    )
                )
                .padding(14.dp)
        ) {
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
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "SpO2 Oxygen",
                            tint = Spo2Cyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SPO2 OXYGEN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$spo2",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = Spo2Cyan,
                        modifier = Modifier.testTag("text_spo2_value")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Circular Saturation Indicator
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1.0f },
                        modifier = Modifier.fillMaxSize(),
                        color = Spo2Cyan.copy(alpha = 0.15f),
                        strokeWidth = 5.dp
                    )
                    CircularProgressIndicator(
                        progress = { (spo2 / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxSize(),
                        color = Spo2Cyan,
                        strokeWidth = 5.dp
                    )
                    Text(
                        text = "$spo2%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

enum class TempUnit {
    CELSIUS, FAHRENHEIT
}

@Composable
fun TemperatureCard(
    tempCelsius: Float,
    tempUnit: TempUnit,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayTemp = if (tempUnit == TempUnit.CELSIUS) {
        String.format("%.1f", tempCelsius)
    } else {
        val fahrenheit = (tempCelsius * 9f / 5f) + 32f
        String.format("%.1f", fahrenheit)
    }

    val unitSymbol = if (tempUnit == TempUnit.CELSIUS) "°C" else "°F"

    val (statusText, statusColor) = when {
        tempCelsius < 35.5f -> "Hypothermia" to Color(0xFF00B4D8)
        tempCelsius in 35.5f..37.5f -> "Normal Temp" to Color(0xFF00E676)
        tempCelsius in 37.6f..38.5f -> "Mild Fever" to Color(0xFFFFB703)
        else -> "High Fever Alert" to Color(0xFFFF1744)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, TempAmber.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .testTag("metric_card_temperature"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(TempBg, Color.Transparent)
                    )
                )
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(TempAmber.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeviceThermostat,
                            contentDescription = "Temperature",
                            tint = TempAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BODY TEMP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = displayTemp,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = TempAmber,
                        modifier = Modifier.testTag("text_temperature_value")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unitSymbol,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Interactive unit toggle pill (touch target >= 48dp)
                Surface(
                    onClick = onToggleUnit,
                    shape = RoundedCornerShape(16.dp),
                    color = TempOrange.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TempAmber.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("button_toggle_temp_unit")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (tempUnit == TempUnit.CELSIUS) "Switch to °F" else "Switch to °C",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TempAmber
                        )
                    }
                }
            }
        }
    }
}
