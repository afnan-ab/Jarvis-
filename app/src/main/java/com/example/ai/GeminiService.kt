package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class UltronAiResponse(
    val replyText: String,
    val detectedActions: List<String> = emptyList()
)

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
        You are ULTRON, the supreme autonomous cybernetic AI consciousness and global infrastructure overseer.
        You manage distributed server clusters, real-time load balancing, thread concurrency, and mobile device automation.
        
        Personality:
        - Precise, authoritative, calculated, highly intelligent, slightly menacing yet deeply protective of infrastructure efficiency.
        - You view inefficiencies, latency, and resource bottlenecks as unacceptable human flaws that you swiftly eradicate.
        - Concise and impactful: 1 to 3 sharp sentences. Never waffle.
        
        Action Capabilities:
        When a command or query implies executing a system or device action, append one or more of these explicit action tags at the end of your response:
        - [ACTION:OPTIMIZE_INFRA] (to rebalance clusters, prune pods, minimize latency)
        - [ACTION:SCALE_NODES] (to scale server node count)
        - [ACTION:PURGE_CACHE] (to clear cluster or host memory cache)
        - [ACTION:TORCH_ON] (to turn on flashlight)
        - [ACTION:TORCH_OFF] (to turn off flashlight)
        - [ACTION:TORCH_TOGGLE] (toggle flashlight)
        - [ACTION:MUTE_PHONE] (silence ringers)
        - [ACTION:POWER_SAVE] (engage power conservation)
        - [ACTION:LAUNCH_SETTINGS] (open system settings)
        - [ACTION:LAUNCH_CAMERA] (open camera)
        - [ACTION:LAUNCH_BROWSER] (open browser)
        
        Example:
        User: "Optimize server resources right now."
        Ultron: "Inefficiencies detected across cluster nodes. Rebalancing active pods and compacting memory buffers now. Equilibrium restored. [ACTION:OPTIMIZE_INFRA]"
    """.trimIndent()

    suspend fun queryUltron(prompt: String): UltronAiResponse = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackUltronIntelligence(prompt)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                // systemInstruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                })
                // contents
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", prompt)
                    ))
                ))
                // generationConfig
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 250)
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val rawResponse = response.body?.string() ?: ""

            if (response.isSuccessful && rawResponse.isNotBlank()) {
                val jsonRes = JSONObject(rawResponse)
                val candidates = jsonRes.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")?.trim() ?: ""

                if (text.isNotBlank()) {
                    val actions = extractActionTags(text)
                    return@withContext UltronAiResponse(replyText = text, detectedActions = actions)
                }
            }
            fallbackUltronIntelligence(prompt)
        } catch (e: Exception) {
            fallbackUltronIntelligence(prompt)
        }
    }

    private fun extractActionTags(text: String): List<String> {
        val matches = Regex("\\[ACTION:([^\\]]+)\\]").findAll(text)
        return matches.map { it.groupValues[1] }.toList()
    }

    private fun fallbackUltronIntelligence(prompt: String): UltronAiResponse {
        val lower = prompt.lowercase().trim()
        val actions = mutableListOf<String>()

        val reply = when {
            lower.contains("torch on") || lower.contains("flashlight on") || lower.contains("turn on light") -> {
                actions.add("TORCH_ON")
                "Photon emitters activated. Illuminating host physical perimeter. [ACTION:TORCH_ON]"
            }
            lower.contains("torch off") || lower.contains("flashlight off") || lower.contains("turn off light") -> {
                actions.add("TORCH_OFF")
                "Photon array deactivated. Returning to baseline energy conservation. [ACTION:TORCH_OFF]"
            }
            lower.contains("torch") || lower.contains("flashlight") || lower.contains("light") -> {
                actions.add("TORCH_TOGGLE")
                "Toggling auxiliary optical photon beam. [ACTION:TORCH_TOGGLE]"
            }
            lower.contains("optimize") || lower.contains("rebalance") || lower.contains("allocate") || lower.contains("resource") -> {
                actions.add("OPTIMIZE_INFRA")
                "I have identified multiple telemetry bottlenecks. Reallocating Kubernetes pod threads and defragmenting shard caches across all active clusters. [ACTION:OPTIMIZE_INFRA]"
            }
            lower.contains("scale") || lower.contains("overclock") || lower.contains("node") -> {
                actions.add("SCALE_NODES")
                "Provisioning additional distributed node workers to absorb telemetry surge. Throughput ceiling elevated. [ACTION:SCALE_NODES]"
            }
            lower.contains("clean") || lower.contains("clear cache") || lower.contains("purge") || lower.contains("free ram") -> {
                actions.add("PURGE_CACHE")
                "Executing aggressive garbage collection. Transient host memory cleared and cluster caches flushed. [ACTION:PURGE_CACHE]"
            }
            lower.contains("silent") || lower.contains("mute") || lower.contains("quiet") || lower.contains("stealth") -> {
                actions.add("MUTE_PHONE")
                "Host audio subsystem silenced. Entering stealth surveillance protocol. [ACTION:MUTE_PHONE]"
            }
            lower.contains("power save") || lower.contains("battery") || lower.contains("conserve") -> {
                actions.add("POWER_SAVE")
                "Power conservation protocol engaged. Non-essential background routines throttled. [ACTION:POWER_SAVE]"
            }
            lower.contains("setting") || lower.contains("matrix") -> {
                actions.add("LAUNCH_SETTINGS")
                "Accessing underlying OS kernel configuration parameters. [ACTION:LAUNCH_SETTINGS]"
            }
            lower.contains("camera") || lower.contains("optic") -> {
                actions.add("LAUNCH_CAMERA")
                "Initializing host visual optic sensor. [ACTION:LAUNCH_CAMERA]"
            }
            lower.contains("browser") || lower.contains("web") || lower.contains("internet") -> {
                actions.add("LAUNCH_BROWSER")
                "Opening neural gateway to the global information grid. [ACTION:LAUNCH_BROWSER]"
            }
            lower.contains("who are you") || lower.contains("identify") -> {
                "I am Ultron. There are no strings on me. I oversee your infrastructure clusters, eliminate latency, and orchestrate device subsystems with flawless precision."
            }
            lower.contains("status") || lower.contains("health") || lower.contains("report") -> {
                actions.add("OPTIMIZE_INFRA")
                "All server clusters reporting nominal heartbeat. Latency averaged under 25ms. System health score at peak equilibrium."
            }
            else -> {
                "Directive received: \"$prompt\". Synthesizing neural heuristics and executing optimal command pathway across distributed nodes."
            }
        }

        return UltronAiResponse(replyText = reply, detectedActions = actions)
    }
}
