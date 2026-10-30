package com.example.ui

import android.app.Application
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DeleteSourceResult
import com.example.data.FolderEntity
import com.example.data.FolderWithItemCount
import com.example.data.MediaEntity
import com.example.data.MediaRepository
import com.example.data.SecurityPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class Screen {
    ONBOARDING,
    LOCK,
    FOLDERS,
    ALL_FILES,
    GALLERY,
    MEDIA_VIEWER,
    SETTINGS,
    RECYCLE_BIN
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    val repository = MediaRepository(application)
    val securityPrefs = SecurityPreferences(application)

    private val _currentScreen = MutableStateFlow<Screen>(
        if (!securityPrefs.isSetupCompleted) Screen.ONBOARDING else Screen.LOCK
    )
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _folders = MutableStateFlow<List<FolderWithItemCount>>(emptyList())
    val folders: StateFlow<List<FolderWithItemCount>> = _folders.asStateFlow()

    private val _currentFolder = MutableStateFlow<FolderWithItemCount?>(null)
    val currentFolder: StateFlow<FolderWithItemCount?> = _currentFolder.asStateFlow()

    private val _currentFolderMedia = MutableStateFlow<List<MediaEntity>>(emptyList())
    val currentFolderMedia: StateFlow<List<MediaEntity>> = _currentFolderMedia.asStateFlow()

    private val _allActiveMedia = MutableStateFlow<List<MediaEntity>>(emptyList())
    val allActiveMedia: StateFlow<List<MediaEntity>> = _allActiveMedia.asStateFlow()

    private val _viewerMediaList = MutableStateFlow<List<MediaEntity>>(emptyList())
    val viewerMediaList: StateFlow<List<MediaEntity>> = _viewerMediaList.asStateFlow()

    private val _recycleBinMedia = MutableStateFlow<List<MediaEntity>>(emptyList())
    val recycleBinMedia: StateFlow<List<MediaEntity>> = _recycleBinMedia.asStateFlow()

    private val _recycleBinFolders = MutableStateFlow<List<FolderEntity>>(emptyList())
    val recycleBinFolders: StateFlow<List<FolderEntity>> = _recycleBinFolders.asStateFlow()

    private val _recycleBinCount = MutableStateFlow(0)
    val recycleBinCount: StateFlow<Int> = _recycleBinCount.asStateFlow()

    private val _viewerIndex = MutableStateFlow(0)
    val viewerIndex: StateFlow<Int> = _viewerIndex.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importProgress = MutableStateFlow(Pair(0, 0))
    val importProgress: StateFlow<Pair<Int, Int>> = _importProgress.asStateFlow()

    private val _pendingSharedUris = MutableStateFlow<List<Uri>?>(null)
    val pendingSharedUris: StateFlow<List<Uri>?> = _pendingSharedUris.asStateFlow()

    private val _deleteConsentIntentSender = MutableStateFlow<IntentSender?>(null)
    val deleteConsentIntentSender: StateFlow<IntentSender?> = _deleteConsentIntentSender.asStateFlow()

    private val _movedFromGalleryMessage = MutableStateFlow<String?>(null)
    val movedFromGalleryMessage: StateFlow<String?> = _movedFromGalleryMessage.asStateFlow()

    fun clearDeleteConsent() {
        _deleteConsentIntentSender.value = null
    }

    fun clearMovedMessage() {
        _movedFromGalleryMessage.value = null
    }

    init {
        // Collect folders
        viewModelScope.launch {
            repository.getAllFoldersWithCount().collectLatest { list ->
                _folders.value = list
                _currentFolder.value?.let { current ->
                    _currentFolder.value = list.find { it.id == current.id } ?: current
                }
            }
        }

        // Collect all active media across all folders
        viewModelScope.launch {
            repository.getAllActiveMedia().collectLatest { list ->
                _allActiveMedia.value = list
            }
        }

        // Collect recycle bin count
        viewModelScope.launch {
            repository.getRecycleBinCount().collectLatest { count ->
                _recycleBinCount.value = count
            }
        }

        // Collect recycle bin media
        viewModelScope.launch {
            repository.getRecycleBinMedia().collectLatest { list ->
                _recycleBinMedia.value = list
            }
        }

        // Collect recycle bin folders
        viewModelScope.launch {
            repository.getRecycleBinFolders().collectLatest { list ->
                _recycleBinFolders.value = list
            }
        }
    }

    fun onUnlockSuccess() {
        _isUnlocked.value = true
        _currentScreen.value = Screen.FOLDERS
    }

    fun onSetupCompleted() {
        _isUnlocked.value = true
        _currentScreen.value = Screen.FOLDERS
    }

    fun lockApp() {
        if (securityPrefs.isSetupCompleted) {
            _isUnlocked.value = false
            _currentScreen.value = Screen.LOCK
        }
    }

    fun selectTab(screen: Screen) {
        when (screen) {
            Screen.FOLDERS -> _currentScreen.value = Screen.FOLDERS
            Screen.ALL_FILES -> _currentScreen.value = Screen.ALL_FILES
            Screen.SETTINGS -> _currentScreen.value = Screen.SETTINGS
            else -> _currentScreen.value = screen
        }
    }

    fun openFolder(folder: FolderWithItemCount) {
        _currentFolder.value = folder
        _currentScreen.value = Screen.GALLERY
        observeFolderMedia(folder.id)
    }

    private fun observeFolderMedia(folderId: String) {
        viewModelScope.launch {
            repository.getMediaForFolder(folderId).collectLatest { mediaList ->
                _currentFolderMedia.value = mediaList
            }
        }
    }

    fun openMediaViewer(index: Int) {
        _viewerMediaList.value = _currentFolderMedia.value
        _viewerIndex.value = index
        _currentScreen.value = Screen.MEDIA_VIEWER
    }

    fun openMediaViewerFromAllFiles(item: MediaEntity, index: Int) {
        _viewerMediaList.value = _allActiveMedia.value
        _viewerIndex.value = index
        _currentScreen.value = Screen.MEDIA_VIEWER
    }

    fun openSettings() {
        _currentScreen.value = Screen.SETTINGS
    }

    fun openRecycleBin() {
        _currentScreen.value = Screen.RECYCLE_BIN
    }

    fun navigateBack() {
        when (_currentScreen.value) {
            Screen.MEDIA_VIEWER -> {
                if (_viewerMediaList.value == _allActiveMedia.value) {
                    _currentScreen.value = Screen.ALL_FILES
                } else {
                    _currentScreen.value = Screen.GALLERY
                }
            }
            Screen.GALLERY -> _currentScreen.value = Screen.FOLDERS
            Screen.RECYCLE_BIN -> _currentScreen.value = Screen.SETTINGS
            Screen.SETTINGS -> _currentScreen.value = Screen.FOLDERS
            Screen.ALL_FILES -> _currentScreen.value = Screen.FOLDERS
            else -> {}
        }
    }

    fun createFolder(name: String, coverUri: String? = null, onCreated: ((FolderEntity) -> Unit)? = null) {
        viewModelScope.launch {
            val folder = repository.createFolder(name, coverUri)
            onCreated?.invoke(folder)
        }
    }

    fun setFolderCover(folderId: String, coverUri: String?) {
        viewModelScope.launch {
            repository.setFolderCover(folderId, coverUri)
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
        }
    }

    fun restoreFolder(folderId: String) {
        viewModelScope.launch {
            repository.restoreFolder(folderId)
        }
    }

    fun permanentDeleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.deleteFolderPermanently(folderId)
        }
    }

    fun importUris(uris: List<Uri>, targetFolderId: String? = null) {
        val folderId = targetFolderId ?: _currentFolder.value?.id ?: _folders.value.firstOrNull()?.id
        if (folderId == null) {
            createFolder("Default") { newFolder ->
                importUris(uris, newFolder.id)
            }
            return
        }

        viewModelScope.launch {
            _isImporting.value = true
            _importProgress.value = Pair(0, uris.size)
            val imported = repository.importUrisToFolder(
                folderId = folderId,
                uris = uris,
                onProgress = { cur, tot ->
                    _importProgress.value = Pair(cur, tot)
                }
            )
            _isImporting.value = false
            _pendingSharedUris.value = null

            if (imported.isNotEmpty() && securityPrefs.isAutoDeleteFromGalleryEnabled) {
                when (val result = repository.deleteSourceUris(uris)) {
                    is DeleteSourceResult.ConsentNeeded -> {
                        _deleteConsentIntentSender.value = result.intentSender
                    }
                    is DeleteSourceResult.Done -> {
                        _movedFromGalleryMessage.value = "Original media removed from phone gallery"
                    }
                }
            }
        }
    }

    fun moveToRecycleBin(ids: List<String>) {
        viewModelScope.launch {
            repository.moveToRecycleBin(ids)
        }
    }

    fun restoreFromRecycleBin(ids: List<String>) {
        viewModelScope.launch {
            repository.restoreFromRecycleBin(ids)
        }
    }

    fun moveMediaToFolder(ids: List<String>, newFolderId: String) {
        viewModelScope.launch {
            repository.moveMediaToFolder(ids, newFolderId)
        }
    }

    fun toggleFavorite(mediaId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(mediaId)
        }
    }

    fun permanentDelete(ids: List<String>) {
        viewModelScope.launch {
            repository.deletePermanently(ids)
        }
    }

    fun emptyRecycleBin() {
        viewModelScope.launch {
            repository.emptyRecycleBin()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
        }
    }

    fun handleSharedUris(uris: List<Uri>) {
        if (uris.isNotEmpty()) {
            _pendingSharedUris.value = uris
        }
    }

    fun dismissSharedOverlay() {
        _pendingSharedUris.value = null
    }
}
