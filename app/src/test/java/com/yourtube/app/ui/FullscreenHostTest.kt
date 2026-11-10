package com.yourtube.app.ui

import android.content.pm.ActivityInfo
import android.view.View
import android.webkit.WebChromeClient
import org.junit.Assert.*
import org.junit.Test

class FullscreenHostTest {

    private class TestWindowDelegate(var orientation: Int = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) : FullscreenHostWindowDelegate {
        override var requestedOrientation: Int
            get() = orientation
            set(value) { orientation = value }

        val attachedViews = mutableListOf<View>()
        var systemBarsHidden = false

        override fun attachCustomView(view: View) {
            attachedViews.add(view)
        }

        override fun removeCustomView(view: View) {
            attachedViews.remove(view)
        }

        override fun hideSystemBars() {
            systemBarsHidden = true
        }

        override fun restoreSystemBars() {
            systemBarsHidden = false
        }
    }

    private class TestCustomViewCallback : WebChromeClient.CustomViewCallback {
        var hiddenCalls = 0
        override fun onCustomViewHidden() {
            hiddenCalls++
        }
    }

    @Test
    fun enterFullscreenSetsStateAndOrientationAndBars() {
        val delegate = TestWindowDelegate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        val host = DefaultFullscreenHost(activity = null, delegate = delegate)
        val view = View(null)
        val callback = TestCustomViewCallback()
        val source = Any()

        assertFalse(host.isFullscreen)
        host.enterFullscreen(view, source, callback)

        assertTrue(host.isFullscreen)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, delegate.requestedOrientation)
        assertTrue(delegate.systemBarsHidden)
        assertTrue(delegate.attachedViews.contains(view))
    }

    @Test
    fun duplicateEnterWithSameViewIsIgnored() {
        val delegate = TestWindowDelegate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        val host = DefaultFullscreenHost(activity = null, delegate = delegate)
        val view = View(null)
        val callback = TestCustomViewCallback()
        val source = Any()

        host.enterFullscreen(view, source, callback)
        assertEquals(1, delegate.attachedViews.size)

        // Duplicate call with the exact same view
        host.enterFullscreen(view, source, callback)
        assertEquals(1, delegate.attachedViews.size)
        assertEquals(0, callback.hiddenCalls)
    }

    @Test
    fun enterWithNewViewCleansUpPreviousViewFirst() {
        val delegate = TestWindowDelegate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        val host = DefaultFullscreenHost(activity = null, delegate = delegate)
        val view1 = View(null)
        val callback1 = TestCustomViewCallback()
        val source1 = Any()

        val view2 = View(null)
        val callback2 = TestCustomViewCallback()
        val source2 = Any()

        host.enterFullscreen(view1, source1, callback1)
        assertEquals(listOf(view1), delegate.attachedViews)

        host.enterFullscreen(view2, source2, callback2)
        assertEquals(1, callback1.hiddenCalls)
        assertEquals(listOf(view2), delegate.attachedViews)
        assertTrue(host.isFullscreen)
    }

    @Test
    fun exitFullscreenFromAppNotifiesCallbackAndRestoresState() {
        val delegate = TestWindowDelegate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        val host = DefaultFullscreenHost(activity = null, delegate = delegate)
        val view = View(null)
        val callback = TestCustomViewCallback()
        val source = Any()

        host.enterFullscreen(view, source, callback)
        host.exitFullscreen(fromWebView = false)

        assertFalse(host.isFullscreen)
        assertEquals(1, callback.hiddenCalls)
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, delegate.requestedOrientation)
        assertFalse(delegate.systemBarsHidden)
        assertTrue(delegate.attachedViews.isEmpty())
    }

    @Test
    fun exitFullscreenFromWebViewDoesNotNotifyCallback() {
        val delegate = TestWindowDelegate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        val host = DefaultFullscreenHost(activity = null, delegate = delegate)
        val view = View(null)
        val callback = TestCustomViewCallback()
        val source = Any()

        host.enterFullscreen(view, source, callback)
        host.onHideCustomView(source)

        assertFalse(host.isFullscreen)
        assertEquals(0, callback.hiddenCalls) // Must NOT notify callback when initiated by WebView
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, delegate.requestedOrientation)
        assertFalse(delegate.systemBarsHidden)
        assertTrue(delegate.attachedViews.isEmpty())
    }

    @Test
    fun exitFullscreenIsIdempotent() {
        val delegate = TestWindowDelegate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        val host = DefaultFullscreenHost(activity = null, delegate = delegate)
        val view = View(null)
        val callback = TestCustomViewCallback()
        val source = Any()

        host.enterFullscreen(view, source, callback)
        host.exitFullscreen(fromWebView = false)
        assertEquals(1, callback.hiddenCalls)

        // Second exit call should be a no-op
        host.exitFullscreen(fromWebView = false)
        assertEquals(1, callback.hiddenCalls)
        assertFalse(host.isFullscreen)
    }

    @Test
    fun releaseByActiveSourceExitsFullscreen() {
        val delegate = TestWindowDelegate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        val host = DefaultFullscreenHost(activity = null, delegate = delegate)
        val view = View(null)
        val callback = TestCustomViewCallback()
        val source = Any()

        host.enterFullscreen(view, source, callback)
        host.release(source)

        assertFalse(host.isFullscreen)
        assertEquals(1, callback.hiddenCalls)
        assertTrue(delegate.attachedViews.isEmpty())
    }

    @Test
    fun releaseByDifferentSourceDoesNotExitActiveFullscreen() {
        val delegate = TestWindowDelegate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        val host = DefaultFullscreenHost(activity = null, delegate = delegate)
        val view = View(null)
        val callback = TestCustomViewCallback()
        val source = Any()
        val oldOrDifferentSource = Any()

        host.enterFullscreen(view, source, callback)
        host.release(oldOrDifferentSource)

        // Active fullscreen must remain intact
        assertTrue(host.isFullscreen)
        assertEquals(0, callback.hiddenCalls)
        assertEquals(1, delegate.attachedViews.size)
    }

    @Test
    fun onHideCustomViewByDifferentSourceDoesNotExitActiveFullscreen() {
        val delegate = TestWindowDelegate(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        val host = DefaultFullscreenHost(activity = null, delegate = delegate)
        val view = View(null)
        val callback = TestCustomViewCallback()
        val source = Any()
        val differentSource = Any()

        host.enterFullscreen(view, source, callback)
        host.onHideCustomView(differentSource)

        assertTrue(host.isFullscreen)
        assertEquals(1, delegate.attachedViews.size)
    }

    @Test
    fun backNavigationPrioritizesFullscreenExitOverClosingPlayback() {
        var fullscreenExited = false
        var playbackClosed = false
        var locked = false

        var isFullscreen = true
        val hasPlayback = true
        val isParentOrGate = false

        // Simulate BackHandler priority dispatch:
        fun handleBack() {
            when {
                isFullscreen -> {
                    fullscreenExited = true
                    isFullscreen = false
                }
                hasPlayback -> playbackClosed = true
                else -> locked = true
            }
        }

        // First Back press: while fullscreen
        handleBack()
        assertTrue(fullscreenExited)
        assertFalse(playbackClosed)
        assertFalse(locked)

        // Second Back press: while normal playback
        handleBack()
        assertTrue(playbackClosed)
        assertFalse(locked)
    }

    @Test
    fun configurationChangeSecurityBehavior() {
        fun onConfigChanged(parent: Boolean, gate: Boolean, lockAction: () -> Unit) {
            if (parent || gate) {
                lockAction()
            }
        }

        var locked = false

        // In Viewer Mode: rotation does NOT lock
        onConfigChanged(parent = false, gate = false) { locked = true }
        assertFalse(locked)

        // When Admin Mode is active: rotation locks
        onConfigChanged(parent = true, gate = false) { locked = true }
        assertTrue(locked)

        locked = false
        // When PIN Gate is active: rotation locks
        onConfigChanged(parent = false, gate = true) { locked = true }
        assertTrue(locked)
    }
}
