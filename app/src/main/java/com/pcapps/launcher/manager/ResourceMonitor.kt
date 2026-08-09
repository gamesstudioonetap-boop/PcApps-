package com.pcapps.launcher.manager

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager

data class ResourceSnapshot(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val appMemoryUsedMb: Long,
    val lowMemory: Boolean,
    val thermalStatus: ThermalStatus
)

enum class ThermalStatus { NONE, LIGHT, MODERATE, SEVERE, CRITICAL, UNKNOWN }

class ResourceMonitor(private val context: Context) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    fun snapshot(): ResourceSnapshot {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val appMemInfo = Runtime.getRuntime()
        val appUsedMb = (appMemInfo.totalMemory() - appMemInfo.freeMemory()) / (1024 * 1024)

        return ResourceSnapshot(
            totalRamMb = memInfo.totalMem / (1024 * 1024),
            availableRamMb = memInfo.availMem / (1024 * 1024),
            appMemoryUsedMb = appUsedMb,
            lowMemory = memInfo.lowMemory,
            thermalStatus = currentThermalStatus()
        )
    }

    private fun currentThermalStatus(): ThermalStatus {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalStatus.UNKNOWN
        return when (powerManager.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalStatus.NONE
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatus.LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatus.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatus.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL,
            PowerManager.THERMAL_STATUS_EMERGENCY,
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatus.CRITICAL
            else -> ThermalStatus.UNKNOWN
        }
    }

    /** Suggested graphics-preset downgrade when thermal pressure is high. Pure recommendation, no UI side effects. */
    fun shouldSuggestPerformanceDowngrade(snapshot: ResourceSnapshot): Boolean =
        snapshot.thermalStatus == ThermalStatus.SEVERE || snapshot.thermalStatus == ThermalStatus.CRITICAL
}
