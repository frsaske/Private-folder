package com.example

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.example.data.SecurityPreferences
import com.example.ui.Screen
import com.example.ui.VaultViewModel
import com.example.ui.components.VaultBottomNavBar
import com.example.ui.screens.*
import com.example.ui.theme.*

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
    val ultraPrivateFolders by viewModel.ultraPrivateFolders.collectAsState()
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
    val context = LocalContext.current
    val deleteConsent by viewModel.deleteConsentIntentSender.collectAsState()
    val movedMessage by viewModel.movedFromGalleryMessage.collectAsState()

    var showUltraPrivateSetupDialog by remember { mutableStateOf(false) }
    var showUltraPrivateUnlockDialog by remember { mutableStateOf(false) }

    val deleteConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.clearDeleteConsent()
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "Original media removed from phone gallery", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(deleteConsent) {
        deleteConsent?.let { sender ->
            try {
                deleteConsentLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } catch (_: Exception) {
                viewModel.clearDeleteConsent()
            }
        }
    }

    LaunchedEffect(movedMessage) {
        movedMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearMovedMessage()
        }
    }

    val showBottomBar = isUnlocked && (
        currentScreen == Screen.FOLDERS ||
        currentScreen == Screen.ALL_FILES ||
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
                            onUltraPrivateClick = {
                                if (viewModel.securityPrefs.isUltraPrivateSetupCompleted) {
                                    showUltraPrivateUnlockDialog = true
                                } else {
                                    showUltraPrivateSetupDialog = true
                                }
                            },
                            onCreateFolder = { name, coverUri -> viewModel.createFolder(name, coverUri, space = "MAIN") },
                            onSetFolderCover = { folderId, coverUri -> viewModel.setFolderCover(folderId, coverUri) },
                            onTogglePinFolder = { folderId, isPinned -> viewModel.togglePinFolder(folderId, isPinned) },
                            onDeleteFolder = { folderId -> viewModel.deleteFolder(folderId) },
                            onOpenSettings = { viewModel.openSettings() }
                        )
                    }

                    Screen.ULTRA_PRIVATE -> {
                        UltraPrivateScreen(
                            folders = ultraPrivateFolders,
                            securityPrefs = viewModel.securityPrefs,
                            onBack = { viewModel.navigateBack() },
                            onFolderClick = { folder -> viewModel.openFolder(folder) },
                            onCreateFolder = { name, coverUri -> viewModel.createFolder(name, coverUri, space = "ULTRA_PRIVATE") },
                            onSetFolderCover = { folderId, coverUri -> viewModel.setFolderCover(folderId, coverUri) },
                            onTogglePinFolder = { folderId, isPinned -> viewModel.togglePinFolder(folderId, isPinned) },
                            onDeleteFolder = { folderId -> viewModel.deleteFolder(folderId) }
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
                        val relevantFolders = if (activeFolder?.space == "ULTRA_PRIVATE") ultraPrivateFolders else folders
                        if (activeFolder != null) {
                            GalleryScreen(
                                folderId = activeFolder.id,
                                folderName = activeFolder.name,
                                mediaList = currentFolderMedia,
                                allFolders = relevantFolders,
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
                                onUltraPrivateClick = {
                                    if (viewModel.securityPrefs.isUltraPrivateSetupCompleted) {
                                        showUltraPrivateUnlockDialog = true
                                    } else {
                                        showUltraPrivateSetupDialog = true
                                    }
                                },
                                onCreateFolder = { name, coverUri -> viewModel.createFolder(name, coverUri, space = "MAIN") },
                                onSetFolderCover = { folderId, coverUri -> viewModel.setFolderCover(folderId, coverUri) },
                                onTogglePinFolder = { folderId, isPinned -> viewModel.togglePinFolder(folderId, isPinned) },
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
                        viewModel.createFolder(name, coverUri, space = "MAIN") { newFolder ->
                            viewModel.importUris(uris, newFolder.id)
                        }
                    },
                    onDismiss = { viewModel.dismissSharedOverlay() },
                    isImporting = isImporting,
                    importProgress = importProgress
                )
            }

            // Ultra Private Setup Dialog
            if (showUltraPrivateSetupDialog) {
                UltraPrivateSetupDialog(
                    onDismiss = { showUltraPrivateSetupDialog = false },
                    onConfirm = { pin ->
                        viewModel.securityPrefs.setUltraPrivatePin(pin)
                        showUltraPrivateSetupDialog = false
                        viewModel.openUltraPrivateSpace()
                    }
                )
            }

            // Ultra Private Unlock Dialog
            if (showUltraPrivateUnlockDialog) {
                UltraPrivateUnlockDialog(
                    securityPrefs = viewModel.securityPrefs,
                    onDismiss = { showUltraPrivateUnlockDialog = false },
                    onSuccess = {
                        showUltraPrivateUnlockDialog = false
                        viewModel.openUltraPrivateSpace()
                    }
                )
            }
        }
    }
}

@Composable
fun UltraPrivateSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (pin: String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Setup Ultra Private PIN",
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Create a separate 4-digit PIN for this isolated vault space.",
                    color = TextGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it; errorText = null },
                    placeholder = { Text("4-digit PIN", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBorder,
                        unfocusedBorderColor = DarkBorder.copy(alpha = 0.5f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ultra_pin_setup_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it; errorText = null },
                    placeholder = { Text("Confirm 4-digit PIN", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBorder,
                        unfocusedBorderColor = DarkBorder.copy(alpha = 0.5f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ultra_pin_confirm_input")
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorText ?: "", color = AccentRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPillBg, contentColor = TextWhite),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (pin.length != 4) {
                                errorText = "PIN must be 4 digits"
                            } else if (pin != confirmPin) {
                                errorText = "PINs do not match"
                            } else {
                                onConfirm(pin)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TextWhite, contentColor = PureBlack),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save & Open", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun UltraPrivateUnlockDialog(
    securityPrefs: SecurityPreferences,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }

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
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Ultra Private Access",
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enter your Ultra Private 4-digit PIN",
                    color = TextGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            pin = it
                            errorText = null
                            if (it.length == 4) {
                                if (securityPrefs.verifyUltraPrivatePin(it)) {
                                    onSuccess()
                                } else {
                                    errorText = "Incorrect Ultra Private PIN"
                                    pin = ""
                                }
                            }
                        }
                    },
                    placeholder = { Text("4-digit PIN", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBorder,
                        unfocusedBorderColor = DarkBorder.copy(alpha = 0.5f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("ultra_pin_unlock_input")
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorText ?: "", color = AccentRed, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPillBg, contentColor = TextWhite),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (securityPrefs.verifyUltraPrivatePin(pin)) {
                                onSuccess()
                            } else {
                                errorText = "Incorrect PIN"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TextWhite, contentColor = PureBlack),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Unlock", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
