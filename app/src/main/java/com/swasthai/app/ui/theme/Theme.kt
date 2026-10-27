package com.swasthai.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppleLightColorScheme = lightColorScheme(
    primary = AppleSystemBlue,
    onPrimary = Color.White,
    primaryContainer = AppleBlueLight,
    onPrimaryContainer = AppleSystemBlue,
    secondary = AppleHealthTeal,
    onSecondary = Color.White,
    secondaryContainer = AppleTealLight,
    onSecondaryContainer = AppleHealthTeal,
    background = AppleGroupedBackground,
    onBackground = AppleLabelPrimary,
    surface = AppleCardSurface,
    onSurface = AppleLabelPrimary,
    surfaceVariant = AppleGroupedBackground,
    onSurfaceVariant = AppleLabelSecondary,
    outline = AppleSeparator,
    outlineVariant = AppleSeparator,
    error = AppleSystemRed,
    onError = Color.White,
    errorContainer = AppleRedLight,
    onErrorContainer = AppleSystemRed
)

@Composable
fun SwasthAITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppleLightColorScheme,
        typography = Typography,
        content = content
    )
}
