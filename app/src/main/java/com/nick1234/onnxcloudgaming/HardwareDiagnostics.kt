package com.nick1234.onnxcloudgaming

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecList
import android.os.Build
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import java.util.Locale

/**
 * Read-only hardware capability inspection for Phase 8.1.
 * It does not change Android graphics-driver settings or streaming behavior.
 */
object HardwareDiagnostics {
    fun buildReport(context: Context): String {
        val memory = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .getMemoryInfo(memory)

        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Build.SOC_MANUFACTURER, Build.SOC_MODEL)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { "Not reported by Android" }
        } else {
            "Not exposed by this Android API level"
        }

        return buildString {
            appendLine("DEVICE")
            appendLine("Model: ${Build.MODEL}")
            appendLine("Device: ${Build.DEVICE}")
            appendLine("Board: ${Build.BOARD}")
            appendLine("Hardware: ${Build.HARDWARE}")
            appendLine("SoC: $soc")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("CPU architecture: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine("Available CPU cores: ${Runtime.getRuntime().availableProcessors()}")
            appendLine(
                "Installed RAM: ${String.format(Locale.US, "%.2f", memory.totalMem / 1073741824.0)} GB"
            )
            appendLine()
            appendLine("GRAPHICS DRIVER (EGL)")
            appendLine(readGpuInfo())
            appendLine()
            appendLine("VIDEO DECODER CAPABILITIES")
            appendLine(readCodecInfo())
            appendLine()
            appendLine("NOTE")
            appendLine("These are capabilities advertised by Android and the active graphics stack.")
            appendLine("They do not prove that a particular stream uses hardware decoding.")
            appendLine("No GPU driver settings have been changed.")
        }
    }

    private fun readGpuInfo(): String {
        var display: EGLDisplay = EGL14.EGL_NO_DISPLAY
        var context: EGLContext = EGL14.EGL_NO_CONTEXT
        var surface: EGLSurface = EGL14.EGL_NO_SURFACE
        var initialized = false
        try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return "EGL display unavailable"

            val versions = IntArray(2)
            if (!EGL14.eglInitialize(display, versions, 0, versions, 1)) {
                return "EGL initialization unavailable"
            }
            initialized = true

            val configAttributes = intArrayOf(
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val count = IntArray(1)
            if (!EGL14.eglChooseConfig(
                    display, configAttributes, 0, configs, 0, configs.size, count, 0
                ) || count[0] == 0
            ) return "No compatible EGL configuration"

            val contextAttributes = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            context = EGL14.eglCreateContext(
                display, configs[0], EGL14.EGL_NO_CONTEXT, contextAttributes, 0
            )
            if (context == EGL14.EGL_NO_CONTEXT) return "OpenGL ES context unavailable"

            val surfaceAttributes = intArrayOf(
                EGL14.EGL_WIDTH, 1,
                EGL14.EGL_HEIGHT, 1,
                EGL14.EGL_NONE
            )
            surface = EGL14.eglCreatePbufferSurface(
                display, configs[0], surfaceAttributes, 0
            )
            if (surface == EGL14.EGL_NO_SURFACE) return "EGL test surface unavailable"

            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
                return "Could not activate EGL test context"
            }

            val vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "Unknown"
            val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown"
            val version = GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown"
            val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS).orEmpty()
            val es3 = extensions.contains("GL_OES") // Extension presence only; not an ES 3 test.
            return "Vendor: $vendor\nRenderer: $renderer\nOpenGL ES: $version"
        } catch (error: Exception) {
            return "GPU query failed: ${error.javaClass.simpleName}: ${error.message}"
        } finally {
            if (display != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(
                    display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT
                )
                if (surface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(display, surface)
                }
                if (context != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(display, context)
                }
                if (initialized) EGL14.eglTerminate(display)
            }
        }
    }

    private fun readCodecInfo(): String {
        val mimeTypes = listOf(
            "video/avc" to "H.264 / AVC",
            "video/hevc" to "H.265 / HEVC",
            "video/x-vnd.on2.vp9" to "VP9",
            "video/av01" to "AV1"
        )
        return try {
            val codecInfos = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
            mimeTypes.joinToString("\n\n") { (mime, label) ->
                val matches = codecInfos.filter { info ->
                    !info.isEncoder && info.supportedTypes.any {
                        it.equals(mime, ignoreCase = true)
                    }
                }
                if (matches.isEmpty()) {
                    "$label: No decoder advertised"
                } else {
                    val lines = matches.take(6).map { info ->
                        val acceleration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            when {
                                info.isHardwareAccelerated -> "hardware-accelerated"
                                info.isSoftwareOnly -> "software-only"
                                else -> "not classified"
                            }
                        } else {
                            "acceleration classification unavailable"
                        }
                        val sizes = try {
                            val caps = info.getCapabilitiesForType(mime).videoCapabilities
                            val p720 = caps.areSizeAndRateSupported(1280, 720, 60.0)
                            val p1080 = caps.areSizeAndRateSupported(1920, 1080, 60.0)
                            "720p60=$p720, 1080p60=$p1080"
                        } catch (_: Exception) {
                            "size/rate details unavailable"
                        }
                        "• ${info.name} ($acceleration; $sizes)"
                    }
                    "$label:\n" + lines.joinToString("\n")
                }
            }
        } catch (error: Exception) {
            "Codec query failed: ${error.javaClass.simpleName}: ${error.message}"
        }
    }
}
