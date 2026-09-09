package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BentoDarkColorScheme = darkColorScheme(
    primary = BentoHeroLavender,
    onPrimary = BentoHeroOnLavender,
    primaryContainer = BentoSubHeroPurple,
    onPrimaryContainer = BentoHeroLavender,
    secondary = BentoTextSub,
    onSecondary = BentoDarkBg,
    tertiary = EmeraldAccent,
    background = BentoDarkBg,
    surface = BentoDarkSurface,
    surfaceVariant = BentoDarkSurfaceVariant,
    onBackground = BentoTextLight,
    onSurface = BentoTextLight,
    onSurfaceVariant = BentoTextMuted,
    outline = BentoDarkBorder,
    error = RoseAccent
)

private val BentoLightColorScheme = lightColorScheme(
    primary = BentoLightPrimary,
    onPrimary = BentoLightOnPrimary,
    primaryContainer = BentoLightPrimaryContainer,
    onPrimaryContainer = BentoLightOnPrimaryContainer,
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    tertiary = EmeraldAccent,
    background = BentoLightBg,
    surface = BentoLightSurface,
    surfaceVariant = BentoLightSurfaceVariant,
    onBackground = Color(0xFF1D1B20),
    onSurface = Color(0xFF1D1B20),
    onSurfaceVariant = Color(0xFF49454F),
    outline = BentoLightBorder,
    error = RoseAccent
)

@Composable
fun StudentScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use strict Bento Grid colors for cohesive aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BentoDarkColorScheme else BentoLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

