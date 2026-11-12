package com.nick1234.onnxcloudgaming

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val green = Color.rgb(16, 124, 16)
    private val dark = Color.rgb(12, 16, 20)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(44, 28, 44, 28)
            setBackgroundColor(dark)
        }

        val brand = TextView(this).apply {
            text = "XBOX  |  CLOUD GAMING"
            textSize = 17f
            letterSpacing = 0.18f
            setTextColor(Color.rgb(155, 220, 155))
            gravity = Gravity.CENTER
        }
        val title = TextView(this).apply {
            text = "onnxcloudgaming"
            textSize = 38f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 8)
        }
        val subtitle = TextView(this).apply {
            text = "Play your favorite games on the big screen."
            textSize = 17f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        val play = makeButton("PLAY NOW", green).apply {
            setOnClickListener { startActivity(Intent(this@MainActivity, GameActivity::class.java)) }
        }
        val browser = makeButton("OPEN IN CHROME", Color.rgb(45, 53, 62)).apply {
            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.xbox.com/play"))
                startActivity(intent)
            }
        }
        val note = TextView(this).apply {
            text = "A compatible browser, Microsoft account, supported region, and eligible Game Pass plan may be required."
            textSize = 12f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 0)
        }

        page.addView(brand)
        page.addView(title)
        page.addView(subtitle)
        page.addView(play, buttonParams())
        page.addView(browser, buttonParams())
        page.addView(note)
        setContentView(page)
        play.requestFocus()
    }

    private fun makeButton(label: String, color: Int) = Button(this).apply {
        text = label
        textSize = 17f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(color)
        minHeight = 60
        isFocusable = true
        isFocusableInTouchMode = true
    }

    private fun buttonParams() = LinearLayout.LayoutParams(340, 62).apply {
        gravity = Gravity.CENTER
        bottomMargin = 12
    }
}