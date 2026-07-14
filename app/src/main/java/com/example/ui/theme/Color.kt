package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Premium Gen-Z Accents
val VioletLavender = Color(0xFFD0BCFF)
val DeepViolet = Color(0xFF381E72)
val NeonGreen = Color(0xFF00FF88)
val ElectricBlue = Color(0xFF00E5FF)
val HotPink = Color(0xFFFF2E93)
val GoldenAmber = Color(0xFFFFB300)

// System Canvas Colors
val AmoledBlack = Color(0xFF050505)
val DarkGrey = Color(0xFF121212)
val LightCanvas = Color(0xFFF8F9FA)

// Glass Transparencies (Overlay depths matching CSS)
val GlassDarkSurface = Color(0x14FFFFFF) // 8% white
val GlassDarkBorder = Color(0x1FFFFFFF)  // 12% white
val GlassLightSurface = Color(0x14000000)
val GlassLightBorder = Color(0x1F000000)

val Accents = listOf(VioletLavender, NeonGreen, ElectricBlue, HotPink, GoldenAmber)

val themeText: Color
    @Composable
    get() = if (LocalIsDarkMode.current) Color.White else Color(0xFF121212)

val themeTextMuted: Color
    @Composable
    get() = if (LocalIsDarkMode.current) Color.White.copy(alpha = 0.6f) else Color(0xFF121212).copy(alpha = 0.6f)

val themeTextFaint: Color
    @Composable
    get() = if (LocalIsDarkMode.current) Color.White.copy(alpha = 0.4f) else Color(0xFF121212).copy(alpha = 0.4f)

val themeDivider: Color
    @Composable
    get() = if (LocalIsDarkMode.current) Color.White.copy(alpha = 0.1f) else Color(0xFF121212).copy(alpha = 0.1f)

val themeCardBg: Color
    @Composable
    get() = if (LocalIsDarkMode.current) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)

val themeCardBgSelected: Color
    @Composable
    get() = if (LocalIsDarkMode.current) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)

val themeDialogBg: Color
    @Composable
    get() = if (LocalIsDarkMode.current) Color(0xFF1E1E1E) else Color(0xFFFFFFFF)

val themeCardBorder: Color
    @Composable
    get() = if (LocalIsDarkMode.current) Color.White.copy(alpha = 0.1f) else Color(0xFF121212).copy(alpha = 0.1f)

