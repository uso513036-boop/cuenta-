package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = CyanNeon,
        onPrimary = Color(0xFF00363D),
        primaryContainer = Color(0xFF004F58),
        onPrimaryContainer = CyanGlow,
        secondary = VioletVault,
        onSecondary = Color(0xFF2C0B56),
        secondaryContainer = Color(0xFF452077),
        onSecondaryContainer = Color(0xFFEADDFF),
        tertiary = EmeraldSafe,
        onTertiary = Color(0xFF003822),
        tertiaryContainer = Color(0xFF005234),
        onTertiaryContainer = EmeraldMint,
        background = DarkBg,
        onBackground = DarkTextPrimary,
        surface = DarkSurface,
        onSurface = DarkTextPrimary,
        surfaceVariant = DarkSurfaceCard,
        onSurfaceVariant = DarkTextSecondary,
        outline = DarkBorder,
        error = RoseLock,
        onError = Color.White
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF006874),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF97F0FF),
        onPrimaryContainer = Color(0xFF001F24),
        secondary = Color(0xFF6750A4),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFEADDFF),
        onSecondaryContainer = Color(0xFF21005D),
        tertiary = Color(0xFF006C47),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFF8CF8C1),
        onTertiaryContainer = Color(0xFF002113),
        background = LightBg,
        onBackground = LightTextPrimary,
        surface = LightSurface,
        onSurface = LightTextPrimary,
        surfaceVariant = LightSurfaceCard,
        onSurfaceVariant = LightTextSecondary,
        outline = LightBorder,
        error = RoseLock,
        onError = Color.White
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent cyber/vault brand styling
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

