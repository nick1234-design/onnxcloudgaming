package com.nick1234.onnxcloudgaming

import android.app.Activity
import android.app.ActivityManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.HandlerThread
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
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
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
    private lateinit var stabilityDiagnostics: StabilityDiagnostics
    private var web: WebView? = null
    private var rendererRecoveryAttempted = false
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // Lightweight performance diagnostics. Sampling runs only while this activity is visible.
    private var diagnosticsThread: HandlerThread? = null
    private var diagnosticsHandler: Handler? = null
    private var previousCpuTimeMs: Long? = null
    private var previousSampleTimeMs: Long? = null
    private var frameMetricsHandler: Handler? = null
    private var frameMetricsListener: Window.OnFrameMetricsAvailableListener? = null
    private val frameLock = Any()
    private var measuredFrames = 0
    private var slowFrames = 0
    private var maxFrameDurationMs = 0.0
    private var totalGpuDurationNs = 0L
    private var maxGpuDurationMs = 0.0
    private var totalDrawDurationNs = 0L
    private var maxDrawDurationMs = 0.0
    private var totalSyncDurationNs = 0L
    private var maxSyncDurationMs = 0.0

    private data class FrameStats(
        val frames: Int,
        val slowFrames: Int,
        val maxTotalMs: Double,
        val averageGpuMs: Double,
        val maxGpuMs: Double,
        val averageDrawMs: Double,
        val maxDrawMs: Double,
        val averageSyncMs: Double,
        val maxSyncMs: Double
    )

    private val performanceSampler = object : Runnable {
        override fun run() {
            val handler = diagnosticsHandler ?: return
            logPerformanceSample()
            handler.postDelayed(this, PERFORMANCE_SAMPLE_INTERVAL_MS)
        }
    }

    @Suppress("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stabilityDiagnostics = StabilityDiagnostics(this)
        stabilityDiagnostics.beginSession()
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

        // Keep the renderer attached to this visible activity. When the app is backgrounded,
        // Android may lower its priority rather than competing with the foreground app.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WebView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_BOUND, true)
        }
        // Avoid allocating a pre-rasterized copy of off-screen WebView content.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            gameWebView.setOffscreenPreRaster(false)
        }

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

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                if (url.startsWith("https://www.xbox.com/")) {
                    view.evaluateJavascript(
                        StreamingSettings.buildInjectionScript(this@GameActivity),
                        null
                    )
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    Log.w(
                        TAG,
                        "Main-frame network load error: code=${error.errorCode}, " +
                            "description=${error.description}"
                    )
                }
                super.onReceivedError(view, request, error)
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
                stabilityDiagnostics.recordEvent(
                    "WebView renderer exited; didCrash=${detail.didCrash()}"
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
        web?.onResume()
        startPerformanceMonitoring()
        startNetworkMonitoring()
    }

    override fun onPause() {
        // Suspend WebView work while another app or the Android home screen is foregrounded.
        // Keep the WebView alive so returning to the game does not force a reload.
        web?.onPause()
        stopPerformanceMonitoring()
        stopNetworkMonitoring()
        super.onPause()
    }

    private fun startNetworkMonitoring() {
        if (networkCallback != null) return

        val manager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager = manager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.i(TAG, "Network available: $network")
            }

            override fun onLost(network: Network) {
                Log.w(TAG, "Network lost: $network")
                stabilityDiagnostics.recordEvent("Network lost during gameplay")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val internet = capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                val validated = capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                val transport = when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
                    else -> "other"
                }
                val metered = !capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                )
                Log.i(
                    TAG,
                    "Network capabilities: transport=$transport internet=$internet " +
                        "validated=$validated metered=$metered"
                )
            }

            override fun onUnavailable() {
                Log.w(TAG, "Network unavailable")
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                manager.registerDefaultNetworkCallback(callback)
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                manager.registerNetworkCallback(request, callback)
            }
            networkCallback = callback

            val activeNetwork = manager.activeNetwork
            val activeCapabilities = activeNetwork?.let {
                manager.getNetworkCapabilities(it)
            }
            if (activeNetwork == null || activeCapabilities == null) {
                Log.w(TAG, "Initial network state: no active network capabilities")
            } else {
                val internet = activeCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                val validated = activeCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )
                Log.i(
                    TAG,
                    "Initial network state: internet=$internet validated=$validated"
                )
            }
            Log.i(TAG, "Network monitoring started")
        } catch (exception: RuntimeException) {
            Log.e(TAG, "Unable to register network monitoring callback", exception)
        }
    }

    private fun stopNetworkMonitoring() {
        val callback = networkCallback ?: return
        try {
            connectivityManager?.unregisterNetworkCallback(callback)
        } catch (exception: RuntimeException) {
            Log.w(TAG, "Unable to unregister network monitoring callback", exception)
        }
        networkCallback = null
        connectivityManager = null
        Log.i(TAG, "Network monitoring stopped")
    }

    private fun startPerformanceMonitoring() {
        previousCpuTimeMs = Process.getElapsedCpuTime()
        previousSampleTimeMs = SystemClock.elapsedRealtime()
        if (diagnosticsThread != null) return
        val thread = HandlerThread("ONNXCloudGaming-Diagnostics").apply { start() }
        diagnosticsThread = thread
        val metricsHandler = Handler(thread.looper)
        diagnosticsHandler = metricsHandler
        metricsHandler.postDelayed(performanceSampler, PERFORMANCE_SAMPLE_INTERVAL_MS)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            frameMetricsHandler = metricsHandler

            val refreshRate = window.decorView.display?.refreshRate?.takeIf { it > 0f } ?: 60f
            val slowFrameThresholdNs = (2_000_000_000.0 / refreshRate).toLong()

            val listener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
                val durationNs = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
                val gpuDurationNs = metrics.getMetric(FrameMetrics.GPU_DURATION)
                val drawDurationNs = metrics.getMetric(FrameMetrics.DRAW_DURATION)
                val syncDurationNs = metrics.getMetric(FrameMetrics.SYNC_DURATION)
                synchronized(frameLock) {
                    measuredFrames++
                    if (durationNs > slowFrameThresholdNs) slowFrames++
                    val durationMs = durationNs / 1_000_000.0
                    if (durationMs > maxFrameDurationMs) maxFrameDurationMs = durationMs

                    totalGpuDurationNs += gpuDurationNs
                    val gpuDurationMs = gpuDurationNs / 1_000_000.0
                    if (gpuDurationMs > maxGpuDurationMs) maxGpuDurationMs = gpuDurationMs

                    totalDrawDurationNs += drawDurationNs
                    val drawDurationMs = drawDurationNs / 1_000_000.0
                    if (drawDurationMs > maxDrawDurationMs) maxDrawDurationMs = drawDurationMs

                    totalSyncDurationNs += syncDurationNs
                    val syncDurationMs = syncDurationNs / 1_000_000.0
                    if (syncDurationMs > maxSyncDurationMs) maxSyncDurationMs = syncDurationMs
                }
            }
            frameMetricsListener = listener
            window.addOnFrameMetricsAvailableListener(listener, metricsHandler)
        }

        Log.i(TAG, "Performance monitoring started; samples every 15 seconds")
    }

    private fun stopPerformanceMonitoring() {
        diagnosticsHandler?.removeCallbacks(performanceSampler)
        frameMetricsListener?.let { listener ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                window.removeOnFrameMetricsAvailableListener(listener)
            }
        }
        frameMetricsListener = null
        frameMetricsHandler = null
        diagnosticsThread?.quitSafely()
        diagnosticsThread = null
        diagnosticsHandler = null
        previousCpuTimeMs = null
        previousSampleTimeMs = null
        synchronized(frameLock) {
            resetFrameMetrics()
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
            val frameCount = measuredFrames
            val divisor = frameCount.coerceAtLeast(1).toDouble()
            val stats = FrameStats(
                frames = frameCount,
                slowFrames = slowFrames,
                maxTotalMs = maxFrameDurationMs,
                averageGpuMs = totalGpuDurationNs / divisor / 1_000_000.0,
                maxGpuMs = maxGpuDurationMs,
                averageDrawMs = totalDrawDurationNs / divisor / 1_000_000.0,
                maxDrawMs = maxDrawDurationMs,
                averageSyncMs = totalSyncDurationNs / divisor / 1_000_000.0,
                maxSyncMs = maxSyncDurationMs
            )
            resetFrameMetrics()
            stats
        }

        stabilityDiagnostics.recordSample(
            appPssMb = appMemory.totalPss / 1024.0,
            availableMemoryMb = systemMemory.availMem / (1024.0 * 1024.0),
            lowMemory = systemMemory.lowMemory,
            thermalStatus = thermal,
            slowFrames = frameStats.slowFrames
        )

        Log.i(
            TAG,
            String.format(
                Locale.US,
                "PERF cpu_one_core_pct=%.1f app_pss_mb=%.1f system_avail_mb=%.1f " +
                    "system_total_mb=%.1f low_memory=%s thermal=%s frames=%d slow_frames=%d " +
                    "max_frame_ms=%.1f gpu_avg_ms=%.2f gpu_max_ms=%.2f " +
                    "draw_avg_ms=%.2f draw_max_ms=%.2f sync_avg_ms=%.2f sync_max_ms=%.2f",
                cpuPercent,
                appMemory.totalPss / 1024.0,
                systemMemory.availMem / (1024.0 * 1024.0),
                systemMemory.totalMem / (1024.0 * 1024.0),
                systemMemory.lowMemory,
                thermal,
                frameStats.frames,
                frameStats.slowFrames,
                frameStats.maxTotalMs,
                frameStats.averageGpuMs,
                frameStats.maxGpuMs,
                frameStats.averageDrawMs,
                frameStats.maxDrawMs,
                frameStats.averageSyncMs,
                frameStats.maxSyncMs
            )
        )
    }

    private fun resetFrameMetrics() {
        measuredFrames = 0
        slowFrames = 0
        maxFrameDurationMs = 0.0
        totalGpuDurationNs = 0L
        maxGpuDurationMs = 0.0
        totalDrawDurationNs = 0L
        maxDrawDurationMs = 0.0
        totalSyncDurationNs = 0L
        maxSyncDurationMs = 0.0
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
        stabilityDiagnostics.recordEvent("Android memory trim requested: level=$level")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.e(TAG, "Android reported low memory while cloud gaming")
        stabilityDiagnostics.recordEvent("Android onLowMemory callback received")
    }

    override fun onDestroy() {
        stabilityDiagnostics.endSession()
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
