package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CustomCommandEntity::class,
        TelemetryRecordEntity::class,
        ActionLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class UltronDatabase : RoomDatabase() {
    abstract fun ultronDao(): UltronDao

    companion object {
        @Volatile
        private var INSTANCE: UltronDatabase? = null

        fun getInstance(context: Context): UltronDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UltronDatabase::class.java,
                    "ultron_assistant.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            populateDefaultCommands(getInstance(context).ultronDao())
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateDefaultCommands(dao: UltronDao) {
            val defaults = listOf(
                CustomCommandEntity(
                    triggerPhrase = "Protocol Omega",
                    description = "Emergency full-cluster optimization and memory compaction",
                    actionType = "OPTIMIZE_INFRA",
                    actionPayload = "ALL",
                    isEnabled = true
                ),
                CustomCommandEntity(
                    triggerPhrase = "Overclock Edge Nodes",
                    description = "Scale edge inference nodes to maximum throughput",
                    actionType = "SCALE_NODES",
                    actionPayload = "AP-South Edge Mesh",
                    isEnabled = true
                ),
                CustomCommandEntity(
                    triggerPhrase = "Purge Host Cache",
                    description = "Clear device memory footprint and storage debris",
                    actionType = "CLEAR_CACHE",
                    actionPayload = "",
                    isEnabled = true
                ),
                CustomCommandEntity(
                    triggerPhrase = "Stealth Sentry",
                    description = "Mute phone audio, silence ringers, enter quiet mode",
                    actionType = "MUTE_PHONE",
                    actionPayload = "SILENT",
                    isEnabled = true
                ),
                CustomCommandEntity(
                    triggerPhrase = "Activate Photon Beam",
                    description = "Toggle device tactical flashlight torch",
                    actionType = "TORCH_TOGGLE",
                    actionPayload = "",
                    isEnabled = true
                ),
                CustomCommandEntity(
                    triggerPhrase = "Launch System Matrix",
                    description = "Open Android system settings console",
                    actionType = "LAUNCH_APP",
                    actionPayload = "com.android.settings",
                    isEnabled = true
                )
            )
            for (cmd in defaults) {
                dao.insertCustomCommand(cmd)
            }
        }
    }
}
