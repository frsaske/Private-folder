package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.MediaEntity
import com.example.ui.components.OpenSourcePressButton
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    mediaList: List<MediaEntity>,
    initialIndex: Int,
    onBack: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteMedia: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    if (mediaList.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val safeIndex = initialIndex.coerceIn(0, mediaList.size - 1)
    val pagerState = rememberPagerState(initialPage = safeIndex, pageCount = { mediaList.size })
    var showControls by remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val currentMedia = mediaList.getOrNull(pagerState.currentPage)

    BackHandler {
        onBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = mediaList.getOrNull(page)
            if (item != null) {
                if (item.isVideo) {
                    VideoViewerItem(
                        item = item,
                        isCurrentPage = pagerState.currentPage == page,
                        onTap = { showControls = !showControls }
                    )
                } else {
                    ZoomablePhotoViewerItem(
                        item = item,
                        onTap = { showControls = !showControls }
                    )
                }
            }
        }

        // Top App Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            triggerHapticFeedback(context)
                            onBack()
                        },
                        modifier = Modifier.testTag("viewer_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Text(
                        text = "${pagerState.currentPage + 1} of ${mediaList.size}",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    IconButton(
                        onClick = {
                            currentMedia?.let {
                                triggerHapticFeedback(context)
                                onToggleFavorite(it.id)
                            }
                        },
                        modifier = Modifier.testTag("viewer_favorite_button")
                    ) {
                        val isFav = currentMedia?.isFavorite == true
                        Icon(
                            imageVector = if (isFav) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFav) AccentRed else Color.White
                        )
                    }
                }
            }
        }

        // Bottom Action Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share
                    ViewerActionButton(
                        icon = Icons.Outlined.Share,
                        label = "Share",
                        onClick = {
                            currentMedia?.let {
                                triggerHapticFeedback(context)
                                shareSingleMedia(context, it)
                            }
                        }
                    )

                    // Info
                    ViewerActionButton(
                        icon = Icons.Outlined.Info,
                        label = "Info",
                        onClick = {
                            triggerHapticFeedback(context)
                            showInfoDialog = true
                        }
                    )

                    // Delete
                    ViewerActionButton(
                        icon = Icons.Default.Delete,
                        label = "Delete",
                        tint = AccentRed,
                        onClick = {
                            currentMedia?.let {
                                triggerHapticFeedback(context)
                                onDeleteMedia(it.id)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showInfoDialog && currentMedia != null) {
        MediaDetailedInfoDialog(
            item = currentMedia,
            onDismiss = { showInfoDialog = false }
        )
    }
}

@Composable
private fun ZoomablePhotoViewerItem(
    item: MediaEntity,
    onTap: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val file = remember(item.filePath) { File(item.filePath) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    },
                    onTap = { onTap() }
                )
            }
            .then(
                if (scale > 1.05f) {
                    Modifier.pointerInput(scale) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1.05f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = file,
            contentDescription = item.originalFileName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        )
    }
}

@Composable
private fun VideoViewerItem(
    item: MediaEntity,
    isCurrentPage: Boolean,
    onTap: () -> Unit
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(isCurrentPage) {
        if (isCurrentPage) {
            videoViewRef?.start()
        } else {
            videoViewRef?.pause()
        }
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (videoViewRef?.isPlaying == true) {
                            videoViewRef?.pause()
                        } else {
                            videoViewRef?.start()
                        }
                        onTap()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                VideoView(context).apply {
                    setVideoPath(item.filePath)
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        if (isCurrentPage) {
                            start()
                        }
                    }
                    videoViewRef = this
                }
            },
            update = { view ->
                if (isCurrentPage) {
                    if (!view.isPlaying) view.start()
                } else {
                    if (view.isPlaying) view.pause()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ViewerActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MediaDetailedInfoDialog(
    item: MediaEntity,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault()) }
    val formattedDate = remember(item.addedAt) { dateFormat.format(Date(item.addedAt)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Media Details",
                    color = TextWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                InfoRow(label = "Name", value = item.originalFileName)
                InfoRow(label = "Type", value = if (item.isVideo) "Video (${item.mimeType})" else "Photo (${item.mimeType})")
                InfoRow(label = "File Size", value = formatFileSize(item.fileSizeBytes))
                if (item.width > 0 && item.height > 0) {
                    InfoRow(label = "Resolution", value = "${item.width} × ${item.height}")
                }
                if (item.isVideo && item.durationMs > 0) {
                    InfoRow(label = "Duration", value = formatDuration(item.durationMs))
                }
                InfoRow(label = "Encrypted On", value = formattedDate)

                Spacer(modifier = Modifier.height(24.dp))

                OpenSourcePressButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = TextWhite,
                    contentColor = PureBlack
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun shareSingleMedia(context: Context, item: MediaEntity) {
    try {
        val file = File(item.filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Media"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Cannot share file", android.widget.Toast.LENGTH_SHORT).show()
    }
}
