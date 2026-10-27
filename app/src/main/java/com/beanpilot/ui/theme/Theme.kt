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
    secondary = EspressoBrown,
    tertiary = MachineBlue,
    background = EspressoDark,
    surface = EspressoBrown,
    onPrimary = EspressoDark,
    onSecondary = FoamCream,
    onBackground = FoamCream,
    onSurface = FoamCream,
)

@Composable
fun BeanPilotTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}
