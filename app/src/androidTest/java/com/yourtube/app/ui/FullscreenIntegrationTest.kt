package com.yourtube.app.ui

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yourtube.app.MainActivity
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullscreenIntegrationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private class TestCallback : WebChromeClient.CustomViewCallback {
        var hiddenCount = 0
        override fun onCustomViewHidden() {
            hiddenCount++
        }
    }

    @Test
    fun nativeFullscreenAttachesToDecorAndSetsLandscapeAndCleansUp() {
        val activity = compose.activity
        val host = DefaultFullscreenHost(activity)
        val testView = View(activity)
        val callback = TestCallback()
        val source = Any()

        val initialOrientation = activity.requestedOrientation

        // Enter fullscreen on main thread
        compose.runOnUiThread {
            host.enterFullscreen(testView, source, callback)
        }
        compose.waitForIdle()

        assertTrue(host.isFullscreen)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, activity.requestedOrientation)

        // Verify view is attached to decorView with MATCH_PARENT and black background
        val decor = activity.window.decorView as ViewGroup
        assertTrue("Test view must be added to decorView", testView.parent === decor)
        val lp = testView.layoutParams
        assertNotNull(lp)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, lp.width)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, lp.height)
        val bg = testView.background as? ColorDrawable
        assertNotNull("Background should be ColorDrawable", bg)
        assertEquals(Color.BLACK, bg?.color)

        // Exit fullscreen via app (e.g. Back button)
        compose.runOnUiThread {
            host.exitFullscreen(fromWebView = false)
        }
        compose.waitForIdle()

        assertFalse(host.isFullscreen)
        assertNull("Test view must be removed from decorView", testView.parent)
        assertEquals(1, callback.hiddenCount)
        assertEquals(initialOrientation, activity.requestedOrientation)
    }

    @Test
    fun webViewInitiatedExitDoesNotCallCustomViewHidden() {
        val activity = compose.activity
        val host = DefaultFullscreenHost(activity)
        val testView = View(activity)
        val callback = TestCallback()
        val source = Any()

        compose.runOnUiThread {
            host.enterFullscreen(testView, source, callback)
        }
        compose.waitForIdle()
        assertTrue(host.isFullscreen)

        // Exit via WebView's onHideCustomView
        compose.runOnUiThread {
            host.onHideCustomView(source)
        }
        compose.waitForIdle()

        assertFalse(host.isFullscreen)
        assertNull("Test view must be removed", testView.parent)
        assertEquals("Callback must not be invoked when exit was initiated by WebView", 0, callback.hiddenCount)
    }

    @Test
    fun releaseByActiveSourceCleansUpFullscreen() {
        val activity = compose.activity
        val host = DefaultFullscreenHost(activity)
        val testView = View(activity)
        val callback = TestCallback()
        val source = Any()

        compose.runOnUiThread {
            host.enterFullscreen(testView, source, callback)
        }
        compose.waitForIdle()
        assertTrue(host.isFullscreen)

        compose.runOnUiThread {
            host.release(source)
        }
        compose.waitForIdle()

        assertFalse(host.isFullscreen)
        assertNull(testView.parent)
        assertEquals(1, callback.hiddenCount)
    }
}
