package com.yourtube.app.data.youtube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

data class YoutubeItem(val id: String, val title: String, val isPlaylist: Boolean)
data class SearchPage(val items: List<YoutubeItem>, val nextPageToken: String?)
data class VideoStatus(val title: String, val madeForKids: Boolean, val embeddable: Boolean)

class YoutubeApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

fun interface YoutubeHttpTransport {
    suspend fun get(url: String, headers: Map<String, String>): String
}

private class UrlConnectionYoutubeTransport : YoutubeHttpTransport {
    override suspend fun get(url: String, headers: Map<String, String>): String = withContext(Dispatchers.IO) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            requestMethod = "GET"
            instanceFollowRedirects = false
            headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
        try {
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.let { input -> InputStreamReader(input, StandardCharsets.UTF_8).use { reader ->
                    val result = StringBuilder()
                    val buffer = CharArray(8 * 1024)
                    while (true) {
                        val count = reader.read(buffer)
                        if (count < 0) break
                        if (result.length + count > YoutubeApi.MAX_RESPONSE_CHARS) {
                            throw YoutubeApiException("YouTube returned an unexpectedly large response.")
                        }
                        result.append(buffer, 0, count)
                    }
                    result.toString()
                } }.orEmpty()
            if (code !in 200..299) {
                val apiReason = runCatching { JSONObject(body).getJSONObject("error").getJSONArray("errors").optJSONObject(0)?.optString("reason") }.getOrNull()
                val message = when (apiReason) {
                    "quotaExceeded", "dailyLimitExceeded" -> "YouTube search quota is exhausted. Try again later."
                    "keyInvalid", "accessNotConfigured", "ipRefererBlocked" -> "YouTube API key is missing, invalid, or restricted incorrectly. Check setup instructions."
                    else -> "YouTube request failed (HTTP $code). Check your connection and try again."
                }
                throw YoutubeApiException(message)
            }
            body
        } catch (e: YoutubeApiException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Do not preserve network exception messages or URLs: they can contain the API key.
            throw YoutubeApiException("Could not connect to YouTube. Check your connection and try again.")
        } finally {
            connection.disconnect()
        }
    }
}

