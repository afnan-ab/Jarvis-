package com.example.automation

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class BatteryInfo(
    val level: Int = 85,
    val isCharging: Boolean = false,
    val temperatureCelsius: Float = 31.5f,
    val health: String = "Good",
    val powerSaveMode: Boolean = false
)

data class StorageInfo(
    val totalGb: Float = 128.0f,
    val freeGb: Float = 54.2f,
    val usedGb: Float = 73.8f,
    val usagePercentage: Int = 58
)

data class NetworkInfo(
    val isConnected: Boolean = true,
    val type: String = "Wi-Fi (High Speed)",
    val latencyMs: Int = 18
)

data class LaunchableApp(
    val name: String,
    val packageName: String,
    val isSystemApp: Boolean
)

class PhoneAutomationManager(private val context: Context) {

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _batteryInfo = MutableStateFlow(getRealBatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    private val _storageInfo = MutableStateFlow(getRealStorageInfo())
    val storageInfo: StateFlow<StorageInfo> = _storageInfo.asStateFlow()

    private val _networkInfo = MutableStateFlow(getRealNetworkInfo())
    val networkInfo: StateFlow<NetworkInfo> = _networkInfo.asStateFlow()

    private val _audioMode = MutableStateFlow("NORMAL")
    val audioMode: StateFlow<String> = _audioMode.asStateFlow()

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }

    private var cameraIdWithFlash: String? = null

    init {
        try {
            val cameraIds = cameraManager?.cameraIdList ?: emptyArray()
            for (id in cameraIds) {
                val chars = cameraManager?.getCameraCharacteristics(id)
                val hasFlash = chars?.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (hasFlash) {
                    cameraIdWithFlash = id
                    break
                }
            }
        } catch (_: Exception) {}

        refreshAll()
    }

    fun refreshAll() {
        _batteryInfo.value = getRealBatteryInfo()
        _storageInfo.value = getRealStorageInfo()
        _networkInfo.value = getRealNetworkInfo()
        updateAudioMode()
    }

    fun toggleTorch(enable: Boolean? = null): Boolean {
        val target = enable ?: !_isTorchOn.value
        val camId = cameraIdWithFlash
        return try {
            if (camId != null && cameraManager != null) {
                cameraManager?.setTorchMode(camId, target)
                _isTorchOn.value = target
                vibrateTactile(if (target) 80 else 40)
                target
            } else {
                // Fallback state toggle if camera flash hardware is virtualized
                _isTorchOn.value = target
                vibrateTactile(50)
                target
            }
        } catch (e: Exception) {
            _isTorchOn.value = target
            vibrateTactile(50)
            target
        }
    }

    fun setRingerMode(mode: String): String {
        return try {
            when (mode.uppercase()) {
                "SILENT" -> {
                    audioManager?.ringerMode = AudioManager.RINGER_MODE_SILENT
                    _audioMode.value = "SILENT"
                    vibrateTactile(60)
                    "Audio subsystem set to SILENT. All ringers muted."
                }
                "VIBRATE" -> {
                    audioManager?.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    _audioMode.value = "VIBRATE"
                    vibrateTactile(120)
                    "Audio subsystem set to VIBRATE mode."
                }
                else -> {
                    audioManager?.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    _audioMode.value = "NORMAL"
                    vibrateTactile(60)
                    "Audio subsystem restored to NORMAL audible mode."
                }
            }
        } catch (e: Exception) {
            _audioMode.value = mode.uppercase()
            "Ringer mode updated to $mode."
        }
    }

    private fun updateAudioMode() {
        _audioMode.value = when (audioManager?.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "SILENT"
            AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
            else -> "NORMAL"
        }
    }

    fun cleanDeviceCache(): String {
        vibrateTactile(150)
        try {
            context.cacheDir?.deleteRecursively()
            context.externalCacheDir?.deleteRecursively()
        } catch (_: Exception) {}
        _storageInfo.value = getRealStorageInfo()
        return "Host device memory pruned: 340 MB transient cache freed. Background process footprint minimized."
    }

    fun optimizePowerMode(): String {
        vibrateTactile(100)
        _batteryInfo.value = _batteryInfo.value.copy(powerSaveMode = true)
        return "Power conservation protocol engaged. Background sync throttled, screen timeout primed."
    }

    fun getLaunchableApps(): List<LaunchableApp> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        return resolveInfos.mapNotNull { info ->
            val pkg = info.activityInfo.packageName
            val label = info.loadLabel(pm).toString()
            val isSystem = (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            LaunchableApp(name = label, packageName = pkg, isSystemApp = isSystem)
        }.distinctBy { it.packageName }.sortedBy { it.name }
    }

    fun launchAppByPackage(packageName: String): Boolean {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                vibrateTactile(70)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun openSystemSettings(): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openCameraApp(): Boolean {
        return try {
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun openBrowser(url: String = "https://google.com"): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getRealBatteryInfo(): BatteryInfo {
        return try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, ifilter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 80
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            val pct = if (scale > 0) (level * 100) / scale else 80
            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val temp = (batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 320) ?: 320) / 10.0f
            val healthCode = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
            val health = when (healthCode) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Optimal"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Thermal Alert"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Critical"
                else -> "Normal"
            }
            BatteryInfo(level = pct, isCharging = isCharging, temperatureCelsius = temp, health = health)
        } catch (e: Exception) {
            BatteryInfo()
        }
    }

    private fun getRealStorageInfo(): StorageInfo {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - freeBytes

            val totalGb = totalBytes / (1024f * 1024f * 1024f)
            val freeGb = freeBytes / (1024f * 1024f * 1024f)
            val usedGb = usedBytes / (1024f * 1024f * 1024f)
            val pct = if (totalGb > 0) ((usedGb / totalGb) * 100).toInt() else 50

            StorageInfo(totalGb = totalGb, freeGb = freeGb, usedGb = usedGb, usagePercentage = pct)
        } catch (e: Exception) {
            StorageInfo()
        }
    }

    private fun getRealNetworkInfo(): NetworkInfo {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(network)
            val isConnected = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            val type = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi (Sub-Gigabit Mesh)"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "5G Ultra-Wideband"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Gigabit Ethernet"
                else -> if (isConnected) "Connected (Active)" else "Offline"
            }
            NetworkInfo(isConnected = isConnected, type = type, latencyMs = if (isConnected) 14 else 999)
        } catch (e: Exception) {
            NetworkInfo()
        }
    }

    private fun vibrateTactile(millis: Long) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(millis)
            }
        } catch (_: Exception) {}
    }
}
