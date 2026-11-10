package com.yourtube.app.data.local

import androidx.room.*
import androidx.room.migration.Migration
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "categories", indices = [Index(value = ["position"], unique = true)])
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val position: Int
)

@Entity(
    tableName = "approved_entries",
    foreignKeys = [ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.SET_NULL)],
    indices = [Index("categoryId"), Index(value = ["position"], unique = true), Index(value = ["playlistId"], unique = true)]
)
data class ApprovedEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Comma-separated immutable snapshot of approved video IDs. */
    val videoIds: String,
    val playlistId: String? = null,
    val categoryId: Long? = null,
    val position: Int,
    val featured: Boolean = false,
    val refreshMode: String = "MANUAL"
)

@Entity(
    tableName = "watch_history",
    foreignKeys = [ForeignKey(entity = ApprovedEntry::class, parentColumns = ["id"], childColumns = ["entryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("entryId")]
)
data class WatchHistory(
    @PrimaryKey val entryId: Long,
    val videoId: String,
    val episodeIndex: Int,
    val lastOpenedAt: Long
)

@Entity(
    tableName = "playback_progress",
    primaryKeys = ["entryId", "videoId"],
    foreignKeys = [ForeignKey(entity = ApprovedEntry::class, parentColumns = ["id"], childColumns = ["entryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("entryId")]
)
data class PlaybackProgress(
    val entryId: Long,
    val videoId: String,
    val positionSeconds: Long,
    val durationSeconds: Long,
    val updatedAt: Long,
    val completed: Boolean = false
)

@Dao
abstract class LibraryDao {
    @Query("SELECT * FROM categories ORDER BY position") abstract fun observeCategories(): Flow<List<Category>>
    @Query("SELECT * FROM approved_entries ORDER BY position") abstract fun observeEntries(): Flow<List<ApprovedEntry>>
    @Query("SELECT * FROM categories ORDER BY position") abstract suspend fun categories(): List<Category>
    @Query("SELECT * FROM approved_entries ORDER BY position") abstract suspend fun entries(): List<ApprovedEntry>
    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun insertCategory(category: Category): Long
    @Update abstract suspend fun updateCategory(category: Category)
    @Delete abstract suspend fun deleteCategory(category: Category)
    @Insert(onConflict = OnConflictStrategy.ABORT) abstract suspend fun insertEntry(entry: ApprovedEntry): Long
    @Update abstract suspend fun updateEntry(entry: ApprovedEntry)
    @Delete abstract suspend fun deleteEntry(entry: ApprovedEntry)
    @Query("SELECT * FROM approved_entries WHERE id = :id") abstract suspend fun entry(id: Long): ApprovedEntry?
    @Query("SELECT * FROM watch_history ORDER BY lastOpenedAt DESC") abstract fun observeWatchHistory(): Flow<List<WatchHistory>>
    @Query("SELECT * FROM watch_history WHERE entryId = :entryId") abstract suspend fun watchHistory(entryId: Long): WatchHistory?
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putWatchHistory(history: WatchHistory)
    @Query("SELECT * FROM playback_progress ORDER BY updatedAt DESC") abstract fun observePlaybackProgress(): Flow<List<PlaybackProgress>>
    @Query("SELECT * FROM playback_progress WHERE entryId = :entryId AND videoId = :videoId") abstract suspend fun playbackProgress(entryId: Long, videoId: String): PlaybackProgress?
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putPlaybackProgress(progress: PlaybackProgress)
    @Query("DELETE FROM playback_progress WHERE entryId = :entryId AND videoId = :videoId") abstract suspend fun deletePlaybackProgress(entryId: Long, videoId: String)

    @Transaction
    open suspend fun deleteCategoryAndCompact(category: Category) {
        deleteCategory(category)
        categories().forEachIndexed { index, item -> updateCategory(item.copy(position = index)) }
    }
}

@Database(entities = [Category::class, ApprovedEntry::class, WatchHistory::class, PlaybackProgress::class], version = 3, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE approved_entries ADD COLUMN featured INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE approved_entries ADD COLUMN refreshMode TEXT NOT NULL DEFAULT 'MANUAL'")
                db.execSQL("CREATE TABLE IF NOT EXISTS watch_history (entryId INTEGER NOT NULL, videoId TEXT NOT NULL, episodeIndex INTEGER NOT NULL, lastOpenedAt INTEGER NOT NULL, PRIMARY KEY(entryId), FOREIGN KEY(entryId) REFERENCES approved_entries(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_watch_history_entryId ON watch_history(entryId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `playback_progress` (`entryId` INTEGER NOT NULL, `videoId` TEXT NOT NULL, `positionSeconds` INTEGER NOT NULL, `durationSeconds` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `completed` INTEGER NOT NULL, PRIMARY KEY(`entryId`, `videoId`), FOREIGN KEY(`entryId`) REFERENCES `approved_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playback_progress_entryId` ON `playback_progress`(`entryId`)")
            }
        }
    }
}
