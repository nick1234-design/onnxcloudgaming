package com.nick1234.onnxcloudgaming

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val green = Color.rgb(16, 124, 16)
    private val brightGreen = Color.rgb(75, 220, 95)
    private val backgroundColor = Color.rgb(8, 12, 15)
    private val panel = Color.rgb(20, 27, 32)
    private val muted = Color.rgb(165, 177, 184)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 22)
            setBackgroundColor(backgroundColor)
            isFocusableInTouchMode = true
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = label("ONNX", 19f, brightGreen, true).apply { letterSpacing = 0.16f }
        val brandSuffix = label("CLOUD GAMING", 14f, Color.WHITE, true).apply {
            letterSpacing = 0.12f
            setPadding(10, 0, 0, 0)
        }
        header.addView(brand)
        header.addView(brandSuffix)
        val spacer = View(this)
        header.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
        val ready = label("●  READY TO PLAY", 12f, brightGreen, true).apply {
            setPadding(14, 8, 14, 8)
            background = rounded(Color.rgb(19, 49, 29), 24)
        }
        header.addView(ready)
        page.addView(header, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val title = label("Your games. Your screen.", 31f, Color.WHITE, true).apply {
            setPadding(0, 22, 0, 3)
        }
        page.addView(title)
        page.addView(label(
            "Choose how you want to play on your TV.", 15f, muted, false
        ))

        val cards = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 22, 0, 12)
        }
        val cloud = makeGameCard(
            "01  /  CLOUD GAMING",
            "Play from the cloud",
            "Jump into Xbox Cloud Gaming. Your game runs on Microsoft's servers.",
            "LAUNCH CLOUD GAMING",
            true
        ) {
            startActivity(Intent(this@MainActivity, GameActivity::class.java))
        }
        val remote = makeGameCard(
            "02  /  REMOTE PLAY",
            "Play from your Xbox",
            "Stream games from your own Xbox console over your home network.",
            "OPEN XBOX APP",
            false
        ) {
            openXboxApp()
        }
        cards.addView(cloud, LinearLayout.LayoutParams(0, 190, 1f).apply {
            rightMargin = 12
        })
        cards.addView(remote, LinearLayout.LayoutParams(0, 190, 1f).apply {
            leftMargin = 12
        })
        page.addView(cards, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val quickActions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val browser = actionButton("OPEN XBOX.COM/PLAY") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.xbox.com/play")))
        }
        val help = actionButton("CONNECTION & HELP") {
            showConnectionHelp()
        }
        val streaming = actionButton("STREAMING SETTINGS") {
            StreamingSettings.show(this@MainActivity)
        }
        quickActions.addView(browser, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, 48
        ).apply { rightMargin = 12 })
        quickActions.addView(help, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, 48
        ).apply { rightMargin = 12 })
        quickActions.addView(streaming, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, 48
        ))
        page.addView(quickActions)

        val footer = label(
            "BUILT FOR THE BIG SCREEN     •     USE YOUR CONTROLLER OR TV REMOTE",
            10f, Color.rgb(105, 119, 127), true
        ).apply { setPadding(0, 18, 0, 0); letterSpacing = 0.08f }
        page.addView(footer)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(backgroundColor)
            addView(page)
        }
        setContentView(scroll)
        cloud.requestFocus()
    }

    private fun makeGameCard(
        eyebrow: String,
        title: String,
        description: String,
        buttonText: String,
        featured: Boolean,
        onClick: () -> Unit
    ): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(22, 17, 22, 17)
            background = rounded(
                if (featured) Color.rgb(18, 39, 25) else panel,
                18,
                if (featured) Color.rgb(39, 125, 57) else Color.rgb(48, 60, 67)
            )
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            setOnClickListener { onClick() }
            setOnFocusChangeListener { view, focused ->
                view.background = rounded(
                    if (featured) {
                        if (focused) Color.rgb(25, 61, 34) else Color.rgb(18, 39, 25)
                    } else {
                        if (focused) Color.rgb(39, 49, 56) else panel
                    },
                    18,
                    if (focused) brightGreen else if (featured) green else Color.rgb(48, 60, 67),
                    if (focused) 3 else 1
                )
                view.scaleX = if (focused) 1.015f else 1f
                view.scaleY = if (focused) 1.015f else 1f
            }
        }
        card.addView(label(eyebrow, 11f, if (featured) brightGreen else muted, true))
        card.addView(label(title, 22f, Color.WHITE, true).apply {
            setPadding(0, 9, 0, 5)
        })
        card.addView(label(description, 12f, muted, false))
        val launch = label("▶   $buttonText", 12f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            setPadding(15, 10, 15, 10)
            background = rounded(if (featured) green else Color.rgb(49, 61, 69), 8)
        }
        card.addView(launch, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 13 })
        return card
    }

    private fun actionButton(text: String, onClick: () -> Unit) = TextView(this).apply {
        this.text = text
        textSize = 11f
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        setPadding(16, 0, 16, 0)
        background = rounded(Color.rgb(31, 40, 46), 8)
        isFocusable = true
        isClickable = true
        setOnFocusChangeListener { view, focused ->
            view.background = rounded(
                if (focused) Color.rgb(48, 67, 54) else Color.rgb(31, 40, 46),
                8, if (focused) brightGreen else Color.rgb(55, 68, 75), if (focused) 2 else 1
            )
        }
        setOnClickListener { onClick() }
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean) =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            typeface = Typeface.create(
                "sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL
            )
        }

    private fun rounded(color: Int, radius: Int, stroke: Int = Color.TRANSPARENT, width: Int = 1) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            if (stroke != Color.TRANSPARENT) setStroke(width, stroke)
        }

    private fun openXboxApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage("com.microsoft.xboxone.smartglass")
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(
                this,
                "Install the Xbox app to use Remote Play, then open it here.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showConnectionHelp() {
        AlertDialog.Builder(this)
            .setTitle("Gaming connection tips")
            .setMessage(
                "For the best experience:\n\n" +
                    "• Use 5 GHz Wi-Fi or Ethernet when available.\n" +
                    "• Keep your Onn box in an open, ventilated spot.\n" +
                    "• For Remote Play, connect your Xbox and Onn box to the same home network.\n" +
                    "• If a game freezes, return to the dashboard and relaunch it."
            )
            .setPositiveButton("GOT IT", null)
            .show()
    }
}
