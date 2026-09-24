package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CrisisAlert
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FallAlertRed
import com.example.ui.theme.FallAlertRedBg
import com.example.ui.theme.FallSafeBg
import com.example.ui.theme.FallSafeGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FallAlertCard(
    fallDetected: Boolean,
    fallSeverity: String,
    lastFallTimestamp: Long?,
    onDismissAlert: () -> Unit,
    onSimulateFall: () -> Unit,
    onCallEmergency: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (fallDetected) {
        // High Alert State
        val infiniteTransition = rememberInfiniteTransition(label = "fallAlertFlash")
        val alertPulse by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alertPulse"
        )

        val formattedTime = lastFallTimestamp?.let {
            SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it))
        } ?: "Just now"

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .scale(alertPulse)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, FallAlertRed, RoundedCornerShape(16.dp))
                .testTag("fall_alert_card_active"),
            color = Color(0xFF2B0A11),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(FallAlertRedBg, Color(0xFF1F050A))
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(FallAlertRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Emergency Alert",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "EMERGENCY: FALL DETECTED!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = FallAlertRed,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Triggered at $formattedTime • Wearable IMU",
                                fontSize = 12.sp,
                                color = Color(0xFFFF8A9E)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = FallAlertRed
                    ) {
                        Text(
                            text = fallSeverity.ifBlank { "IMPACT" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "A sudden impact or rapid downward acceleration was detected on the patient's wearable device. Check on patient immediately.",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCallEmergency,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = FallAlertRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("button_call_emergency")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call Nurse / SOS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onDismissAlert,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF8A9E)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("button_dismiss_fall")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFFF8A9E)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dismiss Alert", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    } else {
        // Normal Safe Guard State
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, FallSafeGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .testTag("fall_alert_card_safe"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(FallSafeBg, Color.Transparent)
                        )
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(FallSafeGreen.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Fall Guard Safe",
                            tint = FallSafeGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FALL GUARD: ACTIVE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = FallSafeGreen,
                                letterSpacing = 0.6.sp
                            )
                        }
                        Text(
                            text = "Wearable motion normal • Zero impact detected",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Quick Fall test button
                Surface(
                    onClick = onSimulateFall,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x22FF1744),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FallAlertRed.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("button_simulate_fall")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CrisisAlert,
                            contentDescription = null,
                            tint = FallAlertRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Test Alert",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = FallAlertRed
                        )
                    }
                }
            }
        }
    }
}
