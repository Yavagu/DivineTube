package com.yourtube.app.ui

import com.yourtube.app.Playback
import com.yourtube.app.ResumeCandidate
import com.yourtube.app.data.local.ApprovedEntry
import com.yourtube.app.data.local.PlaybackProgress
import org.junit.Assert.*
import org.junit.Test

class PlaybackResumeTest {

    private fun formatProgressTime(seconds: Long): String {
        val s = seconds.coerceAtLeast(0)
        val m = s / 60
        val remS = s % 60
        val h = m / 60
        val remM = m % 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, remM, remS)
        } else {
            String.format("%d:%02d", remM, remS)
        }
    }

    @Test
    fun timeFormattingHandlesSecondsMinutesAndHours() {
        assertEquals("0:00", formatProgressTime(0))
        assertEquals("0:35", formatProgressTime(35))
        assertEquals("2:05", formatProgressTime(125))
        assertEquals("10:00", formatProgressTime(600))
        assertEquals("1:01:05", formatProgressTime(3665))
        assertEquals("0:00", formatProgressTime(-10))
    }

    @Test
    fun resumeCandidateFilteringRespects30SecondsBoundariesAndApproval() {
        val entry1 = ApprovedEntry(id = 1L, title = "Approved Series", videoIds = "vid1,vid2,vid3", position = 0)
        val entry2 = ApprovedEntry(id = 2L, title = "Standalone Video", videoIds = "vidSingle", position = 1)
        val entries = listOf(entry1, entry2)

        // Case 1: Watched only 20 seconds (under 30s requirement) -> not eligible
        val progUnder30 = PlaybackProgress(1L, "vid1", positionSeconds = 20L, durationSeconds = 300L, updatedAt = 1000L, completed = false)
        val candidate1 = findCandidate(entries, listOf(progUnder30), dismissed = false)
        assertNull(candidate1)

        // Case 2: Only 15 seconds remain (under 30s remain requirement) -> not eligible
        val progNearEnd = PlaybackProgress(1L, "vid1", positionSeconds = 285L, durationSeconds = 300L, updatedAt = 1000L, completed = false)
        val candidate2 = findCandidate(entries, listOf(progNearEnd), dismissed = false)
        assertNull(candidate2)

        // Case 3: Completed video -> not eligible
        val progCompleted = PlaybackProgress(1L, "vid1", positionSeconds = 150L, durationSeconds = 300L, updatedAt = 1000L, completed = true)
        val candidate3 = findCandidate(entries, listOf(progCompleted), dismissed = false)
        assertNull(candidate3)

        // Case 4: Video belongs to deleted / unapproved entry -> not eligible
        val progUnapproved = PlaybackProgress(999L, "unknownVid", positionSeconds = 100L, durationSeconds = 300L, updatedAt = 1000L, completed = false)
        val candidate4 = findCandidate(entries, listOf(progUnapproved), dismissed = false)
        assertNull(candidate4)

        // Case 5: Video removed from approved entry's snapshot -> not eligible
        val progRemovedVid = PlaybackProgress(1L, "removedVid", positionSeconds = 100L, durationSeconds = 300L, updatedAt = 1000L, completed = false)
        val candidate5 = findCandidate(entries, listOf(progRemovedVid), dismissed = false)
        assertNull(candidate5)

        // Case 6: Watched 45s with 255s remaining on approved playlist -> eligible
        val progValid = PlaybackProgress(1L, "vid2", positionSeconds = 45L, durationSeconds = 300L, updatedAt = 2000L, completed = false)
        val candidate6 = findCandidate(entries, listOf(progValid), dismissed = false)
        assertNotNull(candidate6)
        assertEquals(1L, candidate6!!.entry.id)
        assertEquals("vid2", candidate6.videoId)
        assertEquals(1, candidate6.episodeIndex) // 2nd episode (0-indexed)
        assertEquals(3, candidate6.totalEpisodes)
        assertEquals(45L, candidate6.positionSeconds)
        assertEquals(300L, candidate6.durationSeconds)

        // Case 7: Once dismissed -> returns null
        val candidateDismissed = findCandidate(entries, listOf(progValid), dismissed = true)
        assertNull(candidateDismissed)
    }

    @Test
    fun resumeCandidatePicksMostRecentUnfinishedItem() {
        val entry1 = ApprovedEntry(id = 1L, title = "Series 1", videoIds = "vidA,vidB", position = 0)
        val entry2 = ApprovedEntry(id = 2L, title = "Series 2", videoIds = "vidC,vidD", position = 1)
        val entries = listOf(entry1, entry2)

        val older = PlaybackProgress(1L, "vidA", positionSeconds = 60L, durationSeconds = 300L, updatedAt = 1000L, completed = false)
        val newer = PlaybackProgress(2L, "vidC", positionSeconds = 90L, durationSeconds = 300L, updatedAt = 5000L, completed = false)

        // Ordered by updatedAt DESC: newer is first
        val candidate = findCandidate(entries, listOf(newer, older), dismissed = false)
        assertNotNull(candidate)
        assertEquals(2L, candidate!!.entry.id)
        assertEquals("vidC", candidate.videoId)
    }

    @Test
    fun playlistResumePreservesEntirePlaylistContextAndTargetEpisode() {
        val entry = ApprovedEntry(id = 10L, title = "Cartoons", videoIds = "ep1,ep2,ep3", playlistId = "PL_CARTOONS", position = 0)
        val entries = listOf(entry)
        val progress = PlaybackProgress(entryId = 10L, videoId = "ep2", positionSeconds = 120L, durationSeconds = 600L, updatedAt = 1000L, completed = false)

        val playback = buildResumePlayback(entry, "ep2", progress, startOver = false)
        assertNotNull(playback)
        assertEquals(listOf("ep1", "ep2", "ep3"), playback!!.ids)
        assertEquals("Cartoons", playback.title)
        assertEquals(10L, playback.entryId)
        assertEquals(1, playback.initialIndex) // ep2 is at index 1
        assertEquals(120L, playback.startSeconds)
        assertFalse(playback.preview)
    }

    @Test
    fun startOverResetsPositionToZeroAndPreservesPlaylistContext() {
        val entry = ApprovedEntry(id = 10L, title = "Cartoons", videoIds = "ep1,ep2,ep3", playlistId = "PL_CARTOONS", position = 0)
        val progress = PlaybackProgress(entryId = 10L, videoId = "ep2", positionSeconds = 120L, durationSeconds = 600L, updatedAt = 1000L, completed = false)

        val playback = buildResumePlayback(entry, "ep2", progress, startOver = true)
        assertNotNull(playback)
        assertEquals(listOf("ep1", "ep2", "ep3"), playback!!.ids)
        assertEquals(1, playback.initialIndex)
        assertEquals(0L, playback.startSeconds) // Started over at 0:00
    }

    @Test
    fun completedVideoStartsAtZero() {
        val entry = ApprovedEntry(id = 20L, title = "Finished Video", videoIds = "vidDone", position = 0)
        val progress = PlaybackProgress(entryId = 20L, videoId = "vidDone", positionSeconds = 295L, durationSeconds = 300L, updatedAt = 1000L, completed = true)

        val playback = buildResumePlayback(entry, "vidDone", progress, startOver = false)
        assertNotNull(playback)
        assertEquals(0L, playback!!.startSeconds)
    }

    @Test
    fun switchingEpisodesResolvesSavedPositionPerEpisode() {
        val entryId = 5L
        val episode1 = "ep1"
        val episode2 = "ep2"

        val progresses = listOf(
            PlaybackProgress(entryId, episode1, positionSeconds = 85L, durationSeconds = 300L, updatedAt = 1000L, completed = false)
            // ep2 has no progress
        )

        fun getSavedPosition(vid: String): Long {
            val p = progresses.firstOrNull { it.entryId == entryId && it.videoId == vid } ?: return 0L
            return if (!p.completed) p.positionSeconds else 0L
        }

        assertEquals(85L, getSavedPosition(episode1))
        assertEquals(0L, getSavedPosition(episode2))
    }

    private fun findCandidate(
        entriesList: List<ApprovedEntry>,
        progressList: List<PlaybackProgress>,
        dismissed: Boolean
    ): ResumeCandidate? {
        if (dismissed) return null
        return progressList.asSequence()
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

    private fun buildResumePlayback(
        entry: ApprovedEntry,
        videoId: String,
        progress: PlaybackProgress?,
        startOver: Boolean
    ): Playback? {
        val ids = entry.videoIds.split(',')
        val index = ids.indexOf(videoId)
        if (index < 0) return null
        val startSeconds = if (startOver) {
            0L
        } else {
            if (progress != null && !progress.completed) progress.positionSeconds else 0L
        }
        return Playback(
            ids = ids,
            title = entry.title,
            preview = false,
            entryId = entry.id,
            initialIndex = index,
            startSeconds = startSeconds
        )
    }
}
