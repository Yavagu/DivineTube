package com.yourtube.app.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
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
    var index by remember(playback) { mutableIntStateOf(playback.initialIndex.coerceIn(0, playback.ids.lastIndex.coerceAtLeast(0))) }
    var retry by remember { mutableIntStateOf(0) }
    var allowed by remember(playback, index, retry) { mutableStateOf(false) }
    var failure by remember(playback, index, retry) { mutableStateOf<String?>(null) }
    val id = playback.ids.getOrNull(index)
    fun chooseEpisode(episode: Int) {
        index = episode
        model.selectEpisode(episode)
    }
    LaunchedEffect(id, retry) {
        try {
            require(id != null) { "This saved item has no video." }
            val status = model.playbackStatus(id)
            require(status.embeddable) { "This video does not allow embedded playback." }
            // Both true and false are known MFK statuses; neither substitutes for administrator review.
            allowed = true
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { failure = e.message ?: "Video is unavailable. Check your connection." }
    }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = model::closePlayback, modifier = Modifier.heightIn(min = 52.dp)) { Text("Home") }
        Text(playback.title, style = MaterialTheme.typography.headlineSmall)
        if (playback.ids.size > 1) Text("Episode ${index + 1} of ${playback.ids.size}", style = MaterialTheme.typography.titleMedium)
        if (playback.preview) Text("Admin preview · not approved")
        if (allowed && id != null) key(id, retry) {
            val startSeconds = remember(id, retry) {
                if (playback.startSeconds != null && index == playback.initialIndex) {
                    playback.startSeconds
                } else {
                    model.savedPosition(playback.entryId, id)
                }
            }
            YoutubeEmbed(
                videoId = id,
                startSeconds = startSeconds,
                modifier = Modifier.fillMaxWidth().heightIn(min = 270.dp).aspectRatio(16f / 9f),
                onProgress = { pos, dur, completed ->
                    model.saveProgress(playback.entryId, id, pos, dur, completed)
                }
            ) { failure = it }
        } else if (failure == null) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text("Checking YouTube playback availability…") }
        failure?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Text("Check your internet connection, or try another saved video.")
            Button(onClick = { retry++ }) { Text("Try again") }
        }
        if (playback.ids.size > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { chooseEpisode(index - 1) }, enabled = index > 0, modifier = Modifier.heightIn(min = 52.dp)) { Text("Previous") }
            Text("${index + 1} / ${playback.ids.size}", Modifier.padding(top = 16.dp))
            Button(onClick = { chooseEpisode(index + 1) }, enabled = index < playback.ids.lastIndex, modifier = Modifier.heightIn(min = 52.dp)) { Text("Next") }
        }
        if (playback.ids.size > 1) {
            Text("Episodes", style = MaterialTheme.typography.headlineSmall)
            playback.ids.forEachIndexed { episode, _ ->
                OutlinedButton(onClick = { chooseEpisode(episode) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(if (episode == index) "Now playing · Episode ${episode + 1}" else "Episode ${episode + 1}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Text("YouTube may show recommendations, ads, and links beyond this library. DivineTube opens only approved content from Home.", style = MaterialTheme.typography.bodySmall)
    }
}

// WebView invokes annotated methods reflectively; the declaring class must be accessible.
internal class PlayerEvents(
    private val ready: () -> Unit,
    private val error: (String) -> Unit,
    private val progress: (positionSeconds: Long, durationSeconds: Long, completed: Boolean) -> Unit = { _, _, _ -> }
) {
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
    @JavascriptInterface fun onProgress(position: Double, duration: Double, completed: Boolean) {
        handler.post { progress(position.toLong(), duration.toLong(), completed) }
    }
    @JavascriptInterface fun onProgress(position: Long, duration: Long, completed: Boolean) {
        handler.post { progress(position, duration, completed) }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YoutubeEmbed(
    videoId: String,
    modifier: Modifier,
    startSeconds: Long = 0L,
    fullscreenHost: FullscreenHost? = LocalFullscreenHost.current,
    onProgress: (positionSeconds: Long, durationSeconds: Long, completed: Boolean) -> Unit = { _, _, _ -> },
    error: (String) -> Unit
) {
    val latestError by rememberUpdatedState(error)
    val latestProgress by rememberUpdatedState(onProgress)
    val base = "https://${BuildConfig.APPLICATION_ID}/"
    val startParam = if (startSeconds > 0) "&amp;start=$startSeconds" else ""
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(ready) {
        if (!ready) { delay(25_000); latestError("The YouTube player did not finish loading. Check your connection and try again.") }
    }
    Column {
    if (!ready) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text("Loading YouTube player…")
    }
    AndroidView(modifier = modifier, factory = { context ->
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            settings.mediaPlaybackRequiresUserGesture = true
            settings.setSupportMultipleWindows(false)
            settings.javaScriptCanOpenWindowsAutomatically = false
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    fullscreenHost?.enterFullscreen(view, this@apply, callback)
                }

                override fun onHideCustomView() {
                    fullscreenHost?.onHideCustomView(this@apply)
                }
            }
            addJavascriptInterface(
                PlayerEvents(
                    ready = { ready = true },
                    error = { latestError(it) },
                    progress = { pos, dur, comp -> latestProgress(pos, dur, comp) }
                ),
                "YourTubeEvents"
            )
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    // Do not launch arbitrary activities or navigate the app away from its local player document.
                    // Subframe YouTube navigation remains player-controlled, and cannot enforce an allowlist.
                    val uri = request.url
                    return if (request.isForMainFrame) uri.toString() != base else
                        uri.scheme != "https" || uri.host !in setOf("www.youtube.com", "www.youtube-nocookie.com", "youtube.com")
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, failure: WebResourceError) {
                    if (request.isForMainFrame || request.url.path?.startsWith("/embed/") == true)
                        latestError("Player could not load. Check your internet connection.")
                }
                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                    if (request.isForMainFrame || request.url.path?.startsWith("/embed/") == true)
                        latestError("YouTube player could not load (HTTP ${response.statusCode}). Try again later.")
                }
            }
            loadDataWithBaseURL(base, """
                <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;background:#000"><iframe id="player" width="100%" height="100%"
                src="https://www.youtube-nocookie.com/embed/$videoId?enablejsapi=1&amp;autoplay=0&amp;controls=1&amp;playsinline=1&amp;fs=1&amp;rel=0${startParam}&amp;origin=https%3A%2F%2F${BuildConfig.APPLICATION_ID}"
                frameborder="0" allow="encrypted-media; picture-in-picture; fullscreen" allowfullscreen referrerpolicy="strict-origin-when-cross-origin" title="YouTube video player"></iframe>
                <script>
                var ytPlayer;
                var progressTimer = null;
                function reportProgress(forceCompleted){
                    if(!ytPlayer||typeof ytPlayer.getCurrentTime!=='function')return;
                    try{
                        var pos=Math.floor(ytPlayer.getCurrentTime()||0);
                        var dur=Math.floor(ytPlayer.getDuration()||0);
                        var isEnded=forceCompleted||(dur>10&&pos>=dur-10);
                        YourTubeEvents.onProgress(pos,dur,isEnded);
                    }catch(e){}
                }
                function startTracking(){
                    if(progressTimer)clearInterval(progressTimer);
                    progressTimer=setInterval(function(){reportProgress(false);},5000);
                }
                function stopTracking(){
                    if(progressTimer){clearInterval(progressTimer);progressTimer=null;}
                }
                function onYouTubeIframeAPIReady(){
                    ytPlayer=new YT.Player('player',{
                        events:{
                            onReady:function(){YourTubeEvents.onReady();},
                            onStateChange:function(event){
                                if(event.data===1){
                                    startTracking();
                                }else if(event.data===2){
                                    stopTracking();
                                    reportProgress(false);
                                }else if(event.data===0){
                                    stopTracking();
                                    reportProgress(true);
                                }else{
                                    stopTracking();
                                }
                            },
                            onError:function(e){stopTracking();YourTubeEvents.onError(e.data);}
                        }
                    });
                }
                </script><script src="https://www.youtube.com/iframe_api"></script>
                <style>html,body,#player{width:100%;height:100%;margin:0;border:0;display:block;}html,body{overflow:hidden;}</style></body></html>
            """.trimIndent(), "text/html", "UTF-8", null)
        }
    }, onRelease = { view ->
        view.evaluateJavascript("reportProgress(false);", null)
        fullscreenHost?.release(view)
        view.stopLoading(); view.onPause(); view.removeJavascriptInterface("YourTubeEvents")
        view.loadUrl("about:blank"); view.removeAllViews(); view.destroy()
    })
    }
}
