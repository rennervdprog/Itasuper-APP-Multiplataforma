package com.example.shared.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = ItaSuperPrimary,
    onPrimary = Color.White,
    primaryContainer = ItaSuperHighlightBg,
    onPrimaryContainer = ItaSuperHighlightText,
    secondary = ItaSuperSecondary,
    onSecondary = ItaSuperTextPrimary,
    tertiary = ItaSuperWarning,
    background = ItaSuperBackground,
    onBackground = ItaSuperTextPrimary,
    surface = ItaSuperBackground,
    onSurface = ItaSuperTextPrimary,
    surfaceVariant = ItaSuperSecondary,
    onSurfaceVariant = ItaSuperTextSecondary,
    outline = ItaSuperBorder,
    error = ItaSuperError,
    onError = Color.White
)

@Composable
fun ItaSuperTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
