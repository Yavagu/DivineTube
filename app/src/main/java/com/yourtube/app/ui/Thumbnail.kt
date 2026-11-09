package com.yourtube.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

@Composable
fun Thumbnail(videoId: String?, modifier: Modifier = Modifier) {
    val bitmap = produceState<Bitmap?>(null, videoId) {
        if (videoId == null || !Regex("[A-Za-z0-9_-]{11}").matches(videoId)) return@produceState
        value = withContext(Dispatchers.IO) {
            val connection = URL("https://i.ytimg.com/vi/$videoId/mqdefault.jpg").openConnection() as HttpsURLConnection
            try {
                connection.connectTimeout = 5000; connection.readTimeout = 5000
                connection.instanceFollowRedirects = false
                if (connection.responseCode == 200) connection.inputStream.use { BitmapFactory.decodeStream(it) } else null
            } catch (_: java.io.IOException) { null }
            finally { connection.disconnect() }
        }
    }
    Box(modifier.background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        bitmap.value?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop) }
            ?: Text("YouTube", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
