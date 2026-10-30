package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HighContrastDarkColorScheme = darkColorScheme(
    primary = TextWhite,
    onPrimary = PureBlack,
    primaryContainer = DarkPillBg,
    onPrimaryContainer = TextWhite,
    secondary = TextWhite,
    onSecondary = PureBlack,
    secondaryContainer = DarkPillBg,
    onSecondaryContainer = TextWhite,
    background = PureBlack,
    onBackground = TextWhite,
    surface = PureBlack,
    onSurface = TextWhite,
    surfaceVariant = DarkCardBg,
    onSurfaceVariant = TextGray,
    outline = DarkBorder,
    outlineVariant = DarkSurface,
    error = AccentRed,
    onError = TextWhite
)

@Composable
fun PrivateFolderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HighContrastDarkColorScheme,
        typography = Typography,
        content = content
    )
}
