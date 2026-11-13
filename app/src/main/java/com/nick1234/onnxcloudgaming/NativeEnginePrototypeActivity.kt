package com.nick1234.onnxcloudgaming

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Phase 8.1 hardware inspection screen.
 * It checks the native surface, active EGL renderer and Android-advertised codecs.
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
            textSize = 15f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            setLineSpacing(4f, 1f)
            setPadding(24, 20, 24, 20)
            gravity = Gravity.TOP or Gravity.START
            setBackgroundColor(0xE6080C0F.toInt())
            text = buildReport()
        }
        val textParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.TOP
        ).apply {
            leftMargin = 20
            topMargin = 20
            rightMargin = 20
            bottomMargin = 20
        }
        root.addView(statusText, textParams)
        setContentView(root)
        root.requestFocus()
    }

    private fun buildReport(): String {
        val surfaceLine = if (surfaceAvailable) {
            "SurfaceView: READY (${surfaceWidth}×${surfaceHeight})"
        } else {
            "SurfaceView: WAITING"
        }
        return "ONNX CLOUD GAMING — PHASE 8.1\n" +
            "HARDWARE & GRAPHICS DETECTION\n\n" +
            "$surfaceLine\n\n" +
            HardwareDiagnostics.buildReport(this) +
            "\n\nPress BACK to return to the dashboard."
    }

    private fun refreshReport() {
        if (::statusText.isInitialized) statusText.text = buildReport()
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
