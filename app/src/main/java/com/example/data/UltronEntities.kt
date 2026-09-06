package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_commands")
data class CustomCommandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val triggerPhrase: String,
    val description: String,
    val actionType: String, // "OPTIMIZE_INFRA", "SCALE_NODES", "TORCH_TOGGLE", "CLEAR_CACHE", "MUTE_PHONE", "LAUNCH_APP", "AI_PROMPT"
    val actionPayload: String = "",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "telemetry_records")
data class TelemetryRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clusterId: String,
    val clusterName: String,
    val cpuUsage: Float,
    val memoryUsage: Float,
    val latencyMs: Int,
    val status: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "action_logs")
data class ActionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commandText: String,
    val ultronResponse: String,
    val isVoice: Boolean = false,
    val statusBadge: String = "EXECUTED",
    val timestamp: Long = System.currentTimeMillis()
)
