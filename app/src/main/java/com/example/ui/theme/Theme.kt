package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MangaDarkColorScheme = darkColorScheme(
    primary = FlameOrange,
    onPrimary = Color.White,
    primaryContainer = FlameOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = NeonCyan,
    onSecondary = Color.Black,
    tertiary = StarGold,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DarkSurfaceCard,
    outline = TextMuted
)

private val MangaLightColorScheme = lightColorScheme(
    primary = FlameOrangeDark,
    onPrimary = Color.White,
    primaryContainer = FlameOrangeLight,
    onPrimaryContainer = Color.Black,
    secondary = FlameOrange,
    onSecondary = Color.White,
    tertiary = StarGold,
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF10121D),
    surface = Color.White,
    onSurface = Color(0xFF10121D),
    surfaceVariant = Color(0xFFE8EAFA),
    onSurfaceVariant = Color(0xFF4A4E68)
)

@Composable
fun MangaPillTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Default to sleek immersive dark scheme for manga app
    val colorScheme = if (darkTheme) MangaDarkColorScheme else MangaDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
