package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.ui.Screen
import com.example.ui.VaultViewModel
import com.example.ui.components.VaultBottomNavBar
import com.example.ui.screens.*
import com.example.ui.theme.PrivateFolderTheme
import com.example.ui.theme.PureBlack

class MainActivity : FragmentActivity() {
    private val viewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntentMedia(intent)

        setContent {
            PrivateFolderTheme {
                VaultApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentMedia(intent)
    }

    override fun onStop() {
        super.onStop()
        if (viewModel.securityPrefs.isLockAppEnabled) {
            viewModel.lockApp()
        }
    }

    @Suppress("DEPRECATION")
    private fun handleIntentMedia(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type ?: ""

        if (Intent.ACTION_SEND == action && (type.startsWith("image/") || type.startsWith("video/"))) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (uri != null) {
                viewModel.handleSharedUris(listOf(uri))
            }
        } else if (Intent.ACTION_SEND_MULTIPLE == action && (type.startsWith("image/") || type.startsWith("video/"))) {
            val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
            }
            if (!uris.isNullOrEmpty()) {
                viewModel.handleSharedUris(uris.filterNotNull())
            }
        }
    }
}

@Composable
fun VaultApp(viewModel: VaultViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()
    val currentFolderMedia by viewModel.currentFolderMedia.collectAsState()
    val allActiveMedia by viewModel.allActiveMedia.collectAsState()
    val viewerMediaList by viewModel.viewerMediaList.collectAsState()
    val recycleBinMedia by viewModel.recycleBinMedia.collectAsState()
    val recycleBinFolders by viewModel.recycleBinFolders.collectAsState()
    val recycleBinCount by viewModel.recycleBinCount.collectAsState()
    val viewerIndex by viewModel.viewerIndex.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val pendingSharedUris by viewModel.pendingSharedUris.collectAsState()

    val showBottomBar = isUnlocked && (
        currentScreen == Screen.FOLDERS ||
        currentScreen == Screen.ALL_FILES ||
        currentScreen == Screen.GALLERY ||
        currentScreen == Screen.SETTINGS
    )

    Scaffold(
        containerColor = PureBlack,
        bottomBar = {
            if (showBottomBar) {
                VaultBottomNavBar(
                    currentScreen = currentScreen,
                    onTabSelected = { tab -> viewModel.selectTab(tab) },
                    allFilesCount = allActiveMedia.size
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PureBlack)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screen_transition",
                modifier = Modifier.fillMaxSize()
            ) { screen ->
                when (screen) {
                    Screen.ONBOARDING -> {
                        OnboardingScreen(
                            securityPrefs = viewModel.securityPrefs,
                            onSetupCompleted = { viewModel.onSetupCompleted() }
                        )
                    }

                    Screen.LOCK -> {
                        LockScreen(
                            securityPrefs = viewModel.securityPrefs,
                            onUnlockSuccess = { viewModel.onUnlockSuccess() }
                        )
                    }

                    Screen.FOLDERS -> {
                        FoldersScreen(
                            folders = folders,
                            onFolderClick = { folder -> viewModel.openFolder(folder) },
                            onCreateFolder = { name, coverUri -> viewModel.createFolder(name, coverUri) },
                            onSetFolderCover = { folderId, coverUri -> viewModel.setFolderCover(folderId, coverUri) },
                            onDeleteFolder = { folderId -> viewModel.deleteFolder(folderId) },
                            onOpenSettings = { viewModel.openSettings() }
                        )
                    }

                    Screen.ALL_FILES -> {
                        AllFilesScreen(
                            allMedia = allActiveMedia,
                            allFolders = folders,
                            onMediaClick = { item, index -> viewModel.openMediaViewerFromAllFiles(item, index) },
                            onDeleteSelected = { ids -> viewModel.moveToRecycleBin(ids) },
                            onMoveSelected = { ids, newFolderId -> viewModel.moveMediaToFolder(ids, newFolderId) }
                        )
                    }

                    Screen.GALLERY -> {
                        val activeFolder = currentFolder ?: folders.firstOrNull()
                        if (activeFolder != null) {
                            GalleryScreen(
                                folderId = activeFolder.id,
                                folderName = activeFolder.name,
                                mediaList = currentFolderMedia,
                                allFolders = folders,
                                onBack = { viewModel.navigateBack() },
                                onMediaClick = { index -> viewModel.openMediaViewer(index) },
                                onImportUris = { uris -> viewModel.importUris(uris) },
                                onDeleteSelected = { ids -> viewModel.moveToRecycleBin(ids) },
                                onMoveSelected = { ids, newFolderId -> viewModel.moveMediaToFolder(ids, newFolderId) },
                                isImporting = isImporting,
                                importProgress = importProgress
                            )
                        } else {
                            FoldersScreen(
                                folders = folders,
                                onFolderClick = { folder -> viewModel.openFolder(folder) },
                                onCreateFolder = { name, coverUri -> viewModel.createFolder(name, coverUri) },
                                onSetFolderCover = { folderId, coverUri -> viewModel.setFolderCover(folderId, coverUri) },
                                onDeleteFolder = { folderId -> viewModel.deleteFolder(folderId) },
                                onOpenSettings = { viewModel.openSettings() }
                            )
                        }
                    }

                    Screen.MEDIA_VIEWER -> {
                        MediaViewerScreen(
                            mediaList = viewerMediaList.ifEmpty { currentFolderMedia },
                            initialIndex = viewerIndex,
                            onBack = { viewModel.navigateBack() },
                            onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                            onDeleteMedia = { id ->
                                viewModel.moveToRecycleBin(listOf(id))
                                viewModel.navigateBack()
                            }
                        )
                    }

                    Screen.SETTINGS -> {
                        SettingsScreen(
                            securityPrefs = viewModel.securityPrefs,
                            recycleBinCount = recycleBinCount,
                            onBack = { viewModel.navigateBack() },
                            onOpenRecycleBin = { viewModel.openRecycleBin() },
                            onLockAppNow = { viewModel.lockApp() }
                        )
                    }

                    Screen.RECYCLE_BIN -> {
                        RecycleBinScreen(
                            deletedItems = recycleBinMedia,
                            deletedFolders = recycleBinFolders,
                            onBack = { viewModel.navigateBack() },
                            onRestore = { ids -> viewModel.restoreFromRecycleBin(ids) },
                            onPermanentDelete = { ids -> viewModel.permanentDelete(ids) },
                            onRestoreFolder = { folderId -> viewModel.restoreFolder(folderId) },
                            onPermanentDeleteFolder = { folderId -> viewModel.permanentDeleteFolder(folderId) },
                            onEmptyBin = { viewModel.emptyRecycleBin() }
                        )
                    }
                }
            }

            // Shared URIs Overlay from external Share Sheet
            pendingSharedUris?.let { uris ->
                ShareReceiverOverlay(
                    sharedUris = uris,
                    folders = folders,
                    onSelectFolder = { targetFolderId ->
                        viewModel.importUris(uris, targetFolderId)
                    },
                    onCreateFolderAndSelect = { name, coverUri ->
                        viewModel.createFolder(name, coverUri) { newFolder ->
                            viewModel.importUris(uris, newFolder.id)
                        }
                    },
                    onDismiss = { viewModel.dismissSharedOverlay() },
                    isImporting = isImporting,
                    importProgress = importProgress
                )
            }
        }
    }
}
