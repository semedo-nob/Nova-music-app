package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/* ───────────────────────────── Tokens ───────────────────────────── */

@Immutable
class NovaColors(
    val bg: Color,
    val surface: Color,
    val raisedLight: Color,   // neumorphic highlight (top-left)
    val raisedDark: Color,    // neumorphic shadow (bottom-right)
    val glassTop: Color,
    val glassBottom: Color,
    val glassEdgeTop: Color,
    val glassEdgeBottom: Color,
    val text: Color,
    val textDim: Color,
    val hairline: Color,
    val isDark: Boolean
)

val NovaDark = NovaColors(
    bg = Color(0xFF110F1B),
    surface = Color(0xFF1A1727),
    raisedLight = Color(0xFF2A2540),
    raisedDark = Color(0xFF07060D),
    glassTop = Color(0x2CFFFFFF),
    glassBottom = Color(0x0CFFFFFF),
    glassEdgeTop = Color(0x66FFFFFF),
    glassEdgeBottom = Color(0x12FFFFFF),
    text = Color(0xFFF3F1FA),
    textDim = Color(0xFF9A96B0),
    hairline = Color(0x14FFFFFF),
    isDark = true
)

val NovaLight = NovaColors(
    bg = Color(0xFFE7E4EF),
    surface = Color(0xFFE7E4EF),
    raisedLight = Color(0xFFFFFFFF),
    raisedDark = Color(0xFFB9B4CB),
    glassTop = Color(0xBFFFFFFF),
    glassBottom = Color(0x55FFFFFF),
    glassEdgeTop = Color(0xFFFFFFFF),
    glassEdgeBottom = Color(0x40FFFFFF),
    text = Color(0xFF191527),
    textDim = Color(0xFF5C5873),
    hairline = Color(0x14000000),
    isDark = false
)

val LocalNova = staticCompositionLocalOf { NovaDark }

private val MetalHi = Color(0xFFF4F4F8)
private val MetalMid = Color(0xFFB4B2C2)
private val MetalLo = Color(0xFF6B6980)
private val LedAmber = Color(0xFFF2B35E)

/* ─────────────────────────── Modifiers ─────────────────────────── */

private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}

/** Neumorphic "extruded" plate: light shadow top-left, dark shadow bottom-right. */
fun Modifier.novaRaised(
    n: NovaColors,
    corner: Dp = 20.dp,
    depth: Dp = 6.dp,
    fill: Color = n.surface
): Modifier = this.drawBehind {
    val r = corner.toPx()
    val d = depth.toPx()
    drawIntoCanvas { canvas ->
        val fp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = fill.toArgb()
        }
        fp.setShadowLayer(d * 1.8f, -d, -d, n.raisedLight.toArgb())
        canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, fp)
        fp.setShadowLayer(d * 1.8f, d, d, n.raisedDark.toArgb())
        canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, fp)
        fp.clearShadowLayer()
    }
}

/** Neumorphic "pressed in" trough. */
fun Modifier.novaInset(n: NovaColors, corner: Dp = 16.dp): Modifier {
    val shape = RoundedCornerShape(corner)
    return this
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    n.raisedDark.copy(alpha = if (n.isDark) 0.95f else 0.40f),
                    n.surface,
                    n.raisedLight.copy(alpha = if (n.isDark) 0.55f else 0.80f)
                )
            )
        )
        .border(
            1.dp,
            Brush.linearGradient(listOf(n.raisedDark.copy(alpha = 0.6f), n.raisedLight.copy(alpha = 0.7f))),
            shape
        )
}

/* ─────────────────────────── Backdrop ─────────────────────────── */

