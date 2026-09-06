package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UltronDao {
    // Custom Commands
    @Query("SELECT * FROM custom_commands ORDER BY createdAt DESC")
    fun getAllCustomCommands(): Flow<List<CustomCommandEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCommand(command: CustomCommandEntity): Long

    @Update
    suspend fun updateCustomCommand(command: CustomCommandEntity)

    @Query("DELETE FROM custom_commands WHERE id = :id")
    suspend fun deleteCustomCommandById(id: Long)

    // Action Logs
    @Query("SELECT * FROM action_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentActionLogs(): Flow<List<ActionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionLog(log: ActionLogEntity): Long

    @Query("DELETE FROM action_logs")
    suspend fun clearActionLogs()

    // Telemetry Records
    @Query("SELECT * FROM telemetry_records ORDER BY timestamp DESC LIMIT 100")
    fun getRecentTelemetry(): Flow<List<TelemetryRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetry(record: TelemetryRecordEntity)
}
