package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

val DarkColorScheme = darkColorScheme(
    primary = TechCyanPrimary,
    onPrimary = TechCyanOnPrimary,
    primaryContainer = TechCyanContainer,
    onPrimaryContainer = TechCyanOnContainer,
    secondary = TechBlueSecondary,
    onSecondary = TechBlueOnSecondary,
    secondaryContainer = TechBlueContainer,
    onSecondaryContainer = TechBlueOnContainer,
    tertiary = TechTealTertiary,
    onTertiary = TechTealOnTertiary,
    tertiaryContainer = TechTealContainer,
    onTertiaryContainer = TechTealOnContainer,
    background = TechDarkBackground,
    onBackground = TechDarkOnBackground,
    surface = TechDarkSurface,
    onSurface = TechDarkOnSurface,
    surfaceVariant = TechDarkSurfaceVariant,
    onSurfaceVariant = TechDarkOnSurfaceVariant,
    outline = TechDarkOutline,
    outlineVariant = TechDarkOutlineVariant
)

val LightColorScheme = lightColorScheme(
    primary = TechCyanContainer,
    onPrimary = TechCyanOnContainer,
    secondary = TechBlueContainer,
    onSecondary = TechBlueOnContainer,
    tertiary = TechTealContainer,
    background = TechLightBackground,
    onBackground = TechLightOnBackground,
    surface = TechLightSurface,
    onSurface = TechLightOnSurface,
    surfaceVariant = TechLightSurfaceVariant,
    onSurfaceVariant = TechLightOnSurfaceVariant,
    outline = TechLightOutline
)

@Composable
fun SensorExplorerTheme(
    darkTheme: Boolean = true, // Dark-first technical dashboard
    dynamicColor: Boolean = false, // Keep distinct cyber/telemetry palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = SensorExplorerTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
