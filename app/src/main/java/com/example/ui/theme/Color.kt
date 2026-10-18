package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// "Studio Console" palette: ink-plum base, soft aqua / lavender / coral accents, brass hardware.

// Dark surfaces
val DarkBackgroundStatic = Color(0xFF110F1B)      // Ink plum background
val DarkSurfaceStatic = Color(0xFF1A1727)         // Plate surface (neumorphic base)
val NeumorphicSurfaceElevated = Color(0xFF1A1727)
val DarkSurfaceElevatedStatic = Color(0xFF241F36) // Raised highlight tone
val DarkSurfaceVariantStatic = Color(0x2A1A1727)  // Translucent glass panel
val OnDarkTextStatic = Color(0xFFF3F1FA)          // Warm white text
val OnDarkTextSecondaryStatic = Color(0xFF9A96B0) // Muted lavender-grey

// Light fallback
val LightBackgroundStatic = Color(0xFFE7E4EF)     // Pale lilac-grey
val LightSurfaceStatic = Color(0xFFF6F4FB)
val LightSurfaceVariantStatic = Color(0x0C000000)
val OnLightTextStatic = Color(0xFF191527)
val OnLightTextSecondaryStatic = Color(0xFF5C5873)

// Dynamic getters (unchanged API)
val DarkBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val DarkSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val DarkSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val OnDarkText: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onBackground

val OnDarkTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

// Accents
val NeonCyan = Color(0xFF5CE1E6)                  // Soft aqua
val NeonMagenta = Color(0xFFFB7185)               // Coral rose
val CyanGlowRestrained = Color(0x385CE1E6)
val MagentaGlowRestrained = Color(0x2BFB7185)
val BrassAmber = Color(0xFFF2B35E)                // Hardware / LED mid-tone

// Mapped accents (names kept so existing screens still compile)
val GeminiBlue = NeonCyan
val CyberPurple = Color(0xFFA78BFA)               // Lavender
val NeonPink = NeonMagenta
val SoftCyan = NeonCyan

// Gradients & glass
val CyanMagentaGradient = listOf(NeonCyan, NeonMagenta)
val GeminiGradientList = listOf(NeonCyan, CyberPurple, NeonMagenta)
val GlassBorderColor = Color(0x1FFFFFFF)
val GlassSurfaceFill = Color(0x801A1727)
val GlassHighlightBorder = Color(0x38FFFFFF)

// Depth steps on the same ink-plum ramp, for grooves and raised hardware.
val ConsoleWell = Color(0xFF0C0A14)
val ConsoleHighlight = Color(0xFF322C4A)
