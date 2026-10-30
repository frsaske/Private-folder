package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.FolderWithItemCount
import com.example.data.MediaEntity
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.*
import java.io.File

enum class MediaTypeFilter {
    ALL,
    PHOTOS_ONLY,
    VIDEOS_ONLY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFilesScreen(
    allMedia: List<MediaEntity>,
    allFolders: List<FolderWithItemCount>,
    onMediaClick: (MediaEntity, Int) -> Unit,
    onDeleteSelected: (List<String>) -> Unit,
    onMoveSelected: (List<String>, newFolderId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(MediaTypeFilter.ALL) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var showMoveSheet by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val filteredList = remember(allMedia, selectedFilter) {
        when (selectedFilter) {
            MediaTypeFilter.ALL -> allMedia.sortedByDescending { it.addedAt }
            MediaTypeFilter.PHOTOS_ONLY -> allMedia.filter { !it.isVideo }.sortedByDescending { it.addedAt }
            MediaTypeFilter.VIDEOS_ONLY -> allMedia.filter { it.isVideo }.sortedByDescending { it.addedAt }
        }
    }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedIds.clear()
    }

    Scaffold(
        containerColor = PureBlack,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureBlack)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "All Files",
                            color = TextWhite,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${filteredList.size} items • Latest to oldest",
                            color = TextGray,
                            fontSize = 13.sp
                        )
                    }

                    if (filteredList.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                triggerHapticFeedback(context)
                                isSelectionMode = !isSelectionMode
                                if (!isSelectionMode) selectedIds.clear()
                            },
                            modifier = Modifier.testTag("all_files_select_button")
                        ) {
                            Text(
                                text = if (isSelectionMode) "Done" else "Select",
                                color = TextWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Top Two Buttons: "All Photos" and "All Videos" (plus "All")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPillButton(
                        label = "All (${allMedia.size})",
                        isSelected = selectedFilter == MediaTypeFilter.ALL,
                        onClick = {
                            triggerHapticFeedback(context)
                            selectedFilter = MediaTypeFilter.ALL
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterPillButton(
                        label = "All Photos",
                        isSelected = selectedFilter == MediaTypeFilter.PHOTOS_ONLY,
                        onClick = {
                            triggerHapticFeedback(context)
                            selectedFilter = MediaTypeFilter.PHOTOS_ONLY
                        },
                        modifier = Modifier.weight(1.2f)
                    )

                    FilterPillButton(
                        label = "All Videos",
                        isSelected = selectedFilter == MediaTypeFilter.VIDEOS_ONLY,
                        onClick = {
                            triggerHapticFeedback(context)
                            selectedFilter = MediaTypeFilter.VIDEOS_ONLY
                        },
                        modifier = Modifier.weight(1.2f)
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
                        AllFilesBottomButton(
                            icon = Icons.Outlined.Share,
                            label = "Share",
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                triggerHapticFeedback(context)
                                shareMediaItems(context, filteredList.filter { selectedIds.contains(it.id) })
                            }
                        )

                        // Move
                        AllFilesBottomButton(
                            icon = Icons.Outlined.DriveFileMove,
                            label = "Move",
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                triggerHapticFeedback(context)
                                showMoveSheet = true
                            }
                        )

                        // Info
                        AllFilesBottomButton(
                            icon = Icons.Outlined.Info,
                            label = "Info",
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                triggerHapticFeedback(context)
                                showInfoDialog = true
                            }
                        )

                        // Delete
                        AllFilesBottomButton(
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
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(DarkCardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedFilter == MediaTypeFilter.VIDEOS_ONLY) Icons.Default.Videocam else Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = when (selectedFilter) {
                                MediaTypeFilter.PHOTOS_ONLY -> "No Photos in Vault"
                                MediaTypeFilter.VIDEOS_ONLY -> "No Videos in Vault"
                                MediaTypeFilter.ALL -> "No Media in Vault"
                            },
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add photos and videos to any folder to view them here.",
                            color = TextGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(filteredList, key = { _, item -> item.id }) { index, item ->
                        val isSelected = selectedIds.contains(item.id)
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
                                        onTap = {
                                            triggerHapticFeedback(context)
                                            if (isSelectionMode) {
                                                if (isSelected) selectedIds.remove(item.id)
                                                else selectedIds.add(item.id)
                                            } else {
                                                onMediaClick(item, index)
                                            }
                                        },
                                        onLongPress = {
                                            triggerHapticFeedback(context)
                                            if (!isSelectionMode) {
                                                isSelectionMode = true
                                                selectedIds.add(item.id)
                                            }
                                        }
                                    )
                                }
                                .testTag("all_files_item_${item.id}")
                        ) {
                            AsyncImage(
                                model = file,
                                contentDescription = item.originalFileName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

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
                }
            }
        }
    }

    if (showMoveSheet) {
        MoveToFolderBottomSheet(
            currentFolderId = "",
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
            selectedItems = filteredList.filter { selectedIds.contains(it.id) },
            onDismiss = { showInfoDialog = false }
        )
    }
}

@Composable
private fun FilterPillButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(CircleShape)
            .background(if (isSelected) TextWhite else DarkCardBg)
            .border(1.dp, if (isSelected) TextWhite else DarkBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) PureBlack else TextWhite,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun AllFilesBottomButton(
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
