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
    primary = LearnerBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003D80),
    onPrimaryContainer = Color(0xFFD0E4FF),
    secondary = LearnerAccentGold,
    onSecondary = PortBlack,
    tertiary = LearnerAccentGreen,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = Color(0xFF334155),
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LearnerBlue,
    onPrimary = Color.White,
    primaryContainer = LearnerBlueLight,
    onPrimaryContainer = LearnerBlueDark,
    secondary = LearnerAccentGold,
    onSecondary = PortBlack,
    tertiary = LearnerAccentGreen,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = PortBorder,
  )

@Composable
fun LearnerportTheme(
  darkTheme: Boolean = false, // Always defaults to Light Mode unless explicitly set to Dark Mode
  dynamicColor: Boolean = false, // Keep signature Apple iOS-inspired palette consistent
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
