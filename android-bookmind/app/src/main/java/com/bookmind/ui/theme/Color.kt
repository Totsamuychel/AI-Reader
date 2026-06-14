package com.bookmind.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Full reader palette. Each [AppTheme] picks a handful of these as anchors and
 * the rest of the Material 3 [androidx.compose.material3.ColorScheme] is derived
 * from them in [AppTheme.toColorScheme].
 */
object Palette {
    // Neutrals
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)

    // Paper / sepia
    val Paper = Color(0xFFF5F0E8)
    val PaperInk = Color(0xFF2B2620)
    val Sepia = Color(0xFFFBF0D9)
    val SepiaInk = Color(0xFF5B4636)
    val Brown = Color(0xFF795548)
    val BrownDeep = Color(0xFF6D4C41)

    // Material dark
    val MaterialDark = Color(0xFF121212)
    val MaterialPurple = Color(0xFFBB86FC)
    val AmoledPurple = Color(0xFF7C4DFF)

    // Forest
    val ForestBg = Color(0xFF0D1B0E)
    val ForestGreen = Color(0xFF4CAF50)

    // Ocean
    val OceanBg = Color(0xFF0A1929)
    val OceanBlue = Color(0xFF0288D1)

    // Sunset
    val SunsetBg = Color(0xFFFFF8F0)
    val SunsetOrange = Color(0xFFE64A19)

    // Midnight
    val MidnightBg = Color(0xFF0D0D1A)
    val MidnightIndigo = Color(0xFF3F51B5)

    // Rose
    val RoseBg = Color(0xFFFCE4EC)
    val RosePink = Color(0xFFE91E63)
}
