package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

    val allPhotos = remember(allMedia) { allMedia.filter { !it.isVideo } }
    val allVideos = remember(allMedia) { allMedia.filter { it.isVideo } }

    val filteredList = remember(allMedia, selectedFilter) {
        when (selectedFilter) {
            MediaTypeFilter.ALL -> allMedia.sortedByDescending { it.addedAt }
            MediaTypeFilter.PHOTOS_ONLY -> allPhotos.sortedByDescending { it.addedAt }
            MediaTypeFilter.VIDEOS_ONLY -> allVideos.sortedByDescending { it.addedAt }
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
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSelectionMode) "${selectedIds.size} selected" else "All Files",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (isSelectionMode) {
                        IconButton(
                            onClick = {
                                isSelectionMode = false
                                selectedIds.clear()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Selection",
                                tint = TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
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
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AllFilesBottomButton(
                            icon = Icons.Outlined.Share,
                            label = "Share",
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                triggerHapticFeedback(context)
                                shareMediaItems(context, filteredList.filter { selectedIds.contains(it.id) })
                            }
                        )

                        AllFilesBottomButton(
                            icon = Icons.Outlined.DriveFileMove,
                            label = "Move",
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                triggerHapticFeedback(context)
                                showMoveSheet = true
                            }
                        )

                        AllFilesBottomButton(
                            icon = Icons.Outlined.Info,
                            label = "Info",
                            enabled = selectedIds.isNotEmpty(),
                            onClick = {
                                triggerHapticFeedback(context)
                                showInfoDialog = true
                            }
                        )

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
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item(span = { GridItemSpan(4) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TopFolderCard(
                            title = "All Files",
                            count = allMedia.size,
                            thumbnail = allMedia.firstOrNull()?.filePath,
                            defaultIcon = Icons.Default.Folder,
                            isSelected = selectedFilter == MediaTypeFilter.ALL,
                            onClick = {
                                triggerHapticFeedback(context)
                                selectedFilter = MediaTypeFilter.ALL
                            },
                            modifier = Modifier.weight(1f)
                        )

                        TopFolderCard(
                            title = "All Photos",
                            count = allPhotos.size,
                            thumbnail = allPhotos.firstOrNull()?.filePath,
                            defaultIcon = Icons.Default.Photo,
                            isSelected = selectedFilter == MediaTypeFilter.PHOTOS_ONLY,
                            onClick = {
                                triggerHapticFeedback(context)
                                selectedFilter = MediaTypeFilter.PHOTOS_ONLY
                            },
                            modifier = Modifier.weight(1f)
                        )

                        TopFolderCard(
                            title = "All Videos",
                            count = allVideos.size,
                            thumbnail = allVideos.firstOrNull()?.filePath,
                            defaultIcon = Icons.Default.Videocam,
                            isSelected = selectedFilter == MediaTypeFilter.VIDEOS_ONLY,
                            onClick = {
                                triggerHapticFeedback(context)
                                selectedFilter = MediaTypeFilter.VIDEOS_ONLY
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            if (filteredList.isEmpty()) {
                item(span = { GridItemSpan(4) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (selectedFilter) {
                                MediaTypeFilter.PHOTOS_ONLY -> "No photos found"
                                MediaTypeFilter.VIDEOS_ONLY -> "No videos found"
                                MediaTypeFilter.ALL -> "No media in vault"
                            },
                            color = TextGray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                itemsIndexed(filteredList, key = { _, item -> item.id }) { index, item ->
                    val isSelected = selectedIds.contains(item.id)
                    val file = remember(item.filePath) { File(item.filePath) }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkCardBg)
                            .border(0.5.dp, DarkBorder.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .pointerInput(item.id) {
                                detectTapGestures(
                                    onTap = {
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
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(PureBlack.copy(alpha = 0.75f))
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Video",
                                        tint = TextWhite,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    if (item.durationMs > 0) {
                                        Text(
                                            text = formatDuration(item.durationMs),
                                            color = TextWhite,
                                            fontSize = 9.sp,
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
                                    .background(if (isSelected) TextWhite.copy(alpha = 0.3f) else Color.Transparent)
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) TextWhite else PureBlack.copy(alpha = 0.6f))
                                    .border(1.dp, if (isSelected) TextWhite else DarkBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = PureBlack,
                                        modifier = Modifier.size(12.dp)
                                    )
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
private fun TopFolderCard(
    title: String,
    count: Int,
    thumbnail: String?,
    defaultIcon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "top_folder_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .pointerInput(title) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(DarkCardBg)
                .border(1.dp, DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            val hasValidThumbnail = thumbnail != null && File(thumbnail).exists()
            if (hasValidThumbnail) {
                AsyncImage(
                    model = File(thumbnail),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkPillBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = defaultIcon,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Item count badge inside bottom-right of folder
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(PureBlack.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$count",
                        color = TextWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            color = if (isSelected) TextWhite else TextGray,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "$count items",
            color = TextMuted,
            fontSize = 10.sp
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
            .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (enabled) tint else TextMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (enabled) tint else TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
