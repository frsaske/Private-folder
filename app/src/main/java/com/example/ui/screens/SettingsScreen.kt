package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.SecurityPreferences
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    securityPrefs: SecurityPreferences,
    recycleBinCount: Int,
    onBack: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onLockAppNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var showSecurityEmailDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var isAutoDeleteEnabled by remember { mutableStateOf(securityPrefs.isAutoDeleteFromGalleryEnabled) }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        triggerHapticFeedback(context)
        Toast.makeText(context, "Copied \"$text\" to clipboard", Toast.LENGTH_SHORT).show()
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        containerColor = PureBlack,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Settings",
                    color = TextWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Hero Card (Matching user screenshot!)
            OpenSourceCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkPillBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_icon),
                            contentDescription = "App Icon",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Private Folder",
                            color = TextWhite,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // APP INFO Section (Matching user screenshot!)
            SettingsSectionHeader(title = "APP INFO")
            OpenSourceCard {
                OpenSourceSettingsRow(
                    title = "About app",
                    subtitle = "Creator message & details",
                    leadingIcon = Icons.Default.Info,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    showDivider = false,
                    onClick = { showAboutDialog = true }
                )
            }

            // SECURITY & RECOVERY (Privacy First - Email hidden!)
            SettingsSectionHeader(title = "SECURITY")
            OpenSourceCard {
                OpenSourceSettingsRow(
                    title = "Security Recovery Email",
                    subtitle = "Configured for PIN recovery (Protected)",
                    leadingIcon = Icons.Default.Security,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    showDivider = true,
                    onClick = { showSecurityEmailDialog = true }
                )

                OpenSourceSettingsRow(
                    title = "Change Passcode",
                    subtitle = "Update your 4-digit PIN",
                    leadingIcon = Icons.Default.Key,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    showDivider = true,
                    onClick = { showChangePinDialog = true }
                )

                OpenSourceSettingsRow(
                    title = "Anti-Clear Data Protection",
                    subtitle = "Settings Clear Data blocked • Open Space Manager",
                    leadingIcon = Icons.Default.Shield,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(AccentGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Active",
                                    color = AccentGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    },
                    showDivider = true,
                    onClick = {
                        val intent = Intent(context, com.example.ManageSpaceActivity::class.java)
                        context.startActivity(intent)
                    }
                )

                OpenSourceSettingsRow(
                    title = "Move from Phone Gallery",
                    subtitle = "Auto-delete originals from gallery on import",
                    leadingIcon = Icons.Default.DriveFileMove,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Switch(
                            checked = isAutoDeleteEnabled,
                            onCheckedChange = { checked ->
                                triggerHapticFeedback(context)
                                isAutoDeleteEnabled = checked
                                securityPrefs.isAutoDeleteFromGalleryEnabled = checked
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureBlack,
                                checkedTrackColor = TextWhite,
                                uncheckedThumbColor = TextGray,
                                uncheckedTrackColor = DarkPillBg
                            ),
                            modifier = Modifier.testTag("auto_delete_from_gallery_switch")
                        )
                    },
                    showDivider = true,
                    onClick = {
                        triggerHapticFeedback(context)
                        val newValue = !isAutoDeleteEnabled
                        isAutoDeleteEnabled = newValue
                        securityPrefs.isAutoDeleteFromGalleryEnabled = newValue
                    }
                )

                OpenSourceSettingsRow(
                    title = "Recycle Bin",
                    subtitle = "Recover deleted photos & videos (30 days)",
                    leadingIcon = Icons.Default.Delete,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (recycleBinCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .background(TextWhite, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$recycleBinCount",
                                        color = PureBlack,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    },
                    showDivider = false,
                    onClick = onOpenRecycleBin
                )
            }

            // DEVELOPER & CONTACT Section (Matching user screenshot!)
            SettingsSectionHeader(title = "DEVELOPER & CONTACT")
            OpenSourceCard {
                OpenSourceSettingsRow(
                    title = "Developer: sasuke",
                    subtitle = "Sasuke.winx@gmail.com",
                    leadingIcon = Icons.Default.Person,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        IconButton(
                            onClick = { copyToClipboard("Developer Info", "sasuke - Sasuke.winx@gmail.com") },
                            modifier = Modifier.size(36.dp).testTag("copy_developer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Developer",
                                tint = TextWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    showDivider = true,
                    onClick = { copyToClipboard("Developer Info", "sasuke - Sasuke.winx@gmail.com") }
                )

                OpenSourceSettingsRow(
                    title = "Contact Developer",
                    subtitle = "Sasuke.winx@gmail.com",
                    leadingIcon = Icons.Default.Mail,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Text(
                            text = "Sasuke.winx@gmail.com",
                            color = TextGray,
                            fontSize = 13.sp
                        )
                    },
                    showDivider = false,
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:Sasuke.winx@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Private Folder App Feedback")
                        }
                        try {
                            context.startActivity(emailIntent)
                        } catch (e: Exception) {
                            copyToClipboard("Email", "Sasuke.winx@gmail.com")
                        }
                    }
                )
            }

            // Lock Vault Now Button
            OpenSourcePressButton(
                onClick = onLockAppNow,
                backgroundColor = DarkCardBg,
                contentColor = TextWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("lock_app_now_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lock Vault Now",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showAboutDialog) {
        OpenSourceAboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showSecurityEmailDialog) {
        SecurityEmailDialog(
            securityPrefs = securityPrefs,
            onDismiss = { showSecurityEmailDialog = false },
            onSaved = {
                showSecurityEmailDialog = false
                Toast.makeText(context, "Recovery email saved securely", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            securityPrefs = securityPrefs,
            onDismiss = { showChangePinDialog = false },
            onSuccess = {
                showChangePinDialog = false
                Toast.makeText(context, "PIN updated successfully", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = TextGray,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 8.dp)
    )
}

@Composable
fun SecurityEmailDialog(
    securityPrefs: SecurityPreferences,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(DarkPillBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Add Security Email",
                    color = TextWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter a recovery email for resetting your PIN if forgotten. For your privacy, this email is never displayed in the app.",
                    color = TextGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = {
                        emailInput = it
                        errorText = null
                    },
                    placeholder = { Text("Enter security email", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextWhite,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = TextWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("security_email_input")
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText ?: "",
                        color = AccentRed,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OpenSourcePressButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        backgroundColor = DarkPillBg,
                        contentColor = TextWhite
                    ) {
                        Text("Cancel", fontSize = 14.sp)
                    }

                    OpenSourcePressButton(
                        onClick = {
                            val trimmed = emailInput.trim()
                            if (trimmed.isEmpty() || !trimmed.contains("@") || !trimmed.contains(".")) {
                                errorText = "Please enter a valid email address."
                            } else {
                                securityPrefs.recoveryEmail = trimmed
                                onSaved()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("save_security_email_button"),
                        backgroundColor = TextWhite,
                        contentColor = PureBlack
                    ) {
                        Text("Save", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChangePinDialog(
    securityPrefs: SecurityPreferences,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Change Passcode",
                    color = TextWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) oldPin = it },
                    placeholder = { Text("Current 4-digit PIN", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextWhite,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = TextWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("current_pin_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                    placeholder = { Text("New 4-digit PIN", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextWhite,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = TextWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmNewPin = it },
                    placeholder = { Text("Confirm new 4-digit PIN", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextWhite,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = TextWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("confirm_new_pin_input")
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText ?: "",
                        color = AccentRed,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OpenSourcePressButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        backgroundColor = DarkPillBg,
                        contentColor = TextWhite
                    ) {
                        Text("Cancel", fontSize = 14.sp)
                    }

                    OpenSourcePressButton(
                        onClick = {
                            if (!securityPrefs.verifyPin(oldPin)) {
                                errorText = "Current PIN is incorrect."
                            } else if (newPin.length != 4) {
                                errorText = "New PIN must be 4 digits."
                            } else if (newPin != confirmNewPin) {
                                errorText = "New PINs do not match."
                            } else {
                                securityPrefs.setPin(newPin)
                                onSuccess()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("save_pin_button"),
                        backgroundColor = TextWhite,
                        contentColor = PureBlack
                    ) {
                        Text("Update", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
