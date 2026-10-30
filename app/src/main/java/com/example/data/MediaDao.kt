package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items WHERE folderId = :folderId AND isDeleted = 0 ORDER BY addedAt DESC")
    fun getMediaForFolder(folderId: String): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isDeleted = 0 ORDER BY addedAt DESC")
    fun getAllActiveMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isDeleted = 0 AND isVideo = 0 ORDER BY addedAt DESC")
    fun getAllActivePhotos(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isDeleted = 0 AND isVideo = 1 ORDER BY addedAt DESC")
    fun getAllActiveVideos(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getRecycleBinMedia(): Flow<List<MediaEntity>>

    @Query("SELECT COUNT(*) FROM media_items WHERE isDeleted = 1")
    fun getRecycleBinCount(): Flow<Int>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getMediaById(id: String): MediaEntity?

    @Query("SELECT * FROM media_items WHERE folderId = :folderId")
    suspend fun getMediaByFolderList(folderId: String): List<MediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(mediaList: List<MediaEntity>)

    @Update
    suspend fun updateMedia(media: MediaEntity)

    @Query("UPDATE media_items SET isDeleted = 1, deletedAt = :timestamp WHERE id IN (:ids)")
    suspend fun moveToRecycleBin(ids: List<String>, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE media_items SET isDeleted = 1, deletedAt = :timestamp WHERE folderId = :folderId")
    suspend fun softDeleteMediaByFolder(folderId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE media_items SET isDeleted = 0, deletedAt = NULL WHERE id IN (:ids)")
    suspend fun restoreFromRecycleBin(ids: List<String>)

    @Query("UPDATE media_items SET isDeleted = 0, deletedAt = NULL WHERE folderId = :folderId")
    suspend fun restoreMediaByFolder(folderId: String)

    @Query("UPDATE media_items SET folderId = :newFolderId WHERE id IN (:ids)")
    suspend fun moveMediaToFolder(ids: List<String>, newFolderId: String)

    @Query("UPDATE media_items SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    @Delete
    suspend fun deletePermanently(media: MediaEntity)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deletePermanentlyByIds(ids: List<String>)

    @Query("DELETE FROM media_items WHERE folderId = :folderId")
    suspend fun deleteMediaByFolderId(folderId: String)

    @Query("DELETE FROM media_items WHERE isDeleted = 1")
    suspend fun emptyRecycleBin()

    @Query("SELECT * FROM media_items WHERE isDeleted = 1")
    suspend fun getAllRecycleBinList(): List<MediaEntity>
}
