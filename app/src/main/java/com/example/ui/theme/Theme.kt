package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PocketLlmColorScheme = darkColorScheme(
    primary = AccentMint,
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF005136),
    onPrimaryContainer = AccentMintLight,
    secondary = AccentCyan,
    onSecondary = Color(0xFF003548),
    secondaryContainer = Color(0xFF004D68),
    onSecondaryContainer = Color(0xFFCBE6FF),
    tertiary = AccentPurple,
    onTertiary = Color(0xFF381E72),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    error = AccentRose,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent cyber AI aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PocketLlmColorScheme,
        typography = Typography,
        content = content
    )
}
