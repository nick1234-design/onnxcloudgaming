package com.nick1234.onnxcloudgaming

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight memory and stability recorder for long cloud-gaming sessions.
 * It observes memory pressure and reboot/thermal clues without interrupting playback.
 */
class StabilityDiagnostics(context: Context) {
    companion object {
        private const val PREFS = "onnx_stability_diagnostics"
        private const val MAX_EVENTS = 24
        private const val LOW_AVAILABLE_MEMORY_MB = 128L
        private const val WARNING_AVAILABLE_MEMORY_MB = 256L
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun currentBootCount(): Int = try {
        Settings.Global.getInt(appContext.contentResolver, "boot_count", -1)
    } catch (_: RuntimeException) {
        -1
    }

    private fun thermalRank(status: String): Int = when (status) {
        "none" -> 0
        "light" -> 1
        "moderate" -> 2
        "severe" -> 3
        "critical" -> 4
        "emergency" -> 5
        "shutdown" -> 6
        else -> -1
    }

    @Synchronized
    fun recoverStaleSession() {
        if (!prefs.getBoolean("session_active", false)) return
        val priorStart = prefs.getLong("session_start_wall", System.currentTimeMillis())
        val priorDuration = (System.currentTimeMillis() - priorStart).coerceAtLeast(0L)
        val priorBootCount = prefs.getInt("session_boot_count", -1)
        val bootCountNow = currentBootCount()
        val rebootDetected = priorBootCount >= 0 && bootCountNow >= 0 &&
            priorBootCount != bootCountNow
        val status = if (rebootDetected) {
            "Interrupted — device reboot detected"
        } else {
            "Interrupted (no clean session shutdown; reboot not confirmed)"
        }

        prefs.edit()
            .putBoolean("session_active", false)
            .putString("last_status", status)
            .putLong("last_duration_ms", priorDuration)
            .putBoolean("last_reboot_detected", rebootDetected)
            .putInt("last_session_boot_count", priorBootCount)
            .putInt("last_observed_boot_count", bootCountNow)
            .apply()

        if (rebootDetected) {
            addEvent("DEVICE REBOOT DETECTED: boot count changed from " +
                priorBootCount + " to " + bootCountNow)
        } else {
            addEvent("Previous cloud gaming session did not close normally; " +
                "boot count did not confirm a device reboot")
        }
    }

    @Synchronized
    fun beginSession() {
        val nowWall = System.currentTimeMillis()
        val bootCount = currentBootCount()
        if (prefs.getBoolean("session_active", false)) {
            val priorStart = prefs.getLong("session_start_wall", nowWall)
            val priorDuration = (nowWall - priorStart).coerceAtLeast(0L)
            prefs.edit()
                .putString("last_status", "Interrupted (no clean session shutdown)")
                .putLong("last_duration_ms", priorDuration)
                .putBoolean("session_active", false)
                .apply()
            addEvent("Previous session ended without a clean shutdown")
        }

        prefs.edit()
            .putBoolean("session_active", true)
            .putLong("session_start_wall", nowWall)
            .putLong("session_start_elapsed", SystemClock.elapsedRealtime())
            .putInt("session_boot_count", bootCount)
            .putInt("sample_count", 0)
            .putFloat("max_app_pss_mb", 0f)
            .putFloat("max_dalvik_pss_mb", 0f)
            .putFloat("max_native_pss_mb", 0f)
            .putFloat("last_app_pss_mb", 0f)
            .putFloat("last_private_dirty_mb", 0f)
            .putLong("min_available_mb", Long.MAX_VALUE)
            .putBoolean("low_memory_reported", false)
            .putBoolean("low_available_reported", false)
            .putBoolean("warning_available_reported", false)
            .putBoolean("thermal_reported", false)
            .putString("last_thermal_status", "No samples yet")
            .putString("peak_thermal_status", "No samples yet")
            .putBoolean("last_reboot_detected", false)
            .putInt("last_session_boot_count", bootCount)
            .putInt("last_observed_boot_count", bootCount)
            .putInt("last_slow_frames", 0)
            .apply()
        addEvent("Cloud gaming session started; Android boot count=" + bootCount)
    }

    @Synchronized
    fun recordSample(
        appPssMb: Double,
        availableMemoryMb: Double,
        lowMemory: Boolean,
        thermalStatus: String,
        slowFrames: Int,
        dalvikPssMb: Double,
        nativePssMb: Double,
        privateDirtyMb: Double
    ) {
        val count = prefs.getInt("sample_count", 0) + 1
        val maxPss = maxOf(prefs.getFloat("max_app_pss_mb", 0f), appPssMb.toFloat())
        val maxDalvik = maxOf(prefs.getFloat("max_dalvik_pss_mb", 0f), dalvikPssMb.toFloat())
        val maxNative = maxOf(prefs.getFloat("max_native_pss_mb", 0f), nativePssMb.toFloat())
        val available = availableMemoryMb.toLong().coerceAtLeast(0L)
        val minAvailable = minOf(prefs.getLong("min_available_mb", Long.MAX_VALUE), available)
        val priorPeakThermal = prefs.getString("peak_thermal_status", "No samples yet").orEmpty()
        val peakThermal = if (thermalRank(thermalStatus) > thermalRank(priorPeakThermal)) {
            thermalStatus
        } else {
            priorPeakThermal
        }
        prefs.edit()
            .putInt("sample_count", count)
            .putFloat("max_app_pss_mb", maxPss)
            .putFloat("max_dalvik_pss_mb", maxDalvik)
            .putFloat("max_native_pss_mb", maxNative)
            .putFloat("last_app_pss_mb", appPssMb.toFloat())
            .putFloat("last_private_dirty_mb", privateDirtyMb.toFloat())
            .putLong("min_available_mb", minAvailable)
            .putInt("last_slow_frames", slowFrames)
            .putString("last_thermal_status", thermalStatus)
            .putString("peak_thermal_status", peakThermal)
            .apply()

        if (lowMemory && !prefs.getBoolean("low_memory_reported", false)) {
            addEvent("Android reported low memory during gameplay")
            prefs.edit().putBoolean("low_memory_reported", true).apply()
        } else if (!lowMemory) {
            prefs.edit().putBoolean("low_memory_reported", false).apply()
        }

        if (available < LOW_AVAILABLE_MEMORY_MB &&
            !prefs.getBoolean("low_available_reported", false)
        ) {
            addEvent("Critical memory pressure: available system RAM below 128 MB")
            prefs.edit().putBoolean("low_available_reported", true).apply()
        } else if (available >= LOW_AVAILABLE_MEMORY_MB) {
            prefs.edit().putBoolean("low_available_reported", false).apply()
        }

        if (available < WARNING_AVAILABLE_MEMORY_MB &&
            available >= LOW_AVAILABLE_MEMORY_MB &&
            !prefs.getBoolean("warning_available_reported", false)
        ) {
            addEvent("Memory pressure warning: available system RAM below 256 MB")
            prefs.edit().putBoolean("warning_available_reported", true).apply()
        } else if (available >= WARNING_AVAILABLE_MEMORY_MB) {
            prefs.edit().putBoolean("warning_available_reported", false).apply()
        }

        val hot = thermalStatus == "severe" || thermalStatus == "critical" ||
            thermalStatus == "emergency" || thermalStatus == "shutdown"
        if (hot && !prefs.getBoolean("thermal_reported", false)) {
            addEvent("Android thermal status reached " + thermalStatus)
            prefs.edit().putBoolean("thermal_reported", true).apply()
        } else if (!hot) {
            prefs.edit().putBoolean("thermal_reported", false).apply()
        }
    }

    @Synchronized
    fun recordEvent(message: String) {
        addEvent(message)
    }

    @Synchronized
    fun endSession() {
        if (!prefs.getBoolean("session_active", false)) return
        val start = prefs.getLong("session_start_elapsed", SystemClock.elapsedRealtime())
        val duration = (SystemClock.elapsedRealtime() - start).coerceAtLeast(0L)
        prefs.edit()
            .putBoolean("session_active", false)
            .putString("last_status", "Ended normally")
            .putLong("last_duration_ms", duration)
            .apply()
        addEvent("Cloud gaming session ended normally")
    }

    fun showReport(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Stability report — Phase 8.4")
            .setMessage(buildReport())
            .setPositiveButton("CLOSE", null)
            .show()
    }

    @Synchronized
    private fun addEvent(message: String) {
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val existing = prefs.getString("events", "").orEmpty()
        val events = existing.split("\n").filter { it.isNotBlank() }.toMutableList()
        events.add(stamp + "  " + message)
        while (events.size > MAX_EVENTS) events.removeAt(0)
        prefs.edit().putString("events", events.joinToString("\n")).apply()
    }

    @Synchronized
    private fun buildReport(): String {
        val active = prefs.getBoolean("session_active", false)
        val duration = if (active) {
            (SystemClock.elapsedRealtime() -
                prefs.getLong("session_start_elapsed", SystemClock.elapsedRealtime())).coerceAtLeast(0L)
        } else {
            prefs.getLong("last_duration_ms", 0L)
        }
        val status = if (active) "Session currently active" else
            prefs.getString("last_status", "No completed session recorded")
        val maxPss = prefs.getFloat("max_app_pss_mb", 0f)
        val maxDalvik = prefs.getFloat("max_dalvik_pss_mb", 0f)
        val maxNative = prefs.getFloat("max_native_pss_mb", 0f)
        val lastPss = prefs.getFloat("last_app_pss_mb", 0f)
        val privateDirty = prefs.getFloat("last_private_dirty_mb", 0f)
        val minAvailable = prefs.getLong("min_available_mb", Long.MAX_VALUE)
        val minAvailableText = if (minAvailable == Long.MAX_VALUE) "No samples yet" else minAvailable.toString() + " MB"
        val events = prefs.getString("events", "").orEmpty()
            .split("\n").filter { it.isNotBlank() }.takeLast(8)
        val eventText = if (events.isEmpty()) "No events recorded" else events.joinToString("\n")
        val sessionBootCount = prefs.getInt("last_session_boot_count",
            prefs.getInt("session_boot_count", -1))
        val observedBootCount = currentBootCount()
        val rebootDetected = prefs.getBoolean("last_reboot_detected", false)
        val rebootText = if (rebootDetected) {
            "YES — boot count changed (" + sessionBootCount + " → " + observedBootCount + ")"
        } else {
            "Not confirmed by Android boot count"
        }
        val thermalLast = prefs.getString("last_thermal_status", "No samples yet")
        val thermalPeak = prefs.getString("peak_thermal_status", "No samples yet")

        return "Status: " + status + "\n" +
            "Session duration: " + (duration / 60000L) + " min\n" +
            "Performance samples: " + prefs.getInt("sample_count", 0) + "\n" +
            "Device reboot detected: " + rebootText + "\n" +
            "Android boot count (session/current): " + sessionBootCount + " / " + observedBootCount + "\n" +
            "Thermal status (last / peak): " + thermalLast + " / " + thermalPeak + "\n" +
            "App memory now (PSS): " + String.format(Locale.US, "%.1f", lastPss) + " MB\n" +
            "Peak app memory (PSS): " + String.format(Locale.US, "%.1f", maxPss) + " MB\n" +
            "Peak Dalvik / Java memory: " + String.format(Locale.US, "%.1f", maxDalvik) + " MB\n" +
            "Peak native memory: " + String.format(Locale.US, "%.1f", maxNative) + " MB\n" +
            "Last private dirty memory: " + String.format(Locale.US, "%.1f", privateDirty) + " MB\n" +
            "Lowest available system memory: " + minAvailableText + "\n" +
            "Last slow-frame count: " + prefs.getInt("last_slow_frames", 0) + "\n\n" +
            "Recent events:\n" + eventText + "\n\n" +
            "Thermal status is Android's reported device status, not a direct temperature reading. " +
            "No caches are cleared and playback is not interrupted."
    }
}