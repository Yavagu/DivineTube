package com.yourtube.app.data.youtube

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

data class YoutubeTarget(val videoId: String? = null, val playlistId: String? = null) {
    init { require(videoId != null || playlistId != null) }
}

/** Parses an explicitly shared YouTube URL. It never treats arbitrary text as an ID. */
object YoutubeLink {
    private val videoIdPattern = Regex("^[A-Za-z0-9_-]{11}$")
    private val playlistIdPattern = Regex("^[A-Za-z0-9_-]{10,100}$")
    private val urlPattern = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)

    fun parse(text: String): YoutubeTarget? {
        val matches = urlPattern.findAll(text).toList()
        if (matches.size != 1) return null
        val raw = matches.single().value.trimEnd('.', ',', ';', '!', '?', ')', ']', '}')
        return parseUrl(raw)
    }

    private fun parseUrl(raw: String): YoutubeTarget? = runCatching {
        val uri = URI(raw)
        if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https")) return null
        if (uri.rawUserInfo != null || uri.port != -1 || uri.rawFragment != null) return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        if (host !in setOf("youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com", "youtu.be")) return null
        val path = uri.path ?: return null
        val params = parseQuery(uri.rawQuery ?: "") ?: return null
        val videoValues = params["v"].orEmpty()
        val listValues = params["list"].orEmpty()
        if (videoValues.size > 1 || listValues.size > 1) return null
        var videoId: String? = videoValues.singleOrNull()?.takeIf(videoIdPattern::matches)
        var playlistId: String? = listValues.singleOrNull()?.takeIf(playlistIdPattern::matches)
        if (videoValues.isNotEmpty() && videoId == null) return null
        if (listValues.isNotEmpty() && playlistId == null) return null

        if (host == "youtu.be") {
            if (path.count { it == '/' } != 1) return null
            val pathId = path.removePrefix("/")
            if (!videoIdPattern.matches(pathId)) return null
            if (videoId != null && videoId != pathId) return null
            videoId = pathId
        } else if (path == "/playlist") {
            if (playlistId == null || videoId != null) return null
        } else if (path == "/watch") {
            // A watch URL must identify a video. If it also names a playlist, keep the
            // direct video as the explicit user selection.
            if (videoId == null) return null
            playlistId = null
        } else {
            val parts = path.split('/').filter(String::isNotEmpty)
            if (parts.size != 2 || parts.first() !in setOf("shorts", "live", "embed")) return null
            val pathId = parts.last()
            if (!videoIdPattern.matches(pathId)) return null
            if (videoId != null && videoId != pathId) return null
            videoId = pathId
            playlistId = null
        }
        if (videoId == null && playlistId == null) return null
        YoutubeTarget(videoId, playlistId)
    }.getOrNull()

    private fun parseQuery(raw: String): Map<String, List<String>>? {
        if (raw.isBlank()) return emptyMap()
        val result = linkedMapOf<String, MutableList<String>>()
        for (part in raw.split('&')) {
            if (part.isEmpty()) continue
            val split = part.split('=', limit = 2)
            val key = URLDecoder.decode(split[0], StandardCharsets.UTF_8.name()).lowercase(Locale.ROOT)
            val value = URLDecoder.decode(split.getOrElse(1) { "" }, StandardCharsets.UTF_8.name())
            if (key in setOf("v", "list")) result.getOrPut(key) { mutableListOf() }.add(value)
        }
        return result
    }
}
