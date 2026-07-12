package com.posepilot.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyberGreen,
    onPrimary = ObsidianBg,
    primaryContainer = CyberGreenContainer,
    onPrimaryContainer = CyberGreen,
    secondary = CyberTeal,
    onSecondary = ObsidianBg,
    secondaryContainer = CyberTealContainer,
    onSecondaryContainer = CyberTeal,
    background = ObsidianBg,
    onBackground = TextWhite,
    surface = SlateSurface,
    onSurface = TextWhite,
    error = ErrorNeon,
    onError = ObsidianBg
)

@Composable
fun PosePilotTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
