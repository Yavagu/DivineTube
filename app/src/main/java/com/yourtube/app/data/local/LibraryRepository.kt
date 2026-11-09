package com.yourtube.app.data.local

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class LibraryRepository(private val database: LibraryDatabase) {
    private val dao = database.libraryDao()
    val categories: Flow<List<Category>> = dao.observeCategories()
    val entries: Flow<List<ApprovedEntry>> = dao.observeEntries()

    suspend fun createCategory(name: String): Long {
        val clean = name.trim().take(48)
        require(clean.isNotEmpty()) { "Category name is required" }
        return database.withTransaction {
            val current = dao.categories()
            require(current.none { it.name.equals(clean, ignoreCase = true) }) { "Category already exists" }
            dao.insertCategory(Category(name = clean, position = current.size))
        }
    }

    suspend fun renameCategory(id: Long, name: String) {
        val clean = name.trim().take(48)
        require(clean.isNotEmpty()) { "Category name is required" }
        database.withTransaction {
            val current = dao.categories()
            val target = current.firstOrNull { it.id == id } ?: return@withTransaction
            require(current.none { it.id != id && it.name.equals(clean, ignoreCase = true) }) { "Category already exists" }
            dao.updateCategory(target.copy(name = clean))
        }
    }

    suspend fun deleteCategory(id: Long) = database.withTransaction {
        dao.categories().firstOrNull { it.id == id }?.let { dao.deleteCategoryAndCompact(it) }
    }

    suspend fun moveCategory(id: Long, delta: Int) = database.withTransaction {
        val sorted = dao.categories().toMutableList()
        val old = sorted.indexOfFirst { it.id == id }
        if (old < 0) return@withTransaction
        val dest = (old + delta.coerceIn(-1, 1)).coerceIn(0, sorted.lastIndex)
        if (old == dest) return@withTransaction
        val first = sorted[old]
        val second = sorted[dest]
        // Unique positions require an intermediate slot outside the current range.
        dao.updateCategory(first.copy(position = sorted.size + 1))
        dao.updateCategory(second.copy(position = old))
        dao.updateCategory(first.copy(position = dest))
    }

    suspend fun approve(title: String, videoIds: List<String>, playlistId: String? = null, categoryId: Long? = null): Long {
        require(videoIds.size in 1..MAX_SNAPSHOT_SIZE) { "A snapshot must contain between 1 and $MAX_SNAPSHOT_SIZE video IDs" }
        val cleanIds = videoIds.map { it.trim() }
        require(cleanIds.all(VIDEO_ID::matches)) { "Snapshot contains an invalid video ID" }
        require(playlistId == null || PLAYLIST_ID.matches(playlistId)) { "Invalid playlist ID" }
        val snapshot = cleanIds.distinct().joinToString(",")
        return database.withTransaction {
            val current = dao.entries()
            if (playlistId != null) {
                val existing = current.firstOrNull { it.playlistId == playlistId }
                if (existing != null) {
                    val category = categoryId?.takeIf { id -> dao.categories().any { it.id == id } }
                    dao.updateEntry(existing.copy(
                        title = title.trim().ifEmpty { "YouTube item" }.take(120),
                        videoIds = snapshot,
                        categoryId = category
                    ))
                    return@withTransaction existing.id
                }
            } else {
                val existing = current.firstOrNull { it.playlistId == null && it.videoIds == snapshot }
                if (existing != null) return@withTransaction existing.id
            }
            val category = categoryId?.takeIf { id -> dao.categories().any { it.id == id } }
            dao.insertEntry(ApprovedEntry(title = title.trim().ifEmpty { "YouTube item" }.take(120), videoIds = snapshot,
                playlistId = playlistId, categoryId = category, position = current.size))
        }
    }
    suspend fun removeEntry(id: Long) = database.withTransaction {
        dao.entry(id)?.let { dao.deleteEntry(it) }
        dao.entries().forEachIndexed { index, entry -> if (entry.position != index) dao.updateEntry(entry.copy(position = index)) }
    }

    suspend fun moveEntry(id: Long, delta: Int) = database.withTransaction {
        val list = dao.entries().toMutableList()
        val old = list.indexOfFirst { it.id == id }
        if (old < 0) return@withTransaction
        val dest = (old + delta.coerceIn(-1, 1)).coerceIn(0, list.lastIndex)
        if (old == dest) return@withTransaction
        val entry = list.removeAt(old)
        list.add(dest, entry)
        rewritePositions(list)
    }

    suspend fun assignCategory(entryId: Long, categoryId: Long?) = database.withTransaction {
        val entry = dao.entry(entryId) ?: return@withTransaction
        val valid = categoryId?.takeIf { id -> dao.categories().any { it.id == id } }
        dao.updateEntry(entry.copy(categoryId = valid))
    }

    private suspend fun rewritePositions(list: List<ApprovedEntry>) {
        list.forEachIndexed { index, entry -> dao.updateEntry(entry.copy(position = list.size + index + 1)) }
        list.forEachIndexed { index, entry -> dao.updateEntry(entry.copy(position = index)) }
    }

    companion object {
        private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
        private val PLAYLIST_ID = Regex("^[A-Za-z0-9_-]{10,100}$")
        private const val MAX_SNAPSHOT_SIZE = 500
    }
}
