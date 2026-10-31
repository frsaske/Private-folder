package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class FolderWithItemCount(
    val id: String,
    val name: String,
    val coverUri: String?,
    val colorHex: String,
    val iconName: String,
    val isPinned: Boolean,
    val isUltraPrivate: Boolean,
    val space: String,
    val isDeleted: Boolean,
    val deletedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val itemCount: Int,
    val latestThumbnail: String?
)

@Dao
interface FolderDao {
    @Query("""
        SELECT f.*, 
               COUNT(m.id) as itemCount,
               COALESCE(NULLIF(f.coverUri, ''), (SELECT filePath FROM media_items WHERE folderId = f.id AND isDeleted = 0 ORDER BY addedAt DESC LIMIT 1)) as latestThumbnail
        FROM folders f
        LEFT JOIN media_items m ON f.id = m.folderId AND m.isDeleted = 0
        WHERE f.isDeleted = 0 AND f.space = :space
        GROUP BY f.id
        ORDER BY f.isUltraPrivate DESC, f.isPinned DESC, f.updatedAt DESC
    """)
    fun getFoldersForSpaceWithCount(space: String = "MAIN"): Flow<List<FolderWithItemCount>>

    @Query("UPDATE folders SET isPinned = :isPinned WHERE id = :folderId")
    suspend fun updateFolderPinned(folderId: String, isPinned: Boolean)

    @Query("SELECT * FROM folders WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getRecycleBinFolders(): Flow<List<FolderEntity>>

    @Query("SELECT COUNT(*) FROM folders WHERE isDeleted = 1")
    fun getRecycleBinFoldersCount(): Flow<Int>

    @Query("SELECT name FROM folders WHERE isDeleted = 0")
    suspend fun getAllFolderNames(): List<String>

    @Query("SELECT * FROM folders WHERE isDeleted = 0")
    suspend fun getAllFoldersList(): List<FolderEntity>

    @Query("UPDATE folders SET coverUri = :coverUri, updatedAt = :updatedAt WHERE id = :folderId")
    suspend fun updateFolderCover(folderId: String, coverUri: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE name LIKE '%' || :query || '%' AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun searchFolders(query: String): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("UPDATE folders SET isDeleted = 1, deletedAt = :timestamp WHERE id = :folderId")
    suspend fun softDeleteFolder(folderId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE folders SET isDeleted = 0, deletedAt = NULL WHERE id = :folderId")
    suspend fun restoreFolder(folderId: String)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: String)

    @Query("DELETE FROM folders WHERE isDeleted = 1")
    suspend fun emptyRecycleBinFolders()
}
