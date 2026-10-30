package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.FolderEntity
import com.example.data.MediaEntity
import com.example.ui.components.OpenSourcePressButton
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    deletedItems: List<MediaEntity>,
    deletedFolders: List<FolderEntity> = emptyList(),
    onBack: () -> Unit,
    onRestore: (List<String>) -> Unit,
    onPermanentDelete: (List<String>) -> Unit,
    onRestoreFolder: (String) -> Unit = {},
    onPermanentDeleteFolder: (String) -> Unit = {},
    onEmptyBin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var showEmptyBinDialog by remember { mutableStateOf(false) }
    var folderActionTarget by remember { mutableStateOf<FolderEntity?>(null) }

    val totalCount = deletedItems.size + deletedFolders.size

    BackHandler {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedIds.clear()
        } else {
            onBack()
        }
    }

    fun calculateDaysLeft(deletedAt: Long?): Int {
        if (deletedAt == null) return 30
        val elapsedMs = System.currentTimeMillis() - deletedAt
        val daysElapsed = (elapsedMs / (1000L * 60 * 60 * 24)).toInt()
        return (30 - daysElapsed).coerceIn(1, 30)
    }

    Scaffold(
        containerColor = PureBlack,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Recycle Bin",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalCount items in trash",
                            color = TextGray,
                            fontSize = 12.sp
                        )
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
                        modifier = Modifier.testTag("recycle_bin_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                actions = {
                    if (totalCount > 0) {
                        // Clear Recycle Bin Permanently Button on top right
                        TextButton(
                            onClick = {
                                triggerHapticFeedback(context)
                                showEmptyBinDialog = true
                            },
                            modifier = Modifier.testTag("empty_bin_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All",
                                tint = AccentRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Clear All",
                                color = AccentRed,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureBlack
                )
            )
        },
        bottomBar = {
            if (isSelectionMode && selectedIds.isNotEmpty()) {
                Surface(
                    color = DarkCardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OpenSourcePressButton(
                            onClick = {
                                triggerHapticFeedback(context)
                                onRestore(selectedIds.toList())
                                isSelectionMode = false
                                selectedIds.clear()
                            },
                            backgroundColor = DarkPillBg,
                            contentColor = TextWhite,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, tint = TextWhite, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        OpenSourcePressButton(
                            onClick = {
                                triggerHapticFeedback(context)
                                onPermanentDelete(selectedIds.toList())
                                isSelectionMode = false
                                selectedIds.clear()
                            },
                            backgroundColor = DarkPillBg,
                            contentColor = AccentRed,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete Forever", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Notice Banner
            Surface(
                color = DarkCardBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Items and folders are kept for 30 days before being permanently deleted.",
                        color = TextGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            if (totalCount == 0) {
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
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(DarkCardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Recycle Bin is Empty",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Deleted Folders Section
                    if (deletedFolders.isNotEmpty()) {
                        item {
                            Text(
                                text = "DELETED FOLDERS",
                                color = TextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 6.dp)
                            )
                        }

                        items(deletedFolders, key = { it.id }) { folder ->
                            val daysLeft = calculateDaysLeft(folder.deletedAt)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkCardBg)
                                    .border(1.dp, DarkBorder.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .clickable {
                                        triggerHapticFeedback(context)
                                        folderActionTarget = folder
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = TextWhite,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = folder.name,
                                        color = TextWhite,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "$daysLeft days left before permanent delete",
                                        color = TextGray,
                                        fontSize = 11.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            triggerHapticFeedback(context)
                                            onRestoreFolder(folder.id)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = "Restore Folder",
                                            tint = TextWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            triggerHapticFeedback(context)
                                            onPermanentDeleteFolder(folder.id)
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = "Delete Permanently",
                                            tint = AccentRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Deleted Media Section
                    if (deletedItems.isNotEmpty()) {
                        item {
                            Text(
                                text = "DELETED PHOTOS & VIDEOS",
                                color = TextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 6.dp)
                            )
                        }

                        item {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 1000.dp)
                            ) {
                                items(deletedItems, key = { it.id }) { item ->
                                    val isSelected = selectedIds.contains(item.id)
                                    val file = remember(item.filePath) { File(item.filePath) }
                                    val daysLeft = calculateDaysLeft(item.deletedAt)

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
                                                        isSelectionMode = true
                                                        if (isSelected) selectedIds.remove(item.id)
                                                        else selectedIds.add(item.id)
                                                    },
                                                    onLongPress = {
                                                        triggerHapticFeedback(context)
                                                        isSelectionMode = true
                                                        selectedIds.add(item.id)
                                                    }
                                                )
                                            }
                                            .testTag("recycle_item_${item.id}")
                                    ) {
                                        AsyncImage(
                                            model = file,
                                            contentDescription = item.originalFileName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        if (item.isVideo) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Video",
                                                tint = TextWhite,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(4.dp)
                                                    .size(16.dp)
                                            )
                                        }

                                        // Days left to permanent delete on small bottom right!
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(3.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(PureBlack.copy(alpha = 0.8f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${daysLeft}d left",
                                                color = TextWhite,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
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
            }
        }
    }

    if (showEmptyBinDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyBinDialog = false },
            containerColor = DarkCardBg,
            title = {
                Text(
                    text = "Clear Recycle Bin Permanently?",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "All $totalCount items and folders will be erased immediately. This cannot be undone.",
                    color = TextGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmptyBinDialog = false
                        onEmptyBin()
                        isSelectionMode = false
                        selectedIds.clear()
                    }
                ) {
                    Text("Delete Permanently", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyBinDialog = false }) {
                    Text("Cancel", color = TextWhite)
                }
            }
        )
    }

    if (folderActionTarget != null) {
        AlertDialog(
            onDismissRequest = { folderActionTarget = null },
            containerColor = DarkCardBg,
            title = {
                Text(
                    text = folderActionTarget?.name ?: "Folder",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Restore this folder and its media, or delete permanently?",
                    color = TextGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        folderActionTarget?.let { onRestoreFolder(it.id) }
                        folderActionTarget = null
                    }
                ) {
                    Text("Restore", color = TextWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        folderActionTarget?.let { onPermanentDeleteFolder(it.id) }
                        folderActionTarget = null
                    }
                ) {
                    Text("Delete Forever", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
