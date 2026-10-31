package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.FolderWithItemCount
import com.example.ui.components.OpenSourcePillSearchBar
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    folders: List<FolderWithItemCount>,
    onFolderClick: (FolderWithItemCount) -> Unit,
    onUltraPrivateClick: () -> Unit,
    onCreateFolder: (name: String, coverUri: String?) -> Unit,
    onSetFolderCover: (folderId: String, coverUri: String?) -> Unit,
    onTogglePinFolder: (folderId: String, isPinned: Boolean) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedFolderForOptions by remember { mutableStateOf<FolderWithItemCount?>(null) }
    var folderForCoverSelection by remember { mutableStateOf<FolderWithItemCount?>(null) }
    var folderToDelete by remember { mutableStateOf<FolderWithItemCount?>(null) }

    val filteredFolders = remember(folders, searchQuery) {
        if (searchQuery.isBlank()) folders
        else folders.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && folderForCoverSelection != null) {
            onSetFolderCover(folderForCoverSelection!!.id, uri.toString())
            folderForCoverSelection = null
        }
    }

    Scaffold(
        containerColor = PureBlack,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureBlack)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                // Header: Folders title, search icon toggle, settings icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Folders",
                        color = TextWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isSearchVisible = !isSearchVisible
                                if (!isSearchVisible) searchQuery = ""
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("folders_search_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                triggerHapticFeedback(context)
                                onOpenSettings()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("folders_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Smoothly expandable search bar
                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        OpenSourcePillSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onClear = { searchQuery = "" },
                            placeholder = "Search folders..."
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    triggerHapticFeedback(context)
                    showCreateSheet = true
                },
                containerColor = TextWhite,
                contentColor = PureBlack,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 8.dp, end = 8.dp)
                    .testTag("create_folder_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Folder",
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (filteredFolders.isEmpty()) {
                EmptyFoldersState(
                    onCreateClick = {
                        triggerHapticFeedback(context)
                        showCreateSheet = true
                    },
                    isSearch = searchQuery.isNotBlank()
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredFolders, key = { it.id }) { folder ->
                        FolderGridItem(
                            folder = folder,
                            onClick = {
                                triggerHapticFeedback(context)
                                if (folder.isUltraPrivate) {
                                    onUltraPrivateClick()
                                } else {
                                    onFolderClick(folder)
                                }
                            },
                            onLongClick = {
                                if (!folder.isUltraPrivate) {
                                    triggerHapticFeedback(context)
                                    selectedFolderForOptions = folder
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Options BottomSheet on Long Press (Pin, Set Cover, Delete)
    if (selectedFolderForOptions != null) {
        val folder = selectedFolderForOptions!!
        ModalBottomSheet(
            onDismissRequest = { selectedFolderForOptions = null },
            containerColor = DarkCardBg,
            dragHandle = { BottomSheetDefaults.DragHandle(color = DarkBorder) },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    text = folder.name,
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Pin / Unpin
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val newPinned = !folder.isPinned
                            onTogglePinFolder(folder.id, newPinned)
                            selectedFolderForOptions = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (folder.isPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = if (folder.isPinned) "Unpin from top" else "Pin to top",
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                }

                // Set Cover
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            folderForCoverSelection = folder
                            selectedFolderForOptions = null
                            coverPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Set cover",
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                }

                // Delete Folder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            folderToDelete = folder
                            selectedFolderForOptions = null
                        }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = AccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Delete folder",
                        color = AccentRed,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateFolderBottomSheet(
            onDismiss = { showCreateSheet = false },
            onConfirm = { name, coverUri ->
                showCreateSheet = false
                onCreateFolder(name, coverUri)
            }
        )
    }

    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            containerColor = DarkCardBg,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = "Delete \"${folderToDelete?.name}\"?",
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Items inside will be moved to Recycle Bin.",
                    color = TextGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        folderToDelete?.let { onDeleteFolder(it.id) }
                        folderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { folderToDelete = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkPillBg,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("Cancel", fontSize = 13.sp)
                }
            }
        )
    }
}

@Composable
private fun FolderGridItem(
    folder: FolderWithItemCount,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "folder_scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(folder.id) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            }
            .testTag("folder_item_${folder.id}")
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
            if (folder.isUltraPrivate) {
                // Ultra Private folder has the official app icon
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = folder.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                            )
                        )
                )
            } else if (folder.latestThumbnail != null && (folder.latestThumbnail.startsWith("content://") || File(folder.latestThumbnail).exists())) {
                val model = if (folder.latestThumbnail.startsWith("content://")) Uri.parse(folder.latestThumbnail) else File(folder.latestThumbnail)
                AsyncImage(
                    model = model,
                    contentDescription = folder.name,
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
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Pinned indicator
            if (folder.isPinned && !folder.isUltraPrivate) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(PureBlack.copy(alpha = 0.75f))
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = TextWhite,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            // Item count badge
            if (!folder.isUltraPrivate && folder.itemCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(PureBlack.copy(alpha = 0.75f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${folder.itemCount}",
                        color = TextWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = folder.name,
            color = TextWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (folder.isUltraPrivate) "Ultra Private" else "${folder.itemCount} items",
            color = TextGray,
            fontSize = 10.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyFoldersState(
    onCreateClick: () -> Unit,
    isSearch: Boolean
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
                .size(68.dp)
                .clip(CircleShape)
                .background(DarkCardBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSearch) Icons.Default.Search else Icons.Outlined.CreateNewFolder,
                contentDescription = null,
                tint = TextWhite,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isSearch) "No folders found" else "Start creating folders",
            color = TextWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isSearch) "Try searching for another folder name." else "Organize private photos and videos in custom folders.",
            color = TextGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        if (!isSearch) {
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextWhite,
                    contentColor = PureBlack
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                modifier = Modifier.testTag("empty_state_create_folder_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = PureBlack,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "New Folder",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFolderBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (name: String, coverUri: String?) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var selectedCoverUri by remember { mutableStateOf<String?>(null) }

    val pickCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedCoverUri = uri.toString()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkCardBg,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = DarkBorder)
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "New Folder",
                color = TextWhite,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                placeholder = { Text("Folder Name", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkBorder,
                    unfocusedBorderColor = DarkBorder.copy(alpha = 0.5f),
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    cursorColor = TextWhite
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_folder_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cover image picker row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkPillBg)
                    .clickable {
                        pickCoverLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (selectedCoverUri != null) "Cover Selected" else "Set cover",
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkPillBg,
                        contentColor = TextWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel", fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        val name = folderName.trim().ifEmpty { "New Folder" }
                        onConfirm(name, selectedCoverUri)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TextWhite,
                        contentColor = PureBlack
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("create_folder_submit_button")
                ) {
                    Text("Create", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
