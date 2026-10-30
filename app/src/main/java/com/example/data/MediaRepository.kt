package com.example.data

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MediaRepository(private val context: Context) {
    private val db = VaultDatabase.getDatabase(context)
    private val folderDao = db.folderDao()
    private val mediaDao = db.mediaDao()

    fun getAllFoldersWithCount(): Flow<List<FolderWithItemCount>> =
        folderDao.getAllFoldersWithCount()

    fun getMediaForFolder(folderId: String): Flow<List<MediaEntity>> =
        mediaDao.getMediaForFolder(folderId)

    fun getAllActiveMedia(): Flow<List<MediaEntity>> =
        mediaDao.getAllActiveMedia()

    fun getAllActivePhotos(): Flow<List<MediaEntity>> =
        mediaDao.getAllActivePhotos()

    fun getAllActiveVideos(): Flow<List<MediaEntity>> =
        mediaDao.getAllActiveVideos()

    fun getRecycleBinMedia(): Flow<List<MediaEntity>> =
        mediaDao.getRecycleBinMedia()

    fun getRecycleBinFolders(): Flow<List<FolderEntity>> =
        folderDao.getRecycleBinFolders()

    fun getRecycleBinCount(): Flow<Int> =
        mediaDao.getRecycleBinCount()

    suspend fun createFolder(name: String, coverUri: String? = null, colorHex: String = "#FFFFFF", iconName: String = "folder"): FolderEntity = withContext(Dispatchers.IO) {
        val baseName = name.trim().ifEmpty { "New Folder" }
        val existingNames = folderDao.getAllFolderNames()
        var finalName = baseName
        if (existingNames.any { it.equals(baseName, ignoreCase = true) }) {
            var counter = 1
            while (existingNames.any { it.equals("$baseName ($counter)", ignoreCase = true) }) {
                counter++
            }
            finalName = "$baseName ($counter)"
        }

        val folder = FolderEntity(
            name = finalName,
            coverUri = coverUri,
            colorHex = colorHex,
            iconName = iconName
        )
        folderDao.insertFolder(folder)
        folder
    }

    suspend fun setFolderCover(folderId: String, coverUri: String?) = withContext(Dispatchers.IO) {
        folderDao.updateFolderCover(folderId, coverUri)
    }

    suspend fun deleteFolder(folderId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        folderDao.softDeleteFolder(folderId, now)
        mediaDao.softDeleteMediaByFolder(folderId, now)
    }

    suspend fun restoreFolder(folderId: String) = withContext(Dispatchers.IO) {
        folderDao.restoreFolder(folderId)
        mediaDao.restoreMediaByFolder(folderId)
    }

    suspend fun deleteFolderPermanently(folderId: String) = withContext(Dispatchers.IO) {
        try {
            val folderDir = File(context.filesDir, "vault/$folderId")
            if (folderDir.exists()) {
                folderDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Error deleting folder directory: $folderId", e)
        }
        mediaDao.deleteMediaByFolderId(folderId)
        folderDao.deleteFolderById(folderId)
    }

    suspend fun importUrisToFolder(
        folderId: String,
        uris: List<Uri>,
        onProgress: (current: Int, total: Int) -> Unit
    ): List<MediaEntity> = withContext(Dispatchers.IO) {
        val vaultDir = File(context.filesDir, "vault/$folderId")
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }

        val contentResolver: ContentResolver = context.contentResolver
        val importedList = mutableListOf<MediaEntity>()

        uris.forEachIndexed { index, uri ->
            try {
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val isVideo = mimeType.startsWith("video")
                val originalName = queryFileName(contentResolver, uri) ?: "media_${System.currentTimeMillis()}"

                val extension = if (isVideo) ".mp4" else ".jpg"
                val destFile = File(vaultDir, "${UUID.randomUUID()}$extension")

                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                var width = 0
                var height = 0
                var duration = 0L

                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(destFile.absolutePath)
                    val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    val dur = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    width = w?.toIntOrNull() ?: 0
                    height = h?.toIntOrNull() ?: 0
                    duration = dur?.toLongOrNull() ?: 0L
                    retriever.release()
                } catch (e: Exception) {
                    Log.w("MediaRepository", "Retriever error: ${e.message}")
                }

                val media = MediaEntity(
                    folderId = folderId,
                    filePath = destFile.absolutePath,
                    originalFileName = originalName,
                    mimeType = mimeType,
                    isVideo = isVideo,
                    durationMs = duration,
                    fileSizeBytes = destFile.length(),
                    width = width,
                    height = height
                )
                mediaDao.insertMedia(media)
                importedList.add(media)
            } catch (e: Exception) {
                Log.e("MediaRepository", "Failed to import uri: $uri", e)
            }
            onProgress(index + 1, uris.size)
        }

        folderDao.getFolderById(folderId)?.let {
            folderDao.updateFolder(it.copy(updatedAt = System.currentTimeMillis()))
        }

        importedList
    }

    private fun queryFileName(resolver: ContentResolver, uri: Uri): String? {
        return try {
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun moveToRecycleBin(ids: List<String>) = withContext(Dispatchers.IO) {
        mediaDao.moveToRecycleBin(ids, System.currentTimeMillis())
    }

    suspend fun restoreFromRecycleBin(ids: List<String>) = withContext(Dispatchers.IO) {
        val allFolders = folderDao.getAllFoldersList()
        val allFolderIds = allFolders.map { it.id }.toSet()

        ids.forEach { id ->
            val media = mediaDao.getMediaById(id)
            if (media != null && !allFolderIds.contains(media.folderId)) {
                val targetFolder = allFolders.firstOrNull() ?: createFolder("Restored Items")
                mediaDao.moveMediaToFolder(listOf(id), targetFolder.id)
            }
        }
        mediaDao.restoreFromRecycleBin(ids)
    }

    suspend fun moveMediaToFolder(ids: List<String>, newFolderId: String) = withContext(Dispatchers.IO) {
        mediaDao.moveMediaToFolder(ids, newFolderId)
        folderDao.getFolderById(newFolderId)?.let {
            folderDao.updateFolder(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun toggleFavorite(id: String) = withContext(Dispatchers.IO) {
        mediaDao.toggleFavorite(id)
    }

    suspend fun deletePermanently(ids: List<String>) = withContext(Dispatchers.IO) {
        ids.forEach { id ->
            mediaDao.getMediaById(id)?.let { media ->
                try {
                    val file = File(media.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e("MediaRepository", "Error deleting file ${media.filePath}", e)
                }
            }
        }
        mediaDao.deletePermanentlyByIds(ids)
    }

    suspend fun emptyRecycleBin() = withContext(Dispatchers.IO) {
        val list = mediaDao.getAllRecycleBinList()
        list.forEach { media ->
            try {
                val file = File(media.filePath)
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                // Ignore
            }
        }
        mediaDao.emptyRecycleBin()

        // Also purge deleted folders
        val deletedFolders = folderDao.getAllFoldersList() // All folders check or delete
        try {
            val vaultDir = File(context.filesDir, "vault")
            if (vaultDir.exists()) {
                // Cleanup empty dirs
            }
        } catch (_: Exception) {}
        folderDao.emptyRecycleBinFolders()
    }

    suspend fun clearCache(): Long = withContext(Dispatchers.IO) {
        var freedBytes = 0L
        val cacheDir = context.cacheDir
        if (cacheDir != null && cacheDir.exists()) {
            cacheDir.listFiles()?.forEach { file ->
                freedBytes += file.length()
                file.deleteRecursively()
            }
        }
        freedBytes
    }
}
