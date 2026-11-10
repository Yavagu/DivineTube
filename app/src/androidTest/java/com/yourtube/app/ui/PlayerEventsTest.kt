package com.yourtube.app.ui

import android.annotation.SuppressLint
import android.os.Looper
import android.webkit.WebView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerEventsTest {
    @get:Rule val compose = createComposeRule()

    @SuppressLint("SetJavaScriptEnabled")
    @Test fun javascriptCanDeliverReadyAndErrorOnMainThread() {
        val callbacks = CountDownLatch(2)
        val ready = AtomicBoolean(false)
        val mainThread = AtomicBoolean(true)
        val error = AtomicReference<String>()
        val events = PlayerEvents({
            ready.set(true)
            if (Looper.myLooper() != Looper.getMainLooper()) mainThread.set(false)
            callbacks.countDown()
        }, {
            error.set(it)
            if (Looper.myLooper() != Looper.getMainLooper()) mainThread.set(false)
            callbacks.countDown()
        })
        compose.setContent {
            AndroidView(factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    addJavascriptInterface(events, "YourTubeEvents")
                    // Exercise Chromium's actual reflection bridge, without YouTube/network access.
                    loadDataWithBaseURL("https://com.yourtube.app/", """
                        <html><body><script>
                        YourTubeEvents.onReady();
                        YourTubeEvents.onError(153);
                        </script></body></html>
                    """.trimIndent(), "text/html", "UTF-8", null)
                }
            }, onRelease = { it.removeJavascriptInterface("YourTubeEvents"); it.destroy() })
        }
        assertTrue("WebView did not deliver both JavaScript callbacks", callbacks.await(15, TimeUnit.SECONDS))
        assertTrue(ready.get())
        assertTrue(mainThread.get())
        assertEquals("YouTube rejected the player identity. Check this build's package registration.", error.get())
    }
}
