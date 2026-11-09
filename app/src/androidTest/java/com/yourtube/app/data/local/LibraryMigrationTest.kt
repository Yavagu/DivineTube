package com.yourtube.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryMigrationTest {
    @Test fun migrationPreservesExistingCategoriesAndEntries() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-v1-v2-test.db"
        context.deleteDatabase(name)
        val old = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(name).callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, position INTEGER NOT NULL)")
                        db.execSQL("CREATE UNIQUE INDEX index_categories_position ON categories(position)")
                        db.execSQL("CREATE TABLE approved_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, videoIds TEXT NOT NULL, playlistId TEXT, categoryId INTEGER, position INTEGER NOT NULL, FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
                        db.execSQL("CREATE INDEX index_approved_entries_categoryId ON approved_entries(categoryId)")
                        db.execSQL("CREATE UNIQUE INDEX index_approved_entries_position ON approved_entries(position)")
                        db.execSQL("CREATE UNIQUE INDEX index_approved_entries_playlistId ON approved_entries(playlistId)")
                        db.execSQL("INSERT INTO categories(id,name,position) VALUES(7,'Series',0)")
                        db.execSQL("INSERT INTO approved_entries(id,title,videoIds,playlistId,categoryId,position) VALUES(11,'Saved series','abcdefghijk,lmnopqrstuv','PL1234567890',7,0)")
                        db.execSQL("INSERT OR REPLACE INTO room_master_table(id,identity_hash) VALUES(42,'ae229b9ab917952d98812a4bd7fb9f9f')")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        ).writableDatabase.close()

        val database = Room.databaseBuilder(context, LibraryDatabase::class.java, name)
            .addMigrations(LibraryDatabase.MIGRATION_1_2).build()
        try {
            val entry = database.libraryDao().entries().single()
            assertEquals(11L, entry.id)
            assertEquals("Saved series", entry.title)
            assertEquals("abcdefghijk,lmnopqrstuv", entry.videoIds)
            assertEquals(7L, entry.categoryId)
            assertEquals("MANUAL", entry.refreshMode)
            assertFalse(entry.featured)
            assertEquals("Series", database.libraryDao().categories().single().name)

            // Test watch history on migrated database
            database.libraryDao().putWatchHistory(WatchHistory(11L, "abcdefghijk", 0, 5000L))
            val savedHistory = database.libraryDao().watchHistory(11L)
            assertNotNull(savedHistory)
            assertEquals("abcdefghijk", savedHistory!!.videoId)
            assertEquals(0, savedHistory.episodeIndex)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
