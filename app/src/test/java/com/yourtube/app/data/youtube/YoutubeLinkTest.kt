package com.yourtube.app.data.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeLinkTest {
    @Test fun parsesSupportedVideoShapes() {
        assertEquals(YoutubeTarget(videoId = "abcdefghijk"), YoutubeLink.parse("https://www.youtube.com/watch?v=abcdefghijk"))
        assertEquals(YoutubeTarget(videoId = "abcdefghijk"), YoutubeLink.parse("https://youtu.be/abcdefghijk?t=12"))
        assertEquals(YoutubeTarget(videoId = "abcdefghijk"), YoutubeLink.parse("https://m.youtube.com/shorts/abcdefghijk"))
        assertEquals(YoutubeTarget(videoId = "abcdefghijk"), YoutubeLink.parse("https://youtube.com/live/abcdefghijk"))
        assertEquals(YoutubeTarget(videoId = "abcdefghijk"), YoutubeLink.parse("https://youtube.com/embed/abcdefghijk"))
        assertEquals(YoutubeTarget(videoId = "abcdefghijk"), YoutubeLink.parse("https://music.youtube.com/watch?v=abcdefghijk"))
        assertEquals(YoutubeTarget(videoId = "abcdefghijk"), YoutubeLink.parse("https://www.youtube.com/shorts/abcdefghijk?feature=share"))
        assertEquals(YoutubeTarget(videoId = "abcdefghijk"), YoutubeLink.parse("HTTPS://YOUTU.BE/abcdefghijk"))
        assertEquals(YoutubeTarget(videoId = "abcdefghijk"), YoutubeLink.parse("(https://youtu.be/abcdefghijk)"))
    }

    @Test fun extractsOnlyOneSupportedUrlFromShareText() {
        assertEquals(YoutubeTarget(videoId = "abcdefghijk"), YoutubeLink.parse("Watch this! https://youtu.be/abcdefghijk. Thanks"))
        assertEquals(YoutubeTarget(playlistId = "PL1234567890"), YoutubeLink.parse("https://youtube.com/playlist?list=PL1234567890"))
        assertNull(YoutubeLink.parse("https://youtu.be/abcdefghijk and https://youtu.be/lmnopqrstuv"))
    }

    @Test fun directVideoWinsOverPlaylistInWatchUrl() {
        assertEquals(YoutubeTarget(videoId = "abcdefghijk"), YoutubeLink.parse("https://youtube.com/watch?v=abcdefghijk&list=PL1234567890"))
    }

    @Test fun rejectsLookalikeAndHostileUrls() {
        listOf(
            "https://youtube.com.evil.test/watch?v=abcdefghijk",
            "https://youtube.com@evil.test/watch?v=abcdefghijk",
            "https://evil.test/?next=https://youtu.be/abcdefghijk",
            "https://youtube.com:443/watch?v=abcdefghijk",
            "https://youtube.com/watch?v=abcdefghijk&v=lmnopqrstuv",
            "https://youtu.be/abcdefghijk?v=lmnopqrstuv",
            "https://youtube.com/watch?v=bad",
            "https://youtube.com/playlist?list=bad",
            "javascript://youtube.com/watch?v=abcdefghijk",
            "https://youtube.com/watch?v=abcdefghijk#fragment",
            "https://youtube.com/watch?list=PL1234567890",
            "https://youtube.com/watch?v=abcdefghij",
            "https://youtube.com/watch?v=abcdefghijkl",
            "https://youtube.com/playlist?list=PL1234567",
            "https://youtube.com/playlist?list=" + "A".repeat(101),
            "https://youtube.com/watch?v=abcdefgh!jk",
        ).forEach { assertNull("Should reject $it", YoutubeLink.parse(it)) }
    }
}
