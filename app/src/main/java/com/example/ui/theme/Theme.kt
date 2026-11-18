package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val GhostLineColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = VoidBlack,
    primaryContainer = DeepSurface,
    onPrimaryContainer = NeonCyan,
    secondary = NeonMagenta,
    onSecondary = VoidBlack,
    secondaryContainer = ElevatedSurface,
    onSecondaryContainer = NeonMagenta,
    tertiary = NeonViolet,
    onTertiary = TextPrimary,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = MidnightIndigo,
    onSurface = TextPrimary,
    surfaceVariant = DeepSurface,
    onSurfaceVariant = TextSecondary,
    outline = OutlineGlow,
    error = NeonCoral,
    onError = VoidBlack
)

val GhostLineShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun GhostLineTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GhostLineColorScheme,
        typography = GhostLineTypography,
        shapes = GhostLineShapes,
        content = content
    )
}
