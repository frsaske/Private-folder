package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.FolderWithItemCount
import com.example.data.MediaEntity
import com.example.ui.components.OpenSourcePressButton
import com.example.ui.components.ProgressLoadingDialog
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.*
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    folderId: String,
    folderName: String,
    mediaList: List<MediaEntity>,
    allFolders: List<FolderWithItemCount>,
    onBack: () -> Unit,
    onMediaClick: (index: Int) -> Unit,
    onImportUris: (List<Uri>) -> Unit,
    onDeleteSelected: (List<String>) -> Unit,
    onMoveSelected: (List<String>, newFolderId: String) -> Unit,
    isImporting: Boolean,
    importProgress: Pair<Int, Int>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var sortDescending by remember { mutableStateOf(true) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val sortedList = remember(mediaList, sortDescending) {
        if (sortDescending) {
            mediaList.sortedByDescending { it.addedAt }
        } else {
            mediaList.sortedBy { it.addedAt }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportUris(uris)
        }
    }

    BackHandler {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedIds.clear()
        } else {
            onBack()
        }
    }

    Scaffold(
        containerColor = PureBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = folderName,
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "private-storage/$folderName • ${mediaList.size} items",
                                color = TextGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            triggerHapticFeedback(context)
                            if (isSelectionMode) {
                                isSelectionMode = false
                                selectedIds.clear()
                            } else {
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("gallery_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                actions = {
                    if (mediaList.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                triggerHapticFeedback(context)
                                shareMediaItems(context, mediaList)
                            },
                            modifier = Modifier.testTag("gallery_share_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share All in Folder",
                                tint = TextWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        TextButton(
                            onClick = {
                                triggerHapticFeedback(context)
                                isSelectionMode = !isSelectionMode
                                if (!isSelectionMode) selectedIds.clear()
                            },
                            modifier = Modifier.testTag("gallery_select_toggle_button")
                        ) {
                            Text(
                                text = if (isSelectionMode) "Done" else "Select",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureBlack
                )
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = {
                        triggerHapticFeedback(context)
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    containerColor = TextWhite,
                    contentColor = PureBlack,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 12.dp, end = 12.dp)
                        .testTag("gallery_add_media_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Photos or Videos",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = DarkCardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Share
                        GalleryBottomActionButton(
                            icon = Icons.Outlined.Share,
                            label = "Share",
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                triggerHapticFeedback(context)
                                shareMediaItems(context, sortedList.filter { selectedIds.contains(it.id) })
                            }
                        )

                        // Move
                        GalleryBottomActionButton(
                            icon = Icons.Outlined.DriveFileMove,
                            label = "Move",
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                triggerHapticFeedback(context)
                                showMoveSheet = true
                            }
                        )

                        // Info
                        GalleryBottomActionButton(
                            icon = Icons.Outlined.Info,
                            label = "Info",
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                triggerHapticFeedback(context)
                                showInfoDialog = true
                            }
                        )

                        // Delete
                        GalleryBottomActionButton(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            enabled = selectedIds.isNotEmpty(),
                            tint = AccentRed,
                            onClick = {
                                triggerHapticFeedback(context)
                                onDeleteSelected(selectedIds.toList())
                                isSelectionMode = false
                                selectedIds.clear()
                            }
                        )
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (sortedList.isEmpty()) {
                EmptyGalleryState(
                    onAddClick = {
                        triggerHapticFeedback(context)
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(sortedList, key = { _, item -> item.id }) { index, item ->
                        val isSelected = selectedIds.contains(item.id)

                        MediaGridItem(
                            item = item,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onClick = {
                                triggerHapticFeedback(context)
                                if (isSelectionMode) {
                                    if (isSelected) selectedIds.remove(item.id)
                                    else selectedIds.add(item.id)
                                } else {
                                    onMediaClick(index)
                                }
                            },
                            onLongClick = {
                                triggerHapticFeedback(context)
                                if (!isSelectionMode) {
                                    isSelectionMode = true
                                    selectedIds.add(item.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (isImporting) {
        ProgressLoadingDialog(
            title = "Encrypting & moving files...",
            current = importProgress.first,
            total = importProgress.second
        )
    }

    if (showMoveSheet) {
        MoveToFolderBottomSheet(
            currentFolderId = folderId,
            folders = allFolders,
            onDismiss = { showMoveSheet = false },
            onSelectFolder = { targetFolderId ->
                showMoveSheet = false
                onMoveSelected(selectedIds.toList(), targetFolderId)
                isSelectionMode = false
                selectedIds.clear()
            }
        )
    }

    if (showInfoDialog) {
        SelectedMediaInfoDialog(
            selectedItems = sortedList.filter { selectedIds.contains(it.id) },
            onDismiss = { showInfoDialog = false }
        )
    }
}

@Composable
private fun MediaGridItem(
    item: MediaEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val file = remember(item.filePath) { File(item.filePath) }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCardBg)
            .border(1.dp, DarkBorder.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .pointerInput(item.id) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .testTag("media_item_${item.id}")
    ) {
        AsyncImage(
            model = file,
            contentDescription = item.originalFileName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Video Duration & Icon
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PureBlack.copy(alpha = 0.75f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = TextWhite,
                        modifier = Modifier.size(12.dp)
                    )
                    if (item.durationMs > 0) {
                        Text(
                            text = formatDuration(item.durationMs),
                            color = TextWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Selection Checkmark
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) TextWhite.copy(alpha = 0.35f) else Color.Transparent)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) TextWhite else PureBlack.copy(alpha = 0.6f))
                    .border(1.5.dp, if (isSelected) TextWhite else DarkBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = PureBlack,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryBottomActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    tint: Color = TextWhite,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) tint else TextMuted,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (enabled) tint else TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyGalleryState(
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(DarkCardBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                tint = TextWhite,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No Photos or Videos",
            color = TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Add photos and videos to this private folder. They are encrypted and hidden from your gallery.",
            color = TextGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        OpenSourcePressButton(
            onClick = onAddClick,
            backgroundColor = TextWhite,
            contentColor = PureBlack,
            modifier = Modifier.testTag("empty_gallery_add_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = PureBlack,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Media",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToFolderBottomSheet(
    currentFolderId: String,
    folders: List<FolderWithItemCount>,
    onDismiss: () -> Unit,
    onSelectFolder: (String) -> Unit
) {
    val destinationFolders = remember(folders, currentFolderId) {
        folders.filter { it.id != currentFolderId }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkCardBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Move to Folder",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (destinationFolders.isEmpty()) {
                Text(
                    text = "No other folders available. Create another folder first.",
                    color = TextGray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                destinationFolders.forEach { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFolder(folder.id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = TextWhite,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = folder.name,
                            color = TextWhite,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${folder.itemCount} items",
                            color = TextGray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedMediaInfoDialog(
    selectedItems: List<MediaEntity>,
    onDismiss: () -> Unit
) {
    val totalBytes = remember(selectedItems) { selectedItems.sumOf { it.fileSizeBytes } }
    val photoCount = remember(selectedItems) { selectedItems.count { !it.isVideo } }
    val videoCount = remember(selectedItems) { selectedItems.count { it.isVideo } }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Text(
                    text = "Selection Info",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(14.dp))

                InfoRow(label = "Total Items", value = "${selectedItems.size}")
                InfoRow(label = "Photos", value = "$photoCount")
                InfoRow(label = "Videos", value = "$videoCount")
                InfoRow(label = "Total Vault Size", value = formatFileSize(totalBytes))

                Spacer(modifier = Modifier.height(20.dp))

                OpenSourcePressButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = TextWhite,
                    contentColor = PureBlack
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextGray, fontSize = 14.sp)
        Text(text = value, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.getDefault(), "%.1f GB", gb)
        mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.getDefault(), "%.1f KB", kb)
        else -> "$bytes B"
    }
}

fun shareMediaItems(context: Context, items: List<MediaEntity>) {
    if (items.isEmpty()) return
    try {
        val uris = ArrayList<Uri>()
        items.forEach { media ->
            val file = File(media.filePath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                uris.add(uri)
            }
        }

        if (uris.isEmpty()) return

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = items.first().mimeType
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        context.startActivity(Intent.createChooser(intent, "Share Media"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Cannot share files", android.widget.Toast.LENGTH_SHORT).show()
    }
}
