package com.example.scrollblock.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ScrollBlockDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = NearBlack,
    primaryContainer = ElectricBlueSubtle,
    onPrimaryContainer = ElectricBlueGlow,
    secondary = ElectricBlueVariant,
    onSecondary = Color.White,
    background = NearBlack,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = CardBackgroundElevated,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder
)

@Composable
fun ScrollBlockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ScrollBlockDarkColorScheme,
        typography = Typography,
        content = content
    )
}
