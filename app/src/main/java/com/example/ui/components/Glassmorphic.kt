package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

/**
 * Custom glassmorphism card modifier that draws a subtle dark translucent glass panel,
 * delicate top specular reflection rim, and restrained ambient lighting.
 */
@Composable
fun Modifier.glassmorphicCard(
    cornerRadius: Dp = 24.dp,
    glowColor: Color = Color.Unspecified,
    isGlowCard: Boolean = false
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "glass_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val nova = LocalNova.current
    return this
        .clip(RoundedCornerShape(cornerRadius))
        .background(Brush.verticalGradient(listOf(nova.glassTop, nova.surface.copy(alpha = 0.55f), nova.glassBottom)))
        .border(
            width = if (isGlowCard) 1.2.dp else 1.dp,
            brush = if (isGlowCard) {
                Brush.linearGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.7f),
                        NeonMagenta.copy(alpha = 0.5f)
                    )
                )
            } else {
                Brush.verticalGradient(listOf(nova.glassEdgeTop, nova.glassEdgeBottom))
            },
            shape = RoundedCornerShape(cornerRadius)
        )
        .drawBehind {
            if (glowColor != Color.Unspecified) {
                // Restrained, soft ambient bloom
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        color = glowColor.copy(alpha = 0.10f * glowPulse).toArgb()
                        setShadowLayer(
                            18.dp.toPx(),
                            0f,
                            4.dp.toPx(),
                            glowColor.copy(alpha = 0.16f * glowPulse).toArgb()
                        )
                    }
                    canvas.drawRoundRect(
                        0f, 0f, size.width, size.height,
                        cornerRadius.toPx(), cornerRadius.toPx(),
                        Paint().apply {
                            asFrameworkPaint().set(paint)
                        }
                    )
                }
            }
        }
}

/**
 * A ready-to-use Glassmorphic Card Container.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    glowColor: Color = Color.Unspecified,
    isGlowCard: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .glassmorphicCard(cornerRadius, glowColor, isGlowCard)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun GeminiAmbientGlow(modifier: Modifier = Modifier) {
    NovaBackdrop(accent = NeonCyan, modifier = modifier)
}
