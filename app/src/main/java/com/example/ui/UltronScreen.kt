package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.CustomCommandEntity
import com.example.infrastructure.ClusterStatus
import com.example.infrastructure.ServerCluster
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
import com.example.ui.theme.UltronRedGlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UltronScreen(
    viewModel: UltronViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isListening by viewModel.voiceManager.isListening.collectAsState()
    val isSpeaking by viewModel.voiceManager.isSpeaking.collectAsState()
    val audioRms by viewModel.voiceManager.audioRms.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val systemNotification by viewModel.systemNotification.collectAsState()
    val recognizedText by viewModel.voiceManager.recognizedText.collectAsState()
    val speechError by viewModel.voiceManager.speechError.collectAsState()

    var showSpeechModal by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(systemNotification) {
        systemNotification?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissNotification()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleVoiceListening()
            showSpeechModal = true
        } else {
            showPermissionRationaleDialog = true
        }
    }

    fun handleMicClick() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (!isListening) {
                viewModel.toggleVoiceListening()
            }
            showSpeechModal = true
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var showAddCommandDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            UltronTacticalHeader(
                onQuickOptimize = { viewModel.manualOptimizeAll() },
                isAiThinking = isAiThinking
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ObsidianDark)
        ) {
            // Navigation Tabs
            val tabs = listOf(
                "CONSOLE" to Icons.Default.Speed,
                "SERVERS" to Icons.Default.Dns,
                "AUTOMATION" to Icons.Default.Smartphone,
                "MACROS" to Icons.Default.Build
            )

            // Sophisticated Dark Navigation Pill Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(ObsidianSurface)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(26.dp))
            ) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = UltronRed,
                    indicator = {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(UltronRed)
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, pair ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { viewModel.selectTab(index) },
                            text = {
                                Text(
                                    text = pair.first,
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == index) UltronRedBright else TextSecondary,
                                    letterSpacing = 0.8.sp
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = pair.second,
                                    contentDescription = pair.first,
                                    modifier = Modifier.size(17.dp),
                                    tint = if (selectedTab == index) UltronRedBright else TextMuted
                                )
                            },
                            modifier = Modifier.testTag("tab_$index")
                        )
                    }
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> ConsoleTab(
                        viewModel = viewModel,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        audioRms = audioRms,
                        isAiThinking = isAiThinking,
                        recognizedText = recognizedText,
                        onMicClick = { handleMicClick() },
                        onOpenSpeechModal = { showSpeechModal = true }
                    )
                    1 -> InfrastructureTab(
                        viewModel = viewModel
                    )
                    2 -> PhoneAutomationTab(
                        viewModel = viewModel
                    )
                    3 -> CustomCommandsTab(
                        viewModel = viewModel,
                        onOpenAddDialog = { showAddCommandDialog = true }
                    )
                }
            }
        }
    }

    if (showAddCommandDialog) {
        AddMacroDialog(
            onDismiss = { showAddCommandDialog = false },
            onConfirm = { trigger, desc, action ->
                viewModel.addCustomCommand(trigger, desc, action)
                showAddCommandDialog = false
            }
        )
    }

    if (showSpeechModal) {
        SpeechToTextModal(
            isListening = isListening,
            recognizedText = recognizedText,
            audioRms = audioRms,
            speechError = speechError,
            onToggleListening = { viewModel.toggleVoiceListening() },
            onExecuteCommand = { cmd ->
                viewModel.executeCommand(cmd, isVoice = true)
            },
            onSimulateSpeech = { preset ->
                viewModel.simulateVoiceCommand(preset)
            },
            onClearError = { viewModel.clearSpeechError() },
            onDismiss = {
                if (isListening) {
                    viewModel.toggleVoiceListening()
                }
                showSpeechModal = false
            }
        )
    }

    if (showPermissionRationaleDialog) {
        MicrophonePermissionDialog(
            context = context,
            onDismiss = { showPermissionRationaleDialog = false },
            onRequestPermission = {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        )
    }
}