/** Ambient colour orbs give the glass something to refract. */
@Composable
fun NovaBackdrop(accent: Color, modifier: Modifier = Modifier) {
    val n = LocalNova.current
    Box(
        modifier
            .background(n.bg)
            .drawBehind {
                val a = if (n.isDark) 0.30f else 0.22f
                drawRect(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = a), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.04f),
                        radius = size.width * 0.95f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFFA78BFA).copy(alpha = a * 0.8f), Color.Transparent),
                        center = Offset(size.width * 0.0f, size.height * 0.42f),
                        radius = size.width * 0.9f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFFFB7185).copy(alpha = a * 0.6f), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.9f),
                        radius = size.width * 1.0f
                    )
                )
            }
    )
}

/* ───────────────────────────── Glass ───────────────────────────── */

@Composable
fun NovaGlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 26.dp,
    glow: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val n = LocalNova.current
    val shape = RoundedCornerShape(corner)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(n.glassTop, n.glassBottom)))
            .drawBehind {
                // specular sheen along the top edge
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = if (n.isDark) 0.07f else 0.35f), Color.Transparent),
                        endY = size.height * 0.45f
                    )
                )
                if (glow != null) {
                    drawRect(
                        Brush.radialGradient(
                            listOf(glow.copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(size.width * 0.08f, 0f),
                            radius = size.width * 0.8f
                        )
                    )
                }
            }
            .border(1.dp, Brush.verticalGradient(listOf(n.glassEdgeTop, n.glassEdgeBottom)), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        content = content
    )
}

@Composable
fun NovaSection(
    title: String,
    accent: Color,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val n = LocalNova.current
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 8.dp, bottom = 10.dp)
        ) {
            Box(
                Modifier
                    .size(width = 16.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.2f))))
            )
            Spacer(Modifier.width(8.dp))
            Text(title, color = n.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        NovaGlassCard(glow = accent, content = content)
        if (footer != null) {
            Text(
                footer,
                color = n.textDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp)
            )
        }
    }
}

@Composable
fun NovaRow(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    divider: Boolean = true,
    trailing: (@Composable () -> Unit)? = null
) {
    val n = LocalNova.current
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    Modifier
                        .size(42.dp)
                        .novaRaised(n, 13.dp, 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(14.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = n.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle, color = n.textDim, fontSize = 12.sp, lineHeight = 16.sp)
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(12.dp))
                trailing()
            }
        }
        if (divider) {
            Box(
                Modifier
                    .padding(start = if (icon != null) 72.dp else 16.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(n.hairline)
            )
        }
    }
}

/* ───────────────────────── Neumorphic controls ───────────────────────── */

