package com.messagestudio.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF0F766E)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCEFEA),
    onPrimaryContainer = Color(0xFF00201D),
    secondaryContainer = Color(0xFFD9ECE9)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5FD3C6),
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFFCCEFEA)
)

@Composable
fun StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
