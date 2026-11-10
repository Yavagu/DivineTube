package com.yourtube.app.data.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object YoutubeTitleFetcher {
    suspend fun fetchTitle(videoId: String): String? = withContext(Dispatchers.IO) {
        if (!Regex("^[A-Za-z0-9_-]{11}$").matches(videoId)) return@withContext null
        try {
            val url = URL("https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                instanceFollowRedirects = false
            }
            try {
                if (conn.responseCode in 200..299) {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val title = JSONObject(text).optString("title")
                    if (title.isNotBlank()) title else null
                } else null
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }
}
