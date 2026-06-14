package com.bookmind.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Reading font choices. The premium-reader prompt asks for named families
 * (Georgia, Merriweather, OpenDyslexic, …). To stay dependency-free we map them
 * onto the generic families the platform always ships; bundling the real .ttf
 * files later only requires changing [fontFamily].
 */
enum class ReaderFont(val displayName: String, val fontFamily: FontFamily) {
    SYSTEM("System", FontFamily.Default),
    SERIF("Serif (Georgia-like)", FontFamily.Serif),
    SANS("Sans (Roboto-like)", FontFamily.SansSerif),
    MONO("Monospace", FontFamily.Monospace),
    CURSIVE("Cursive", FontFamily.Cursive);

    companion object {
        fun fromName(name: String?): ReaderFont =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

private val default = Typography()

/** A little more weight and tighter display sizes than the defaults. */
val AppTypography = Typography(
    headlineMedium = default.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = default.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = default.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = default.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
    titleSmall = default.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = default.labelLarge.copy(fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
)
