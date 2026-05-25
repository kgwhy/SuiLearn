package com.suilearn.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Forest = Color(0xFF214D43)
private val ForestDeep = Color(0xFF17221C)
private val Mint = Color(0xFFEAF2EC)
private val WarmGold = Color(0xFFC78A11)
private val GoldContainer = Color(0xFFFFF4D8)
private val Clay = Color(0xFFB94F3E)
private val ClayContainer = Color(0xFFFFE9E3)
private val Paper = Color(0xFFFAFBF8)
private val Canvas = Color(0xFFF7F8F5)
private val Line = Color(0xFFDDE4DA)
private val Muted = Color(0xFF59645E)

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = Forest,
    onPrimaryContainer = Color.White,
    secondary = WarmGold,
    onSecondary = ForestDeep,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = ForestDeep,
    tertiary = Clay,
    onTertiary = Color.White,
    tertiaryContainer = ClayContainer,
    onTertiaryContainer = ForestDeep,
    background = Canvas,
    onBackground = ForestDeep,
    surface = Paper,
    onSurface = ForestDeep,
    surfaceVariant = Mint,
    onSurfaceVariant = Muted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Paper,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Mint,
    outline = Color(0xFFAFC8BD),
    outlineVariant = Line,
    error = Clay,
    errorContainer = ClayContainer,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8D5C8),
    onPrimary = Color(0xFF0D2B23),
    primaryContainer = Color(0xFF173C34),
    onPrimaryContainer = Color(0xFFEAF7F1),
    secondary = Color(0xFFE8C36E),
    onSecondary = Color(0xFF332300),
    secondaryContainer = Color(0xFF493600),
    onSecondaryContainer = Color(0xFFFFF1C6),
    tertiary = Color(0xFFFFB5A6),
    onTertiary = Color(0xFF4A170F),
    tertiaryContainer = Color(0xFF6B2A20),
    onTertiaryContainer = Color(0xFFFFDAD2),
)

@Composable
fun SuiLearnTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