/** Small Data API v3 client. Supply an Android-restricted key; never log request URLs. */
class YoutubeApi(
    private val apiKey: String,
    private val packageName: String,
    private val certificateSha1: String,
    private val transport: YoutubeHttpTransport = UrlConnectionYoutubeTransport(),
) {
    private val headers get() = mapOf("X-Android-Package" to packageName, "X-Android-Cert" to certificateSha1)

    suspend fun search(query: String, playlists: Boolean = false, pageToken: String? = null): SearchPage {
        if (query.isBlank()) return SearchPage(emptyList(), null)
        val type = if (playlists) "playlist" else "video"
        val json = request("search", mapOf("part" to "snippet", "type" to type, "maxResults" to "25", "q" to query.trim(), "pageToken" to pageToken))
        val items = json.getJSONArray("items").let { array ->
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val idObject = item.optJSONObject("id") ?: return@mapNotNull null
                val id = idObject.optString(if (playlists) "playlistId" else "videoId")
                val title = item.optJSONObject("snippet")?.optString("title").orEmpty()
                val valid = if (playlists) isPlaylistId(id) else isVideoId(id)
                if (valid && title.isNotBlank()) YoutubeItem(id, title, playlists) else null
            }
        }
        return SearchPage(items, json.optString("nextPageToken").takeIf(String::isNotBlank))
    }

    /** Must be called successfully before embedding each video. Unknown MFK is a failure. */
    suspend fun videoStatus(id: String): VideoStatus {
        requireVideoId(id)
        val json = request("videos", mapOf("part" to "snippet,status", "id" to id))
        val item = json.optJSONArray("items")?.optJSONObject(0)
            ?: throw YoutubeApiException("This video is unavailable or no longer public.")
        val status = item.optJSONObject("status") ?: throw YoutubeApiException("YouTube did not provide this video's playback status.")
        if (!status.has("madeForKids") || status.isNull("madeForKids")) {
            throw YoutubeApiException("YouTube did not provide this video's Made for Kids status, so it cannot be played here yet.")
        }
        val title = item.optJSONObject("snippet")?.optString("title").orEmpty()
        if (!status.has("embeddable") || status.isNull("embeddable")) {
            throw YoutubeApiException("YouTube did not provide this video's embed status.")
        }
        return VideoStatus(title, status.getBoolean("madeForKids"), status.getBoolean("embeddable"))
    }

    /** Retrieves a bounded reviewed playlist snapshot; partial snapshots are rejected. */
    suspend fun playlistVideos(id: String): List<YoutubeItem> {
        if (!isPlaylistId(id)) throw YoutubeApiException("Invalid YouTube playlist link.")
        val result = mutableListOf<YoutubeItem>()
        var token: String? = null
        val seenTokens = mutableSetOf<String>()
        do {
            val json = request("playlistItems", mapOf("part" to "snippet,contentDetails", "playlistId" to id, "maxResults" to "50", "pageToken" to token))
            val array = json.optJSONArray("items") ?: throw YoutubeApiException("YouTube returned an incomplete playlist response.")
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: throw YoutubeApiException("YouTube returned an incomplete playlist item.")
                val videoId = item.optJSONObject("contentDetails")?.optString("videoId").orEmpty()
                val title = item.optJSONObject("snippet")?.optString("title").orEmpty()
                if (!isVideoId(videoId) || title.isBlank() || title.equals("Private video", true) || title.equals("Deleted video", true)) {
                    throw YoutubeApiException("This playlist contains a private or unavailable video. Review it in YouTube before importing.")
                }
                result += YoutubeItem(videoId, title, false)
                if (result.size > MAX_PLAYLIST_ITEMS) throw YoutubeApiException("This playlist has more than $MAX_PLAYLIST_ITEMS items and is too large to review at once.")
            }
            token = json.optString("nextPageToken").takeIf(String::isNotBlank)
            if (token != null && !seenTokens.add(token)) {
                throw YoutubeApiException("YouTube returned a repeating playlist page. The playlist could not be imported safely.")
            }
            if (token != null && array.length() == 0) {
                throw YoutubeApiException("YouTube returned an incomplete playlist page. The playlist could not be imported safely.")
            }
            if (token != null && result.size >= MAX_PLAYLIST_ITEMS) {
                throw YoutubeApiException("This playlist has more than $MAX_PLAYLIST_ITEMS items and cannot be imported as a complete snapshot.")
            }
        } while (token != null)
        return result
    }

    private suspend fun request(endpoint: String, parameters: Map<String, String?>): JSONObject {
        if (apiKey.isBlank()) throw YoutubeApiException("Add a YouTube Data API key in local.properties to search and verify video playback eligibility.")
        val query = (parameters + ("key" to apiKey)).filterValues { it != null }.entries.joinToString("&") {
            "${encode(it.key)}=${encode(it.value!!)}"
        }
        val body = try {
            transport.get("https://www.googleapis.com/youtube/v3/$endpoint?$query", headers)
        } catch (e: YoutubeApiException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Never attach transport exceptions because their messages may contain the URL/key.
            throw YoutubeApiException("Could not connect to YouTube. Check your connection and try again.")
        }
        return try { JSONObject(body) } catch (_: JSONException) {
            throw YoutubeApiException("YouTube returned an invalid response. Try again later.")
        }
    }

    private fun encode(value: String) = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    private fun requireVideoId(id: String) { if (!isVideoId(id)) throw YoutubeApiException("Invalid YouTube video link.") }

    companion object {
        const val MAX_PLAYLIST_ITEMS = 500
        const val MAX_RESPONSE_CHARS = 2 * 1024 * 1024
        fun isVideoId(value: String) = value.matches(Regex("^[A-Za-z0-9_-]{11}$"))
        fun isPlaylistId(value: String) = value.matches(Regex("^[A-Za-z0-9_-]{10,100}$"))
    }
}
