package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.automation.LaunchableApp
import com.example.infrastructure.ClusterStatus
import com.example.infrastructure.ServerCluster
import com.example.infrastructure.ServerTelemetryPoint
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
import kotlin.math.roundToInt

enum class MetricViewMode {
    COMBINED,
    CPU_ONLY,
    RAM_ONLY,
    NETWORK_ONLY
}

/**
 * Real-Time Server Resource Usage Dashboard modeled after Recharts.
 * Visualizes CPU, RAM, and Network load metrics with live streaming telemetry,
 * interactive touch/drag scrubber tooltips, multi-series area gradients,
 * and an integrated Host App Launcher.
 */
@Composable
fun ServerResourceDashboard(
    viewModel: UltronViewModel,
    modifier: Modifier = Modifier
) {
    val clusters by viewModel.infraManager.clusters.collectAsState()
    val stats by viewModel.infraManager.globalStats.collectAsState()
    val telemetryHistory by viewModel.infraManager.telemetryHistory.collectAsState()
    val launchableApps by viewModel.launchableApps.collectAsState()

    var selectedClusterId by remember { mutableStateOf("ALL") }
    var metricViewMode by remember { mutableStateOf(MetricViewMode.COMBINED) }
    var isCpuVisible by remember { mutableStateOf(true) }
    var isRamVisible by remember { mutableStateOf(true) }
    var isNetworkVisible by remember { mutableStateOf(true) }
    var isStreamingPaused by remember { mutableStateOf(false) }

    var showAppLauncherModal by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Pulsing real-time streaming indicator
    val infiniteTransition = rememberInfiniteTransition(label = "StreamingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Current active cluster metrics
    val activeCluster = clusters.firstOrNull { it.id == selectedClusterId }
    val currentCpu = activeCluster?.cpuUsage ?: stats.averageCpu
    val currentRam = activeCluster?.memoryUsage ?: stats.averageMemory
    val currentNetwork = activeCluster?.bandwidthGbps ?: stats.totalBandwidthGbps

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("server_resource_dashboard"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Dashboard Top Header & Streaming Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isStreamingPaused) UltronAmber
                                    else UltronGreen.copy(alpha = pulseAlpha)
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isStreamingPaused) "TELEMETRY PAUSED" else "LIVE RECHARTS TELEMETRY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isStreamingPaused) UltronAmber else UltronGreen,
                            letterSpacing = 1.2.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Resource Usage Dashboard",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Quick Pause / Play Button
                    IconButton(
                        onClick = { isStreamingPaused = !isStreamingPaused },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianSurfaceVariant)
                            .testTag("toggle_streaming_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isStreamingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "Toggle Stream",
                            tint = if (isStreamingPaused) UltronGreen else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Open App Quick Modal Launcher Button
                    Button(
                        onClick = {
                            viewModel.refreshInstalledApps()
                            showAppLauncherModal = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = UltronRed),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("dashboard_open_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "OPEN APP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }

        // 2. Cluster Selector Chips
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    ClusterChip(
                        name = "Global Fleet (All)",
                        isSelected = selectedClusterId == "ALL",
                        onClick = { selectedClusterId = "ALL" },
                        testTag = "cluster_chip_all"
                    )
                }
                items(clusters) { cluster ->
                    ClusterChip(
                        name = cluster.name.split(" ").take(2).joinToString(" "),
                        isSelected = selectedClusterId == cluster.id,
                        onClick = { selectedClusterId = cluster.id },
                        testTag = "cluster_chip_${cluster.id}"
                    )
                }
            }
        }

        // 3. Real-Time Metric KPI Cards (CPU, RAM, Network)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricKpiCard(
                    title = "CPU LOAD",
                    value = "${currentCpu.roundToInt()}%",
                    subValue = if (currentCpu > 80f) "High Load" else "Nominal",
                    accentColor = UltronRedBright,
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )

                MetricKpiCard(
                    title = "RAM USAGE",
                    value = "${currentRam.roundToInt()}%",
                    subValue = "${(currentRam * 0.64f).roundToInt()} / 64 GB",
                    accentColor = UltronAmber,
                    icon = Icons.Default.Memory,
                    modifier = Modifier.weight(1f)
                )

                MetricKpiCard(
                    title = "NETWORK",
                    value = "${currentNetwork.roundToInt()}G",
                    subValue = "${(currentNetwork * 12.5f).roundToInt()} MB/s",
                    accentColor = UltronCyan,
                    icon = Icons.Default.Dns,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Interactive Recharts-Style Chart Container
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recharts_chart_card"),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(22.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Recharts Header with Mode Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "REAL-TIME TELEMETRY MATRIX",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (selectedClusterId == "ALL") "Global Aggregated Load" else (activeCluster?.name ?: "Cluster Load"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        // Metric View Mode Pills
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianSurfaceVariant)
                                .padding(2.dp)
                        ) {
                            MetricModeButton(
                                label = "ALL",
                                isSelected = metricViewMode == MetricViewMode.COMBINED,
                                onClick = {
                                    metricViewMode = MetricViewMode.COMBINED
                                    isCpuVisible = true
                                    isRamVisible = true
                                    isNetworkVisible = true
                                }
                            )
                            MetricModeButton(
                                label = "CPU",
                                isSelected = metricViewMode == MetricViewMode.CPU_ONLY,
                                onClick = {
                                    metricViewMode = MetricViewMode.CPU_ONLY
                                    isCpuVisible = true
                                    isRamVisible = false
                                    isNetworkVisible = false
                                }
                            )
                            MetricModeButton(
                                label = "RAM",
                                isSelected = metricViewMode == MetricViewMode.RAM_ONLY,
                                onClick = {
                                    metricViewMode = MetricViewMode.RAM_ONLY
                                    isCpuVisible = false
                                    isRamVisible = true
                                    isNetworkVisible = false
                                }
                            )
                            MetricModeButton(
                                label = "NET",
                                isSelected = metricViewMode == MetricViewMode.NETWORK_ONLY,
                                onClick = {
                                    metricViewMode = MetricViewMode.NETWORK_ONLY
                                    isCpuVisible = false
                                    isRamVisible = false
                                    isNetworkVisible = true
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Recharts Canvas Chart with Interactive Scrubber
                    RechartsInteractiveCanvas(
                        data = telemetryHistory,
                        showCpu = isCpuVisible,
                        showRam = isRamVisible,
                        showNetwork = isNetworkVisible,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Recharts Legend with toggleable series
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RechartsLegendItem(
                            label = "CPU Load (%)",
                            color = UltronRedBright,
                            isActive = isCpuVisible,
                            onToggle = { isCpuVisible = !isCpuVisible }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        RechartsLegendItem(
                            label = "RAM Usage (%)",
                            color = UltronAmber,
                            isActive = isRamVisible,
                            onToggle = { isRamVisible = !isRamVisible }
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        RechartsLegendItem(
                            label = "Network (Gbps)",
                            color = UltronCyan,
                            isActive = isNetworkVisible,
                            onToggle = { isNetworkVisible = !isNetworkVisible }
                        )
                    }
                }
            }
        }

        // 5. Host Application Launcher Section ("And it can open app")
        item {
            HostAppLauncherSection(
                onOpenSettings = { viewModel.phoneManager.openSystemSettings() },
                onOpenBrowser = { viewModel.phoneManager.openBrowser() },
                onOpenCamera = { viewModel.phoneManager.openCameraApp() },
                onOpenTerminal = { viewModel.selectTab(0) },
                onOpenAllApps = {
                    viewModel.refreshInstalledApps()
                    showAppLauncherModal = true
                }
            )
        }

        // 6. Cluster Status Breakdown & Autonomous Optimization Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            text = "CLUSTER INFRASTRUCTURE ALLOCATION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${clusters.size} NODES MONITORED",
                            fontSize = 9.sp,
                            color = UltronCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    clusters.forEach { cluster ->
                        ClusterRowItem(
                            cluster = cluster,
                            onOptimize = { viewModel.manualScaleCluster(cluster.id, 4) }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { viewModel.manualOptimizeAll() },
                        colors = ButtonDefaults.buttonColors(containerColor = UltronRed),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("dashboard_rebalance_button")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REBALANCE SERVER CLUSTERS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Full App Launcher Dialog (Opens any installed device app)
    if (showAppLauncherModal) {
        AppLauncherModal(
            apps = launchableApps,
            onLaunchApp = { pkg, name ->
                viewModel.launchApp(pkg, name)
                showAppLauncherModal = false
            },
            onDismiss = { showAppLauncherModal = false }
        )
    }
}

// -------------------------------------------------------------
// Interactive Recharts Canvas Implementation
// -------------------------------------------------------------

@Composable
fun RechartsInteractiveCanvas(
    data: List<ServerTelemetryPoint>,
    showCpu: Boolean,
    showRam: Boolean,
    showNetwork: Boolean,
    modifier: Modifier = Modifier
) {
    var touchX by remember { mutableFloatStateOf(-1f) }
    var isDragging by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0A0A0A))
            .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchX = offset.x
                        isDragging = true
                        tryAwaitRelease()
                        isDragging = false
                        touchX = -1f
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        touchX = offset.x
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        touchX = -1f
                    },
                    onDragCancel = {
                        isDragging = false
                        touchX = -1f
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        touchX = change.position.x
                    }
                )
            }
            .testTag("recharts_canvas_box")
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        val paddingLeft = 32f
        val paddingRight = 16f
        val paddingTop = 20f
        val paddingBottom = 26f

        val graphWidth = (width - paddingLeft - paddingRight).coerceAtLeast(1f)
        val graphHeight = (height - paddingTop - paddingBottom).coerceAtLeast(1f)

        val points = data.ifEmpty {
            listOf(
                ServerTelemetryPoint(0, "00:00:00", 50f, 50f, 50f),
                ServerTelemetryPoint(1, "00:00:02", 55f, 52f, 58f)
            )
        }

        // Calculate hovered index based on touchX
        val hoveredIndex = if (touchX >= paddingLeft && touchX <= width - paddingRight && points.size > 1) {
            val progress = ((touchX - paddingLeft) / graphWidth).coerceIn(0f, 1f)
            (progress * (points.size - 1)).roundToInt().coerceIn(0, points.size - 1)
        } else null

        val hoveredPoint = hoveredIndex?.let { points.getOrNull(it) }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeDashed = Stroke(
                width = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
                cap = StrokeCap.Round
            )

            // 1. Cartesian Grid Horizontal Lines & Y-Axis Labels (100%, 75%, 50%, 25%, 0%)
            val gridSteps = 4
            for (i in 0..gridSteps) {
                val y = paddingTop + (graphHeight * (i.toFloat() / gridSteps))
                drawLine(
                    color = Color(0x1AFFFFFF),
                    start = Offset(paddingLeft, y),
                    end = Offset(width - paddingRight, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                )
            }

            // 2. Build multi-series paths for CPU, RAM, Network
            if (points.size >= 2) {
                val stepX = graphWidth / (points.size - 1)

                fun calculateY(valuePercent: Float): Float {
                    val clamped = valuePercent.coerceIn(0f, 100f)
                    return paddingTop + graphHeight - (clamped / 100f * graphHeight)
                }

                // Function to draw smooth Recharts Area & Stroke
                fun drawSeries(
                    metricValues: List<Float>,
                    lineColor: Color,
                    gradientTopColor: Color
                ) {
                    val linePath = Path()
                    val areaPath = Path()

                    val firstX = paddingLeft
                    val firstY = calculateY(metricValues.first())

                    linePath.moveTo(firstX, firstY)
                    areaPath.moveTo(firstX, paddingTop + graphHeight)
                    areaPath.lineTo(firstX, firstY)

                    for (i in 0 until metricValues.size - 1) {
                        val currentX = paddingLeft + (i * stepX)
                        val currentY = calculateY(metricValues[i])
                        val nextX = paddingLeft + ((i + 1) * stepX)
                        val nextY = calculateY(metricValues[i + 1])

                        val controlX1 = currentX + (nextX - currentX) / 2f
                        val controlY1 = currentY
                        val controlX2 = currentX + (nextX - currentX) / 2f
                        val controlY2 = nextY

                        linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, nextX, nextY)
                        areaPath.cubicTo(controlX1, controlY1, controlX2, controlY2, nextX, nextY)
                    }

                    areaPath.lineTo(paddingLeft + ((metricValues.size - 1) * stepX), paddingTop + graphHeight)
                    areaPath.close()

                    // Fill Gradient Area
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(gradientTopColor, Color.Transparent),
                            startY = paddingTop,
                            endY = paddingTop + graphHeight
                        )
                    )

                    // Draw Smooth Stroke Line
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )
                }

                // Draw Network Area (Back)
                if (showNetwork) {
                    drawSeries(
                        metricValues = points.map { it.networkLoadGbps.coerceIn(0f, 100f) },
                        lineColor = UltronCyan,
                        gradientTopColor = UltronCyan.copy(alpha = 0.25f)
                    )
                }

                // Draw RAM Area (Middle)
                if (showRam) {
                    drawSeries(
                        metricValues = points.map { it.ramUsage },
                        lineColor = UltronAmber,
                        gradientTopColor = UltronAmber.copy(alpha = 0.30f)
                    )
                }

                // Draw CPU Area (Front)
                if (showCpu) {
                    drawSeries(
                        metricValues = points.map { it.cpuUsage },
                        lineColor = UltronRedBright,
                        gradientTopColor = UltronRedBright.copy(alpha = 0.35f)
                    )
                }

                // 3. Draw Vertical Scrubber Guideline & Highlight Dots if Touching
                if (hoveredIndex != null && hoveredPoint != null) {
                    val indicatorX = paddingLeft + (hoveredIndex * stepX)

                    // Vertical tracking guideline
                    drawLine(
                        color = Color(0x88FFFFFF),
                        start = Offset(indicatorX, paddingTop),
                        end = Offset(indicatorX, paddingTop + graphHeight),
                        strokeWidth = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                    )

                    // Helper to draw highlighted marker dots
                    fun drawMarker(value: Float, color: Color) {
                        val markerY = calculateY(value)
                        drawCircle(
                            color = color.copy(alpha = 0.3f),
                            radius = 9f,
                            center = Offset(indicatorX, markerY)
                        )
                        drawCircle(
                            color = color,
                            radius = 5f,
                            center = Offset(indicatorX, markerY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5f,
                            center = Offset(indicatorX, markerY)
                        )
                    }

                    if (showCpu) drawMarker(hoveredPoint.cpuUsage, UltronRedBright)
                    if (showRam) drawMarker(hoveredPoint.ramUsage, UltronAmber)
                    if (showNetwork) drawMarker(hoveredPoint.networkLoadGbps, UltronCyan)
                }
            }
        }

        // Y-Axis Static Labels
        Column(
            modifier = Modifier
                .padding(start = 6.dp, top = 14.dp)
                .height(160.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text("100%", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text("75%", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text("50%", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text("25%", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text("0%", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
        }

        // X-Axis Time Labels at Bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 16.dp, bottom = 4.dp)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("-60s", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text("-30s", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text("-15s", fontSize = 8.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
            Text("LIVE", fontSize = 8.sp, color = UltronGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        // Floating Recharts Tooltip Card
        if (hoveredPoint != null) {
            val stepX = if (points.size > 1) graphWidth / (points.size - 1) else 0f
            val targetX = paddingLeft + (hoveredIndex * stepX)
            // Flip tooltip side if near right border to prevent clipping
            val tooltipOffset = if (targetX > width * 0.6f) -165 else 15

            Box(
                modifier = Modifier
                    .offset { IntOffset((targetX + tooltipOffset).toInt(), 16) }
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xE6141414))
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = hoveredPoint.timeLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (showCpu) {
                        Text(
                            text = "● CPU: ${hoveredPoint.cpuUsage.roundToInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = UltronRedBright
                        )
                    }
                    if (showRam) {
                        Text(
                            text = "● RAM: ${hoveredPoint.ramUsage.roundToInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = UltronAmber
                        )
                    }
                    if (showNetwork) {
                        Text(
                            text = "● NET: ${hoveredPoint.networkLoadGbps.roundToInt()} Gbps",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = UltronCyan
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Supporting UI Components
// -------------------------------------------------------------

@Composable
fun MetricKpiCard(
    title: String,
    value: String,
    subValue: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subValue,
                fontSize = 10.sp,
                color = accentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ClusterChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) UltronRed else ObsidianSurface)
            .border(
                1.dp,
                if (isSelected) UltronRedBright else ObsidianBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextSecondary
        )
    }
}

@Composable
fun MetricModeButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) UltronRed else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else TextMuted,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun RechartsLegendItem(
    label: String,
    color: Color,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onToggle() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isActive) color else TextMuted.copy(alpha = 0.4f))
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isActive) TextPrimary else TextMuted,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Integrated Host Application Launcher dock embedded within the Server Dashboard.
 */
@Composable
fun HostAppLauncherSection(
    onOpenSettings: () -> Unit,
    onOpenBrowser: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenAllApps: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("host_app_launcher_card"),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(22.dp),
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x33DC2626))
                            .padding(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Launch,
                            contentDescription = null,
                            tint = UltronRedBright,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "HOST APPLICATION LAUNCHER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Launch device apps while monitoring server loads",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                Button(
                    onClick = onOpenAllApps,
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("open_all_apps_button")
                ) {
                    Text("MORE APPS", fontSize = 9.sp, color = UltronCyan, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick App Launch Icon Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickAppIcon(
                    name = "Settings",
                    icon = Icons.Default.Settings,
                    accentColor = UltronCyan,
                    onClick = onOpenSettings,
                    testTag = "quick_app_settings"
                )
                QuickAppIcon(
                    name = "Browser",
                    icon = Icons.Default.Language,
                    accentColor = UltronGreen,
                    onClick = onOpenBrowser,
                    testTag = "quick_app_browser"
                )
                QuickAppIcon(
                    name = "Camera",
                    icon = Icons.Default.CameraAlt,
                    accentColor = UltronAmber,
                    onClick = onOpenCamera,
                    testTag = "quick_app_camera"
                )
                QuickAppIcon(
                    name = "Console",
                    icon = Icons.Default.Terminal,
                    accentColor = UltronRedBright,
                    onClick = onOpenTerminal,
                    testTag = "quick_app_console"
                )
                QuickAppIcon(
                    name = "All Apps",
                    icon = Icons.Default.Apps,
                    accentColor = Color(0xFFA855F7),
                    onClick = onOpenAllApps,
                    testTag = "quick_app_all"
                )
            }
        }
    }
}

@Composable
fun QuickAppIcon(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ObsidianSurfaceVariant)
                .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            fontSize = 10.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ClusterRowItem(
    cluster: ServerCluster,
    onOptimize: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianSurfaceVariant)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cluster.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CPU: ${cluster.cpuUsage.roundToInt()}%",
                    fontSize = 10.sp,
                    color = if (cluster.cpuUsage > 80f) UltronRedBright else UltronCyan
                )
                Text(
                    text = "RAM: ${cluster.memoryUsage.roundToInt()}%",
                    fontSize = 10.sp,
                    color = UltronAmber
                )
                Text(
                    text = "${cluster.bandwidthGbps.roundToInt()} Gbps",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when (cluster.status) {
                        ClusterStatus.OPTIMAL -> Color(0x2222C55E)
                        ClusterStatus.SURGE -> Color(0x33DC2626)
                        ClusterStatus.ALERT -> Color(0x33DC2626)
                        else -> Color(0x22F59E0B)
                    }
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = cluster.status.name,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = when (cluster.status) {
                    ClusterStatus.OPTIMAL -> UltronGreen
                    ClusterStatus.SURGE, ClusterStatus.ALERT -> UltronRedBright
                    else -> UltronAmber
                },
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Searchable Full Application Drawer Dialog allowing the user to select and launch
 * any installed Android app on the system.
 */
@Composable
fun AppLauncherModal(
    apps: List<LaunchableApp>,
    onLaunchApp: (packageName: String, appName: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .height(560.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(ObsidianDark)
                .border(1.2.dp, ObsidianBorder, RoundedCornerShape(28.dp))
                .padding(18.dp)
                .testTag("app_launcher_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = UltronRedBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LAUNCH INSTALLED APP",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ObsidianSurfaceVariant)
                            .testTag("close_app_launcher_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search application name or package...", fontSize = 12.sp, color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianSurface,
                        unfocusedContainerColor = ObsidianSurface,
                        focusedBorderColor = UltronRed,
                        unfocusedBorderColor = ObsidianBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${filteredApps.size} APPLICATIONS READY TO LAUNCH",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.8.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Apps List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(ObsidianSurface)
                                .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                                .clickable { onLaunchApp(app.packageName, app.name) }
                                .padding(12.dp)
                                .testTag("launch_app_item_${app.name.replace(" ", "_")}"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ObsidianSurfaceVariant)
                                        .border(0.8.dp, ObsidianBorder, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (app.isSystemApp) Icons.Default.Settings else Icons.Default.Apps,
                                        contentDescription = null,
                                        tint = if (app.isSystemApp) UltronCyan else UltronRedBright,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = app.packageName,
                                        fontSize = 9.sp,
                                        color = TextMuted,
                                        maxLines = 1
                                    )
                                }
                            }

                            Button(
                                onClick = { onLaunchApp(app.packageName, app.name) },
                                colors = ButtonDefaults.buttonColors(containerColor = UltronRed),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("OPEN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
