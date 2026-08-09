package com.pcapps.launcher.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7EB6FF),
    secondary = Color(0xFF9AD1FF),
    background = Color(0xFF0E1116),
    surface = Color(0xFF161B22)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1A73E8),
    secondary = Color(0xFF4285F4)
)

@Composable
fun PCAppsTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
