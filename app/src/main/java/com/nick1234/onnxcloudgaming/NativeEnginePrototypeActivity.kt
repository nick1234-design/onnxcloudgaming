package com.nick1234.onnxcloudgaming

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaCodecList
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.TextView

/**
 * First native-engine research milestone.
 *
 * This screen verifies that Android can create a native SurfaceView and reports
 * available AVC decoder capabilities. It intentionally does not connect to Xbox
 * or claim to decode a live stream yet.
 */
class NativeEnginePrototypeActivity : Activity(), SurfaceHolder.Callback {
    private lateinit var statusText: TextView
    private var surfaceAvailable = false
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            isFocusableInTouchMode = true
        }

        val surface = SurfaceView(this).apply {
            holder.addCallback(this@NativeEnginePrototypeActivity)
            setBackgroundColor(Color.BLACK)
        }
        root.addView(
            surface,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        statusText = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 17f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setLineSpacing(5f, 1f)
            setPadding(28, 24, 28, 24)
            gravity = Gravity.TOP or Gravity.START
            setBackgroundColor(0xCC080C0F.toInt())
            text = buildReport()
        }
        val textParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.TOP
        ).apply {
            leftMargin = 28
            topMargin = 28
            rightMargin = 28
        }
        root.addView(statusText, textParams)
        setContentView(root)
        root.requestFocus()
    }

    private fun buildReport(): String {
        val decoderReport = try {
            val decoders = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
                .filter { info ->
                    !info.isEncoder && info.supportedTypes.any {
                        it.equals("video/avc", ignoreCase = true)
                    }
                }

            if (decoders.isEmpty()) {
                "H.264 decoder: No decoder advertised"
            } else {
                decoders.take(8).joinToString(
                    separator = "\n",
                    prefix = "H.264 decoder candidates:\n"
                ) { info ->
                    val hardware = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (info.isHardwareAccelerated) "hardware"
                        else if (info.isSoftwareOnly) "software"
                        else "not classified"
                    } else {
                        "hardware status unavailable on this Android version"
                    }
                    val capability = try {
                        val video = info.getCapabilitiesForType("video/avc").videoCapabilities
                        val supports720 = video.areSizeAndRateSupported(1280, 720, 60.0)
                        val supports1080 = video.areSizeAndRateSupported(1920, 1080, 60.0)
                        "720p60=$supports720, 1080p60=$supports1080"
                    } catch (_: Exception) {
                        "resolution capability query unavailable"
                    }
                    "• ${info.name} ($hardware; $capability)"
                }
            }
        } catch (error: Exception) {
            "Decoder query failed: ${error.javaClass.simpleName}: ${error.message}"
        }

        return "ONNX CLOUD GAMING — NATIVE ENGINE LAB\n\n" +
            "Milestone 1: Android native surface + decoder capability check\n\n" +
            "SurfaceView: ${if (surfaceAvailable) "READY" else "WAITING"}" +
            if (surfaceAvailable) " (${surfaceWidth}×${surfaceHeight})\n\n"
            else "\n\n" +
            decoderReport + "\n\n" +
            "IMPORTANT: This is a diagnostics prototype only. It does not start an " +
            "Xbox session or decode a live game stream yet. Your existing WebView " +
            "Cloud Gaming path is unchanged.\n\n" +
            "Press BACK to return to the dashboard."
    }

    private fun refreshReport() {
        if (::statusText.isInitialized) {
            statusText.text = buildReport()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceAvailable = holder.surface.isValid
        refreshReport()
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        surfaceAvailable = holder.surface.isValid
        surfaceWidth = width
        surfaceHeight = height
        refreshReport()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceAvailable = false
        surfaceWidth = 0
        surfaceHeight = 0
        refreshReport()
    }
}
