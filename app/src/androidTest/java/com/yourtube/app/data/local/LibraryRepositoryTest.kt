package com.yourtube.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryRepositoryTest {
    private lateinit var context: Context
    private lateinit var db: LibraryDatabase
    private lateinit var repo: LibraryRepository

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).build()
        repo = LibraryRepository(db)
    }
    @After fun tearDown() = db.close()

    @Test fun categoryDeletionUnassignsSavedContent() = runBlocking {
        val category = repo.createCategory("Stories")
        val entry = repo.approve("Video", listOf("abcdefghijk"), categoryId = category)
        assertEquals(category, db.libraryDao().entries().single().categoryId)
        repo.deleteCategory(category)
        assertNull(db.libraryDao().entries().single { it.id == entry }.categoryId)
        assertTrue(db.libraryDao().categories().isEmpty())
    }

    @Test fun duplicateApprovalAndOrderingAreStable() = runBlocking {
        val first = repo.approve("one", listOf("abcdefghijk"))
        val duplicate = repo.approve("duplicate", listOf("abcdefghijk"))
        val second = repo.approve("two", listOf("lmnopqrstuv"))
        assertEquals(first, duplicate)
        repo.moveEntry(second, -1)
        assertEquals(listOf(second, first), db.libraryDao().entries().map { it.id })
        assertEquals(listOf(0, 1), db.libraryDao().entries().map { it.position })
    }

    @Test fun explicitPlaylistApprovalRefreshesSnapshotWithoutChangingOrder() = runBlocking {
        val first = repo.approve("Playlist", listOf("abcdefghijk"), playlistId = "PL1234567890")
        repo.approve("Updated title", listOf("lmnopqrstuv", "mnopqrstuvw"), playlistId = "PL1234567890")
        val saved = db.libraryDao().entries().single()
        assertEquals(first, saved.id)
        assertEquals("Updated title", saved.title)
        assertEquals("lmnopqrstuv,mnopqrstuvw", saved.videoIds)
        assertEquals(0, saved.position)
    }

    @Test fun invalidOrOversizedSnapshotsAreRejected() = runBlocking {
        assertThrows(IllegalArgumentException::class.java) { runBlocking { repo.approve("bad", listOf("short")) } }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.approve("too many", List(501) { "abcdefghijk" }) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repo.approve("bad playlist", listOf("abcdefghijk"), playlistId = "PLshort") }
        }
    }

    @Test fun databaseSurvivesCloseAndReopen() = runBlocking {
        val name = "repository-reopen-test.db"
        context.deleteDatabase(name)
        db.close()
        db = Room.databaseBuilder(context, LibraryDatabase::class.java, name).build()
        repo = LibraryRepository(db)
        val category = repo.createCategory("Persisted")
        repo.approve("Saved", listOf("abcdefghijk"), categoryId = category)
        db.close()

        db = Room.databaseBuilder(context, LibraryDatabase::class.java, name).build()
        repo = LibraryRepository(db)
        assertEquals("Persisted", db.libraryDao().categories().single().name)
        assertEquals("abcdefghijk", db.libraryDao().entries().single().videoIds)
        context.deleteDatabase(name)
    }
}
