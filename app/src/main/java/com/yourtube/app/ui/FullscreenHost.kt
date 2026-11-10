package com.yourtube.app.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Host controller managing the native fullscreen view lifecycle for YouTube playback.
 */
interface FullscreenHost {
    val isFullscreen: Boolean
    fun enterFullscreen(view: View, source: Any, callback: WebChromeClient.CustomViewCallback)
    fun exitFullscreen(fromWebView: Boolean = false)
    fun onHideCustomView(source: Any)
    fun release(source: Any)
    fun destroy()
}

val LocalFullscreenHost = compositionLocalOf<FullscreenHost?> { null }

internal interface FullscreenHostWindowDelegate {
    var requestedOrientation: Int
    fun attachCustomView(view: View)
    fun removeCustomView(view: View)
    fun hideSystemBars()
    fun restoreSystemBars()
}

class DefaultFullscreenHost internal constructor(
    private var activity: Activity?,
    private val delegate: FullscreenHostWindowDelegate? = null
) : FullscreenHost {

    constructor(activity: Activity) : this(activity, null)

    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null
    private var fullscreenSource: Any? = null
    private var originalOrientation: Int? = null
    private var originalSystemBarsBehavior: Int? = null

    private val _isFullscreen = mutableStateOf(false)
    override val isFullscreen: Boolean get() = _isFullscreen.value

    override fun enterFullscreen(view: View, source: Any, callback: WebChromeClient.CustomViewCallback) {
        if (customView === view) return

        if (customView != null) {
            exitFullscreen(fromWebView = false)
        }

        if (delegate != null) {
            if (originalOrientation == null) {
                originalOrientation = delegate.requestedOrientation
            }
            delegate.attachCustomView(view)
            customView = view
            customViewCallback = callback
            fullscreenSource = source
            _isFullscreen.value = true
            delegate.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            delegate.hideSystemBars()
            return
        }

        val act = activity ?: return
        val window = act.window ?: return
        val decor = window.decorView as? ViewGroup ?: return

        if (originalOrientation == null) {
            originalOrientation = act.requestedOrientation
        }

        val insetsController = WindowCompat.getInsetsController(window, decor)
        if (originalSystemBarsBehavior == null) {
            originalSystemBarsBehavior = insetsController.systemBarsBehavior
        }

        (view.parent as? ViewGroup)?.removeView(view)
        view.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        view.setBackgroundColor(Color.BLACK)
        decor.addView(view)

        customView = view
        customViewCallback = callback
        fullscreenSource = source
        _isFullscreen.value = true

        try {
            act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } catch (_: Exception) {
            // Safe fallback if windowing mode prevents orientation change
        }

        try {
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    override fun exitFullscreen(fromWebView: Boolean) {
        if (customView == null && !_isFullscreen.value) return

        val viewToRemove = customView
        val callbackToNotify = customViewCallback

        customView = null
        customViewCallback = null
        fullscreenSource = null
        _isFullscreen.value = false

        if (delegate != null) {
            viewToRemove?.let { delegate.removeCustomView(it) }
            if (!fromWebView) {
                try {
                    callbackToNotify?.onCustomViewHidden()
                } catch (_: Exception) {}
            }
            originalOrientation?.let {
                delegate.requestedOrientation = it
                originalOrientation = null
            }
            delegate.restoreSystemBars()
            return
        }

        viewToRemove?.let { view ->
            (view.parent as? ViewGroup)?.removeView(view)
        }

        if (!fromWebView) {
            try {
                callbackToNotify?.onCustomViewHidden()
            } catch (_: Exception) {}
        }

        restoreOrientation()
        restoreSystemBars()
    }

    override fun onHideCustomView(source: Any) {
        if (fullscreenSource == source) {
            exitFullscreen(fromWebView = true)
        }
    }

    override fun release(source: Any) {
        if (fullscreenSource == source) {
            exitFullscreen(fromWebView = false)
        }
    }

    override fun destroy() {
        exitFullscreen(fromWebView = false)
        activity = null
    }

    private fun restoreOrientation() {
        val act = activity ?: return
        val original = originalOrientation ?: return
        originalOrientation = null
        try {
            act.requestedOrientation = original
        } catch (_: Exception) {}
    }

    private fun restoreSystemBars() {
        val act = activity ?: return
        val window = act.window ?: return
        val decor = window.decorView
        val insetsController = WindowCompat.getInsetsController(window, decor)
        try {
            originalSystemBarsBehavior?.let {
                insetsController.systemBarsBehavior = it
            }
            originalSystemBarsBehavior = null
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        } catch (_: Exception) {}
    }
}
