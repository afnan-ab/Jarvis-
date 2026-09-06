package com.example.data

import kotlinx.coroutines.flow.Flow

class UltronRepository(private val dao: UltronDao) {
    val customCommands: Flow<List<CustomCommandEntity>> = dao.getAllCustomCommands()
    val actionLogs: Flow<List<ActionLogEntity>> = dao.getRecentActionLogs()
    val recentTelemetry: Flow<List<TelemetryRecordEntity>> = dao.getRecentTelemetry()

    suspend fun insertCustomCommand(command: CustomCommandEntity): Long = dao.insertCustomCommand(command)

    suspend fun updateCustomCommand(command: CustomCommandEntity) = dao.updateCustomCommand(command)

    suspend fun deleteCustomCommand(id: Long) = dao.deleteCustomCommandById(id)

    suspend fun logAction(commandText: String, responseText: String, isVoice: Boolean, badge: String = "EXECUTED") {
        dao.insertActionLog(
            ActionLogEntity(
                commandText = commandText,
                ultronResponse = responseText,
                isVoice = isVoice,
                statusBadge = badge
            )
        )
    }

    suspend fun recordTelemetry(record: TelemetryRecordEntity) {
        dao.insertTelemetry(record)
    }

    suspend fun clearLogs() = dao.clearActionLogs()
}
