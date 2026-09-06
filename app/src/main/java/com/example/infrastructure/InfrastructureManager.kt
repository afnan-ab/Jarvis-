package com.example.infrastructure

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

data class ServerTelemetryPoint(
    val timestamp: Long,
    val timeLabel: String,
    val cpuUsage: Float,
    val ramUsage: Float,
    val networkLoadGbps: Float,
    val clusterId: String = "ALL"
)

data class ServerCluster(
    val id: String,
    val name: String,
    val region: String,
    val nodesCount: Int,
    val activePods: Int,
    val cpuUsage: Float,      // 0..100
    val memoryUsage: Float,   // 0..100
    val bandwidthGbps: Float,
    val latencyMs: Int,
    val status: ClusterStatus = ClusterStatus.OPTIMAL,
    val lastAction: String = "Monitoring stream active",
    val cpuHistory: List<Float> = listOf(45f, 48f, 52f, 50f, 49f, 53f)
)

enum class ClusterStatus {
    OPTIMAL,
    SURGE,
    HEALING,
    ALERT,
    OVERCLOCKED
}

data class GlobalInfraStats(
    val totalNodes: Int = 160,
    val totalPods: Int = 1024,
    val averageCpu: Float = 52.4f,
    val averageMemory: Float = 61.2f,
    val totalBandwidthGbps: Float = 68.5f,
    val avgLatencyMs: Int = 24,
    val overallHealthScore: Int = 94,
    val activeAlerts: Int = 0,
    val totalOptimizationsRun: Int = 14
)

class InfrastructureManager(private val scope: CoroutineScope) {

    private val initialClusters = listOf(
        ServerCluster(
            id = "us-east-1",
            name = "Alpha-Core Kubernetes",
            region = "US-East (N. Virginia)",
            nodesCount = 64,
            activePods = 512,
            cpuUsage = 58.4f,
            memoryUsage = 64.1f,
            bandwidthGbps = 24.2f,
            latencyMs = 18,
            cpuHistory = listOf(52f, 55f, 60f, 58f, 54f, 58f)
        ),
        ServerCluster(
            id = "eu-central-1",
            name = "Apex Cassandra Datastore",
            region = "EU-Central (Frankfurt)",
            nodesCount = 32,
            activePods = 192,
            cpuUsage = 72.8f,
            memoryUsage = 78.5f,
            bandwidthGbps = 18.6f,
            latencyMs = 32,
            status = ClusterStatus.SURGE,
            lastAction = "High query IOPS detected",
            cpuHistory = listOf(64f, 68f, 75f, 79f, 74f, 73f)
        ),
        ServerCluster(
            id = "ap-south-1",
            name = "AP-South Edge Mesh",
            region = "Asia-Pacific (Mumbai)",
            nodesCount = 48,
            activePods = 384,
            cpuUsage = 44.0f,
            memoryUsage = 49.3f,
            bandwidthGbps = 15.1f,
            latencyMs = 45,
            cpuHistory = listOf(40f, 42f, 43f, 45f, 44f, 44f)
        ),
        ServerCluster(
            id = "global-ai",
            name = "Neural Tensor Inference",
            region = "Multi-Region Distributed",
            nodesCount = 16,
            activePods = 64,
            cpuUsage = 68.2f,
            memoryUsage = 82.0f,
            bandwidthGbps = 10.6f,
            latencyMs = 12,
            cpuHistory = listOf(60f, 65f, 70f, 67f, 69f, 68f)
        )
    )

    private val _clusters = MutableStateFlow<List<ServerCluster>>(initialClusters)
    val clusters: StateFlow<List<ServerCluster>> = _clusters.asStateFlow()

    private val _globalStats = MutableStateFlow(calculateStats(initialClusters, 0))
    val globalStats: StateFlow<GlobalInfraStats> = _globalStats.asStateFlow()

    private val _telemetryHistory = MutableStateFlow<List<ServerTelemetryPoint>>(generateInitialHistory())
    val telemetryHistory: StateFlow<List<ServerTelemetryPoint>> = _telemetryHistory.asStateFlow()

    private var optimizationCount = 0

    init {
        startTelemetrySimulation()
    }

