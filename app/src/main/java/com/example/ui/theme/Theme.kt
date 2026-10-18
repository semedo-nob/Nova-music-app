package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = GeminiBlue,
    secondary = CyberPurple,
    tertiary = NeonPink,
    background = DarkBackgroundStatic,
    surface = DarkSurfaceStatic,
    surfaceVariant = DarkSurfaceVariantStatic,
    surfaceContainerLowest = ConsoleWell,
    surfaceContainerLow = DarkBackgroundStatic,
    surfaceContainer = DarkSurfaceStatic,
    surfaceContainerHigh = DarkSurfaceElevatedStatic,
    surfaceContainerHighest = ConsoleHighlight,
    outline = GlassBorderColor,
    onPrimary = DarkBackgroundStatic,
    onSecondary = DarkBackgroundStatic,
    onTertiary = DarkBackgroundStatic,
    onBackground = OnDarkTextStatic,
    onSurface = OnDarkTextStatic,
    onSurfaceVariant = OnDarkTextSecondaryStatic
)

private val LightColorScheme = lightColorScheme(
    primary = GeminiBlue,
    secondary = CyberPurple,
    tertiary = NeonPink,
    background = LightBackgroundStatic,
    surface = LightSurfaceStatic,
    surfaceVariant = LightSurfaceVariantStatic,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onBackground = OnLightTextStatic,
    onSurface = OnLightTextStatic,
    onSurfaceVariant = OnLightTextSecondaryStatic
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val systemBgColor = if (darkTheme) DarkBackgroundStatic else LightBackgroundStatic
            window.statusBarColor = systemBgColor.toArgb()
            window.navigationBarColor = systemBgColor.toArgb()

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
