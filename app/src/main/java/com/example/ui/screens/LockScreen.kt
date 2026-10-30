package com.example.ui.screens

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.SecurityPreferences
import com.example.ui.components.NumericKeypad
import com.example.ui.components.OpenSourcePressButton
import com.example.ui.components.PinDotsIndicator
import com.example.ui.components.triggerHapticFeedback
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    securityPrefs: SecurityPreferences,
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }

    val shakeAnim = remember { Animatable(0f) }

    fun triggerBiometrics() {
        val activity = context as? FragmentActivity ?: return
        val biometricManager = BiometricManager.from(context)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        triggerHapticFeedback(context)
                        onUnlockSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Private Folder")
                .setSubtitle("Use Biometrics to access your vault")
                .setNegativeButtonText("Use Passcode")
                .build()

            prompt.authenticate(promptInfo)
        }
    }

    LaunchedEffect(Unit) {
        if (securityPrefs.isBiometricsEnabled) {
            triggerBiometrics()
        }
    }

    fun onDigit(d: String) {
        if (enteredPin.length < 4) {
            val newPin = enteredPin + d
            enteredPin = newPin
            errorMessage = null

            if (newPin.length == 4) {
                if (securityPrefs.verifyPin(newPin)) {
                    triggerHapticFeedback(context)
                    onUnlockSuccess()
                } else {
                    errorMessage = "Incorrect Passcode"
                    coroutineScope.launch {
                        triggerHapticFeedback(context)
                        shakeAnim.animateTo(
                            targetValue = 0f,
                            animationSpec = keyframes {
                                durationMillis = 400
                                -25f at 50
                                25f at 100
                                -20f at 150
                                20f at 200
                                -10f at 250
                                10f at 300
                                0f at 400
                            }
                        )
                        enteredPin = ""
                    }
                }
            }
        }
    }

    fun onDelete() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    Scaffold(
        containerColor = PureBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Lock Icon & Text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(DarkPillBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = TextWhite,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Private Folder",
                    color = TextWhite,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter 4-Digit Passcode",
                    color = TextGray,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                PinDotsIndicator(
                    pinLength = enteredPin.length,
                    shakeOffset = shakeAnim.value
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = AccentRed,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 14.dp)
                    )
                }
            }

            // Keypad
            NumericKeypad(
                onDigitClick = { onDigit(it) },
                onDeleteClick = { onDelete() },
                onBiometricClick = { triggerBiometrics() },
                showBiometric = securityPrefs.isBiometricsEnabled,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Forgot PIN
            TextButton(
                onClick = { showForgotDialog = true },
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .testTag("forgot_pin_button")
            ) {
                Text(
                    text = "Forgot PIN?",
                    color = TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (showForgotDialog) {
        ForgotPinDialog(
            securityPrefs = securityPrefs,
            onDismiss = { showForgotDialog = false },
            onPinResetSuccess = {
                showForgotDialog = false
                enteredPin = ""
                errorMessage = "New PIN set. Please unlock."
            }
        )
    }
}

@Composable
fun ForgotPinDialog(
    securityPrefs: SecurityPreferences,
    onDismiss: () -> Unit,
    onPinResetSuccess: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var isEmailVerified by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkCardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.padding(12.dp)
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
                        imageVector = if (isEmailVerified) Icons.Default.LockOpen else Icons.Outlined.Mail,
                        contentDescription = null,
                        tint = TextWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (!isEmailVerified) "PIN Recovery" else "Set New 4-Digit PIN",
                    color = TextWhite,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (!isEmailVerified) {
                        "Enter your security recovery email to verify identity and reset your passcode."
                    } else {
                        "Enter and confirm your new 4-digit PIN."
                    },
                    color = TextGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (!isEmailVerified) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            dialogError = null
                        },
                        placeholder = { Text("Enter your recovery email", color = TextMuted, fontSize = 14.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextWhite,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            cursorColor = TextWhite
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("recovery_email_input")
                    )
                } else {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                        placeholder = { Text("New 4-Digit PIN", color = TextMuted) },
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

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                        placeholder = { Text("Confirm 4-Digit PIN", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextWhite,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            cursorColor = TextWhite
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input")
                    )
                }

                if (dialogError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = dialogError ?: "",
                        color = AccentRed,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
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
                            if (!isEmailVerified) {
                                if (securityPrefs.verifyRecoveryEmail(emailInput)) {
                                    isEmailVerified = true
                                    dialogError = null
                                } else {
                                    dialogError = "Email did not match registered recovery address."
                                }
                            } else {
                                if (newPin.length != 4) {
                                    dialogError = "PIN must be exactly 4 digits."
                                } else if (newPin != confirmPin) {
                                    dialogError = "PINs do not match."
                                } else {
                                    securityPrefs.setPin(newPin)
                                    onPinResetSuccess()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("dialog_confirm_button"),
                        backgroundColor = TextWhite,
                        contentColor = PureBlack
                    ) {
                        Text(if (!isEmailVerified) "Verify" else "Save PIN", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
