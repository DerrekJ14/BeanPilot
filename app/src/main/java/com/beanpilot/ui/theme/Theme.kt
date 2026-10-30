package com.beanpilot.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = EspressoBrown,
    secondary = EspressoGold,
    tertiary = MachineBlue,
    background = EspressoBackground,
    surface = FoamCream,
    onPrimary = Color.White,
    onSecondary = EspressoDark,
    onBackground = EspressoDark,
    onSurface = EspressoDark,
)

private val DarkColors = darkColorScheme(
    primary = EspressoGold,
    secondary = MachineBlue,
    tertiary = MachineBlue,
    background = EspressoDark,
    surface = Color(0xFF12161B),
    surfaceVariant = Color(0xFF1B2229),
    surfaceContainerLowest = Color(0xFF0D1014),
    surfaceContainerLow = Color(0xFF12161B),
    surfaceContainer = Color(0xFF171C22),
    surfaceContainerHigh = Color(0xFF20272F),
    surfaceContainerHighest = Color(0xFF29323B),
    onPrimary = EspressoDark,
    onSecondary = FoamCream,
    onBackground = FoamCream,
    onSurface = FoamCream,
    onSurfaceVariant = Color(0xFFABB6C2),
    outline = Color(0xFF46515D),
    error = Color(0xFFFF6B6B),
)

@Composable
fun BeanPilotTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}
