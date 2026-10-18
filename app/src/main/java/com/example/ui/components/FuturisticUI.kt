package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.auth.UserProfile
import com.example.ui.theme.*

// Studio Console hardware. Surfaces come from the theme so plates stay ink-plum.
val NeumorphicBase = DarkBackgroundStatic
val NeumorphicButtonSurface = DarkSurfaceElevatedStatic
val NeumorphicLightShadow = Color(0x22FFFFFF)
val NeumorphicDarkShadow = Color(0x99000000)
val SpecularEdgeColor = GlassHighlightBorder
val IOSCardBackground = DarkSurfaceStatic.copy(alpha = 0.8f)
val IOSDividerColor = Color(0x1AFFFFFF)
private val MetalHi = Color(0xFFF4F4F8)
private val MetalMid = Color(0xFFB4B2C2)
private val MetalLo = Color(0xFF6B6980)

/**
 * Tactile Neumorphic & Glassmorphic Button with realistic physical press depression,
 * outer specular highlight, and restrained neon glow.
 */
@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    glowColor: Color = NeonCyan,
    isAccent: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val nova = LocalNova.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "button_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .then(
                when {
                    isAccent -> Modifier
                    isPressed -> Modifier.novaInset(nova, cornerRadius)
                    else -> Modifier.novaRaised(nova, cornerRadius, 4.dp)
                }
            )
            .clip(RoundedCornerShape(cornerRadius))
            .then(
                if (isAccent) Modifier.background(Brush.linearGradient(listOf(NeonCyan, glowColor)))
                else Modifier
            )
            .border(
                width = if (isAccent) 1.dp else 0.dp,
                color = if (isAccent) Color.White.copy(alpha = 0.45f) else Color.Transparent,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

/**
 * Circular Neumorphic Tactile Button with physical-button appearance,
 * dual-ring bevel, and restrained cyan glow (ideal for Play/Pause, Skip Next/Prev).
 */
@Composable
fun NeumorphicIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    iconSize: Dp = 26.dp,
    tint: Color = OnDarkText,
    glowColor: Color = NeonCyan,
    isPrimary: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "icon_btn_scale"
    )

    val nova = LocalNova.current
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .then(
                if (isPressed) Modifier.novaInset(nova, size / 2)
                else Modifier.novaRaised(nova, size / 2, if (isPrimary) 5.dp else 4.dp)
            )
            .clip(CircleShape)
            .then(
                if (isPrimary) Modifier.background(Brush.verticalGradient(listOf(MetalHi, MetalMid, MetalLo)))
                else Modifier
            )
            .border(
                width = if (isPrimary) 1.dp else 0.dp,
                color = Color.White.copy(alpha = if (isPrimary) 0.8f else 0f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isPrimary) glowColor else tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Tactile Seek Bar styled with a cyan-to-magenta gradient and a subtle metallic thumb.
 * Provides smooth dragging, tap-to-seek, and high precision scrub control.
 */
@Composable
fun TactileGradientSeekBar(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var barWidthPx by remember { mutableFloatStateOf(1f) }

    val safeDuration = duration.coerceAtLeast(1L)
    val actualFraction = (position.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val displayFraction = if (isDragging) dragFraction else actualFraction

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.18f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "thumb_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .onSizeChanged { barWidthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(safeDuration) {
                detectTapGestures { offset ->
                    val frac = (offset.x / barWidthPx).coerceIn(0f, 1f)
                    onSeek((frac * safeDuration).toLong())
                }
            }
            .pointerInput(safeDuration) {
                detectDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragFraction = (offset.x / barWidthPx).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeek((dragFraction * safeDuration).toLong())
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // 1. Inactive Track (Debossed groove with subtle inner shadow)
        val nova = LocalNova.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .novaInset(nova, 3.dp)
        )

        // 2. Active Track (Vibrant Cyan-to-Magenta Gradient with subtle restrained glow)
        Box(
            modifier = Modifier
                .fillMaxWidth(displayFraction)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            NeonCyan,
                            NeonMagenta
                        )
                    )
                )
        )

        // 3. Subtle Metallic Thumb with brushed texture, chrome rim, and cyan micro-indicator
        val thumbOffsetPx = (displayFraction * barWidthPx) - 10.dp.value // center thumb
        Box(
            modifier = Modifier
                .offset(x = thumbOffsetPx.coerceAtLeast(0f).dp)
                .size(20.dp)
                .scale(thumbScale)
                .drawBehind {
                    // Soft physical drop shadow
                    drawIntoCanvas { canvas ->
                        val paint = Paint().asFrameworkPaint().apply {
                            color = Color(0x99000000).toArgb()
                            setShadowLayer(
                                4.dp.toPx(),
                                0f,
                                2.dp.toPx(),
                                Color(0x80000000).toArgb()
                            )
                        }
                        val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
                        val thumbRadius = this.size.width / 2f
                        canvas.drawCircle(
                            centerOffset,
                            thumbRadius,
                            Paint().apply { asFrameworkPaint().set(paint) }
                        )
                    }
                }
                .clip(CircleShape)
                // Brushed brass hardware
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFE7B8),
                            BrassAmber,
                            Color(0xFFD4923A),
                            Color(0xFF8C5A22)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(40f, 40f)
                    )
                )
                // Chrome rim highlight
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0x44FFFFFF),
                            Color(0x22000000)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Subtle Cyan center micro-pip
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(NeonCyan)
                    .border(0.5.dp, Color.White, CircleShape)
            )
        }
    }
}

