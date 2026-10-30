package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SecurityPreferences
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun OnboardingScreen(
    securityPrefs: SecurityPreferences,
    onSetupCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(0) } // 0: Intro, 1: Enter PIN, 2: Confirm PIN, 3: Add Recovery Email
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var recoveryEmailInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun onDigitPressed(d: String) {
        if (currentStep == 1) {
            if (firstPin.length < 4) {
                firstPin += d
                errorMessage = null
                if (firstPin.length == 4) {
                    currentStep = 2
                }
            }
        } else if (currentStep == 2) {
            if (confirmPin.length < 4) {
                confirmPin += d
                errorMessage = null
                if (confirmPin.length == 4) {
                    if (firstPin == confirmPin) {
                        securityPrefs.setPin(firstPin)
                        currentStep = 3 // Move to Add Security Email
                    } else {
                        errorMessage = "Passcodes do not match. Please try again."
                        confirmPin = ""
                        firstPin = ""
                        currentStep = 1
                    }
                }
            }
        }
    }

    fun onDeletePressed() {
        if (currentStep == 1 && firstPin.isNotEmpty()) {
            firstPin = firstPin.dropLast(1)
        } else if (currentStep == 2 && confirmPin.isNotEmpty()) {
            confirmPin = confirmPin.dropLast(1)
        }
    }

    Scaffold(
        containerColor = PureBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
            },
            label = "onboarding_steps",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { step ->
            when (step) {
                0 -> {
                    // Intro Screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(DarkPillBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Private Folder",
                                    tint = TextWhite,
                                    modifier = Modifier.size(42.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Private Folder",
                                color = TextWhite,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                letterSpacing = (-0.5).sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Open-source high contrast pure black vault for your private photos and videos.",
                                color = TextGray,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 21.sp
                            )
                        }

                        // Feature Highlights (Privacy Focused - No plain email shown!)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OnboardingFeatureRow(
                                icon = Icons.Default.Folder,
                                title = "On-Device Sandbox",
                                description = "Photos and videos are hidden from system gallery and saved locally."
                            )
                            OnboardingFeatureRow(
                                icon = Icons.Default.Fingerprint,
                                title = "Passcode & Biometrics",
                                description = "Secured with a 4-digit PIN and fingerprint/face authentication."
                            )
                            OnboardingFeatureRow(
                                icon = Icons.Outlined.Mail,
                                title = "Security Recovery Email",
                                description = "Private backup email for PIN recovery kept safe and encrypted."
                            )
                        }

                        // Start Button
                        OpenSourcePressButton(
                            onClick = {
                                triggerHapticFeedback(context)
                                currentStep = 1
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .testTag("onboarding_start_button"),
                            backgroundColor = TextWhite,
                            contentColor = PureBlack
                        ) {
                            Text(
                                text = "Create Passcode",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                1, 2 -> {
                    // PIN Setup & Confirmation
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(DarkCardBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = TextWhite,
                                    modifier = Modifier.size(30.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = if (step == 1) "Create a Passcode" else "Confirm Passcode",
                                color = TextWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (step == 1) "Enter a 4-digit PIN for your vault" else "Re-enter your 4-digit PIN to confirm",
                                color = TextGray,
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            PinDotsIndicator(
                                pinLength = if (step == 1) firstPin.length else confirmPin.length
                            )

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = AccentRed,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        NumericKeypad(
                            onDigitClick = { onDigitPressed(it) },
                            onDeleteClick = { onDeletePressed() },
                            showBiometric = false,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                    }
                }

                3 -> {
                    // Step 3: Add Security Recovery Email Dialog (As requested by user!)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 40.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(DarkPillBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = TextWhite,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Add Security Email",
                                color = TextWhite,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Enter a recovery email for password recovery. For your privacy, this email is kept hidden and is only used to verify your identity if you ever forget your PIN.",
                                color = TextGray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            OutlinedTextField(
                                value = recoveryEmailInput,
                                onValueChange = {
                                    recoveryEmailInput = it
                                    errorMessage = null
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
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().testTag("onboarding_security_email_input")
                            )

                            if (errorMessage != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = AccentRed,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        OpenSourcePressButton(
                            onClick = {
                                val email = recoveryEmailInput.trim().ifEmpty { SecurityPreferences.DEFAULT_BACKUP_EMAIL }
                                securityPrefs.recoveryEmail = email
                                triggerHapticFeedback(context)
                                onSetupCompleted()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp)
                                .testTag("onboarding_save_email_button"),
                            backgroundColor = TextWhite,
                            contentColor = PureBlack
                        ) {
                            Text(
                                text = "Save & Enter Vault",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkCardBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TextWhite,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextGray,
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        }
    }
}
