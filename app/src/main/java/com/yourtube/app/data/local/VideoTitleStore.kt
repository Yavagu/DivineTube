package com.yourtube.app.data.local

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

class VideoTitleStore(context: Context) {
    private val prefs = context.getSharedPreferences("video_titles", Context.MODE_PRIVATE)
    private val cache = ConcurrentHashMap<String, String>()

    init {
        prefs.all.forEach { (key, value) ->
            if (value is String && key.isNotBlank() && value.isNotBlank()) {
                cache[key] = value
            }
        }
    }

    fun getTitle(videoId: String): String? = cache[videoId]

    fun saveTitle(videoId: String, title: String) {
        if (videoId.isBlank() || title.isBlank()) return
        cache[videoId] = title
        prefs.edit().putString(videoId, title).apply()
    }

    fun saveTitles(titles: Map<String, String>) {
        if (titles.isEmpty()) return
        cache.putAll(titles)
        val editor = prefs.edit()
        titles.forEach { (k, v) ->
            if (k.isNotBlank() && v.isNotBlank()) editor.putString(k, v)
        }
        editor.apply()
    }
}
