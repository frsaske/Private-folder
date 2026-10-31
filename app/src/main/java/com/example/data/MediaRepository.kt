package com.example.data

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed class DeleteSourceResult {
    object Done : DeleteSourceResult()
    data class ConsentNeeded(val intentSender: IntentSender) : DeleteSourceResult()
}

class MediaRepository(private val context: Context) {
    private val db = VaultDatabase.getDatabase(context)
    private val folderDao = db.folderDao()
    private val mediaDao = db.mediaDao()

    fun getAllFoldersWithCount(space: String = "MAIN"): Flow<List<FolderWithItemCount>> =
        folderDao.getFoldersForSpaceWithCount(space)

    fun getMediaForFolder(folderId: String): Flow<List<MediaEntity>> =
        mediaDao.getMediaForFolder(folderId)

    fun getAllActiveMedia(space: String = "MAIN"): Flow<List<MediaEntity>> =
        mediaDao.getAllActiveMedia(space)

    fun getAllActivePhotos(space: String = "MAIN"): Flow<List<MediaEntity>> =
        mediaDao.getAllActivePhotos(space)

    fun getAllActiveVideos(space: String = "MAIN"): Flow<List<MediaEntity>> =
        mediaDao.getAllActiveVideos(space)

    fun getRecycleBinMedia(space: String = "MAIN"): Flow<List<MediaEntity>> =
        mediaDao.getRecycleBinMedia(space)

    fun getRecycleBinFolders(): Flow<List<FolderEntity>> =
        folderDao.getRecycleBinFolders()

    fun getRecycleBinCount(space: String = "MAIN"): Flow<Int> =
        mediaDao.getRecycleBinCount(space)

    suspend fun togglePinFolder(folderId: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        folderDao.updateFolderPinned(folderId, isPinned)
    }

    suspend fun ensureUltraPrivateFolder() = withContext(Dispatchers.IO) {
        val existing = folderDao.getFolderById("ultra_private_root")
        if (existing == null) {
            folderDao.insertFolder(
                FolderEntity(
                    id = "ultra_private_root",
                    name = "Ultra Private",
                    coverUri = null,
                    colorHex = "#FF2D55",
                    iconName = "app_icon",
                    isPinned = true,
                    isUltraPrivate = true,
                    space = "MAIN"
                )
            )
        }
    }

    suspend fun createFolder(
        name: String,
        coverUri: String? = null,
        colorHex: String = "#FFFFFF",
        iconName: String = "folder",
        space: String = "MAIN",
        isUltraPrivate: Boolean = false
    ): FolderEntity = withContext(Dispatchers.IO) {
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
            iconName = iconName,
            space = space,
            isUltraPrivate = isUltraPrivate
        )
        folderDao.insertFolder(folder)
        folder
    }

    private fun getPrivateStorageDir(folderName: String): File {
        val safeName = folderName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifEmpty { "general" }
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val privateStorageRoot = File(base, "private-storage")
        if (!privateStorageRoot.exists()) {
            privateStorageRoot.mkdirs()
            try { File(privateStorageRoot, ".nomedia").createNewFile() } catch (_: Exception) {}
        }
        val folderDir = File(privateStorageRoot, safeName)
        if (!folderDir.exists()) {
            folderDir.mkdirs()
            try { File(folderDir, ".nomedia").createNewFile() } catch (_: Exception) {}
        }
        return folderDir
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
            val folder = folderDao.getFolderById(folderId)
            if (folder != null) {
                val folderDir = getPrivateStorageDir(folder.name)
                if (folderDir.exists()) {
                    folderDir.deleteRecursively()
                }
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
        val folder = folderDao.getFolderById(folderId)
        val folderName = folder?.name ?: "folder_$folderId"
        val targetSpace = folder?.space ?: "MAIN"
        val storageDir = getPrivateStorageDir(folderName)

        val contentResolver: ContentResolver = context.contentResolver
        val importedList = mutableListOf<MediaEntity>()

        uris.forEachIndexed { index, uri ->
            try {
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val isVideo = mimeType.startsWith("video")
                val originalName = queryFileName(contentResolver, uri) ?: "media_${System.currentTimeMillis()}"

                val extension = if (isVideo) ".mp4" else ".jpg"
                val safeFileName = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}$extension"
                val destFile = File(storageDir, safeFileName)

                // Copy 1:1 original data without quality loss or resizing
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
                    height = height,
                    space = targetSpace
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

    suspend fun deleteSourceUris(uris: List<Uri>): DeleteSourceResult = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) return@withContext DeleteSourceResult.Done

        val mediaStoreUris = mutableListOf<Uri>()

        for (uri in uris) {
            val resolved = resolveMediaStoreUri(uri)
            if (resolved != null && resolved.authority == MediaStore.AUTHORITY) {
                mediaStoreUris.add(resolved)
            } else {
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (_: Exception) {
                }
            }
        }

        if (mediaStoreUris.isEmpty()) {
            return@withContext DeleteSourceResult.Done
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return@withContext try {
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, mediaStoreUris)
                DeleteSourceResult.ConsentNeeded(pendingIntent.intentSender)
            } catch (e: Exception) {
                directDelete(mediaStoreUris)
                DeleteSourceResult.Done
            }
        }

        for (uri in mediaStoreUris) {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                    return@withContext DeleteSourceResult.ConsentNeeded(e.userAction.actionIntent.intentSender)
                }
            } catch (_: Exception) {
            }
        }

        DeleteSourceResult.Done
    }

    private fun directDelete(uris: List<Uri>) {
        for (uri in uris) {
            try {
                context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {
            }
        }
    }

    private fun resolveMediaStoreUri(uri: Uri): Uri? {
        val uriStr = uri.toString()
        if (uriStr.startsWith("content://media/external/")) {
            return uri
        }

        try {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                    if (idCol >= 0) {
                        val id = cursor.getLong(idCol)
                        val mimeType = context.contentResolver.getType(uri) ?: ""
                        val baseUri = if (mimeType.startsWith("video")) {
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        } else {
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        }
                        return ContentUris.withAppendedId(baseUri, id)
                    }
                }
            }
        } catch (_: Exception) {
        }

        val id = uri.lastPathSegment?.toLongOrNull()
        if (id != null) {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val baseUri = if (mimeType.startsWith("video")) {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            return ContentUris.withAppendedId(baseUri, id)
        }

        if (DocumentsContract.isDocumentUri(context, uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            if (docId != null && docId.contains(":")) {
                val parts = docId.split(":")
                val mediaId = parts.getOrNull(1)?.toLongOrNull()
                if (mediaId != null) {
                    val baseUri = if (parts[0].equals("video", ignoreCase = true)) {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    return ContentUris.withAppendedId(baseUri, mediaId)
                }
            }
        }

        return if (uri.authority == MediaStore.AUTHORITY) uri else null
    }
}
