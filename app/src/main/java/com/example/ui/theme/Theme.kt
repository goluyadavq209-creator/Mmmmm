package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ClipGeniusDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = Color.White,
    primaryContainer = DeepRoyalPurple,
    onPrimaryContainer = SoftLavender,
    secondary = CyberMagenta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF43123A),
    onSecondaryContainer = Color(0xFFFBCFE8),
    tertiary = CyanSpark,
    onTertiary = ObsidianBlack,
    tertiaryContainer = Color(0xFF0E3440),
    onTertiaryContainer = Color(0xFFA5F3FC),
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineBright,
    error = CrimsonError,
    onError = Color.White,
    errorContainer = Color(0xFF4C0D1D),
    onErrorContainer = Color(0xFFFECDD3)
)

private val ClipGeniusShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun ClipGeniusTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClipGeniusDarkColorScheme,
        typography = Typography,
        shapes = ClipGeniusShapes,
        content = content
    )
}
