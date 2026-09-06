package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiService
import com.example.automation.LaunchableApp
import com.example.automation.PhoneAutomationManager
import com.example.automation.VoiceManager
import com.example.data.CustomCommandEntity
import com.example.data.UltronDatabase
import com.example.data.UltronRepository
import com.example.infrastructure.InfrastructureManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TerminalMessage(
    val id: Long = System.currentTimeMillis() + (0..999).random(),
    val sender: String, // "ULTRON" or "COMMANDER"
    val content: String,
    val isUltron: Boolean,
    val actionBadge: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class UltronViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UltronRepository
    val infraManager: InfrastructureManager
    val phoneManager: PhoneAutomationManager
    val voiceManager: VoiceManager
    private val geminiService = GeminiService()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _terminalMessages = MutableStateFlow<List<TerminalMessage>>(
        listOf(
            TerminalMessage(
                sender = "ULTRON",
                content = "I am ULTRON. Infrastructure surveillance grid active. 4 distributed server clusters synchronized. Awaiting command directive.",
                isUltron = true,
                actionBadge = "SYSTEM ONLINE"
            )
        )
    )
    val terminalMessages: StateFlow<List<TerminalMessage>> = _terminalMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _systemNotification = MutableStateFlow<String?>(null)
    val systemNotification: StateFlow<String?> = _systemNotification.asStateFlow()

    val customCommands: StateFlow<List<CustomCommandEntity>>
    val actionLogs = MutableStateFlow<List<com.example.data.ActionLogEntity>>(emptyList())

    val launchableApps = MutableStateFlow<List<LaunchableApp>>(emptyList())

    init {
        val db = UltronDatabase.getInstance(application)
        repository = UltronRepository(db.ultronDao())
        infraManager = InfrastructureManager(viewModelScope)
        phoneManager = PhoneAutomationManager(application)
        voiceManager = VoiceManager(application) { voiceText ->
            executeCommand(voiceText, isVoice = true)
        }

        customCommands = repository.customCommands.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.actionLogs.collect { logs ->
                actionLogs.value = logs
            }
        }

        viewModelScope.launch {
            launchableApps.value = phoneManager.getLaunchableApps()
        }
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun dismissNotification() {
        _systemNotification.value = null
    }

    fun toggleVoiceListening() {
        if (voiceManager.isListening.value) {
            voiceManager.stopListening()
        } else {
            val started = voiceManager.startListening()
            if (!started && voiceManager.speechError.value != null) {
                _systemNotification.value = voiceManager.speechError.value
            }
        }
    }

    fun commitVoiceInput() {
        voiceManager.commitCurrentSpeech()
    }

    fun simulateVoiceCommand(phrase: String) {
        voiceManager.simulateSpeechInput(phrase)
    }

    fun clearSpeechError() {
        voiceManager.clearError()
    }

    fun executeCommand(commandText: String, isVoice: Boolean = false) {
        val trimmed = commandText.trim()
        if (trimmed.isEmpty()) return

        // Check if matching any custom command trigger phrase
        val matchedCustom = customCommands.value.firstOrNull {
            it.isEnabled && it.triggerPhrase.equals(trimmed, ignoreCase = true)
        }

        // Add user command to terminal
        val userMsg = TerminalMessage(
            sender = "COMMANDER",
            content = trimmed,
            isUltron = false,
            actionBadge = if (isVoice) "VOICE IN" else "INPUT"
        )
        _terminalMessages.value = _terminalMessages.value + userMsg

        if (matchedCustom != null) {
            executeCustomCommand(matchedCustom)
            return
        }

        val lowerCmd = trimmed.lowercase()
        if (lowerCmd.startsWith("open app ") || lowerCmd.startsWith("launch app ") || lowerCmd.startsWith("open ") || lowerCmd.startsWith("launch ")) {
            val appQuery = lowerCmd.removePrefix("open app ")
                .removePrefix("launch app ")
                .removePrefix("open ")
                .removePrefix("launch ")
                .trim()
            val apps = launchableApps.value.ifEmpty { phoneManager.getLaunchableApps() }
            val matchedApp = apps.firstOrNull {
                it.name.lowercase().contains(appQuery) || it.packageName.lowercase().contains(appQuery)
            }
            if (matchedApp != null) {
                val success = launchApp(matchedApp.packageName, matchedApp.name)
                val reply = if (success) "Dispatched process for ${matchedApp.name}. Context switched to host application." else "Could not open ${matchedApp.name}."
                voiceManager.speak(reply)
                return
            }
        }

        // Execute via Gemini AI or Heuristics
        viewModelScope.launch {
            _isAiThinking.value = true
            val aiResponse = geminiService.queryUltron(trimmed)
            _isAiThinking.value = false

            // Process detected actions
            val executedBadges = mutableListOf<String>()
            for (action in aiResponse.detectedActions) {
                val badge = applyAction(action)
                executedBadges.add(badge)
            }

            val badgeSummary = if (executedBadges.isNotEmpty()) executedBadges.joinToString(" | ") else null

            val ultronMsg = TerminalMessage(
                sender = "ULTRON",
                content = aiResponse.replyText.replace(Regex("\\[ACTION:[^\\]]+\\]"), "").trim(),
                isUltron = true,
                actionBadge = badgeSummary
            )
            _terminalMessages.value = _terminalMessages.value + ultronMsg

            // Voice response
            voiceManager.speak(ultronMsg.content)

            // Persist to Room
            repository.logAction(
                commandText = trimmed,
                responseText = ultronMsg.content,
                isVoice = isVoice,
                badge = badgeSummary ?: "PROCESSED"
            )
        }
    }

    private fun applyAction(action: String): String {
        return when {
            action.startsWith("OPTIMIZE_INFRA") -> {
                val result = infraManager.optimizeAll()
                _systemNotification.value = result
                "INFRA BALANCED"
            }
            action.startsWith("SCALE_NODES") -> {
                val firstCluster = infraManager.clusters.value.firstOrNull()?.id ?: "us-east-1"
                val result = infraManager.scaleNodes(firstCluster, 8)
                _systemNotification.value = result
                "NODES SCALED"
            }
            action.startsWith("PURGE_CACHE") -> {
                val cacheResult = phoneManager.cleanDeviceCache()
                infraManager.purgeCache("eu-central-1")
                _systemNotification.value = cacheResult
                "CACHE PURGED"
            }
            action.startsWith("TORCH_ON") -> {
                phoneManager.toggleTorch(true)
                "TORCH ON"
            }
            action.startsWith("TORCH_OFF") -> {
                phoneManager.toggleTorch(false)
                "TORCH OFF"
            }
            action.startsWith("TORCH_TOGGLE") -> {
                val state = phoneManager.toggleTorch()
                if (state) "TORCH ON" else "TORCH OFF"
            }
            action.startsWith("MUTE_PHONE") -> {
                phoneManager.setRingerMode("SILENT")
                "MUTED"
            }
            action.startsWith("POWER_SAVE") -> {
                phoneManager.optimizePowerMode()
                "POWER CONSERVED"
            }
            action.startsWith("LAUNCH_SETTINGS") -> {
                phoneManager.openSystemSettings()
                "SETTINGS OPENED"
            }
            action.startsWith("LAUNCH_CAMERA") -> {
                phoneManager.openCameraApp()
                "CAMERA OPENED"
            }
            action.startsWith("LAUNCH_BROWSER") -> {
                phoneManager.openBrowser()
                "BROWSER OPENED"
            }
            action.startsWith("LAUNCH_APP:") -> {
                val pkg = action.removePrefix("LAUNCH_APP:").trim()
                phoneManager.launchAppByPackage(pkg)
                "APP LAUNCHED"
            }
            else -> "EXECUTED"
        }
    }

    fun executeCustomCommand(cmd: CustomCommandEntity) {
        val result = applyAction(cmd.actionType)
        val speech = "Custom protocol ${cmd.triggerPhrase} engaged. ${cmd.description}."

        val ultronMsg = TerminalMessage(
            sender = "ULTRON",
            content = speech,
            isUltron = true,
            actionBadge = "MACRO: $result"
        )
        _terminalMessages.value = _terminalMessages.value + ultronMsg
        voiceManager.speak(speech)

        viewModelScope.launch {
            repository.logAction(cmd.triggerPhrase, speech, isVoice = false, badge = "MACRO: $result")
        }
    }

    fun addCustomCommand(trigger: String, desc: String, actionType: String, payload: String = "") {
        viewModelScope.launch {
            repository.insertCustomCommand(
                CustomCommandEntity(
                    triggerPhrase = trigger.trim(),
                    description = desc.trim(),
                    actionType = actionType,
                    actionPayload = payload.trim(),
                    isEnabled = true
                )
            )
            _systemNotification.value = "Custom protocol \"$trigger\" initialized."
        }
    }

    fun toggleCustomCommand(cmd: CustomCommandEntity) {
        viewModelScope.launch {
            repository.updateCustomCommand(cmd.copy(isEnabled = !cmd.isEnabled))
        }
    }

    fun deleteCustomCommand(id: Long) {
        viewModelScope.launch {
            repository.deleteCustomCommand(id)
            _systemNotification.value = "Custom protocol terminated."
        }
    }

    // Direct manual controls
    fun manualOptimizeAll() {
        val res = infraManager.optimizeAll()
        voiceManager.speak("Full infrastructure optimization protocol completed. All telemetry channels re-aligned.")
        val ultronMsg = TerminalMessage(
            sender = "ULTRON",
            content = res,
            isUltron = true,
            actionBadge = "GLOBAL OPTIMIZE"
        )
        _terminalMessages.value = _terminalMessages.value + ultronMsg
        _systemNotification.value = res
        viewModelScope.launch {
            repository.logAction("MANUAL_OPTIMIZE_ALL", res, false, "GLOBAL OPTIMIZE")
        }
    }

    fun manualScaleCluster(clusterId: String, delta: Int) {
        val res = infraManager.scaleNodes(clusterId, delta)
        _systemNotification.value = res
        voiceManager.speak("Cluster node capacity updated.")
    }

    fun manualPurgeCluster(clusterId: String) {
        val res = infraManager.purgeCache(clusterId)
        _systemNotification.value = res
        voiceManager.speak("Distributed cache invalidated.")
    }

    fun launchApp(packageName: String, appName: String): Boolean {
        val success = phoneManager.launchAppByPackage(packageName)
        val status = if (success) "Launched $appName" else "Failed to launch $appName"
        _systemNotification.value = status
        _terminalMessages.value = _terminalMessages.value + TerminalMessage(
            sender = "ULTRON",
            content = if (success) "Host application '$appName' initiated via kernel launcher." else "Unable to dispatch intent for '$appName'.",
            isUltron = true,
            actionBadge = if (success) "APP OPENED" else "LAUNCH FAILED"
        )
        return success
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            launchableApps.value = phoneManager.getLaunchableApps()
        }
    }

    fun clearTerminal() {
        _terminalMessages.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
    }
}