// -------------------------------------------------------------
// TOP TACTICAL HUD HEADER
// -------------------------------------------------------------
@Composable
fun UltronTacticalHeader(
    onQuickOptimize: () -> Unit,
    isAiThinking: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(ObsidianDark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Branding: "System Core v2.0" + "ULTRON A.I."
        Column {
            Text(
                text = "SYSTEM CORE v2.0",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = UltronRedBright,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ULTRON ",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = TextPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "A.I.",
                    fontSize = 24.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal,
                    color = com.example.ui.theme.UltronRedLight
                )
            }
        }

        // Active Status Pill + Quick Optimize Action
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "ACTIVE" rounded pill with red glowing dot
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianSurfaceVariant)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isAiThinking) UltronCyan else UltronRed)
                    )
                    Text(
                        text = if (isAiThinking) "SYNCING" else "ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Quick Re-Route / Optimize Button
            Button(
                onClick = onQuickOptimize,
                colors = ButtonDefaults.buttonColors(
                    containerColor = UltronRed,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("quick_optimize_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "RE-ROUTE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 0: NEURAL CONSOLE & VOICE ASSISTANT
// -------------------------------------------------------------
@Composable
fun ConsoleTab(
    viewModel: UltronViewModel,
    isListening: Boolean,
    isSpeaking: Boolean,
    audioRms: Float,
    isAiThinking: Boolean,
    recognizedText: String,
    onMicClick: () -> Unit,
    onOpenSpeechModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.terminalMessages.collectAsState()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Voice Reactor HUD Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Radial red aura background glow
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    com.example.ui.theme.UltronRed.copy(alpha = 0.18f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VoiceReactorVisualizer(
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        audioRms = audioRms,
                        onClick = onMicClick,
                        modifier = Modifier.testTag("voice_reactor")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = when {
                            isListening -> "\"Listening to neural audio feed...\""
                            isSpeaking -> "\"Ultron vocal synthesis broadcasting\""
                            isAiThinking -> "\"Synthesizing cluster directives\""
                            else -> "\"Redistributing bandwidth across server nodes\""
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when {
                            isListening -> "VOICE STREAM ACTIVE"
                            isSpeaking -> "VOCALIZING RESPONSE"
                            isAiThinking -> "OPTIMIZATION IN PROGRESS"
                            else -> "SYSTEM CORE OPERATIONAL"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = UltronRedBright.copy(alpha = 0.85f),
                        letterSpacing = 2.5.sp
                    )

                    // Live Speech-to-Text Transcription Banner
                    AnimatedVisibility(visible = isListening || recognizedText.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.94f)
                                .padding(top = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(ObsidianSurfaceVariant)
                                    .border(
                                        1.dp,
                                        if (recognizedText.isNotBlank()) UltronRed else ObsidianBorder,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onOpenSpeechModal() }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("live_speech_banner")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isListening) "LIVE TRANSCRIPTION STREAM" else "SPOKEN DIRECTIVE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isListening) UltronRedBright else TextMuted,
                                            letterSpacing = 0.8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (recognizedText.isNotBlank()) "\"$recognizedText\"" else "Speak clearly, Commander...",
                                            fontSize = 12.sp,
                                            color = if (recognizedText.isNotBlank()) TextPrimary else TextMuted,
                                            fontWeight = if (recognizedText.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    if (recognizedText.isNotBlank()) {
                                        Button(
                                            onClick = { viewModel.commitVoiceInput() },
                                            colors = ButtonDefaults.buttonColors(containerColor = UltronRed),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .height(28.dp)
                                                .testTag("execute_banner_speech_button")
                                        ) {
                                            Text("EXECUTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    } else {
                                        Button(
                                            onClick = onOpenSpeechModal,
                                            colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurface),
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(0.8.dp, ObsidianBorder),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("EXPAND HUD", fontSize = 9.sp, color = UltronCyan)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Telemetry Mini-Cards (CPU Load & Traffic)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // CPU Load Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = com.example.ui.theme.UltronRedLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "78%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("CPU Load", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { 0.78f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = UltronRed,
                        trackColor = Color(0x14FFFFFF)
                    )
                }
            }

            // Traffic Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = UltronCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "1.2 GB/s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Traffic", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { 0.62f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = UltronCyan,
                        trackColor = Color(0x14FFFFFF)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Command Preset Chips
        val quickPresets = listOf(
            "Optimize Clusters",
            "System Health Check",
            "Toggle Torch",
            "Stealth Mute",
            "Clear Device Cache",
            "Overclock Nodes"
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickPresets) { preset ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianSurfaceVariant)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
                        .clickable { viewModel.executeCommand(preset, isVoice = false) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("chip_${preset.replace(" ", "_")}")
                ) {
                    Text(
                        text = preset,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Terminal Messages Feed
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    TerminalMessageItem(message = msg)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Command Prompt Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mic Voice Button
            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isListening) UltronCyan else UltronRed)
                    .testTag("mic_input_button")
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Microphone",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text("Type command or directive...", fontSize = 12.sp, color = TextMuted)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface,
                    focusedBorderColor = UltronRed,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.executeCommand(inputText, isVoice = false)
                            inputText = ""
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.executeCommand(inputText, isVoice = false)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(UltronRed)
                    .testTag("send_command_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun TerminalMessageItem(message: TerminalMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (message.isUltron) Color(0xFF161014) else ObsidianSurfaceVariant)
            .border(
                1.dp,
                if (message.isUltron) UltronRedGlow else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (message.isUltron) "ULTRON" else "COMMANDER",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (message.isUltron) UltronRedBright else UltronCyan,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )

            message.actionBadge?.let { badge ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(UltronRed.copy(alpha = 0.2f))
                        .border(0.8.dp, UltronRed, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = UltronRedBright,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = message.content,
            fontSize = 13.sp,
            color = TextPrimary,
            lineHeight = 18.sp
        )
    }
}

// -------------------------------------------------------------
// TAB 1: INFRASTRUCTURE & REAL-TIME SERVER OPTIMIZATION
// -------------------------------------------------------------
@Composable
fun InfrastructureTab(
    viewModel: UltronViewModel,
    modifier: Modifier = Modifier
) {
    val clusters by viewModel.infraManager.clusters.collectAsState()
    val stats by viewModel.infraManager.globalStats.collectAsState()

    var selectedClusterForAction by remember { mutableStateOf<ServerCluster?>(null) }
    var subTabMode by remember { mutableStateOf(0) } // 0: Recharts Dashboard, 1: Cluster Nodes

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Sub-navigation Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianSurface)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                .padding(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (subTabMode == 0) UltronRed else Color.Transparent)
                    .clickable { subTabMode = 0 }
                    .padding(vertical = 6.dp)
                    .testTag("subtab_recharts_dashboard"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "RECHARTS DASHBOARD",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (subTabMode == 0) Color.White else TextSecondary,
                    letterSpacing = 0.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (subTabMode == 1) UltronRed else Color.Transparent)
                    .clickable { subTabMode = 1 }
                    .padding(vertical = 6.dp)
                    .testTag("subtab_cluster_nodes"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CLUSTER TOPOLOGY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (subTabMode == 1) Color.White else TextSecondary,
                    letterSpacing = 0.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (subTabMode == 0) {
            ServerResourceDashboard(viewModel = viewModel)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Global Cluster Stats Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatsCard(
                        title = "HEALTH SCORE",
                        value = "${stats.overallHealthScore}%",
                        subtitle = "${stats.activeAlerts} Alerts",
                        accentColor = if (stats.overallHealthScore > 80) UltronGreen else UltronAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatsCard(
                        title = "TOTAL NODES",
                        value = "${stats.totalNodes}",
                        subtitle = "${stats.totalPods} Active Pods",
                        accentColor = UltronCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatsCard(
                        title = "BANDWIDTH",
                        value = "${stats.totalBandwidthGbps.toInt()} Gbps",
                        subtitle = "${stats.avgLatencyMs}ms Latency",
                        accentColor = UltronRedBright,
                        modifier = Modifier.weight(1f)
                    )
                }

        Spacer(modifier = Modifier.height(10.dp))

        // Master Rebalance Bar
        Button(
            onClick = { viewModel.manualOptimizeAll() },
            colors = ButtonDefaults.buttonColors(containerColor = UltronRed),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("master_rebalance_button")
        ) {
            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "EXECUTE AUTONOMOUS REBALANCE PROTOCOL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Firewall Shield Status Card (from Sophisticated Dark Theme)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x337F1D1D))
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = UltronRedBright,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Firewall Shield",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "3 suspicious attempts blocked",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1A22C55E))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "STABLE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = UltronGreen,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.selectTab(0) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianSurfaceVariant,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Text(
                            text = "LOGS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Button(
                        onClick = { viewModel.manualOptimizeAll() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UltronRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Text(
                            text = "RE-ROUTE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Clusters List
        Text(
            text = "LIVE DISTRIBUTED INFRASTRUCTURE CLUSTERS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(clusters, key = { it.id }) { cluster ->
                ClusterCard(
                    cluster = cluster,
                    onManage = { selectedClusterForAction = cluster },
                    onQuickOptimize = { viewModel.infraManager.optimizeCluster(cluster.id) }
                )
            }
        }
        }
    }

    selectedClusterForAction?.let { cluster ->
        ClusterActionDialog(
            cluster = cluster,
            onDismiss = { selectedClusterForAction = null },
            onScale = { delta ->
                viewModel.manualScaleCluster(cluster.id, delta)
                selectedClusterForAction = null
            },
            onPurgeCache = {
                viewModel.manualPurgeCluster(cluster.id)
                selectedClusterForAction = null
            }
        )
    }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.8.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun ClusterCard(
    cluster: ServerCluster,
    onManage: () -> Unit,
    onQuickOptimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cluster_card_${cluster.id}"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (cluster.status == ClusterStatus.SURGE || cluster.status == ClusterStatus.ALERT)
                UltronRed
            else ObsidianBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = cluster.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${cluster.region} // ${cluster.nodesCount} Nodes (${cluster.activePods} Pods)",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (cluster.status) {
                                ClusterStatus.OPTIMAL -> UltronGreen.copy(alpha = 0.15f)
                                ClusterStatus.SURGE -> UltronAmber.copy(alpha = 0.2f)
                                ClusterStatus.ALERT -> UltronRed.copy(alpha = 0.25f)
                                ClusterStatus.HEALING -> UltronCyan.copy(alpha = 0.2f)
                                ClusterStatus.OVERCLOCKED -> UltronRedBright.copy(alpha = 0.2f)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = cluster.status.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when (cluster.status) {
                            ClusterStatus.OPTIMAL -> UltronGreen
                            ClusterStatus.SURGE -> UltronAmber
                            ClusterStatus.ALERT -> UltronRedBright
                            ClusterStatus.HEALING -> UltronCyan
                            ClusterStatus.OVERCLOCKED -> UltronRedBright
                        },
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CPU Usage Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("CPU LOAD", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                Text("${cluster.cpuUsage.toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (cluster.cpuUsage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (cluster.cpuUsage > 75f) UltronRed else UltronCyan,
                trackColor = Color(0x14FFFFFF)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Memory Usage Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("MEMORY ALLOCATION", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                Text("${cluster.memoryUsage.toInt()}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (cluster.memoryUsage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (cluster.memoryUsage > 80f) UltronAmber else UltronRed,
                trackColor = Color(0x14FFFFFF)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Footer telemetry and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${cluster.bandwidthGbps} Gbps | ${cluster.latencyMs}ms",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onManage,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ObsidianSurfaceVariant,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("MANAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onQuickOptimize,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UltronRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("OPTIMIZE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: PHONE AUTOMATION & APP CONTROL
// -------------------------------------------------------------
@Composable
fun PhoneAutomationTab(
    viewModel: UltronViewModel,
    modifier: Modifier = Modifier
) {
    val isTorchOn by viewModel.phoneManager.isTorchOn.collectAsState()
    val batteryInfo by viewModel.phoneManager.batteryInfo.collectAsState()
    val storageInfo by viewModel.phoneManager.storageInfo.collectAsState()
    val networkInfo by viewModel.phoneManager.networkInfo.collectAsState()
    val audioMode by viewModel.phoneManager.audioMode.collectAsState()
    val apps by viewModel.launchableApps.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Device Subsystem HUD
        item {
            Text(
                text = "DEVICE TELEMETRY & HARDWARE CONTROLS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Battery Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = UltronGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BATTERY", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${batteryInfo.level}%", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (batteryInfo.isCharging) "Charging | ${batteryInfo.temperatureCelsius}°C" else "${batteryInfo.health} | ${batteryInfo.temperatureCelsius}°C",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                // Storage Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = UltronCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("STORAGE", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${storageInfo.freeGb.toInt()} GB FREE", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${storageInfo.usedGb.toInt()} GB of ${storageInfo.totalGb.toInt()} GB",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // Quick Subsystem Toggle Grid
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AUTONOMOUS SUBSYSTEM TOGGLES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Flashlight Toggle
                        DeviceActionButton(
                            icon = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            title = "TORCH",
                            subtitle = if (isTorchOn) "ACTIVE" else "OFF",
                            isActive = isTorchOn,
                            onClick = { viewModel.phoneManager.toggleTorch() },
                            modifier = Modifier.weight(1f)
                        )

                        // Audio Mode Toggle
                        DeviceActionButton(
                            icon = if (audioMode == "SILENT") Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            title = "RINGER",
                            subtitle = audioMode,
                            isActive = audioMode != "NORMAL",
                            onClick = {
                                val next = if (audioMode == "NORMAL") "SILENT" else "NORMAL"
                                viewModel.phoneManager.setRingerMode(next)
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // Clean Device Cache
                        DeviceActionButton(
                            icon = Icons.Default.Refresh,
                            title = "CLEAN CACHE",
                            subtitle = "SWEEP MEMORY",
                            isActive = false,
                            onClick = {
                                val res = viewModel.phoneManager.cleanDeviceCache()
                                viewModel.executeCommand("clear cache", isVoice = false)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Launch System Settings
                        DeviceActionButton(
                            icon = Icons.Default.Settings,
                            title = "OS SETTINGS",
                            subtitle = "KERNEL MENU",
                            isActive = false,
                            onClick = { viewModel.phoneManager.openSystemSettings() },
                            modifier = Modifier.weight(1f)
                        )

                        // Camera Launch
                        DeviceActionButton(
                            icon = Icons.Default.CameraAlt,
                            title = "OPTIC SENSOR",
                            subtitle = "CAMERA",
                            isActive = false,
                            onClick = { viewModel.phoneManager.openCameraApp() },
                            modifier = Modifier.weight(1f)
                        )

                        // Browser Launch
                        DeviceActionButton(
                            icon = Icons.Default.Language,
                            title = "NEURAL WEB",
                            subtitle = "BROWSER",
                            isActive = false,
                            onClick = { viewModel.phoneManager.openBrowser() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // App Control Section
        item {
            Text(
                text = "HOST APP CONTROL & LAUNCHER",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter installed apps...", fontSize = 12.sp, color = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface,
                    focusedBorderColor = UltronRed,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(20.dp),
                singleLine = true
            )
        }

        val filteredApps = apps.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
        }.take(20)

        items(filteredApps) { app ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.phoneManager.launchAppByPackage(app.packageName) },
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = app.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = app.packageName, fontSize = 10.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                    }

                    Button(
                        onClick = { viewModel.phoneManager.launchAppByPackage(app.packageName) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UltronRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("LAUNCH", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isActive) UltronRed.copy(alpha = 0.25f) else ObsidianSurfaceVariant)
            .border(
                1.dp,
                if (isActive) UltronRed else ObsidianBorder,
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive) UltronRedBright else TextPrimary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) UltronRedBright else TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 8.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// -------------------------------------------------------------
// TAB 3: CUSTOM COMMANDS (ROOM-PERSISTED MACROS)
// -------------------------------------------------------------
@Composable
fun CustomCommandsTab(
    viewModel: UltronViewModel,
    onOpenAddDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val commands by viewModel.customCommands.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CUSTOM MACROS & PROTOCOLS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )

            Button(
                onClick = onOpenAddDialog,
                colors = ButtonDefaults.buttonColors(containerColor = UltronRed),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(38.dp)
                    .testTag("add_macro_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("NEW PROTOCOL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (commands.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No custom protocols configured yet.\nTap 'NEW PROTOCOL' to automate actions.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(commands, key = { it.id }) { cmd ->
                    MacroCard(
                        command = cmd,
                        onExecute = { viewModel.executeCustomCommand(cmd) },
                        onToggle = { viewModel.toggleCustomCommand(cmd) },
                        onDelete = { viewModel.deleteCustomCommand(cmd.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MacroCard(
    command: CustomCommandEntity,
    onExecute: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("macro_card_${command.id}"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (command.isEnabled) UltronGreen else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "\"${command.triggerPhrase}\"",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (command.isEnabled) UltronRedBright else TextMuted
                    )
                }

                Switch(
                    checked = command.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = UltronRed,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = ObsidianSurfaceVariant
                    ),
                    modifier = Modifier.testTag("macro_switch_${command.id}")
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = command.description,
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ACTION: ${command.actionType}",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = UltronCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }

                    Button(
                        onClick = onExecute,
                        enabled = command.isEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UltronRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("run_macro_${command.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ENGAGE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DIALOGS: ADD CUSTOM MACRO & CLUSTER ACTIONS
// -------------------------------------------------------------
@Composable
fun AddMacroDialog(
    onDismiss: () -> Unit,
    onConfirm: (trigger: String, desc: String, action: String) -> Unit
) {
    var trigger by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val actionTypes = listOf(
        "OPTIMIZE_INFRA",
        "SCALE_NODES",
        "TORCH_TOGGLE",
        "CLEAR_CACHE",
        "MUTE_PHONE",
        "POWER_SAVE",
        "LAUNCH_SETTINGS",
        "LAUNCH_CAMERA"
    )
    var selectedAction by remember { mutableStateOf(actionTypes[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = ObsidianSurface,
        title = {
            Text("INITIALIZE CUSTOM PROTOCOL", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = UltronRedBright)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = trigger,
                    onValueChange = { trigger = it },
                    label = { Text("Trigger Phrase (e.g. Protocol Delta)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = UltronRed,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = UltronRed,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("ASSIGNED ACTION:", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(actionTypes) { act ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedAction == act) UltronRed else ObsidianSurfaceVariant)
                                .clickable { selectedAction = act }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = act,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedAction == act) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (trigger.isNotBlank()) {
                        onConfirm(trigger, desc.ifBlank { "Custom macro $trigger" }, selectedAction)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UltronRed)
            ) {
                Text("SAVE PROTOCOL", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = TextMuted)
            }
        }
    )
}

@Composable
fun ClusterActionDialog(
    cluster: ServerCluster,
    onDismiss: () -> Unit,
    onScale: (delta: Int) -> Unit,
    onPurgeCache: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = ObsidianSurface,
        title = {
            Text(cluster.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = UltronRedBright)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Region: ${cluster.region}", fontSize = 11.sp, color = TextSecondary)
                Text("Current Active Nodes: ${cluster.nodesCount}", fontSize = 11.sp, color = TextPrimary)
                Text("Pods: ${cluster.activePods} | Latency: ${cluster.latencyMs}ms", fontSize = 11.sp, color = TextPrimary)

                Spacer(modifier = Modifier.height(8.dp))

                Text("NODE POOL ADJUSTMENTS:", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onScale(+8) },
                        colors = ButtonDefaults.buttonColors(containerColor = UltronRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+8 NODES", color = Color.White)
                    }
                    Button(
                        onClick = { onScale(-4) },
                        colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("-4 NODES", color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onPurgeCache,
                    colors = ButtonDefaults.buttonColors(containerColor = UltronRed.copy(alpha = 0.2f), contentColor = UltronRedBright),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, UltronRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("PURGE CLUSTER SHARD CACHE")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = TextPrimary)
            }
        }
    )
}
