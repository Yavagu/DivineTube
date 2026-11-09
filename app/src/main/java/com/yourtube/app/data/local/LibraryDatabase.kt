package com.yourtube.app.data.local

import androidx.room.*
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
    val position: Int
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

    @Transaction
    open suspend fun deleteCategoryAndCompact(category: Category) {
        deleteCategory(category)
        categories().forEachIndexed { index, item -> updateCategory(item.copy(position = index)) }
    }
}

@Database(entities = [Category::class, ApprovedEntry::class], version = 1, exportSchema = true)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
}
