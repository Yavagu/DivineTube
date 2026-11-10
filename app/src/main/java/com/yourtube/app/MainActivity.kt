package com.yourtube.app

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import com.yourtube.app.ui.DefaultFullscreenHost
import com.yourtube.app.ui.FullscreenHost
import com.yourtube.app.ui.LocalFullscreenHost
import com.yourtube.app.ui.YourTubeApp

class MainActivity : ComponentActivity() {
    private val model: AppViewModel by viewModels()
    private val fullscreenHost: FullscreenHost by lazy { DefaultFullscreenHost(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent {
            CompositionLocalProvider(LocalFullscreenHost provides fullscreenHost) {
                YourTubeApp(model)
            }
        }
        if (savedInstanceState == null) receive(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Rotating while the PIN gate or Admin Mode is active invalidates parent authorization.
        if (model.state.value.parent || model.state.value.gate) {
            model.lock()
        }
    }

    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }

    private fun receive(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.let(model::importShared)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean { model.touch(); return super.dispatchTouchEvent(ev) }

    override fun onPause() {
        fullscreenHost.exitFullscreen()
        model.lock()
        super.onPause()
    }

    override fun onDestroy() {
        fullscreenHost.destroy()
        super.onDestroy()
    }
}
