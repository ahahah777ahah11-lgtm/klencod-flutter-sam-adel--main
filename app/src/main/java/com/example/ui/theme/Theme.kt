package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val FinancialLightColorScheme = lightColorScheme(
    primary = FinancialDarkBlue,
    onPrimary = Color.White,
    primaryContainer = FinancialBlue.copy(alpha = 0.15f),
    onPrimaryContainer = FinancialDarkBlue,
    secondary = FinancialBlue,
    onSecondary = Color.White,
    secondaryContainer = FinancialTurquoise.copy(alpha = 0.2f),
    onSecondaryContainer = FinancialDarkBlue,
    tertiary = FinancialTurquoise,
    onTertiary = FinancialDarkBlue,
    background = FinancialLightBg,
    onBackground = TextPrimary,
    surface = FinancialSurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFEAEFF5),
    onSurfaceVariant = TextSecondary,
    outline = BorderLight
)

private val FinancialDarkColorScheme = darkColorScheme(
    primary = FinancialTurquoise,
    onPrimary = FinancialDarkBlue,
    primaryContainer = FinancialBlue.copy(alpha = 0.35f),
    onPrimaryContainer = Color.White,
    secondary = FinancialBlue,
    onSecondary = Color.White,
    tertiary = FinancialTurquoise,
    onTertiary = FinancialDarkBlue,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

private val CrimsonLightColorScheme = lightColorScheme(
    primary = CrimsonRed,
    onPrimary = Color.White,
    primaryContainer = CrimsonRed.copy(alpha = 0.15f),
    onPrimaryContainer = CrimsonRed,
    secondary = Color(0xFF8B0000),
    onSecondary = Color.White,
    tertiary = Color(0xFFE53935),
    background = CrimsonLightBg,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFFBE9E7),
    onSurfaceVariant = TextSecondary,
    outline = BorderLight
)

private val CrimsonDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF5252),
    onPrimary = Color.White,
    primaryContainer = CrimsonRed.copy(alpha = 0.4f),
    onPrimaryContainer = Color.White,
    secondary = CrimsonRed,
    onSecondary = Color.White,
    tertiary = Color(0xFFFF8A80),
    background = Color(0xFF1A0A0C),
    onBackground = Color(0xFFF5E6E8),
    surface = Color(0xFF2C1417),
    onSurface = Color(0xFFF5E6E8),
    surfaceVariant = Color(0xFF3B1E22),
    outline = Color(0xFF5C2B30)
)

@Composable
fun SmartDebtTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeStyle: String = "blue_teal", // blue_teal or red_white
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        themeStyle == "red_white" -> if (darkTheme) CrimsonDarkColorScheme else CrimsonLightColorScheme
        else -> if (darkTheme) FinancialDarkColorScheme else FinancialLightColorScheme
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
