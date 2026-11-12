package com.nick1234.onnxcloudgaming

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.util.Log
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast

class GameActivity : Activity() {
    companion object {
        private const val TAG = "ONNXCloudGaming"
    }
    private lateinit var container: FrameLayout
    private var web: WebView? = null
    private var rendererRecoveryAttempted = false

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
        web?.let {
            it.stopLoading()
            container.removeView(it)
            it.destroy()
        }
        web = null
        super.onDestroy()
    }
}
