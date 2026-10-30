package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.Screen
import com.example.ui.theme.*

@Composable
fun VaultBottomNavBar(
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit,
    allFilesCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        color = PureBlack,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = DarkBorder.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home / Folders Tab
            NavBarItem(
                selected = currentScreen == Screen.FOLDERS,
                label = "Home",
                selectedIcon = Icons.Default.Home,
                unselectedIcon = Icons.Outlined.Home,
                onClick = {
                    triggerHapticFeedback(context)
                    onTabSelected(Screen.FOLDERS)
                },
                testTag = "bottom_nav_home"
            )

            // Center Tab: All Files (Latest to oldest photos and videos)
            NavBarItem(
                selected = currentScreen == Screen.ALL_FILES,
                label = "All Files",
                selectedIcon = Icons.Default.PhotoLibrary,
                unselectedIcon = Icons.Outlined.PhotoLibrary,
                badgeCount = if (allFilesCount > 0) allFilesCount else null,
                onClick = {
                    triggerHapticFeedback(context)
                    onTabSelected(Screen.ALL_FILES)
                },
                testTag = "bottom_nav_all_files"
            )

            // Settings Tab
            NavBarItem(
                selected = currentScreen == Screen.SETTINGS,
                label = "Settings",
                selectedIcon = Icons.Default.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                onClick = {
                    triggerHapticFeedback(context)
                    onTabSelected(Screen.SETTINGS)
                },
                testTag = "bottom_nav_settings"
            )
        }
    }
}

@Composable
private fun NavBarItem(
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    badgeCount: Int? = null
) {
    val iconColor by animateColorAsState(
        targetValue = if (selected) TextWhite else TextMuted,
        label = "nav_icon_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(32.dp)
                .clip(CircleShape)
                .background(if (selected) DarkPillBg else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount != null) {
                        Badge(
                            containerColor = TextWhite,
                            contentColor = PureBlack,
                            modifier = Modifier.offset(x = 4.dp, y = (-2).dp)
                        ) {
                            Text(
                                text = "$badgeCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (selected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = if (selected) TextWhite else TextMuted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
