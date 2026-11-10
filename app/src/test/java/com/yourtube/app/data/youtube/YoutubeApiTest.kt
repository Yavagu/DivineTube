package com.yourtube.app.data.youtube

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeApiTest {
    @Test fun searchEncodesQueryAndUsesAndroidIdentityHeaders() = runBlocking {
        var requested = ""
        var headers = emptyMap<String, String>()
        val api = YoutubeApi("key", "com.test.app", "AA:BB", YoutubeHttpTransport { url, h ->
            requested = url; headers = h
            """{"items":[{"id":{"videoId":"abcdefghijk"},"snippet":{"title":"A video"}}],"nextPageToken":"next"}"""
        })
        val page = api.search("fun & games")
        assertTrue(requested.contains("q=fun+%26+games"))
        assertEquals("com.test.app", headers["X-Android-Package"])
        assertEquals("AA:BB", headers["X-Android-Cert"])
        assertEquals(listOf(YoutubeItem("abcdefghijk", "A video", false)), page.items)
        assertEquals("next", page.nextPageToken)
    }

    @Test fun statusRequiresMadeForKidsFieldAndReturnsEmbedEligibility() = runBlocking {
        val api = YoutubeApi("key", "pkg", "cert", YoutubeHttpTransport { _, _ ->
            """{"items":[{"snippet":{"title":"Video"},"status":{"madeForKids":true,"embeddable":false}}]}"""
        })
        val status = api.videoStatus("abcdefghijk")
        assertTrue(status.madeForKids)
        assertFalse(status.embeddable)
        assertEquals("Video", status.title)
    }

    @Test fun unknownStatusFailsClosed() = runBlocking {
        val api = YoutubeApi("key", "pkg", "cert", YoutubeHttpTransport { _, _ ->
            """{"items":[{"snippet":{"title":"Video"},"status":{"embeddable":true}}]}"""
        })
        try {
            api.videoStatus("abcdefghijk")
            throw AssertionError("Expected unknown Made for Kids status to fail")
        } catch (e: YoutubeApiException) { assertTrue(e.message!!.contains("Made for Kids")) }
    }

    @Test fun playlistPaginationBuildsCompleteSnapshot() = runBlocking {
        var calls = 0
        val api = YoutubeApi("key", "pkg", "cert", YoutubeHttpTransport { url, _ ->
            calls++
            if (url.contains("pageToken")) {
                """{"items":[{"contentDetails":{"videoId":"lmnopqrstuv"},"snippet":{"title":"Second"}}]}"""
            } else {
                """{"nextPageToken":"page2","items":[{"contentDetails":{"videoId":"abcdefghijk"},"snippet":{"title":"First"}}]}"""
            }
        })
        assertEquals(listOf(YoutubeItem("abcdefghijk", "First", false), YoutubeItem("lmnopqrstuv", "Second", false)), api.playlistVideos("PL1234567890"))
        assertEquals(2, calls)
    }

    @Test fun missingApiKeyExplainsSetupWithoutCallingTransport() = runBlocking {
        var called = false
        val api = YoutubeApi("", "pkg", "cert", YoutubeHttpTransport { _, _ -> called = true; "{}" })
        try {
            api.search("test")
            throw AssertionError("Expected missing key failure")
        } catch (e: YoutubeApiException) { assertTrue(e.message!!.contains("local.properties")) }
        assertFalse(called)
    }

    @Test fun missingEmbeddableStatusFailsClosed() = runBlocking {
        val api = YoutubeApi("key", "pkg", "cert", YoutubeHttpTransport { _, _ ->
            """{"items":[{"snippet":{"title":"Video"},"status":{"madeForKids":false}}]}"""
        })
        try {
            api.videoStatus("abcdefghijk")
            throw AssertionError("Expected unknown embed status to fail")
        } catch (e: YoutubeApiException) { assertTrue(e.message!!.contains("embed status")) }
    }

    @Test fun cancellationPropagatesAndTransportDetailsAreNotExposed() = runBlocking {
        val cancelled = YoutubeApi("key", "pkg", "cert", YoutubeHttpTransport { _, _ -> throw CancellationException("cancel") })
        try {
            cancelled.search("test")
            throw AssertionError("Expected cancellation")
        } catch (_: CancellationException) { }

        val broken = YoutubeApi("secret-key", "pkg", "cert", YoutubeHttpTransport { _, _ ->
            throw IllegalStateException("https://googleapis.test/?key=secret-key")
        })
        try {
            broken.search("test")
            throw AssertionError("Expected request failure")
        } catch (e: YoutubeApiException) {
            assertFalse(e.message.orEmpty().contains("secret-key"))
            assertEquals(null, e.cause)
        }
    }

    @Test fun repeatingPageTokenAndUnavailablePlaylistItemsFail() = runBlocking {
        var calls = 0
        val repeating = YoutubeApi("key", "pkg", "cert", YoutubeHttpTransport { _, _ ->
            calls++
            """{"nextPageToken":"same","items":[{"contentDetails":{"videoId":"abcdefghijk"},"snippet":{"title":"First"}}]}"""
        })
        try {
            repeating.playlistVideos("PL1234567890")
            throw AssertionError("Expected repeated token failure")
        } catch (e: YoutubeApiException) { assertTrue(e.message!!.contains("repeating")) }
        assertEquals(2, calls)

        val unavailable = YoutubeApi("key", "pkg", "cert", YoutubeHttpTransport { _, _ ->
            """{"items":[{"contentDetails":{"videoId":"abcdefghijk"},"snippet":{"title":"Private video"}}]}"""
        })
        try {
            unavailable.playlistVideos("PL1234567890")
            throw AssertionError("Expected unavailable entry failure")
        } catch (e: YoutubeApiException) { assertTrue(e.message!!.contains("unavailable")) }
    }

    @Test fun playlistIgnoresPrivateAndUnavailableVideos() = runBlocking {
        val api = YoutubeApi("key", "pkg", "cert", YoutubeHttpTransport { _, _ ->
            """{"items":[
                {"contentDetails":{"videoId":"abcdefghijk"},"snippet":{"title":"Valid First"}},
                {"contentDetails":{"videoId":"privatevid01"},"snippet":{"title":"Private video"}},
                {"contentDetails":{"videoId":"deletedvid01"},"snippet":{"title":"Deleted video"}},
                {"contentDetails":{"videoId":"lmnopqrstuv"},"snippet":{"title":"Valid Second"}}
            ]}"""
        })
        val items = api.playlistVideos("PL1234567890")
        assertEquals(2, items.size)
        assertEquals(listOf(YoutubeItem("abcdefghijk", "Valid First", false), YoutubeItem("lmnopqrstuv", "Valid Second", false)), items)
    }
}
