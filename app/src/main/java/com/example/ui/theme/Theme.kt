package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BaseDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    secondary = ElectricBlue,
    tertiary = HotPink,
    background = DarkGrey,
    surface = DarkGrey,
    onBackground = Color.White,
    onSurface = Color.White
)

private val BaseLightColorScheme = lightColorScheme(
    primary = Color(0xFF00835F),
    secondary = Color(0xFF007A99),
    tertiary = Color(0xFFC2005A),
    background = LightCanvas,
    surface = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

@Composable
fun BeatFlowTheme(
    isDarkMode: Boolean = true,
    isAmoledMode: Boolean = true,
    accentColorIndex: Int = 0,
    content: @Composable () -> Unit
) {
    // Select the accent color
    val selectedAccent = Accents.getOrElse(accentColorIndex) { NeonGreen }

    val colorScheme = if (isDarkMode) {
        val bg = if (isAmoledMode) AmoledBlack else DarkGrey
        BaseDarkColorScheme.copy(
            primary = selectedAccent,
            background = bg,
            surface = bg,
            onBackground = Color.White,
            onSurface = Color.White
        )
    } else {
        BaseLightColorScheme.copy(
            primary = selectedAccent,
            onBackground = Color(0xFF121212),
            onSurface = Color(0xFF121212)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
