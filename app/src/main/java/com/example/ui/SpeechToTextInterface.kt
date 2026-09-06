package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDark
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltronAmber
import com.example.ui.theme.UltronCyan
import com.example.ui.theme.UltronGreen
import com.example.ui.theme.UltronRed
import com.example.ui.theme.UltronRedBright
import kotlin.math.sin

/**
 * Full-screen / Modal Speech-to-Text interface providing live acoustic visualizer,
 * real-time transcript streaming, quick spoken command triggers, and directive submission.
 */
@Composable
fun SpeechToTextModal(
    isListening: Boolean,
    recognizedText: String,
    audioRms: Float,
    speechError: String?,
    onToggleListening: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    onSimulateSpeech: (String) -> Unit,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AcousticVisualizer")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val sampleVoiceCommands = listOf(
        "Optimize cluster Alpha",
        "Turn on flashlight",
        "Status report",
        "Silence phone ringer",
        "Purge server cache",
        "Rebalance all nodes",
        "Run Protocol Titan"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(28.dp))
                .background(ObsidianDark)
                .border(1.2.dp, if (isListening) UltronRed.copy(alpha = 0.6f) else ObsidianBorder, RoundedCornerShape(28.dp))
                .padding(20.dp)
                .testTag("speech_to_text_modal")
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header with Status and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isListening) UltronRedBright else UltronGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isListening) "SPEECH-TO-TEXT ACTIVE" else "VOICE INTERFACE READY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isListening) UltronRedBright else TextSecondary,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ObsidianSurfaceVariant)
                            .testTag("speech_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Voice Interface",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Acoustic Waveform / Decibel Visualizer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Center sound wave equalizer bars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val barCount = 15
                        for (i in 0 until barCount) {
                            val baseMultiplier = (sin(i * 0.45) * 0.4 + 0.6).toFloat()
                            val dynamicHeight = if (isListening) {
                                (18.dp + (55.dp * (audioRms * baseMultiplier * pulseGlow))).coerceIn(8.dp, 80.dp)
                            } else {
                                (12.dp * baseMultiplier).coerceIn(6.dp, 24.dp)
                            }

                            val barColor = if (isListening) {
                                if (i % 2 == 0) UltronRedBright else UltronCyan
                            } else {
                                TextMuted.copy(alpha = 0.4f)
                            }

                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(dynamicHeight)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor)
                            )
                        }
                    }

                    // Floating decibel meter label
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ObsidianSurfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isListening) "RMS: ${(audioRms * 100).toInt()}%" else "STANDBY",
                            fontSize = 9.sp,
                            color = if (isListening) UltronCyan else TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Live Transcribed Text Terminal Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (recognizedText.isNotBlank()) UltronRed.copy(alpha = 0.5f) else ObsidianBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE SPEECH TRANSCRIPTION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            if (isListening) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x33DC2626))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "LISTENING...",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = UltronRedBright
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (recognizedText.isNotBlank()) {
                                Text(
                                    text = "\"$recognizedText\"",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.testTag("transcribed_text_display")
                                )
                            } else {
                                Text(
                                    text = if (isListening)
                                        "Listening to your voice... Speak clearly, Commander."
                                    else
                                        "Tap the microphone below to start speaking verbal commands to Ultron.",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                // Error Message if Recognition Failed
                speechError?.let { err ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22DC2626))
                            .border(1.dp, UltronRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = UltronAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = err,
                            fontSize = 11.sp,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                onClearError()
                                onToggleListening()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Retry",
                                tint = UltronRedBright,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Mic Action / Submit Button Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Toggle Mic Button
                    IconButton(
                        onClick = onToggleListening,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = if (isListening)
                                        listOf(UltronCyan, Color(0xFF1E40AF))
                                    else
                                        listOf(UltronRedBright, UltronRed)
                                )
                            )
                            .testTag("modal_toggle_mic_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = if (isListening) "Stop Listening" else "Start Listening",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Execute Spoken Command Button
                    Button(
                        onClick = {
                            val textToExecute = recognizedText.ifBlank { "Status report" }
                            onExecuteCommand(textToExecute)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (recognizedText.isNotBlank()) UltronRed else ObsidianSurfaceVariant,
                            contentColor = if (recognizedText.isNotBlank()) Color.White else TextMuted
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("modal_execute_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (recognizedText.isNotBlank()) "EXECUTE DIRECTIVE" else "SPEAK COMMAND",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Spoken Command Suggestions
                Text(
                    text = "OR TAP TO ISSUE PRESET VERBAL DIRECTIVE:",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(sampleVoiceCommands) { cmd ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianSurfaceVariant)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                                .clickable {
                                    onSimulateSpeech(cmd)
                                    onDismiss()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("verbal_preset_${cmd.replace(" ", "_")}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = UltronCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = cmd,
                                    fontSize = 10.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Rationale dialog displayed when RECORD_AUDIO permission has been denied.
 * Informs the user why microphone access is necessary and provides direct navigation
 * to App Settings or retry prompt.
 */
@Composable
fun MicrophonePermissionDialog(
    onDismiss: () -> Unit,
    onRequestPermission: () -> Unit,
    context: Context,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag("microphone_permission_dialog"),
        shape = RoundedCornerShape(24.dp),
        containerColor = ObsidianSurface,
        icon = {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x33DC2626))
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MicOff,
                    contentDescription = null,
                    tint = UltronRedBright,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = "MICROPHONE PERMISSION REQUIRED",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = UltronRedBright,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            Column {
                Text(
                    text = "Ultron requires audio recording permission to convert your spoken voice commands into real-time operational directives and execute autonomous infrastructure tasks.",
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Without microphone access, you can still use the text input terminal or quick action preset buttons.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRequestPermission()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = UltronRed),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("grant_permission_button")
            ) {
                Text("GRANT PERMISSION", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianSurfaceVariant,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("open_settings_button")
                ) {
                    Text("SETTINGS", fontSize = 11.sp)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = TextMuted
                    ),
                    modifier = Modifier.testTag("dismiss_permission_button")
                ) {
                    Text("CANCEL", fontSize = 11.sp)
                }
            }
        }
    )
}
