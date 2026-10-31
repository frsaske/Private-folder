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
        Toast.makeText(context, "Copied \"$text\"", Toast.LENGTH_SHORT).show()
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
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Settings",
                    color = TextWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Hero Card matching PinterestDL style
            OpenSourceCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_icon),
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Private Folder",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Version 1.0.0",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // APP INFO
            SettingsSectionHeader(title = "APP INFO")
            OpenSourceCard {
                OpenSourceSettingsRow(
                    title = "About app",
                    leadingIcon = Icons.Default.Info,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    showDivider = false,
                    onClick = { showAboutDialog = true }
                )
            }

            // SECURITY
            SettingsSectionHeader(title = "SECURITY")
            OpenSourceCard {
                OpenSourceSettingsRow(
                    title = "Security Email",
                    leadingIcon = Icons.Default.Security,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    showDivider = true,
                    onClick = { showSecurityEmailDialog = true }
                )

                OpenSourceSettingsRow(
                    title = "Change Passcode",
                    leadingIcon = Icons.Default.Key,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    showDivider = true,
                    onClick = { showChangePinDialog = true }
                )

                OpenSourceSettingsRow(
                    title = "Anti-Clear Data Protection",
                    leadingIcon = Icons.Default.Shield,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    showDivider = true,
                    onClick = {
                        val intent = Intent(context, com.example.ManageSpaceActivity::class.java)
                        context.startActivity(intent)
                    }
                )

                OpenSourceSettingsRow(
                    title = "Move from Phone Gallery",
                    leadingIcon = Icons.Default.DriveFileMove,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Switch(
                            checked = isAutoDeleteEnabled,
                            onCheckedChange = { checked ->
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
                        val newValue = !isAutoDeleteEnabled
                        isAutoDeleteEnabled = newValue
                        securityPrefs.isAutoDeleteFromGalleryEnabled = newValue
                    }
                )

                OpenSourceSettingsRow(
                    title = "Recycle Bin",
                    leadingIcon = Icons.Default.Delete,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (recycleBinCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .background(TextWhite, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$recycleBinCount",
                                        color = PureBlack,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = TextGray,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    },
                    showDivider = false,
                    onClick = onOpenRecycleBin
                )
            }

            // DEVELOPER & CONTACT
            SettingsSectionHeader(title = "DEVELOPER & CONTACT")
            OpenSourceCard {
                OpenSourceSettingsRow(
                    title = "Developer: sasuke",
                    leadingIcon = Icons.Default.Person,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        IconButton(
                            onClick = { copyToClipboard("Developer", "sasuke - Sasuke.winx@gmail.com") },
                            modifier = Modifier.size(32.dp).testTag("copy_developer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = TextWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    showDivider = true,
                    onClick = { copyToClipboard("Developer", "sasuke - Sasuke.winx@gmail.com") }
                )

                OpenSourceSettingsRow(
                    title = "Contact Developer",
                    leadingIcon = Icons.Default.Mail,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Text(
                            text = "Sasuke.winx@gmail.com",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    },
                    showDivider = true,
                    onClick = {
                        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:Sasuke.winx@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Private Folder App Feedback")
                        }
                        try {
                            context.startActivity(emailIntent)
                        } catch (_: Exception) {
                            copyToClipboard("Email", "Sasuke.winx@gmail.com")
                        }
                    }
                )

                OpenSourceSettingsRow(
                    title = "Get Source Code",
                    leadingIcon = Icons.Default.Code,
                    iconBackgroundColor = DarkPillBg,
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    showDivider = false,
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/devsasuke/Private-folder/"))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
            }

            // Lock Vault Now Button
            Button(
                onClick = {
                    triggerHapticFeedback(context)
                    onLockAppNow()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkCardBg,
                    contentColor = TextWhite
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder.copy(alpha = 0.5f)),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("lock_app_now_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lock Vault Now",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
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
                Toast.makeText(context, "Recovery email saved", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            securityPrefs = securityPrefs,
            onDismiss = { showChangePinDialog = false },
            onSuccess = {
                showChangePinDialog = false
                Toast.makeText(context, "Passcode updated", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = TextGray,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 4.dp)
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
                    text = "Security Recovery Email",
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enter a recovery email for resetting your PIN if forgotten. This email is never displayed in the app.",
                    color = TextGray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = {
                        emailInput = it
                        errorText = null
                    },
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

                Spacer(modifier = Modifier.height(16.dp))

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
                            val trimmed = emailInput.trim()
                            if (trimmed.isEmpty() || !trimmed.contains("@") || !trimmed.contains(".")) {
                                errorText = "Please enter a valid email address."
                            } else {
                                securityPrefs.recoveryEmail = trimmed
                                onSaved()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TextWhite,
                            contentColor = PureBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("save_security_email_button")
                    ) {
                        Text("Save", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                Text(
                    text = "Change Passcode",
                    color = TextWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) oldPin = it },
                    placeholder = { Text("Current 4-digit PIN", color = TextMuted, fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkBorder,
                        unfocusedBorderColor = DarkBorder.copy(alpha = 0.5f),
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = TextWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("change_pin_old_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

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
                    modifier = Modifier.fillMaxWidth().testTag("change_pin_new_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmNewPin = it },
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
                    modifier = Modifier.fillMaxWidth().testTag("change_pin_confirm_input")
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText ?: "",
                        color = AccentRed,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TextWhite,
                            contentColor = PureBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("change_pin_submit_button")
                    ) {
                        Text("Update", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
