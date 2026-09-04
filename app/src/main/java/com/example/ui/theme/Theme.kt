package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DentalColorScheme =
  lightColorScheme(
    primary = DentalPrimary,
    onPrimary = Color.White,
    primaryContainer = DentalSupport,
    onPrimaryContainer = DentalPrimaryDark,
    secondary = DentalSecondary,
    onSecondary = Color.White,
    background = DentalBackground,
    onBackground = Color(0xFF1E1E1E),
    surface = DentalSurface,
    onSurface = Color(0xFF1E1E1E)
  )

@Composable
fun DentalProTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DentalColorScheme,
    typography = Typography,
    content = content
  )
}

