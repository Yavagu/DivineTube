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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Playback(val ids: List<String>, val title: String, val preview: Boolean, val entryId: Long? = null)
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
    private val mutable = MutableStateFlow(AppState())
    val state = mutable.asStateFlow()
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
    fun play(entry: ApprovedEntry) {
        // Re-read from the approved stream; caller-provided IDs are never accepted as Kids launch targets.
        val approved = entries.value.firstOrNull { it.id == entry.id } ?: return
        mutable.value = state.value.copy(playback = Playback(approved.videoIds.split(','), approved.title, false, approved.id))
    }
    fun preview(id: String, title: String) {
        if (state.value.parent && Regex("[A-Za-z0-9_-]{11}").matches(id))
            mutable.value = state.value.copy(playback = Playback(listOf(id), title, true))
    }
    fun closePlayback() { mutable.value = state.value.copy(playback = null) }
    suspend fun playbackStatus(id: String) = app.youtube.videoStatus(id)
}
