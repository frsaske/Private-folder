package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.FolderWithItemCount
import com.example.data.SecurityPreferences
import com.example.ui.components.OpenSourcePillSearchBar
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UltraPrivateScreen(
    folders: List<FolderWithItemCount>,
    securityPrefs: SecurityPreferences,
    onBack: () -> Unit,
    onFolderClick: (FolderWithItemCount) -> Unit,
    onCreateFolder: (name: String, coverUri: String?) -> Unit,
    onSetFolderCover: (folderId: String, coverUri: String?) -> Unit,
    onTogglePinFolder: (folderId: String, isPinned: Boolean) -> Unit,
    onDeleteFolder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
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

    BackHandler {
        onBack()
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                triggerHapticFeedback(context)
                                onBack()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "Ultra Private",
                            color = TextWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                isSearchVisible = !isSearchVisible
                                if (!isSearchVisible) searchQuery = ""
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                triggerHapticFeedback(context)
                                showSettingsDialog = true
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Ultra Private Settings",
                                tint = TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                triggerHapticFeedback(context)
                                onBack()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock",
                                tint = TextWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isSearchVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        OpenSourcePillSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onClear = { searchQuery = "" },
                            placeholder = "Search ultra private folders..."
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
                    .testTag("ultra_private_create_folder_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Ultra Private Folder",
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
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(DarkCardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = TextWhite,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Ultra Private Space",
                            color = TextWhite,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Folders and media here are completely isolated from your main vault.",
                            color = TextGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                triggerHapticFeedback(context)
                                showCreateSheet = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TextWhite,
                                contentColor = PureBlack
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = PureBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Folder", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredFolders, key = { it.id }) { folder ->
                        UltraPrivateFolderItem(
                            folder = folder,
                            onClick = {
                                triggerHapticFeedback(context)
                                onFolderClick(folder)
                            },
                            onLongClick = {
                                triggerHapticFeedback(context)
                                selectedFolderForOptions = folder
                            }
                        )
                    }
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
                    Text("Set cover", color = TextWhite, fontSize = 14.sp)
                }

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
                    Text("Delete folder", color = AccentRed, fontSize = 14.sp)
                }
            }
        }
    }

    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            containerColor = DarkCardBg,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Delete \"${folderToDelete?.name}\"?", color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            },
            text = {
                Text("Items inside will be safely moved to Recycle Bin.", color = TextGray, fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        folderToDelete?.let { onDeleteFolder(it.id) }
                        folderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = TextWhite),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontSize = 13.sp)
                }
            },
            dismissButton = {
                Button(
                    onClick = { folderToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkPillBg, contentColor = TextWhite),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel", fontSize = 13.sp)
                }
            }
        )
    }

    if (showSettingsDialog) {
        UltraPrivateSettingsDialog(
            securityPrefs = securityPrefs,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun UltraPrivateFolderItem(
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
            if (folder.latestThumbnail != null && (folder.latestThumbnail.startsWith("content://") || File(folder.latestThumbnail).exists())) {
                val model = if (folder.latestThumbnail.startsWith("content://")) folder.latestThumbnail else File(folder.latestThumbnail)
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

            if (folder.isPinned) {
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

            if (folder.itemCount > 0) {
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
            text = "${folder.itemCount} items",
            color = TextGray,
            fontSize = 10.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun UltraPrivateSettingsDialog(
    securityPrefs: SecurityPreferences,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf("MAIN") } // "MAIN", "CHANGE_PIN", "RECOVERY_EMAIL"
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var recoveryEmailInput by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf<String?>(null) }

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
                    .padding(20.dp)
            ) {
                Text(
                    text = when (mode) {
                        "CHANGE_PIN" -> "Change Ultra Private PIN"
                        "RECOVERY_EMAIL" -> "Ultra Private Recovery Email"
                        else -> "Ultra Private Settings"
                    },
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                when (mode) {
                    "MAIN" -> {
                        Button(
                            onClick = { mode = "CHANGE_PIN" },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkPillBg, contentColor = TextWhite),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Change 4-Digit PIN", fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { mode = "RECOVERY_EMAIL" },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkPillBg, contentColor = TextWhite),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Set Security Recovery Email", fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = TextWhite, contentColor = PureBlack),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Done", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    "CHANGE_PIN" -> {
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                            placeholder = { Text("New 4-digit PIN", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkBorder,
                                unfocusedBorderColor = DarkBorder.copy(alpha = 0.5f),
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                cursorColor = TextWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                            placeholder = { Text("Confirm new 4-digit PIN", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkBorder,
                                unfocusedBorderColor = DarkBorder.copy(alpha = 0.5f),
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                cursorColor = TextWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (statusText != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = statusText ?: "", color = AccentRed, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { mode = "MAIN" },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkPillBg, contentColor = TextWhite),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back", fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    if (newPin.length != 4) {
                                        statusText = "PIN must be 4 digits"
                                    } else if (newPin != confirmPin) {
                                        statusText = "PINs do not match"
                                    } else {
                                        securityPrefs.setUltraPrivatePin(newPin)
                                        mode = "MAIN"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TextWhite, contentColor = PureBlack),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    "RECOVERY_EMAIL" -> {
                        OutlinedTextField(
                            value = recoveryEmailInput,
                            onValueChange = { recoveryEmailInput = it; statusText = null },
                            placeholder = { Text("Recovery email address", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkBorder,
                                unfocusedBorderColor = DarkBorder.copy(alpha = 0.5f),
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                cursorColor = TextWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (statusText != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = statusText ?: "", color = AccentRed, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { mode = "MAIN" },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkPillBg, contentColor = TextWhite),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Back", fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    val trimmed = recoveryEmailInput.trim()
                                    if (trimmed.isEmpty() || !trimmed.contains("@")) {
                                        statusText = "Enter a valid email"
                                    } else {
                                        securityPrefs.ultraPrivateRecoveryEmail = trimmed
                                        mode = "MAIN"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TextWhite, contentColor = PureBlack),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