/**
 * Prominent Album Artwork Card with elevated dark surface, soft neumorphic depth,
 * subtle top glass specular edge, and restrained ambient backlight bloom.
 */
@Composable
fun ProminentAlbumArtCard(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    size: Dp = 280.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                // Restrained, rich ambient backlight bloom
                drawIntoCanvas { canvas ->
                    val paint = Paint().asFrameworkPaint().apply {
                        color = NeonCyan.copy(alpha = 0.16f).toArgb()
                        setShadowLayer(
                            28.dp.toPx(),
                            0f,
                            8.dp.toPx(),
                            NeonMagenta.copy(alpha = 0.14f).toArgb()
                        )
                    }
                    canvas.drawRoundRect(
                        0f, 0f, this.size.width, this.size.height,
                        cornerRadius.toPx(), cornerRadius.toPx(),
                        Paint().apply { asFrameworkPaint().set(paint) }
                    )
                }
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(NeumorphicSurfaceElevated) // #1C1C28
            .border(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x44FFFFFF),           // Subtle top specular highlight
                        Color(0x18FFFFFF),           // Glass edge
                        NeonCyan.copy(alpha = 0.20f) // Subtle cyan rim
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!artworkUrl.isNullOrEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artworkUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Album Artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                DarkSurfaceStatic,
                                DarkBackgroundStatic
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(56.dp)
                )
            }
        }
    }
}

/**
 * iOS-style Segmented Control with smooth tactile sliding/floating pill indicator.
 */
@Composable
fun IOSSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNova.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .novaInset(nova, 22.dp)
            .padding(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = selectedIndex == index

                val selectedBgModifier = if (isSelected) Modifier.novaRaised(nova, 19.dp, 3.dp) else Modifier

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(19.dp))
                        .then(selectedBgModifier)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            brush = if (isSelected) {
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x55FFFFFF),
                                        NeonCyan.copy(alpha = 0.25f)
                                    )
                                )
                            } else {
                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            },
                            shape = RoundedCornerShape(19.dp)
                        )
                        .clickable { onItemSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) NeonCyan else nova.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * iOS-style Inset Grouped Section Card.
 */
@Composable
fun IOSGroupedSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            NovaSection(title = title, accent = NeonCyan, footer = footer, content = content)
        } else {
            NovaGlassCard(content = content)
            if (footer != null) {
                Text(
                    text = footer,
                    fontSize = 12.sp,
                    color = LocalNova.current.textDim,
                    modifier = Modifier.padding(start = 10.dp, top = 8.dp, end = 10.dp)
                )
            }
        }
    }
}

/**
 * Single Row inside an iOS Inset Grouped Section.
 */
