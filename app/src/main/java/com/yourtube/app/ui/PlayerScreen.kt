package com.yourtube.app.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yourtube.app.AppViewModel
import com.yourtube.app.BuildConfig
import com.yourtube.app.Playback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(playback: Playback, model: AppViewModel, modifier: Modifier) {
    var index by remember(playback) { mutableIntStateOf(0) }
    var retry by remember { mutableIntStateOf(0) }
    var allowed by remember(playback, index, retry) { mutableStateOf(false) }
    var failure by remember(playback, index, retry) { mutableStateOf<String?>(null) }
    val id = playback.ids.getOrNull(index)
    LaunchedEffect(id, retry) {
        try {
            require(id != null) { "This saved item has no video." }
            val status = model.playbackStatus(id)
            require(status.embeddable) { "This video does not allow embedded playback." }
            // Both true and false are known MFK statuses; neither substitutes for a parent's review.
            allowed = true
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { failure = e.message ?: "Video is unavailable. Check your connection." }
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = model::closePlayback) { Text("Back to library") }
        Text(playback.title, style = MaterialTheme.typography.titleLarge)
        if (playback.preview) Text("Parent preview • not an approval")
        if (allowed && id != null) key(id, retry) {
            YoutubeEmbed(id, Modifier.fillMaxWidth().heightIn(min = 220.dp).aspectRatio(16f / 9f)) { failure = it }
        } else if (failure == null) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Checking YouTube playback availability…") }
        failure?.let {
            Text(if (model.state.value.parent) it else "This video can't be played right now. Please ask a parent for help.", color = MaterialTheme.colorScheme.error)
            Text("Check your internet connection, or try another saved video.")
            Button(onClick = { retry++ }) { Text("Try again") }
        }
        if (playback.ids.size > 1) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { index-- }, enabled = index > 0) { Text("Previous") }
            Text("${index + 1} / ${playback.ids.size}", Modifier.padding(top = 16.dp))
            OutlinedButton(onClick = { index++ }, enabled = index < playback.ids.lastIndex) { Text("Next") }
        }
        Text("YouTube may show recommendations, ads, and links beyond this library. Only this app's launch targets are parent-approved.", style = MaterialTheme.typography.bodySmall)
    }
}

private class PlayerEvents(private val ready: () -> Unit, private val error: (String) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    @JavascriptInterface fun onReady() { handler.post { ready() } }
    @JavascriptInterface fun onError(code: Int) {
        val message = when (code) {
            100 -> "This video has been removed, is private, or is unavailable."
            101, 150 -> "The video owner does not allow embedded playback."
            153 -> "YouTube rejected the player identity. Check this build's package registration."
            else -> "YouTube could not play this video (error $code)."
        }
        handler.post { error(message) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YoutubeEmbed(videoId: String, modifier: Modifier, error: (String) -> Unit) {
    val latestError by rememberUpdatedState(error)
    val base = "https://${BuildConfig.APPLICATION_ID}/"
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(ready) {
        if (!ready) { delay(25_000); latestError("The YouTube player did not finish loading. Check your connection and try again.") }
    }
    Column(modifier) {
    if (!ready) LinearProgressIndicator(Modifier.fillMaxWidth())
    AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { context ->
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = true
            settings.setSupportMultipleWindows(false)
            settings.javaScriptCanOpenWindowsAutomatically = false
            addJavascriptInterface(PlayerEvents({ ready = true }, { latestError(it) }), "YourTubeEvents")
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    // Do not launch arbitrary activities or navigate the app away from its local player document.
                    // Subframe YouTube navigation remains player-controlled, and cannot enforce an allowlist.
                    val uri = request.url
                    return if (request.isForMainFrame) uri.toString() != base else
                        uri.scheme != "https" || uri.host !in setOf("www.youtube.com", "www.youtube-nocookie.com", "youtube.com")
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                    if (request.isForMainFrame) latestError("Player could not load. Check your internet connection.")
                }
            }
            loadDataWithBaseURL(base, """
                <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;background:#000"><iframe id="player" width="100%" height="100%"
                src="https://www.youtube-nocookie.com/embed/$videoId?enablejsapi=1&amp;autoplay=0&amp;controls=1&amp;playsinline=1&amp;fs=0&amp;rel=0&amp;origin=https%3A%2F%2F${BuildConfig.APPLICATION_ID}"
                frameborder="0" allow="encrypted-media; picture-in-picture" referrerpolicy="strict-origin-when-cross-origin" title="YouTube video player"></iframe>
                <script src="https://www.youtube.com/iframe_api"></script><script>
                function onYouTubeIframeAPIReady(){new YT.Player('player',{
                events:{onReady:function(){YourTubeEvents.onReady();},onError:function(e){YourTubeEvents.onError(e.data);}}});}
                </script><style>html,body,#player{width:100%;height:100%;}</style></body></html>
            """.trimIndent(), "text/html", "UTF-8", null)
        }
    }, onRelease = { view ->
        view.stopLoading(); view.onPause(); view.removeJavascriptInterface("YourTubeEvents")
        view.loadUrl("about:blank"); view.removeAllViews(); view.destroy()
    })
    }
}
