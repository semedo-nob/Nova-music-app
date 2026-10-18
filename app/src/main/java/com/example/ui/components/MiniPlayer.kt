package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MiniPlayer(
    viewModel: MusicViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val playbackError by viewModel.playbackError.collectAsState()

    if (currentTrack == null) return

    val infiniteTransition = rememberInfiniteTransition(label = "mini_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val playInteraction = remember { MutableInteractionSource() }
    val isPlayPressed by playInteraction.collectIsPressedAsState()
    val playScale by animateFloatAsState(
        targetValue = if (isPlayPressed) 0.90f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "play_scale"
    )

    val nova = LocalNova.current
    val shape = RoundedCornerShape(26.dp)
    var swipeX by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .offset { androidx.compose.ui.unit.IntOffset(swipeX.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (abs(swipeX) > 160f) viewModel.dismissPlayback()
                        swipeX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        swipeX += dragAmount
                    }
                )
            }
            .novaRaised(nova, 26.dp, 5.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(nova.glassTop, nova.glassBottom)))
            .border(1.dp, Brush.verticalGradient(listOf(nova.glassEdgeTop, nova.glassEdgeBottom)), shape)
            .clickable(onClick = onExpand)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prominent Album Artwork with subtle rotation and squircle frame
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .novaRaised(nova, 14.dp, 3.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(nova.surface),
                    contentAlignment = Alignment.Center
                ) {
                    val artwork = currentTrack?.artworkUrl
                    if (!artwork.isNullOrEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(artwork)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Mini album art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(if (isPlaying) rotation else 0f)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Song & Artist details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentTrack?.title ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = nova.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = playbackError ?: "${currentTrack?.artist ?: ""} · Lossless Audio",
                        fontSize = 11.sp,
                        color = if (playbackError != null) NeonMagenta else nova.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Tactile Circular Play / Pause Button with Restrained Cyan Glow
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .scale(playScale)
                        .then(if (isPlayPressed) Modifier.novaInset(nova, 19.dp) else Modifier.novaRaised(nova, 19.dp, 4.dp))
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = playInteraction,
                            indication = null
                        ) { viewModel.playPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Next Track Button
                IconButton(
                    onClick = { viewModel.next() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip next",
                        tint = nova.text,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // High-precision Hairline Progress Indicator (Cyan-to-Magenta Gradient)
            val progressFactor = if (duration > 0) position.toFloat() / duration.toFloat() else 0f
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(nova.raisedDark)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFactor.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(NeonCyan, NeonMagenta)
                            )
                        )
                )
            }
        }
    }
}