    private fun generateInitialHistory(): List<ServerTelemetryPoint> {
        val now = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val points = mutableListOf<ServerTelemetryPoint>()
        var curCpu = 56.4f
        var curRam = 61.2f
        var curNet = 68.5f

        for (i in 24 downTo 0) {
            val t = now - (i * 2000L)
            curCpu = (curCpu + (Random.nextFloat() * 4f - 2f)).coerceIn(35f, 85f)
            curRam = (curRam + (Random.nextFloat() * 2f - 1f)).coerceIn(40f, 90f)
            curNet = (curNet + (Random.nextFloat() * 6f - 3f)).coerceIn(30f, 95f)
            points.add(
                ServerTelemetryPoint(
                    timestamp = t,
                    timeLabel = timeFormat.format(Date(t)),
                    cpuUsage = curCpu,
                    ramUsage = curRam,
                    networkLoadGbps = curNet,
                    clusterId = "ALL"
                )
            )
        }
        return points
    }

    private fun startTelemetrySimulation() {
        scope.launch(Dispatchers.Default) {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            while (true) {
                delay(2000)
                _clusters.update { list ->
                    list.map { cluster ->
                        if (cluster.status == ClusterStatus.HEALING) {
                            // Recovering cluster
                            val newCpu = max(35f, cluster.cpuUsage - 4.5f)
                            val newMem = max(40f, cluster.memoryUsage - 3.2f)
                            val history = (cluster.cpuHistory + newCpu).takeLast(10)
                            cluster.copy(
                                cpuUsage = newCpu,
                                memoryUsage = newMem,
                                status = if (newCpu < 50f) ClusterStatus.OPTIMAL else ClusterStatus.HEALING,
                                cpuHistory = history
                            )
                        } else {
                            // Micro fluctuations
                            val deltaCpu = (Random.nextFloat() * 4f) - 1.8f
                            val deltaMem = (Random.nextFloat() * 2f) - 0.9f
                            val newCpu = min(99f, max(20f, cluster.cpuUsage + deltaCpu))
                            val newMem = min(99f, max(25f, cluster.memoryUsage + deltaMem))
                            val history = (cluster.cpuHistory + newCpu).takeLast(10)
                            val newStatus = when {
                                newCpu > 85f -> ClusterStatus.ALERT
                                newCpu > 70f -> ClusterStatus.SURGE
                                else -> ClusterStatus.OPTIMAL
                            }
                            cluster.copy(
                                cpuUsage = newCpu,
                                memoryUsage = newMem,
                                status = newStatus,
                                cpuHistory = history
                            )
                        }
                    }
                }
                val currentClusters = _clusters.value
                val newStats = calculateStats(currentClusters, optimizationCount)
                _globalStats.value = newStats

                // Record real-time Recharts telemetry point
                val now = System.currentTimeMillis()
                val newPoint = ServerTelemetryPoint(
                    timestamp = now,
                    timeLabel = timeFormat.format(Date(now)),
                    cpuUsage = newStats.averageCpu,
                    ramUsage = newStats.averageMemory,
                    networkLoadGbps = newStats.totalBandwidthGbps,
                    clusterId = "ALL"
                )
                _telemetryHistory.update { history ->
                    (history + newPoint).takeLast(30)
                }
            }
        }
    }

    fun optimizeAll(): String {
        optimizationCount++
        _clusters.update { list ->
            list.map { cluster ->
                val newCpu = max(32f, cluster.cpuUsage * 0.65f)
                val newMem = max(38f, cluster.memoryUsage * 0.70f)
                val newLatency = max(8, cluster.latencyMs - 6)
                val history = (cluster.cpuHistory + newCpu).takeLast(10)
                cluster.copy(
                    cpuUsage = newCpu,
                    memoryUsage = newMem,
                    latencyMs = newLatency,
                    status = ClusterStatus.HEALING,
                    lastAction = "Autonomous pod defragmentation & dynamic memory pruning complete",
                    cpuHistory = history
                )
            }
        }
        _globalStats.value = calculateStats(_clusters.value, optimizationCount)
        return "All 4 clusters rebalanced: CPU load reduced by ~35%, memory pruned, IOPS stabilized."
    }

