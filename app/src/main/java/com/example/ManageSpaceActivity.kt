package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.data.MediaEntity
import com.example.data.MediaRepository
import com.example.data.SecurityPreferences
import com.example.ui.components.NumericKeypad
import com.example.ui.components.OpenSourceCard
import com.example.ui.components.OpenSourcePressButton
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class ManageSpaceActivity : FragmentActivity() {
    private lateinit var securityPrefs: SecurityPreferences
    private lateinit var repository: MediaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        securityPrefs = SecurityPreferences(this)
        repository = MediaRepository(this)

        setContent {
            PrivateFolderTheme {
                ManageSpaceScreen(
                    securityPrefs = securityPrefs,
                    repository = repository,
                    onFinish = { finish() },
                    onRequestBiometrics = { onSuccess ->
                        showBiometricPrompt(onSuccess)
                    }
                )
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Space Manager")
            .setSubtitle("Authenticate with biometrics to manage storage")
            .setNegativeButtonText("Use PIN")
            .build()

        try {
            prompt.authenticate(info)
        } catch (_: Exception) {}
    }
}

private enum class SpaceManagerView {
    OVERVIEW,
    MEDIA_CLEANUP
}

private enum class MediaCategoryFilter {
    ALL,
    PHOTOS,
    VIDEOS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageSpaceScreen(
    securityPrefs: SecurityPreferences,
    repository: MediaRepository,
    onFinish: () -> Unit,
    onRequestBiometrics: (onSuccess: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isUnlocked by remember { mutableStateOf(!securityPrefs.isSetupCompleted) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }

    // Media states
    var activeMediaList by remember { mutableStateOf<List<MediaEntity>>(emptyList()) }
    var recycleBinList by remember { mutableStateOf<List<MediaEntity>>(emptyList()) }
    var cacheSizeBytes by remember { mutableStateOf(0L) }
    var currentView by remember { mutableStateOf(SpaceManagerView.OVERVIEW) }
    var selectedFilter by remember { mutableStateOf(MediaCategoryFilter.ALL) }
    val selectedItemIds = remember { mutableStateListOf<String>() }

    var showConfirmEmptyBinDialog by remember { mutableStateOf(false) }
    var showConfirmDeleteItemsDialog by remember { mutableStateOf(false) }

    fun refreshStats() {
        coroutineScope.launch {
            activeMediaList = repository.getAllActiveMedia().first()
            recycleBinList = repository.getRecycleBinMedia().first()
            cacheSizeBytes = context.cacheDir?.listFiles()?.sumOf { it.length() } ?: 0L
        }
    }

    LaunchedEffect(Unit) {
        if (securityPrefs.isSetupCompleted && securityPrefs.isBiometricsEnabled) {
            onRequestBiometrics {
                isUnlocked = true
                refreshStats()
            }
        }
        refreshStats()
    }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            refreshStats()
        }
    }

    val totalMediaBytes = remember(activeMediaList) {
        activeMediaList.sumOf { it.fileSizeBytes }
    }
    val totalTrashBytes = remember(recycleBinList) {
        recycleBinList.sumOf { it.fileSizeBytes }
    }
    val totalVaultBytes = remember(totalMediaBytes, totalTrashBytes, cacheSizeBytes) {
        totalMediaBytes + totalTrashBytes + cacheSizeBytes
    }

    val filteredCleanupList = remember(activeMediaList, selectedFilter) {
        when (selectedFilter) {
            MediaCategoryFilter.ALL -> activeMediaList.sortedByDescending { it.addedAt }
            MediaCategoryFilter.PHOTOS -> activeMediaList.filter { !it.isVideo }.sortedByDescending { it.addedAt }
            MediaCategoryFilter.VIDEOS -> activeMediaList.filter { it.isVideo }.sortedByDescending { it.addedAt }
        }
    }

    BackHandler {
        if (currentView == SpaceManagerView.MEDIA_CLEANUP) {
            currentView = SpaceManagerView.OVERVIEW
            selectedItemIds.clear()
        } else {
            onFinish()
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
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentView == SpaceManagerView.MEDIA_CLEANUP) {
                                currentView = SpaceManagerView.OVERVIEW
                                selectedItemIds.clear()
                            } else {
                                onFinish()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Manage Space",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PureBlack)
        ) {
            if (!isUnlocked) {
                // PIN Lock screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_icon),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Private Folder",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Enter 4-digit PIN",
                            color = TextGray,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // PIN Dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(4) { idx ->
                                val filled = idx < enteredPin.length
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(if (filled) TextWhite else DarkPillBg)
                                        .border(1.dp, if (filled) TextWhite else DarkBorder, CircleShape)
                                )
                            }
                        }

                        if (pinError != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = pinError!!,
                                color = AccentRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Keypad
                    NumericKeypad(
                        onDigitClick = { digit ->
                            if (enteredPin.length < 4) {
                                pinError = null
                                enteredPin += digit
                                if (enteredPin.length == 4) {
                                    if (securityPrefs.verifyPin(enteredPin)) {
                                        triggerHapticFeedback(context)
                                        isUnlocked = true
                                        refreshStats()
                                    } else {
                                        triggerHapticFeedback(context)
                                        pinError = "Incorrect PIN"
                                        enteredPin = ""
                                    }
                                }
                            }
                        },
                        onDeleteClick = {
                            if (enteredPin.isNotEmpty()) {
                                enteredPin = enteredPin.dropLast(1)
                                pinError = null
                            }
                        },
                        showBiometric = securityPrefs.isBiometricsEnabled,
                        onBiometricClick = {
                            onRequestBiometrics {
                                isUnlocked = true
                                refreshStats()
                            }
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            } else {
                // Unlocked Manage Space UI
                AnimatedContent(
                    targetState = currentView,
                    label = "space_view_transition"
                ) { view ->
                    when (view) {
                        SpaceManagerView.OVERVIEW -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Vault Storage Usage Card
                                Text(
                                    text = "STORAGE BREAKDOWN",
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp)
                                )

                                OpenSourceCard {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Total Vault Storage",
                                                    color = TextGray,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = formatSpaceSize(totalVaultBytes),
                                                    color = TextWhite,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(DarkPillBg, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = "${activeMediaList.size} Files",
                                                    color = TextWhite,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

                                        // Itemized Rows
                                        StorageDetailRow(
                                            icon = Icons.Default.Photo,
                                            label = "Photos & Videos",
                                            detail = "${activeMediaList.count { !it.isVideo }} photos • ${activeMediaList.count { it.isVideo }} videos",
                                            size = formatSpaceSize(totalMediaBytes)
                                        )

                                        StorageDetailRow(
                                            icon = Icons.Default.Delete,
                                            label = "Recycle Bin",
                                            detail = "${recycleBinList.size} items in trash",
                                            size = formatSpaceSize(totalTrashBytes)
                                        )

                                        StorageDetailRow(
                                            icon = Icons.Default.Cached,
                                            label = "App Cache",
                                            detail = "Thumbnails and temp cache",
                                            size = formatSpaceSize(cacheSizeBytes)
                                        )
                                    }
                                }

                                // Storage Management Actions
                                Text(
                                    text = "MANAGE STORAGE & CLEANUP",
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp)
                                )

                                OpenSourceCard {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // 1. Delete Photos & Videos
                                        SpaceActionRow(
                                            icon = Icons.Default.PhotoLibrary,
                                            title = "Delete Photos & Videos",
                                            subtitle = "Select and permanently delete specific media items",
                                            actionLabel = "Open",
                                            showDivider = true,
                                            onClick = {
                                                currentView = SpaceManagerView.MEDIA_CLEANUP
                                            }
                                        )

                                        // 2. Empty Recycle Bin
                                        SpaceActionRow(
                                            icon = Icons.Default.DeleteSweep,
                                            title = "Empty Recycle Bin",
                                            subtitle = if (recycleBinList.isEmpty()) "Recycle bin is empty" else "Permanently wipe ${recycleBinList.size} trash items (${formatSpaceSize(totalTrashBytes)})",
                                            actionLabel = "Empty",
                                            enabled = recycleBinList.isNotEmpty(),
                                            isDestructive = true,
                                            showDivider = true,
                                            onClick = {
                                                showConfirmEmptyBinDialog = true
                                            }
                                        )

                                        // 3. Clear Cache
                                        SpaceActionRow(
                                            icon = Icons.Default.CleaningServices,
                                            title = "Clear Thumbnail Cache",
                                            subtitle = "Free ${formatSpaceSize(cacheSizeBytes)} temporary cache",
                                            actionLabel = "Clear",
                                            enabled = cacheSizeBytes > 0,
                                            showDivider = false,
                                            onClick = {
                                                coroutineScope.launch {
                                                    val freed = repository.clearCache()
                                                    triggerHapticFeedback(context)
                                                    refreshStats()
                                                    Toast.makeText(context, "Freed ${formatSpaceSize(freed)} cache", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        SpaceManagerView.MEDIA_CLEANUP -> {
                            // Specific Media Selection & Deletion Grid (Only pics & vids deletion!)
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(PureBlack)
                            ) {
                                // Filter row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CleanupFilterPill(
                                        label = "All (${activeMediaList.size})",
                                        isSelected = selectedFilter == MediaCategoryFilter.ALL,
                                        onClick = { selectedFilter = MediaCategoryFilter.ALL },
                                        modifier = Modifier.weight(1f)
                                    )
                                    CleanupFilterPill(
                                        label = "Photos",
                                        isSelected = selectedFilter == MediaCategoryFilter.PHOTOS,
                                        onClick = { selectedFilter = MediaCategoryFilter.PHOTOS },
                                        modifier = Modifier.weight(1f)
                                    )
                                    CleanupFilterPill(
                                        label = "Videos",
                                        isSelected = selectedFilter == MediaCategoryFilter.VIDEOS,
                                        onClick = { selectedFilter = MediaCategoryFilter.VIDEOS },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Text(
                                    text = "Select photos and videos to delete permanently:",
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )

                                if (filteredCleanupList.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No media available in this category.",
                                            color = TextGray,
                                            fontSize = 14.sp
                                        )
                                    }
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                    ) {
                                        items(filteredCleanupList, key = { it.id }) { item ->
                                            val isSelected = selectedItemIds.contains(item.id)
                                            val file = remember(item.filePath) { File(item.filePath) }

                                            Box(
                                                modifier = Modifier
                                                    .aspectRatio(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(DarkCardBg)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) AccentRed else DarkBorder.copy(alpha = 0.4f),
                                                        shape = RoundedCornerShape(10.dp)
                                                    )
                                                    .clickable {
                                                        triggerHapticFeedback(context)
                                                        if (isSelected) {
                                                            selectedItemIds.remove(item.id)
                                                        } else {
                                                            selectedItemIds.add(item.id)
                                                        }
                                                    }
                                            ) {
                                                AsyncImage(
                                                    model = file,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )

                                                // Selection indicator
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(6.dp)
                                                        .size(22.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) AccentRed else PureBlack.copy(alpha = 0.6f))
                                                        .border(1.5.dp, if (isSelected) AccentRed else TextWhite, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = TextWhite,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }

                                                // File size badge
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .padding(4.dp)
                                                        .background(PureBlack.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = formatSpaceSize(item.fileSizeBytes),
                                                        color = TextWhite,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Bottom action bar for deleting selected media
                                Surface(
                                    color = DarkCardBg,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${selectedItemIds.size} Selected",
                                                color = TextWhite,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val selectedBytes = remember(selectedItemIds.size, activeMediaList) {
                                                activeMediaList.filter { selectedItemIds.contains(it.id) }.sumOf { it.fileSizeBytes }
                                            }
                                            Text(
                                                text = "Reclaims ${formatSpaceSize(selectedBytes)}",
                                                color = TextGray,
                                                fontSize = 12.sp
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (filteredCleanupList.isNotEmpty()) {
                                                TextButton(
                                                    onClick = {
                                                        if (selectedItemIds.size == filteredCleanupList.size) {
                                                            selectedItemIds.clear()
                                                        } else {
                                                            selectedItemIds.clear()
                                                            selectedItemIds.addAll(filteredCleanupList.map { it.id })
                                                        }
                                                    }
                                                ) {
                                                    Text(
                                                        text = if (selectedItemIds.size == filteredCleanupList.size) "Deselect All" else "Select All",
                                                        color = TextWhite,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    showConfirmDeleteItemsDialog = true
                                                },
                                                enabled = selectedItemIds.isNotEmpty(),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = AccentRed,
                                                    contentColor = TextWhite,
                                                    disabledContainerColor = DarkPillBg,
                                                    disabledContentColor = TextMuted
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteForever,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Delete",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
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

    // Confirmation dialog for Empty Trash
    if (showConfirmEmptyBinDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmEmptyBinDialog = false },
            containerColor = DarkCardBg,
            title = {
                Text(
                    text = "Empty Recycle Bin?",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all ${recycleBinList.size} items (${formatSpaceSize(totalTrashBytes)}) from the recycle bin. This cannot be undone.",
                    color = TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmEmptyBinDialog = false
                        coroutineScope.launch {
                            repository.emptyRecycleBin()
                            triggerHapticFeedback(context)
                            refreshStats()
                            Toast.makeText(context, "Recycle bin emptied", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Delete Forever", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmEmptyBinDialog = false }) {
                    Text("Cancel", color = TextWhite)
                }
            }
        )
    }

    // Confirmation dialog for deleting selected photos/videos
    if (showConfirmDeleteItemsDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteItemsDialog = false },
            containerColor = DarkCardBg,
            title = {
                Text(
                    text = "Permanently Delete Media?",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Permanently delete ${selectedItemIds.size} selected items? This will free space immediately and cannot be undone.",
                    color = TextGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val idsToDelete = selectedItemIds.toList()
                        showConfirmDeleteItemsDialog = false
                        coroutineScope.launch {
                            repository.deletePermanently(idsToDelete)
                            selectedItemIds.clear()
                            triggerHapticFeedback(context)
                            refreshStats()
                            Toast.makeText(context, "Deleted ${idsToDelete.size} items", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("Delete Forever", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteItemsDialog = false }) {
                    Text("Cancel", color = TextWhite)
                }
            }
        )
    }
}

@Composable
private fun StorageDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    detail: String,
    size: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DarkPillBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = label,
                    color = TextWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = detail,
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = size,
            color = TextWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SpaceActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    showDivider: Boolean = true,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDestructive) AccentRed.copy(alpha = 0.15f) else DarkPillBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDestructive) AccentRed else TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        color = if (enabled) TextWhite else TextMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        color = if (enabled) TextGray else TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (!enabled) DarkPillBg
                        else if (isDestructive) AccentRed.copy(alpha = 0.2f)
                        else TextWhite.copy(alpha = 0.1f)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionLabel,
                    color = if (!enabled) TextMuted else if (isDestructive) AccentRed else TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                color = DarkBorder.copy(alpha = 0.35f),
                modifier = Modifier.padding(start = 68.dp)
            )
        }
    }
}

@Composable
private fun CleanupFilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (isSelected) TextWhite else DarkCardBg)
            .border(1.dp, if (isSelected) TextWhite else DarkBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) PureBlack else TextWhite,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun formatSpaceSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> "%.2f GB".format(gb)
        mb >= 1.0 -> "%.1f MB".format(mb)
        kb >= 1.0 -> "%.1f KB".format(kb)
        else -> "$bytes B"
    }
}
