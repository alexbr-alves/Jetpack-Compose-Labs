package com.alexbralves.boardingpassmotion.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorScheme =
  darkColorScheme(
    primary = SkyAccent,
    secondary = Gold,
    background = Night,
    surface = DeepNavy,
    onPrimary = Color.White,
    onSecondary = Color(0xFF191305),
    onBackground = Color.White,
    onSurface = Color.White,
  )

@Composable
fun BoardingPassMotionTheme(content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = ColorScheme, typography = Typography, content = content)
}
