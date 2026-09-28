package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AriseDarkColorScheme = darkColorScheme(
    primary = AriseCyanNeon,
    onPrimary = Color(0xFF03101E),
    primaryContainer = AriseSurfaceElevated,
    onPrimaryContainer = AriseCyanNeon,
    secondary = AriseShadowViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1B4E),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = AriseGoldRank,
    onTertiary = Color(0xFF2D1E00),
    tertiaryContainer = Color(0xFF3F2B06),
    onTertiaryContainer = Color(0xFFFDE68A),
    background = AriseVoidBlack,
    onBackground = AriseTextPrimary,
    surface = AriseDeepNavy,
    onSurface = AriseTextPrimary,
    surfaceVariant = AriseSurfaceDark,
    onSurfaceVariant = AriseTextSecondary,
    outline = AriseBorderGlow,
    error = AriseCrimson,
    onError = Color.White
)

@Composable
fun AriseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AriseDarkColorScheme,
        typography = Typography,
        content = content
    )
}