@Composable
fun NovaButton(
    text: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val n = LocalNova.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    Row(
        modifier
            .then(if (pressed) Modifier.novaInset(n, 14.dp) else Modifier.novaRaised(n, 14.dp, 4.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
        Text(text, color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun NovaSegmented(
    options: List<String>,
    selected: String,
    accent: Color,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val n = LocalNova.current
    Row(
        modifier
            .novaInset(n, 18.dp)
            .horizontalScroll(rememberScrollState())
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { opt ->
            val sel = opt == selected
            val textColor by animateColorAsState(if (sel) accent else n.textDim, label = "seg")
            Box(
                Modifier
                    .then(if (sel) Modifier.novaRaised(n, 13.dp, 3.dp) else Modifier)
                    .clip(RoundedCornerShape(13.dp))
                    .noRippleClickable { onSelect(opt) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    opt,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/** Glossy gem swatch on a neumorphic plate. Pressed-in when selected. */
@Composable
fun NovaGem(
    label: String,
    c1: Color,
    c2: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val n = LocalNova.current
    val s by animateFloatAsState(
        if (selected) 1f else 0.9f,
        spring(Spring.DampingRatioMediumBouncy),
        label = "gem"
    )
    Column(
        modifier.noRippleClickable(onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(60.dp)
                .then(if (selected) Modifier.novaInset(n, 30.dp) else Modifier.novaRaised(n, 30.dp, 5.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .scale(s)
                    .drawBehind {
                        if (selected) {
                            drawCircle(
                                Brush.radialGradient(listOf(c1.copy(alpha = 0.5f), Color.Transparent)),
                                radius = size.width * 0.95f
                            )
                        }
                        drawCircle(
                            Brush.radialGradient(
                                listOf(lerp(c1, Color.White, 0.65f), c1, c2),
                                center = Offset(size.width * 0.35f, size.height * 0.3f),
                                radius = size.width * 0.9f
                            )
                        )
                    }
                    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.Transparent)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            color = if (selected) n.text else n.textDim,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/* ───────────────────────── Skeuomorphic hardware ───────────────────────── */

/** Metal-knob toggle in an inset track with an LED that lights up when on. */
@Composable
fun NovaSkeuoSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, accent: Color) {
    val n = LocalNova.current
    val knobX by animateDpAsState(if (checked) 28.dp else 2.dp, spring(Spring.DampingRatioLowBouncy), label = "knob")
    val glow by animateFloatAsState(if (checked) 1f else 0f, tween(300), label = "glow")
    Box(
        Modifier
            .size(width = 60.dp, height = 32.dp)
            .novaInset(n, 16.dp)
            .drawBehind {
                drawRoundRect(
                    accent.copy(alpha = 0.38f * glow),
                    cornerRadius = CornerRadius(size.height / 2)
                )
            }
            .noRippleClickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .offset(x = knobX)
                .size(28.dp)
                .shadow(5.dp, CircleShape)
                .background(Brush.verticalGradient(listOf(MetalHi, MetalMid, MetalLo)), CircleShape)
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), Color.Black.copy(alpha = 0.25f))), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .background(
                        Brush.verticalGradient(listOf(MetalLo, lerp(Color(0xFFCFCDDA), accent, glow))),
                        CircleShape
                    )
            )
        }
    }
}

/** Mixer-style fader. level is 0..1. */
@Composable
fun NovaFader(level: Float, accent: Color, label: String) {
    val n = LocalNova.current
    val g by animateFloatAsState(level, tween(450), label = "fader")
    val trackH = 96.dp
    val knobH = 18.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .width(28.dp)
                .height(trackH)
                .novaInset(n, 14.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // groove
            Box(
                Modifier
                    .align(Alignment.Center)
                    .padding(vertical = knobH / 2)
                    .fillMaxHeight()
                    .width(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(n.raisedDark.copy(alpha = 0.85f))
            )
            // lit portion
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = knobH / 2)
                    .width(4.dp)
                    .height((trackH - knobH) * g)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.35f))))
            )
            // cap
            Box(
                Modifier
                    .offset(y = (trackH - knobH) * (1f - g))
                    .width(22.dp)
                    .height(knobH)
                    .shadow(4.dp, RoundedCornerShape(6.dp))
                    .background(Brush.verticalGradient(listOf(MetalHi, MetalMid, MetalLo)), RoundedCornerShape(6.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.width(12.dp).height(2.dp).background(accent, CircleShape))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = n.textDim, fontSize = 11.sp)
    }
}

/** Segmented LED level meter in an inset window. */
@Composable
fun NovaLedMeter(ratio: Float, accent: Color, modifier: Modifier = Modifier, segments: Int = 24) {
    val n = LocalNova.current
    val animated by animateFloatAsState(ratio.coerceIn(0f, 1f), tween(600), label = "led")
    val litCount = (animated * segments).roundToInt()
    Row(
        modifier
            .fillMaxWidth()
            .novaInset(n, 12.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(segments) { i ->
            val t = i / (segments - 1f)
            val led = if (t < 0.6f) lerp(accent, LedAmber, t / 0.6f) else lerp(LedAmber, Color(0xFFFB7185), (t - 0.6f) / 0.4f)
            val lit = i < litCount
            Box(
                Modifier
                    .weight(1f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (lit) Brush.verticalGradient(listOf(led, led.copy(alpha = 0.6f)))
                        else Brush.verticalGradient(listOf(n.raisedDark.copy(alpha = 0.6f), n.raisedDark.copy(alpha = 0.3f)))
                    )
            )
        }
    }
}
