package com.nick1234.onnxcloudgaming

import android.app.Activity
import android.app.ActivityManager
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.util.Log
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.FrameMetrics
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import java.util.Locale

class GameActivity : Activity() {
    companion object {
        private const val TAG = "ONNXCloudGaming"
        private const val PERFORMANCE_SAMPLE_INTERVAL_MS = 15_000L
    }

    private lateinit var container: FrameLayout
    private var web: WebView? = null
    private var rendererRecoveryAttempted = false

    // Lightweight performance diagnostics. Sampling runs only while this activity is visible.
    private val diagnosticsHandler = Handler(Looper.getMainLooper())
    private var previousCpuTimeMs: Long? = null
    private var previousSampleTimeMs: Long? = null
    private var frameMetricsThread: HandlerThread? = null
    private var frameMetricsHandler: Handler? = null
    private var frameMetricsListener: Window.OnFrameMetricsAvailableListener? = null
    private val frameLock = Any()
    private var measuredFrames = 0
    private var slowFrames = 0
    private var maxFrameDurationMs = 0.0

    private val performanceSampler = object : Runnable {
        override fun run() {
            if (isFinishing || isDestroyed) return
            logPerformanceSample()
            diagnosticsHandler.postDelayed(this, PERFORMANCE_SAMPLE_INTERVAL_MS)
        }
    }

    @Suppress("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )

        container = FrameLayout(this)
        setContentView(container)
        createGameWebView(loadXbox = true)
    }

    @Suppress("SetJavaScriptEnabled")
    private fun createGameWebView(loadXbox: Boolean) {
        val gameWebView = WebView(this)
        web = gameWebView

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(gameWebView, true)

        gameWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadsImagesAutomatically = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            userAgentString = WebSettings.getDefaultUserAgent(this@GameActivity)
        }

        gameWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return !(url.startsWith("https://") || url.startsWith("http://"))
            }

            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                Log.e(
                    TAG,
                    "WebView renderer exited: didCrash=${detail.didCrash()}, " +
                        "priorityAtExit=${detail.rendererPriorityAtExit()}"
                )
                if (isFinishing || isDestroyed) return true

                // Recover once per activity session; repeated renderer exits need diagnosis.
                if (!rendererRecoveryAttempted) {
                    rendererRecoveryAttempted = true
                    container.removeView(view)
                    view.destroy()
                    createGameWebView(loadXbox = true)
                    Toast.makeText(
                        this@GameActivity,
                        "The game browser recovered. Please try again.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this@GameActivity,
                        "The game browser stopped again. Exit and reopen the app.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return true
            }
        }

        gameWebView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                super.onShowCustomView(view, callback)
                view.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
            }
        }

        container.addView(
            gameWebView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        if (loadXbox) gameWebView.loadUrl("https://www.xbox.com/play")
    }

    override fun onResume() {
        super.onResume()
        startPerformanceMonitoring()
    }

    override fun onPause() {
        stopPerformanceMonitoring()
        super.onPause()
    }

    private fun startPerformanceMonitoring() {
        previousCpuTimeMs = Process.getElapsedCpuTime()
        previousSampleTimeMs = SystemClock.elapsedRealtime()
        diagnosticsHandler.removeCallbacks(performanceSampler)
        diagnosticsHandler.postDelayed(performanceSampler, PERFORMANCE_SAMPLE_INTERVAL_MS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val thread = HandlerThread("ONNXCloudGaming-FrameMetrics").apply { start() }
            frameMetricsThread = thread
            frameMetricsHandler = Handler(thread.looper)

            val refreshRate = window.decorView.display?.refreshRate?.takeIf { it > 0f } ?: 60f
            val slowFrameThresholdNs = (2_000_000_000.0 / refreshRate).toLong()

            val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
                val durationNs = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
                synchronized(frameLock) {
                    measuredFrames++
                    if (durationNs > slowFrameThresholdNs) slowFrames++
                    val durationMs = durationNs / 1_000_000.0
                    if (durationMs > maxFrameDurationMs) maxFrameDurationMs = durationMs
                }
            }
            frameMetricsListener = listener
            window.addOnFrameMetricsAvailableListener(listener, frameMetricsHandler)
        }

        Log.i(TAG, "Performance monitoring started; samples every 15 seconds")
    }

    private fun stopPerformanceMonitoring() {
        diagnosticsHandler.removeCallbacks(performanceSampler)
        frameMetricsListener?.let { listener ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                window.removeOnFrameMetricsAvailableListener(listener)
            }
        }
        frameMetricsListener = null
        frameMetricsHandler = null
        frameMetricsThread?.quitSafely()
        frameMetricsThread = null
        previousCpuTimeMs = null
        previousSampleTimeMs = null
        synchronized(frameLock) {
            measuredFrames = 0
            slowFrames = 0
            maxFrameDurationMs = 0.0
        }
    }

    private fun logPerformanceSample() {
        val nowMs = SystemClock.elapsedRealtime()
        val cpuNowMs = Process.getElapsedCpuTime()
        val priorCpuMs = previousCpuTimeMs
        val priorSampleMs = previousSampleTimeMs
        val cpuPercent = if (priorCpuMs != null && priorSampleMs != null && nowMs > priorSampleMs) {
            ((cpuNowMs - priorCpuMs).coerceAtLeast(0L).toDouble() /
                (nowMs - priorSampleMs).toDouble()) * 100.0
        } else {
            -1.0
        }
        previousCpuTimeMs = cpuNowMs
        previousSampleTimeMs = nowMs

        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val systemMemory = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(systemMemory)

        val appMemory = Debug.MemoryInfo()
        Debug.getMemoryInfo(appMemory)

        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            thermalStatusName(powerManager.currentThermalStatus)
        } else {
            "unsupported"
        }

        val frameStats = synchronized(frameLock) {
            val stats = Triple(measuredFrames, slowFrames, maxFrameDurationMs)
            measuredFrames = 0
            slowFrames = 0
            maxFrameDurationMs = 0.0
            stats
        }

        Log.i(
            TAG,
            String.format(
                Locale.US,
                "PERF cpu_one_core_pct=%.1f app_pss_mb=%.1f system_avail_mb=%.1f " +
                    "system_total_mb=%.1f low_memory=%s thermal=%s frames=%d slow_frames=%d " +
                    "max_frame_ms=%.1f",
                cpuPercent,
                appMemory.totalPss / 1024.0,
                systemMemory.availMem / (1024.0 * 1024.0),
                systemMemory.totalMem / (1024.0 * 1024.0),
                systemMemory.lowMemory,
                thermal,
                frameStats.first,
                frameStats.second,
                frameStats.third
            )
        )
    }

    private fun thermalStatusName(status: Int): String = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "none"
        PowerManager.THERMAL_STATUS_LIGHT -> "light"
        PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
        PowerManager.THERMAL_STATUS_SEVERE -> "severe"
        PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
        else -> "unknown_$status"
    }

    override fun onBackPressed() {
        val currentWebView = web
        if (currentWebView != null && currentWebView.canGoBack()) {
            currentWebView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.w(TAG, "GameActivity memory trim requested: level=$level")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.e(TAG, "Android reported low memory while cloud gaming")
    }

    override fun onDestroy() {
        stopPerformanceMonitoring()
        web?.let {
            it.stopLoading()
            container.removeView(it)
            it.destroy()
        }
        web = null
        super.onDestroy()
    }
}
