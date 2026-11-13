package com.nick1234.onnxcloudgaming

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persistent, lightweight stability recorder for long cloud-gaming sessions.
 * It records diagnostics only and does not change streaming behavior.
 */
class StabilityDiagnostics(context: Context) {
    companion object {
        private const val PREFS = "onnx_stability_diagnostics"
        private const val MAX_EVENTS = 24
        private const val LOW_AVAILABLE_MEMORY_MB = 128L
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun recoverStaleSession() {
        if (!prefs.getBoolean("session_active", false)) return
        val priorStart = prefs.getLong("session_start_wall", System.currentTimeMillis())
        val priorDuration = (System.currentTimeMillis() - priorStart).coerceAtLeast(0L)
        prefs.edit()
            .putBoolean("session_active", false)
            .putString("last_status", "Interrupted (no clean session shutdown)")
            .putLong("last_duration_ms", priorDuration)
            .apply()
        addEvent("Previous cloud gaming session did not close normally")
    }

    @Synchronized
    fun beginSession() {
        val nowWall = System.currentTimeMillis()
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
            .putInt("sample_count", 0)
            .putFloat("max_app_pss_mb", 0f)
            .putLong("min_available_mb", Long.MAX_VALUE)
            .putBoolean("low_memory_reported", false)
            .putBoolean("low_available_reported", false)
            .putBoolean("thermal_reported", false)
            .putInt("last_slow_frames", 0)
            .apply()
        addEvent("Cloud gaming session started")
    }

    @Synchronized
    fun recordSample(
        appPssMb: Double,
        availableMemoryMb: Double,
        lowMemory: Boolean,
        thermalStatus: String,
        slowFrames: Int
    ) {
        val count = prefs.getInt("sample_count", 0) + 1
        val maxPss = maxOf(prefs.getFloat("max_app_pss_mb", 0f), appPssMb.toFloat())
        val available = availableMemoryMb.toLong().coerceAtLeast(0L)
        val minAvailable = minOf(prefs.getLong("min_available_mb", Long.MAX_VALUE), available)
        prefs.edit()
            .putInt("sample_count", count)
            .putFloat("max_app_pss_mb", maxPss)
            .putLong("min_available_mb", minAvailable)
            .putInt("last_slow_frames", slowFrames)
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
            addEvent("Available system memory dropped below 128 MB")
            prefs.edit().putBoolean("low_available_reported", true).apply()
        } else if (available >= LOW_AVAILABLE_MEMORY_MB) {
            prefs.edit().putBoolean("low_available_reported", false).apply()
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
            .setTitle("Stability report")
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
        val minAvailable = prefs.getLong("min_available_mb", Long.MAX_VALUE)
        val minAvailableText = if (minAvailable == Long.MAX_VALUE) "No samples yet" else minAvailable.toString() + " MB"
        val events = prefs.getString("events", "").orEmpty()
            .split("\n").filter { it.isNotBlank() }.takeLast(8)
        val eventText = if (events.isEmpty()) "No events recorded" else events.joinToString("\n")
        return "Status: " + status + "\n" +
            "Session duration: " + (duration / 60000L) + " min\n" +
            "Performance samples: " + prefs.getInt("sample_count", 0) + "\n" +
            "Peak app memory (PSS): " + String.format(Locale.US, "%.1f", maxPss) + " MB\n" +
            "Lowest available system memory: " + minAvailableText + "\n" +
            "Last slow-frame count: " + prefs.getInt("last_slow_frames", 0) + "\n\n" +
            "Recent events:\n" + eventText + "\n\n" +
            "Note: an interrupted session means Android did not call the normal shutdown path; " +
            "it does not by itself identify the cause."
    }
}
