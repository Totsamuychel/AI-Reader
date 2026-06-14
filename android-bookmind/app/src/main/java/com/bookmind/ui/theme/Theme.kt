package com.bookmind.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext

/**
 * Ten built-in reader themes (+ a CUSTOM slot). Each defines a few anchor
 * colors; [toColorScheme] expands them into a full Material 3 scheme so every
 * component stays themable.
 */
enum class AppTheme(
    val displayName: String,
    val isDark: Boolean,
    val primaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val textColor: Color
) {
    SYSTEM("System default", false, Palette.MaterialPurple, Palette.White, Palette.White, Palette.Black),
    DARK_AMOLED("Dark AMOLED", true, Palette.AmoledPurple, Palette.Black, Color(0xFF0A0A0A), Color(0xFFECECEC)),
    DARK_MATERIAL("Dark Material", true, Palette.MaterialPurple, Palette.MaterialDark, Color(0xFF1E1E1E), Color(0xFFE6E1E5)),
    LIGHT_PAPER("Paper", false, Palette.Brown, Palette.Paper, Color(0xFFEDE6D8), Palette.PaperInk),
    SEPIA("Sepia", false, Palette.BrownDeep, Palette.Sepia, Color(0xFFF3E6C8), Palette.SepiaInk),
    FOREST("Forest", true, Palette.ForestGreen, Palette.ForestBg, Color(0xFF142415), Color(0xFFDCEFD9)),
    OCEAN("Ocean", true, Palette.OceanBlue, Palette.OceanBg, Color(0xFF102A43), Color(0xFFD6E6F5)),
    SUNSET("Sunset", false, Palette.SunsetOrange, Palette.SunsetBg, Color(0xFFFFEFE0), Color(0xFF3E2723)),
    MIDNIGHT("Midnight", true, Palette.MidnightIndigo, Palette.MidnightBg, Color(0xFF16162B), Color(0xFFDADAF0)),
    ROSE("Rose", false, Palette.RosePink, Palette.RoseBg, Color(0xFFFFF0F5), Color(0xFF3E1B2A)),
    CUSTOM("Custom", false, Palette.MaterialPurple, Palette.White, Palette.White, Palette.Black);

    fun toColorScheme(): ColorScheme {
        val onPrimary = if (primaryColor.luminance() > 0.5f) Palette.Black else Palette.White
        val base = if (isDark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = primaryColor,
            onPrimary = onPrimary,
            primaryContainer = primaryColor.copy(alpha = if (isDark) 0.35f else 0.20f).compositeOver(surfaceColor),
            onPrimaryContainer = textColor,
            secondary = primaryColor,
            background = backgroundColor,
            onBackground = textColor,
            surface = surfaceColor,
            onSurface = textColor,
            surfaceVariant = surfaceColor,
            onSurfaceVariant = textColor.copy(alpha = 0.75f).compositeOver(surfaceColor)
        )
    }

    companion object {
        fun fromName(name: String?): AppTheme =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

private fun Color.compositeOver(background: Color): Color {
    val a = alpha + background.alpha * (1f - alpha)
    if (a == 0f) return Color.Transparent
    val r = (red * alpha + background.red * background.alpha * (1f - alpha)) / a
    val g = (green * alpha + background.green * background.alpha * (1f - alpha)) / a
    val b = (blue * alpha + background.blue * background.alpha * (1f - alpha)) / a
    return Color(r, g, b, a)
}

/** Lets any composable read the active theme (for previews, e-ink toggles, etc.). */
val LocalAppTheme = staticCompositionLocalOf { AppTheme.SYSTEM }

@Composable
fun BookMindTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val context = LocalContext.current

    val colorScheme: ColorScheme = when {
        appTheme == AppTheme.SYSTEM && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        appTheme == AppTheme.SYSTEM ->
            if (systemDark) darkColorScheme() else lightColorScheme()
        else -> appTheme.toColorScheme()
    }

    CompositionLocalProvider(LocalAppTheme provides appTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
