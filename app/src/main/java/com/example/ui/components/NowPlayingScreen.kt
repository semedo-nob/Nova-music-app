package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Track
import com.example.ui.components.HolographicVinylDisc
import com.example.ui.components.IOSSegmentedControl
import com.example.ui.components.NeumorphicButton
import com.example.ui.components.NeumorphicIconButton
import com.example.ui.components.PlaybackVisualizer
import com.example.ui.components.TactileFader
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.launch
import androidx.media3.common.Player

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    viewModel: MusicViewModel,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val position by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()

    val context = LocalContext.current
    val nova = LocalNova.current
    val coroutineScope = rememberCoroutineScope()

    // Sub-UI States
    var showEqSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0 = Player, 1 = Lyrics

    // Artwork Rotation Animation
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ),
        label = "art_rotate"
    )
    val animatedAngle = if (isPlaying && activeTab == 0) rotationAngle else 0f

    // Format millisecond durations to string (m:ss)
    fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format("%d:%02d", mins, secs)
    }

    // Default lyrics map
    val defaultLyrics = listOf(
        LyricLine(0L, "✨ Galactic Synth Intro - Tuning Frequencies ✨"),
        LyricLine(8000L, "Floating through the neon sky..."),
        LyricLine(16000L, "Watching cosmic clouds sail on by..."),
        LyricLine(24000L, "In the virtual deep, we find our sound..."),
        LyricLine(32000L, "Holograms dancing all around..."),
        LyricLine(42000L, "🌠 Quantum beat, pulse through my soul! 🌠"),
        LyricLine(50000L, "Gemini is calling, lose control..."),
        LyricLine(60000L, "Riding on waves of liquid cyan light..."),
        LyricLine(70000L, "Nova is burning through the cosmic night..."),
        LyricLine(82000L, "🎸 Synthesizer Interlude - Overdrive Active 🎸"),
        LyricLine(98000L, "Every orbit carries us closer now..."),
        LyricLine(110000L, "Floating above the rings of Saturn somehow..."),
        LyricLine(125000L, "All our coordinates perfectly aligned..."),
        LyricLine(140000L, "No digital static left behind..."),
        LyricLine(160000L, "✨ Fading into the celestial silence... ✨"),
        LyricLine(175000L, "Credits: Nova AI DJ Music Labs")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(nova.bg)
    ) {
        // 1. Blurred background image of album cover for ultimate dynamic atmosphere
        currentTrack?.artworkUrl?.let { url ->
            coil.compose.AsyncImage(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        } ?: Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkSurfaceVariant,
                            DarkBackground
                        )
                    )
                )
        )

        // Floating ambient glowing background overlay
        GeminiAmbientGlow(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMinimize) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize player",
                        tint = OnDarkText,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Sub tabs: Vinyl vs Cover vs Queue vs Lyrics
                IOSSegmentedControl(
                    items = listOf("Vinyl", "Cover", "Queue", "Lyrics"),
                    selectedIndex = activeTab,
                    onItemSelected = { activeTab = it },
                    modifier = Modifier.width(280.dp)
                )

                IconButton(onClick = { showSleepTimerDialog = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Alarm,
                        contentDescription = "Sleep timer",
                        tint = OnDarkText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Viewport (Vinyl, Cover, Queue, or Lyrics)
            AnimatedContent(
                targetState = activeTab,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "tab_animation"
            ) { tab ->
                when (tab) {
                    0 -> {
                        // Skeuomorphic Holographic Spinning Vinyl Turntable
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            HolographicVinylDisc(
                                isPlaying = isPlaying,
                                artworkUrl = currentTrack?.artworkUrl,
                                discSize = 275.dp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            PlaybackVisualizer(isPlaying = isPlaying)
                        }
                    }
                    1 -> {
                        // Prominent Squircle Album Cover Card with ambient backlight & glass edge
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ProminentAlbumArtCard(
                                artworkUrl = currentTrack?.artworkUrl,
                                size = 280.dp,
                                cornerRadius = 32.dp
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            PlaybackVisualizer(isPlaying = isPlaying)
                        }
                    }
                    2 -> {
                        // Interactive Queue Management View
                        val currentQueue by viewModel.queue.collectAsState()
                        val currentIdx by viewModel.currentIndex.collectAsState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x221C1C28))
                                .border(1.dp, GlassBorderColor, RoundedCornerShape(20.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Playback Queue (${currentQueue.size})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonCyan
                                )
                                if (currentQueue.isNotEmpty()) {
                                    TextButton(onClick = { viewModel.clearQueue() }) {
                                        Text("Clear", color = OnDarkTextSecondary, fontSize = 12.sp)
                                    }
                                }
                            }

                            if (currentQueue.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Queue is empty", color = OnDarkTextSecondary, fontSize = 13.sp)
                                }
                            } else {
                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    itemsIndexed(currentQueue) { index, track ->
                                        val isCurrent = index == currentIdx
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isCurrent) Color(0x3300E5FF) else Color(0x14FFFFFF)
                                                )
                                                .border(
                                                    if (isCurrent) 1.dp else 0.5.dp,
                                                    if (isCurrent) NeonCyan else GlassBorderColor,
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    viewModel.playTrack(track, currentQueue)
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 11.sp,
                                                color = if (isCurrent) NeonCyan else OnDarkTextSecondary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(22.dp)
                                            )

                                            coil.compose.AsyncImage(
                                                model = track.artworkUrl,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = track.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isCurrent) NeonCyan else OnDarkText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = track.artist,
                                                    fontSize = 11.sp,
                                                    color = OnDarkTextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            if (isCurrent) {
                                                Text(
                                                    text = "Playing",
                                                    fontSize = 10.sp,
                                                    color = NeonCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(end = 6.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { viewModel.removeFromQueue(index) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove",
                                                    tint = OnDarkTextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        // Synced Holographic Lyrics
                        val trackLyrics = remember(currentTrack) {
                            viewModel.getLyricsForCurrentTrack()
                        }
                        val formattedLyrics = trackLyrics.mapIndexed { idx, line ->
                            val cleanText = line.replace(Regex("^\\[\\d+:\\d+\\.\\d+\\]\\s*"), "")
                            LyricLine(idx * 7000L, cleanText)
                        }
                        LyricsView(
                            lyrics = formattedLyrics,
                            currentPosition = position,
                            onSeek = { viewModel.seekTo(it) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Song Metadata (Title + Artist + Hi-Res Badge)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Text(
                    text = currentTrack?.title ?: "Cosmic Horizon",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnDarkText,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentTrack?.artist ?: "Nova Aurora",
                        fontSize = 15.sp,
                        color = OnDarkTextSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0x2E00E5FF))
                            .border(0.5.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text("24-BIT FLAC", fontSize = 8.sp, color = NeonCyan, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tactile Seek Bar with Cyan-to-Magenta Gradient and Subtle Metallic Thumb
            Column(modifier = Modifier.fillMaxWidth()) {
                TactileGradientSeekBar(
                    position = position,
                    duration = duration,
                    onSeek = { viewModel.seekTo(it) }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(position),
                        color = OnDarkTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    val remainingMs = (duration - position).coerceAtLeast(0L)
                    Text(
                        text = "-${formatTime(remainingMs)}",
                        color = OnDarkTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                NovaLedMeter(
                    ratio = if (duration > 0L) position.toFloat() / duration.toFloat() else 0f,
                    accent = NeonCyan
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Playing Controls Row (Next, Prev, Play/Pause, Shuffle, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = if (shuffleEnabled) Icons.Filled.Shuffle else Icons.Outlined.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffleEnabled) GeminiBlue else OnDarkTextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                NeumorphicIconButton(
                    onClick = { viewModel.previous() },
                    icon = Icons.Default.SkipPrevious,
                    contentDescription = "Previous track",
                    size = 52.dp,
                    iconSize = 28.dp,
                    tint = OnDarkText
                )

                NeumorphicIconButton(
                    onClick = { viewModel.playPause() },
                    icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    size = 72.dp,
                    iconSize = 36.dp,
                    isPrimary = true,
                    glowColor = GeminiBlue
                )

                NeumorphicIconButton(
                    onClick = { viewModel.next() },
                    icon = Icons.Default.SkipNext,
                    contentDescription = "Next track",
                    size = 52.dp,
                    iconSize = 28.dp,
                    tint = OnDarkText
                )

                IconButton(onClick = { viewModel.toggleRepeatMode() }) {
                    val icon = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Filled.RepeatOne
                        Player.REPEAT_MODE_ALL -> Icons.Filled.Repeat
                        else -> Icons.Outlined.Repeat
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != Player.REPEAT_MODE_OFF) GeminiBlue else OnDarkTextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sub control bar (Equalizer, Queue list, speed control, favorite, share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showEqSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Audio Equalizer",
                        tint = GeminiBlue
                    )
                }

                // Playback speed toggle
                Row(
                    modifier = Modifier
                        .novaRaised(nova, 12.dp, 3.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val nextSpeed = when (speed) {
                                1.0f -> 1.25f
                                1.25f -> 1.5f
                                1.5f -> 0.75f
                                else -> 1.0f
                            }
                            viewModel.setPlaybackSpeed(nextSpeed)
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Speed",
                        tint = OnDarkText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${speed}x", color = OnDarkText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Favorite toggle
                IconButton(onClick = {
                    currentTrack?.let {
                        viewModel.toggleFavorite(it.id)
                        val text = if (it.isFavorite) "Removed from Liked Songs 💔" else "Added to Liked Songs ❤️"
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    }
                }) {
                    val isFav = currentTrack?.isFavorite == true
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFav) NeonPink else OnDarkText
                    )
                }

                // Share song
                IconButton(onClick = {
                    currentTrack?.let {
                        Toast.makeText(context, "Sharing track: ${it.title} via Nova Mesh! 🚀", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share track",
                        tint = OnDarkText
                    )
                }
            }
        }
    }

    // --- Equalizer Bottom Sheet / Dialog ---
    if (showEqSheet) {
        Dialog(onDismissRequest = { showEqSheet = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                glowColor = GeminiBlue
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Nova EQ Tuner",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnDarkText
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Preset chips
                    val presets = listOf("Flat", "Rock", "Pop", "Jazz", "Custom")
                    val currentPreset by viewModel.eqPreset.collectAsState()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        presets.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .then(
                                        if (currentPreset == preset) Modifier.novaInset(nova, 12.dp)
                                        else Modifier.novaRaised(nova, 12.dp, 3.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.selectEqPreset(preset) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    preset,
                                    fontSize = 11.sp,
                                    color = if (currentPreset == preset) NeonCyan else nova.textDim,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 5-Band Skeumorphic Tactile Faders
                    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
                    val eqBands by viewModel.eqBands.collectAsState()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bandLabels.forEachIndexed { idx, label ->
                            val currentMb = eqBands.getOrNull(idx) ?: 0
                            val currentDb = (currentMb / 100f).coerceIn(-10f, 10f)
                            TactileFader(
                                value = currentDb,
                                onValueChange = { db ->
                                    viewModel.updateEqBand(idx.toShort(), (db * 100).toInt().toShort())
                                },
                                label = label,
                                accentColor = if (idx % 2 == 0) GeminiBlue else CyberPurple
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Bass & Virtualizer Sliders
                    val bass by viewModel.bassLevel.collectAsState()
                    val virt by viewModel.virtualizerLevel.collectAsState()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NovaFader(level = bass / 1000f, accent = NeonCyan, label = "Bass")
                        NovaFader(level = virt / 1000f, accent = CyberPurple, label = "Space")
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Sub Bass: ${bass / 10}%", fontSize = 12.sp, color = OnDarkTextSecondary)
                        Slider(
                            value = bass.toFloat(),
                            onValueChange = { viewModel.setBassBoost(it.toInt()) },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(activeTrackColor = GeminiBlue, thumbColor = SoftCyan)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("3D Holographic Spatializer: ${virt / 10}%", fontSize = 12.sp, color = OnDarkTextSecondary)
                        Slider(
                            value = virt.toFloat(),
                            onValueChange = { viewModel.setVirtualizer(it.toInt()) },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(activeTrackColor = CyberPurple, thumbColor = NeonPink)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    NeumorphicButton(
                        onClick = { showEqSheet = false },
                        isAccent = true,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Equalizer Settings", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // --- Sleep Timer Dialog ---
    if (showSleepTimerDialog) {
        val remaining by viewModel.sleepTimeRemaining.collectAsState()

        Dialog(onDismissRequest = { showSleepTimerDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                glowColor = NeonPink
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Nebula Sleep Timer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnDarkText
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (remaining != null) {
                        Text(
                            "Time Remaining: ${formatTime(remaining!!)}",
                            fontSize = 16.sp,
                            color = GeminiBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.cancelSleepTimer()
                                showSleepTimerDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                        ) {
                            Text("Abort Sleep Sequence", color = Color.White)
                        }
                    } else {
                        Text(
                            "Set sleep coordinates to power down the audio engines:",
                            color = OnDarkTextSecondary,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val minuteOptions = listOf(5, 15, 30, 45, 60)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            minuteOptions.forEach { mins ->
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0x1F000000))
                                        .clickable {
                                            viewModel.startSleepTimer(mins)
                                            showSleepTimerDialog = false
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text("${mins}m", color = GeminiBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class LyricLine(
    val timestampMs: Long,
    val lineText: String
)

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    currentPosition: Long,
    onSeek: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Find the currently active line
    val activeIndex = lyrics.indexOfLast { currentPosition >= it.timestampMs }.coerceAtLeast(0)

    // Auto scroll lyrics as they match playback position
    LaunchedEffect(activeIndex) {
        if (lyrics.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(activeIndex)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = 120.dp), // allows centralizing active line
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(lyrics) { index, line ->
                val isActive = index == activeIndex
                val alphaAnim by animateFloatAsState(
                    targetValue = if (isActive) 1.0f else 0.4f,
                    animationSpec = tween(300)
                )
                val scaleAnim by animateFloatAsState(
                    targetValue = if (isActive) 1.15f else 0.95f,
                    animationSpec = tween(300)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSeek(line.timestampMs) }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = line.lineText,
                        fontSize = (18f * scaleAnim).sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = (if (isActive) SoftCyan else OnDarkText).copy(alpha = alphaAnim),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