    fun optimizeCluster(clusterId: String): String {
        var resultMessage = ""
        _clusters.update { list ->
            list.map { cluster ->
                if (cluster.id == clusterId) {
                    val newCpu = max(30f, cluster.cpuUsage * 0.60f)
                    val newMem = max(35f, cluster.memoryUsage * 0.65f)
                    val history = (cluster.cpuHistory + newCpu).takeLast(10)
                    resultMessage = "${cluster.name}: Rebalanced ${cluster.activePods} pods across ${cluster.nodesCount} nodes. Load down to ${newCpu.toInt()}%."
                    cluster.copy(
                        cpuUsage = newCpu,
                        memoryUsage = newMem,
                        status = ClusterStatus.OPTIMAL,
                        lastAction = "Targeted optimization executed at ${System.currentTimeMillis() % 10000}",
                        cpuHistory = history
                    )
                } else cluster
            }
        }
        _globalStats.value = calculateStats(_clusters.value, ++optimizationCount)
        return resultMessage
    }

    fun scaleNodes(clusterId: String, deltaNodes: Int): String {
        var msg = ""
        _clusters.update { list ->
            list.map { cluster ->
                if (cluster.id == clusterId) {
                    val newNodes = max(4, cluster.nodesCount + deltaNodes)
                    val newPods = newNodes * 8
                    val newCpu = max(20f, cluster.cpuUsage * (cluster.nodesCount.toFloat() / newNodes))
                    msg = "${cluster.name}: Scaled node pool to $newNodes nodes ($newPods pods). Resource capacity recalculated."
                    cluster.copy(
                        nodesCount = newNodes,
                        activePods = newPods,
                        cpuUsage = newCpu,
                        status = ClusterStatus.OVERCLOCKED,
                        lastAction = "Auto-scaled node pool: ${if (deltaNodes >= 0) "+$deltaNodes" else "$deltaNodes"} nodes"
                    )
                } else cluster
            }
        }
        _globalStats.value = calculateStats(_clusters.value, optimizationCount)
        return msg
    }

    fun purgeCache(clusterId: String): String {
        var msg = ""
        _clusters.update { list ->
            list.map { cluster ->
                if (cluster.id == clusterId) {
                    val newMem = max(28f, cluster.memoryUsage * 0.55f)
                    msg = "${cluster.name}: L2/L3 cache invalidated. 42.8 GB transient objects purged. Memory usage: ${newMem.toInt()}%."
                    cluster.copy(
                        memoryUsage = newMem,
                        status = ClusterStatus.OPTIMAL,
                        lastAction = "Distributed cache purged"
                    )
                } else cluster
            }
        }
        _globalStats.value = calculateStats(_clusters.value, optimizationCount)
        return msg
    }

    private fun calculateStats(clusters: List<ServerCluster>, optimizations: Int): GlobalInfraStats {
        val totalNodes = clusters.sumOf { it.nodesCount }
        val totalPods = clusters.sumOf { it.activePods }
        val avgCpu = if (clusters.isNotEmpty()) clusters.map { it.cpuUsage }.average().toFloat() else 0f
        val avgMem = if (clusters.isNotEmpty()) clusters.map { it.memoryUsage }.average().toFloat() else 0f
        val totalBandwidth = clusters.sumOf { it.bandwidthGbps.toDouble() }.toFloat()
        val avgLatency = if (clusters.isNotEmpty()) clusters.map { it.latencyMs }.average().toInt() else 0
        val alerts = clusters.count { it.status == ClusterStatus.ALERT || it.status == ClusterStatus.SURGE }
        val health = max(50, 100 - (alerts * 12) - ((avgCpu - 40f).coerceAtLeast(0f) * 0.5f).toInt())

        return GlobalInfraStats(
            totalNodes = totalNodes,
            totalPods = totalPods,
            averageCpu = avgCpu,
            averageMemory = avgMem,
            totalBandwidthGbps = totalBandwidth,
            avgLatencyMs = avgLatency,
            overallHealthScore = health,
            activeAlerts = alerts,
            totalOptimizationsRun = optimizations
        )
    }
}
