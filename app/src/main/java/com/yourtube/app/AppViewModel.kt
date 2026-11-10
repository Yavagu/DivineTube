package com.yourtube.app

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yourtube.app.data.local.ApprovedEntry
import com.yourtube.app.data.youtube.YoutubeItem
import com.yourtube.app.data.youtube.YoutubeLink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Playback(
    val ids: List<String>,
    val title: String,
    val preview: Boolean,
    val entryId: Long? = null,
    val initialIndex: Int = 0,
    val startSeconds: Long? = null,
)
data class ResumeCandidate(
    val entry: ApprovedEntry,
    val videoId: String,
    val episodeIndex: Int,
    val totalEpisodes: Int,
    val positionSeconds: Long,
    val durationSeconds: Long
)
data class FeedItem(
    val entryId: Long,
    val videoId: String,
    val title: String,
    val isPlaylistEpisode: Boolean = false,
    val playlistTitle: String? = null,
    val episodeIndex: Int? = null,
    val totalEpisodes: Int? = null,
    val categoryId: Long? = null,
)
data class Review(val title: String, val ids: List<String>, val playlistId: String? = null)
data class AppState(
    val ready: Boolean = false, val configured: Boolean = false,
    val parent: Boolean = false, val gate: Boolean = false, val busy: Boolean = false,
    val message: String? = null, val importText: String = "", val results: List<YoutubeItem> = emptyList(),
    val nextPage: String? = null, val review: Review? = null, val playback: Playback? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as YourTubeApplication
    val categories = app.library.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val entries = app.library.entries.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val histories = app.library.watchHistory.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val progresses = app.library.playbackProgress.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    private val itemOrderMap = mutableMapOf<Pair<Long, String>, Int>()
    private val feedRandom = java.util.Random()

    val homeFeed = entries.map { entryList ->
        val items = mutableListOf<FeedItem>()
        val seen = mutableSetOf<Pair<Long, String>>()
        for (entry in entryList) {
            val ids = entry.videoIds.split(',').map { it.trim() }.filter { it.isNotBlank() }
            if (entry.playlistId == null) {
                val vId = ids.firstOrNull() ?: continue
                val key = Pair(entry.id, vId)
                if (seen.add(key)) {
                    items.add(
                        FeedItem(
                            entryId = entry.id,
                            videoId = vId,
                            title = entry.title,
                            isPlaylistEpisode = false,
                            playlistTitle = null,
                            episodeIndex = null,
                            totalEpisodes = null,
                            categoryId = entry.categoryId
                        )
                    )
                }
            } else {
                val uniqueIds = ids.distinct()
                uniqueIds.forEach { vId ->
                    val key = Pair(entry.id, vId)
                    if (seen.add(key)) {
                        val originalIndex = ids.indexOf(vId)
                        items.add(
                            FeedItem(
                                entryId = entry.id,
                                videoId = vId,
                                title = if (ids.size > 1) "${entry.title} — Episode ${originalIndex + 1}" else entry.title,
                                isPlaylistEpisode = true,
                                playlistTitle = entry.title,
                                episodeIndex = originalIndex,
                                totalEpisodes = ids.size,
                                categoryId = entry.categoryId
                            )
                        )
                    }
                }
            }
        }
        items.sortedBy { item ->
            val key = Pair(item.entryId, item.videoId)
            synchronized(itemOrderMap) {
                itemOrderMap.getOrPut(key) { feedRandom.nextInt() }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val mutable = MutableStateFlow(AppState())
    val state = mutable.asStateFlow()
    private val _resumePromptDismissed = MutableStateFlow(false)
    fun dismissResumePrompt() { _resumePromptDismissed.value = true }

    val resumeCandidate = combine(
        entries,
        progresses,
        state,
        _resumePromptDismissed
    ) { entriesList, progressList, appState, dismissed ->
        if (dismissed || appState.parent || appState.gate || appState.playback != null) {
            null
        } else {
            progressList.asSequence()
                .filter { !it.completed && it.positionSeconds >= 30 && (it.durationSeconds - it.positionSeconds >= 30) }
                .mapNotNull { prog ->
                    val entry = entriesList.firstOrNull { it.id == prog.entryId } ?: return@mapNotNull null
                    val ids = entry.videoIds.split(',')
                    val index = ids.indexOf(prog.videoId)
                    if (index < 0) return@mapNotNull null
                    ResumeCandidate(
                        entry = entry,
                        videoId = prog.videoId,
                        episodeIndex = index,
                        totalEpisodes = ids.size,
                        positionSeconds = prog.positionSeconds,
                        durationSeconds = prog.durationSeconds
                    )
                }
                .firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private var epoch = 0L
    private var lastTouch = SystemClock.elapsedRealtime()
    private var work: Job? = null
    private var searchQuery = ""
    private var searchPlaylists = false
    init {
        viewModelScope.launch {
            try { mutable.value = mutable.value.copy(ready = true, configured = app.pins.isConfigured()) }
            catch (_: Exception) { mutable.value = mutable.value.copy(message = "Secure storage could not be opened. Restart the app; access remains locked.") }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if ((state.value.parent || state.value.gate) && SystemClock.elapsedRealtime() - lastTouch >= 120_000) lock()
            }
        }
    }
    fun touch() { lastTouch = SystemClock.elapsedRealtime() }
    fun showGate() { if (state.value.ready) { touch(); mutable.value = state.value.copy(gate = true, message = null) } }
    fun lock() {
        epoch++
        work?.cancel()
        mutable.value = state.value.copy(parent = false, gate = false, busy = false, results = emptyList(),
            review = null, playback = null, message = null, nextPage = null)
    }
    fun importShared(text: String) {
        lock()
        // Untrusted imports never approve anything or bypass authentication.
        mutable.value = state.value.copy(importText = text.take(4096), gate = true)
        touch()
    }
    fun authenticate(pin: String, confirmation: String, consent: Boolean) {
        if (!state.value.gate || state.value.busy || !state.value.ready) return
        val token = epoch
        mutable.value = state.value.copy(busy = true, message = null)
        work = viewModelScope.launch {
            try {
                val configured = app.pins.isConfigured()
                val success = if (!configured) {
                    require(consent) { "Please read and accept the privacy and playback notice." }
                    require(pin == confirmation) { "PINs do not match." }
                    app.pins.setup(pin)
                    true
                } else {
                    val result = app.pins.verify(pin)
                    if (!result.success) throw IllegalArgumentException(
                        if (result.retryAfterSeconds > 0) "Try again in ${result.retryAfterSeconds} seconds." else "Incorrect PIN.")
                    true
                }
                if (token == epoch && success) {
                    touch()
                    mutable.value = state.value.copy(configured = true, parent = true, gate = false, busy = false)
                    refreshTrustedPlaylists()
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (token == epoch) mutable.value = state.value.copy(busy = false, message = e.message ?: "PIN verification failed.") }
        }
    }
    private fun parentWork(action: suspend () -> Unit) {
        if (!state.value.parent || state.value.busy) return
        touch()
        val token = epoch
        mutable.value = state.value.copy(busy = true, message = null)
        work = viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (epoch == token) mutable.value = state.value.copy(message = e.message ?: "Operation failed. Try again.") }
            finally { if (epoch == token) mutable.value = state.value.copy(busy = false) }
        }
    }
    fun message(text: String?) { mutable.value = state.value.copy(message = text) }
    fun clearReview() { mutable.value = state.value.copy(review = null) }
    fun labelReview(title: String) {
        if (state.value.parent && !state.value.busy) mutable.value = state.value.copy(review = state.value.review?.copy(title = title.take(120)))
    }
    fun search(query: String, playlists: Boolean, more: Boolean = false) = parentWork {
        require(query.isNotBlank()) { "Enter a search term." }
        val token = epoch
        val continuation = if (more && query == searchQuery && playlists == searchPlaylists) state.value.nextPage else null
        val page = app.youtube.search(query.trim(), playlists, continuation)
        if (token == epoch) {
            searchQuery = query; searchPlaylists = playlists
            mutable.value = state.value.copy(results = if (continuation != null) (state.value.results + page.items).distinctBy { it.id } else page.items,
                nextPage = page.nextPageToken, message = if (page.items.isEmpty()) "No results found." else null)
        }
    }
    fun prepareLink(text: String, title: String, manualVideos: String = "") = parentWork {
        val parsed = YoutubeLink.parse(text) ?: error("Use a valid YouTube video or playlist link.")
        val token = epoch
        val review = if (parsed.videoId != null) Review(title.ifBlank { "Video ${parsed.videoId}" }, listOf(parsed.videoId))
        else {
            val playlistId = parsed.playlistId ?: error("No playlist ID.")
            val items = if (manualVideos.isNotBlank()) manualVideos.lines().filter { it.isNotBlank() }.map {
                YoutubeLink.parse(it)?.videoId ?: error("Each snapshot line must be a valid video link.")
            }.distinct() else app.youtube.playlistVideos(playlistId).map { it.id }.distinct()
            require(items.isNotEmpty()) { "Playlist contains no available videos." }
            Review(title.ifBlank { "Playlist $playlistId" }, items, playlistId)
        }
        if (token == epoch) mutable.value = state.value.copy(review = review, importText = "")
    }
    fun reviewResult(item: YoutubeItem) = prepareLink(
        if (item.isPlaylist) "https://www.youtube.com/playlist?list=${item.id}" else "https://youtu.be/${item.id}", "")
    fun approve(categoryId: Long?) = parentWork {
        val r = state.value.review ?: return@parentWork
        app.library.approve(r.title, r.ids, r.playlistId, categoryId)
        mutable.value = state.value.copy(review = null, message = "Approved and saved on this device.")
    }
    fun createCategory(name: String) = parentWork { app.library.createCategory(name.trim()) }
    fun renameCategory(id: Long, name: String) = parentWork { app.library.renameCategory(id, name.trim()) }
    fun deleteCategory(id: Long) = parentWork { app.library.deleteCategory(id) }
    fun moveCategory(id: Long, delta: Int) = parentWork { app.library.moveCategory(id, delta) }
    fun removeEntry(id: Long) = parentWork { app.library.removeEntry(id) }
    fun moveEntry(id: Long, delta: Int) = parentWork { app.library.moveEntry(id, delta) }
    fun assignCategory(id: Long, categoryId: Long?) = parentWork { app.library.assignCategory(id, categoryId) }
    fun setFeatured(id: Long) = parentWork { app.library.setFeatured(id) }
    fun setRefreshMode(id: Long, mode: String) = parentWork { app.library.setRefreshMode(id, mode) }
    fun refreshPlaylist(id: Long) = parentWork {
        val entry = entries.value.firstOrNull { it.id == id } ?: error("Approved content was not found.")
        val playlistId = entry.playlistId ?: error("Only playlists can be refreshed.")
        val items = app.youtube.playlistVideos(playlistId)
        if (entry.refreshMode == "AUTOMATIC") {
            app.library.refreshPlaylistSnapshot(id, items.map { it.id })
            mutable.value = state.value.copy(message = "Trusted playlist updated from YouTube.")
        } else {
            mutable.value = state.value.copy(
                review = Review(entry.title, items.map { it.id }, playlistId),
                message = "Review the refreshed playlist before approving it.",
            )
        }
    }
    fun play(entry: ApprovedEntry) {
        // Re-read from the approved stream; caller-provided IDs are never accepted as Viewer launch targets.
        val approved = entries.value.firstOrNull { it.id == entry.id } ?: return
        val ids = approved.videoIds.split(',')
        val history = histories.value.firstOrNull { it.entryId == approved.id }
        val savedIndex = history?.videoId?.let(ids::indexOf)?.takeIf { it >= 0 }
            ?: history?.episodeIndex?.coerceIn(0, ids.lastIndex)
            ?: 0
        val targetVideoId = ids.getOrNull(savedIndex)
        val startSeconds = savedPosition(approved.id, targetVideoId)
        mutable.value = state.value.copy(
            playback = Playback(ids, approved.title, false, approved.id, savedIndex, startSeconds)
        )
        playerEpisodeOpened(savedIndex)
    }
    fun preview(id: String, title: String) {
        if (state.value.parent && Regex("[A-Za-z0-9_-]{11}").matches(id))
            mutable.value = state.value.copy(playback = Playback(listOf(id), title, true))
    }
    fun closePlayback() { mutable.value = state.value.copy(playback = null) }
    fun selectEpisode(index: Int) {
        val playback = state.value.playback ?: return
        if (index !in playback.ids.indices) return
        val targetVideoId = playback.ids.getOrNull(index)
        val startSeconds = savedPosition(playback.entryId, targetVideoId)
        mutable.value = state.value.copy(playback = playback.copy(initialIndex = index, startSeconds = startSeconds))
        playerEpisodeOpened(index)
    }
    fun savedPosition(entryId: Long?, videoId: String?): Long {
        if (entryId == null || videoId == null) return 0L
        val prog = progresses.value.firstOrNull { it.entryId == entryId && it.videoId == videoId } ?: return 0L
        return if (!prog.completed) prog.positionSeconds else 0L
    }
    fun saveProgress(entryId: Long?, videoId: String?, positionSeconds: Long, durationSeconds: Long, completed: Boolean) {
        if (entryId == null || videoId == null || videoId.isBlank() || positionSeconds < 0) return
        val isCompleted = completed || (durationSeconds > 10 && positionSeconds >= durationSeconds - 10)
        viewModelScope.launch {
            app.library.savePlaybackProgress(
                entryId = entryId,
                videoId = videoId,
                positionSeconds = if (isCompleted) 0L else positionSeconds,
                durationSeconds = durationSeconds,
                completed = isCompleted
            )
        }
    }
    fun clearProgress(entryId: Long, videoId: String) {
        viewModelScope.launch {
            app.library.clearPlaybackProgress(entryId, videoId)
        }
    }
    fun resumePlayback(entryId: Long, videoId: String, startOver: Boolean = false) {
        val approved = entries.value.firstOrNull { it.id == entryId } ?: return
        val ids = approved.videoIds.split(',')
        val index = ids.indexOf(videoId)
        if (index < 0) return
        val startSeconds = if (startOver) {
            clearProgress(entryId, videoId)
            0L
        } else {
            savedPosition(entryId, videoId)
        }
        mutable.value = state.value.copy(
            playback = Playback(
                ids = ids,
                title = approved.title,
                preview = false,
                entryId = approved.id,
                initialIndex = index,
                startSeconds = startSeconds
            )
        )
        playerEpisodeOpened(index)
    }
    fun playFeedItem(item: FeedItem) {
        resumePlayback(item.entryId, item.videoId, startOver = false)
    }
    fun playerEpisodeOpened(index: Int) {
        val playback = state.value.playback ?: return
        val entryId = playback.entryId ?: return
        val id = playback.ids.getOrNull(index) ?: return
        viewModelScope.launch {
            runCatching { app.library.recordWatch(entryId, index, System.currentTimeMillis()) }
                .onFailure { mutable.value = state.value.copy(message = "Could not save Continue Watching.") }
        }
    }
    suspend fun playbackStatus(id: String) = app.youtube.videoStatus(id)

    private fun refreshTrustedPlaylists() {
        if (!state.value.parent || state.value.busy) return
        val trusted = entries.value.filter { it.playlistId != null && it.refreshMode == "AUTOMATIC" }
        if (trusted.isEmpty()) return
        parentWork {
            var updated = 0
            trusted.forEach { entry ->
                val snapshot = app.youtube.playlistVideos(entry.playlistId!!).map { it.id }
                app.library.refreshPlaylistSnapshot(entry.id, snapshot)
                updated++
            }
            mutable.value = state.value.copy(message = "$updated trusted playlist(s) refreshed.")
        }
    }
}
