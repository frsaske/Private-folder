package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val coverUri: String? = null,
    val colorHex: String = "#0A84FF",
    val iconName: String = "folder",
    val isPinned: Boolean = false,
    val isUltraPrivate: Boolean = false,
    val space: String = "MAIN",
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "media_items")
data class MediaEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val folderId: String,
    val filePath: String,
    val originalFileName: String,
    val mimeType: String,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val isFavorite: Boolean = false,
    val space: String = "MAIN",
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val addedAt: Long = System.currentTimeMillis()
)
