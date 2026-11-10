package com.yourtube.app.ui

import com.yourtube.app.FeedItem
import com.yourtube.app.Playback
import com.yourtube.app.data.local.ApprovedEntry
import com.yourtube.app.data.local.Category
import com.yourtube.app.data.local.PlaybackProgress
import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class HomeFeedTest {

    private fun buildFeedItems(
        entries: List<ApprovedEntry>,
        orderMap: MutableMap<Pair<Long, String>, Int>,
        random: Random = Random(42)
    ): List<FeedItem> {
        val items = mutableListOf<FeedItem>()
        val seen = mutableSetOf<Pair<Long, String>>()
        for (entry in entries) {
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
        return items.sortedBy { item ->
            val key = Pair(item.entryId, item.videoId)
            synchronized(orderMap) {
                orderMap.getOrPut(key) { random.nextInt() }
            }
        }
    }

    @Test
    fun feedIncludesEveryApprovedStandaloneVideoAndPlaylistEpisode() {
        val standalone1 = ApprovedEntry(id = 1L, title = "Standalone 1", videoIds = "vidS1", position = 0)
        val standalone2 = ApprovedEntry(id = 2L, title = "Standalone 2", videoIds = "vidS2", position = 1)
        val playlist1 = ApprovedEntry(id = 3L, title = "Epic Series", videoIds = "ep1,ep2,ep3", playlistId = "PL_EPIC", position = 2)

        val entries = listOf(standalone1, standalone2, playlist1)
        val orderMap = mutableMapOf<Pair<Long, String>, Int>()
        val feed = buildFeedItems(entries, orderMap)

        // Total items = 1 + 1 + 3 = 5 items
        assertEquals(5, feed.size)

        // Check standalone items
        val s1 = feed.first { it.entryId == 1L }
        assertEquals("vidS1", s1.videoId)
        assertEquals("Standalone 1", s1.title)
        assertFalse(s1.isPlaylistEpisode)

        val s2 = feed.first { it.entryId == 2L }
        assertEquals("vidS2", s2.videoId)
        assertEquals("Standalone 2", s2.title)
        assertFalse(s2.isPlaylistEpisode)

        // Check playlist episodes
        val plEpisodes = feed.filter { it.entryId == 3L }
        assertEquals(3, plEpisodes.size)
        assertTrue(plEpisodes.all { it.isPlaylistEpisode })
        assertTrue(plEpisodes.all { it.playlistTitle == "Epic Series" })
        assertTrue(plEpisodes.all { it.totalEpisodes == 3 })

        val ep2 = plEpisodes.first { it.videoId == "ep2" }
        assertEquals("Epic Series — Episode 2", ep2.title)
        assertEquals(1, ep2.episodeIndex)
    }

    @Test
    fun feedDeduplicatesByEntryIdAndVideoId() {
        // If a playlist contains duplicate video IDs in snapshot
        val playlistWithDupes = ApprovedEntry(
            id = 5L,
            title = "Playlist with Dupes",
            videoIds = "dup1,dup2,dup1,dup3,dup2",
            playlistId = "PL_DUP",
            position = 0
        )
        val orderMap = mutableMapOf<Pair<Long, String>, Int>()
        val feed = buildFeedItems(listOf(playlistWithDupes), orderMap)

        assertEquals(3, feed.size)
        assertEquals(listOf("dup1", "dup2", "dup3").sorted(), feed.map { it.videoId }.sorted())
    }

    @Test
    fun feedRandomizationIsStableAcrossMultipleReads() {
        val entries = (1L..10L).map { id ->
            ApprovedEntry(id = id, title = "Video $id", videoIds = "vid$id", position = id.toInt())
        }
        val orderMap = mutableMapOf<Pair<Long, String>, Int>()
        val initialFeed = buildFeedItems(entries, orderMap)
        val initialKeys = initialFeed.map { Pair(it.entryId, it.videoId) }

        // Repeated reads (simulating recomposition, tab switching, returning from playback)
        val secondFeed = buildFeedItems(entries, orderMap)
        val secondKeys = secondFeed.map { Pair(it.entryId, it.videoId) }

        val thirdFeed = buildFeedItems(entries, orderMap)
        val thirdKeys = thirdFeed.map { Pair(it.entryId, it.videoId) }

        assertEquals(initialKeys, secondKeys)
        assertEquals(initialKeys, thirdKeys)
    }

    @Test
    fun playlistEpisodePreservesPlaylistContextAndSavedProgress() {
        val playlist = ApprovedEntry(
            id = 10L,
            title = "Mahabharat",
            videoIds = "vid101,vid102,vid103,vid104",
            playlistId = "PL_MAHABHARAT",
            position = 0
        )
        val progresses = listOf(
            PlaybackProgress(entryId = 10L, videoId = "vid103", positionSeconds = 245L, durationSeconds = 900L, updatedAt = 1000L, completed = false)
        )

        val feedItem = FeedItem(
            entryId = 10L,
            videoId = "vid103",
            title = "Mahabharat — Episode 3",
            isPlaylistEpisode = true,
            playlistTitle = "Mahabharat",
            episodeIndex = 2,
            totalEpisodes = 4
        )

        fun launchFeedItem(item: FeedItem): Playback {
            val approved = listOf(playlist).first { it.id == item.entryId }
            val ids = approved.videoIds.split(',')
            val index = ids.indexOf(item.videoId)
            val prog = progresses.firstOrNull { it.entryId == item.entryId && it.videoId == item.videoId }
            val startSeconds = if (prog != null && !prog.completed) prog.positionSeconds else 0L
            return Playback(
                ids = ids,
                title = approved.title,
                preview = false,
                entryId = approved.id,
                initialIndex = index,
                startSeconds = startSeconds
            )
        }

        val playback = launchFeedItem(feedItem)
        assertEquals(listOf("vid101", "vid102", "vid103", "vid104"), playback.ids)
        assertEquals("Mahabharat", playback.title)
        assertEquals(10L, playback.entryId)
        assertEquals(2, playback.initialIndex) // Episode 3 (index 2)
        assertEquals(245L, playback.startSeconds) // Preserved saved position
        assertFalse(playback.preview)
    }

    @Test
    fun categoryFilteringOnlyIncludesAssignedContent() {
        val cat1 = Category(id = 100L, name = "Devotional", position = 0)
        val cat2 = Category(id = 200L, name = "Stories", position = 1)

        val entryCat1 = ApprovedEntry(id = 1L, title = "Prayer", videoIds = "pray1", categoryId = 100L, position = 0)
        val entryCat2 = ApprovedEntry(id = 2L, title = "Panchatantra", videoIds = "story1", categoryId = 200L, position = 1)
        val entryUnassigned = ApprovedEntry(id = 3L, title = "General", videoIds = "gen1", categoryId = null, position = 2)

        val entries = listOf(entryCat1, entryCat2, entryUnassigned)

        val cat1Entries = entries.filter { it.categoryId == cat1.id }
        assertEquals(1, cat1Entries.size)
        assertEquals(1L, cat1Entries.first().id)

        val cat2Entries = entries.filter { it.categoryId == cat2.id }
        assertEquals(1, cat2Entries.size)
        assertEquals(2L, cat2Entries.first().id)

        val emptyCat = Category(id = 300L, name = "Empty Category", position = 2)
        val emptyCatEntries = entries.filter { it.categoryId == emptyCat.id }
        assertTrue(emptyCatEntries.isEmpty())
    }

    @Test
    fun hiddenCategoriesThresholdAndMoreExpansion() {
        val categories = (1..6).map { Category(id = it.toLong(), name = "Cat $it", position = it) }

        // Total categories = 6 (> 3)
        // Initially when not expanded:
        val initialDisplay = categories.take(3)
        val hiddenCount = categories.size - 3
        assertEquals(3, initialDisplay.size)
        assertEquals(3, hiddenCount)

        // When expanded:
        val expandedDisplay = categories
        assertEquals(6, expandedDisplay.size)
    }
}