@Composable
fun IOSGroupedRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = NeonCyan,
    iconBackground: Color = Color(0x2200E5FF),
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true
) {
    NovaRow(
        title = title,
        accent = iconTint,
        subtitle = subtitle,
        icon = icon,
        onClick = onClick,
        divider = showDivider,
        trailing = trailingContent ?: if (onClick != null) {
            {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = LocalNova.current.textDim,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            null
        }
    )
}

/**
 * Skeumorphic Holographic Spinning Vinyl Turntable Disc.
 * Features realistic continuous rotation when playing, etched vinyl groove rings,
 * holographic laser reflections, and prominent center album artwork sticker!
 */
@Composable
fun HolographicVinylDisc(
    isPlaying: Boolean,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    discSize: Dp = 260.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier
            .size(discSize)
            .rotate(currentRotation),
        contentAlignment = Alignment.Center
    ) {
        // Outer vinyl body & grooves drawn on canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.width / 2f

            // Outer Vinyl Dark Body (#101016)
            drawCircle(
                color = ConsoleWell,
                radius = outerRadius,
                center = center
            )

            // Outer rim bevel
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        DarkSurfaceElevatedStatic,
                        ConsoleWell,
                        ConsoleHighlight,
                        ConsoleWell
                    )
                ),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // Skeumorphic etched micro-grooves
            val grooveCount = 8
            for (i in 1..grooveCount) {
                val grooveRadius = outerRadius * (0.42f + (i.toFloat() * 0.065f))
                drawCircle(
                    color = Color(0x18FFFFFF),
                    radius = grooveRadius,
                    center = center,
                    style = Stroke(width = 0.75.dp.toPx())
                )
            }

            // Restrained holographic sheen arcs across the grooves
            drawArc(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonCyan.copy(alpha = 0.20f),
                        Color.Transparent
                    )
                ),
                startAngle = 45f,
                sweepAngle = 45f,
                useCenter = true
            )
            drawArc(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonMagenta.copy(alpha = 0.16f),
                        Color.Transparent
                    )
                ),
                startAngle = 225f,
                sweepAngle = 45f,
                useCenter = true
            )
        }

        // Center Album Artwork Label (The "Sticker" - prominent high-res art)
        Box(
            modifier = Modifier
                .size(discSize * 0.40f)
                .clip(CircleShape)
                .border(2.dp, Color(0x55FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!artworkUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(artworkUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Vinyl Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(NeonCyan, NeonMagenta))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Center spindle hole
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(ConsoleWell)
                    .border(1.5.dp, Color(0x99FFFFFF), CircleShape)
            )
        }
    }
}

/**
 * Visualizer with restrained Cyan & Magenta bars.
 */
@Composable
fun PlaybackVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 18
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val waveA by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveA"
    )
    val waveB by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveB"
    )

    val nova = LocalNova.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .novaInset(nova, 16.dp)
            .height(36.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val factor = if ((i % 2) == 0) waveA else waveB
            val barHeight = if (isPlaying) (10.dp + (16.dp * factor)) else 4.dp
            val barColor = if (i < barCount / 2) NeonCyan else NeonMagenta

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor.copy(alpha = 0.85f))
            )
        }
    }
}

/**
 * Skeumorphic Tactile Equalizer Fader Slider.
 */
@Composable
fun TactileFader(
    value: Float, // -10f to +10f
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = -10f..10f,
    accentColor: Color = NeonCyan
) {
    Column(
        modifier = modifier.width(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${if (value > 0) "+" else ""}${value.toInt()}dB",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (value != 0f) accentColor else OnDarkTextSecondary
        )

        Spacer(modifier = Modifier.height(6.dp))

        val nova = LocalNova.current
        val span = (range.endInclusive - range.start).coerceAtLeast(0.01f)
        val fraction = ((value - range.start) / span).coerceIn(0f, 1f)
        val trackHeight = 150.dp
        val capHeight = 18.dp
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(trackHeight)
                .pointerInput(range) {
                    detectTapGestures { offset ->
                        val next = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                        onValueChange(range.start + next * span)
                    }
                }
                .pointerInput(range) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val next = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                        onValueChange(range.start + next * span)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(14.dp)
                    .fillMaxHeight()
                    .novaInset(nova, 7.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .width(14.dp)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Brush.verticalGradient(listOf(accentColor, accentColor.copy(alpha = 0.35f))))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (trackHeight - capHeight) * (1f - fraction))
                    .size(width = 28.dp, height = capHeight)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFFF4F4F8), Color(0xFFB4B2C2), Color(0xFF6B6980))),
                        RoundedCornerShape(6.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 10.sp,
            color = OnDarkTextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * iOS & Futuristic Navigation Header with Title, Subtitle, and User Avatar Pill.
 */
@Composable
fun IOSFuturisticHeader(
    title: String,
    subtitle: String,
    userProfile: UserProfile?,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val gradientColors = listOf(NeonCyan, CyberPurple, NeonMagenta)
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.horizontalGradient(colors = gradientColors)
                ),
                letterSpacing = (-0.6).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = OnDarkTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // User Avatar Button with subtle glowing ring
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ConsoleHighlight,
                            NeumorphicSurfaceElevated
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.6f),
                            NeonMagenta.copy(alpha = 0.4f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { onAvatarClick() }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0x33000000)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!userProfile?.photoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(userProfile?.photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "User Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = userProfile?.displayName?.take(10) ?: "Pilot",
                            color = OnDarkText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (userProfile?.isPremium == true) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonMagenta.copy(alpha = 0.25f))
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            ) {
                                Text("VIP", fontSize = 8.sp, color = NeonMagenta, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                    Text(
                        text = if (userProfile?.provider == "google.com") "Google ID" else "Nova Deck",
                        color = OnDarkTextSecondary,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
