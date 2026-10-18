package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.ui.components.AuthModalDialog
import com.example.ui.components.LocalNova
import com.example.ui.components.NovaButton
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.novaInset
import com.example.ui.components.novaRaised
import com.example.ui.theme.BrassAmber
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.viewmodel.MusicViewModel
import java.util.Calendar

private val MetalHi = Color(0xFFF4F4F8)
private val MetalMid = Color(0xFFB4B2C2)
private val MetalLo = Color(0xFF6B6980)
private val MetalIcon = Color(0xFF3A3750)
private val RecentPlaylistEnd = Color(0xFF2A8FBD)

@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val allTracks by viewModel.allTracks.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scannedCount by viewModel.scannedCount.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isListening by viewModel.isShazamListening.collectAsState()
    val identified by viewModel.shazamResult.collectAsState()
    val captureError by viewModel.shazamError.collectAsState()

    val context = LocalContext.current
    val nova = LocalNova.current
    var showAuthDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All") }
    val searching = searchQuery.trim().isNotEmpty()

    val (hello, role) = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 0..11 -> "Good morning" to "Explorer"
            in 12..16 -> "Good afternoon" to "Pilot"
            in 17..21 -> "Good evening" to "Dreamer"
            else -> "Late night" to "Cosmic chill"
        }
    }

    val mediaPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val storageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.scanLocalTracks()
            Toast.makeText(context, "Scanning local storage…", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Storage permission denied. Showing preset music only.", Toast.LENGTH_LONG).show()
        }
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startRecognition() else viewModel.onRecognitionPermissionDenied()
    }

    LaunchedEffect(scannedCount) {
        if (scannedCount != null) {
            Toast.makeText(context, "Scan complete: $scannedCount new tracks", Toast.LENGTH_LONG).show()
        }
    }

    val genres = remember(allTracks) {
        val preset = listOf("All", "Synthwave", "Lofi", "Electronic", "Techno", "Ambient")
        val extra = allTracks.map { it.genre }
            .filter { name -> name.isNotBlank() && !name.equals("Unknown", true) && preset.none { it.equals(name, true) } }
            .distinct()
        preset + extra
    }
    val genreTracks = remember(allTracks, selectedGenre) {
        if (selectedGenre == "All") allTracks
        else allTracks.filter { it.genre.equals(selectedGenre, ignoreCase = true) }
    }
    val visibleTracks = remember(genreTracks, favoriteTracks, searchQuery, searching) {
        if (searching) {
            genreTracks.filter {
                it.title.contains(searchQuery, true) ||
                    it.artist.contains(searchQuery, true) ||
                    it.album.contains(searchQuery, true)
            }
        } else {
            genreTracks.filter { track -> favoriteTracks.any { it.id == track.id } }
        }
    }
    val specials = remember(allTracks) { allTracks.filter { !it.isLocal } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .padding(bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hello,
                        color = nova.text,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.8).sp
                    )
                    Text(
                        text = "$role, welcome to Nova Deck",
                        color = nova.textDim,
                        fontSize = 14.sp
                    )
                }
                AvatarPill(
                    letter = currentUser?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "N",
                    photoUrl = currentUser?.photoUrl,
                    onClick = { showAuthDialog = true }
                )
            }
        }

        item {
            SearchGlass(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClear = { searchQuery = "" }
            )
            Spacer(Modifier.height(14.dp))
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
            ) {
                items(genres) { genre ->
                    GenreChip(
                        label = genre,
                        selected = genre.equals(selectedGenre, true),
                        onClick = { selectedGenre = genre }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (!searching) {
            item {
                SectionTitle("Nova Sound Capture")
                CaptureCard(
                    listening = isListening,
                    identified = identified,
                    error = captureError,
                    onMic = {
                        if (isListening) {
                            viewModel.cancelRecognition()
                        } else if (
                            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.startRecognition()
                        } else {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onPlay = { track ->
                        viewModel.playTrack(track, listOf(track))
                        viewModel.clearShazam()
                        Toast.makeText(context, "Playing ${track.title}", Toast.LENGTH_SHORT).show()
                    }
                )
                Spacer(Modifier.height(26.dp))
            }

            item {
                SectionTitle("Nova Specials")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    items(specials, key = { it.id }) { track ->
                        SpecialTrackCard(track = track, onClick = { viewModel.playTrack(track, specials) })
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            item {
                SectionTitle("Smart playlists")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmartPlaylistCard(
                        title = "Liked songs",
                        subtitle = favoriteTracks.size.let { count ->
                            if (count == 1) "1 track" else "$count tracks"
                        },
                        icon = Icons.Default.Favorite,
                        gradient = Brush.linearGradient(listOf(NeonMagenta, CyberPurple)),
                        onClick = {
                            if (favoriteTracks.isNotEmpty()) {
                                viewModel.playTrack(favoriteTracks.first(), favoriteTracks)
                                Toast.makeText(context, "Playing your liked songs", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No liked songs yet", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SmartPlaylistCard(
                        title = "Recently played",
                        subtitle = "Pick up again",
                        icon = Icons.Default.History,
                        gradient = Brush.linearGradient(listOf(NeonCyan, RecentPlaylistEnd)),
                        onClick = {
                            if (recentlyPlayed.isNotEmpty()) {
                                viewModel.playTrack(recentlyPlayed.first(), recentlyPlayed)
                                Toast.makeText(context, "Playing recently played", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Nothing played yet", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(26.dp))
            }

            item {
                ScannerCard(
                    scanning = isScanning,
                    onScan = { storageLauncher.launch(mediaPermission) }
                )
                Spacer(Modifier.height(28.dp))
            }
        }

        item {
            SectionTitle(
                if (searching) "Search results (${visibleTracks.size})" else "Starred beats"
            )
        }
        if (visibleTracks.isEmpty()) {
            item {
                Text(
                    text = if (searching) "No tracks match your search." else "Tap the heart on a track to star it.",
                    color = nova.textDim,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            items(visibleTracks, key = { it.id }) { track ->
                TrackListItem(
                    track = track,
                    onPlay = { viewModel.playTrack(track, visibleTracks) },
                    onFavorite = { viewModel.toggleFavorite(track.id) }
                )
            }
        }
    }

    if (showAuthDialog) {
        AuthModalDialog(viewModel = viewModel, onDismiss = { showAuthDialog = false })
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = LocalNova.current.text,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 12.dp)
    )
}

@Composable
private fun AvatarPill(letter: String, photoUrl: String?, onClick: () -> Unit) {
    val nova = LocalNova.current
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Brush.sweepGradient(listOf(NeonCyan, CyberPurple, NeonMagenta, NeonCyan)))
            .clickable(onClick = onClick)
            .padding(3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(nova.surface),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(photoUrl),
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(letter, color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SearchGlass(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    val nova = LocalNova.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(Brush.verticalGradient(listOf(nova.glassTop, nova.glassBottom)))
            .border(1.dp, Brush.verticalGradient(listOf(nova.glassEdgeTop, nova.glassEdgeBottom)), RoundedCornerShape(25.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = nova.text, fontSize = 15.sp),
            cursorBrush = SolidColor(NeonCyan),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text("Search songs, artists, albums", color = nova.textDim, fontSize = 15.sp)
                    }
                    inner()
                }
            }
        )
        if (query.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Clear",
                tint = nova.textDim,
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onClear)
            )
        }
    }
}

@Composable
private fun GenreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val nova = LocalNova.current
    val shape = RoundedCornerShape(15.dp)
    Text(
        text = label,
        color = if (selected) NeonCyan else nova.textDim,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .then(if (selected) Modifier.novaInset(nova, 15.dp) else Modifier.novaRaised(nova, 15.dp, 4.dp))
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 8.dp)
    )
}

@Composable
private fun CaptureCard(
    listening: Boolean,
    identified: Track?,
    error: String?,
    onMic: () -> Unit,
    onPlay: (Track) -> Unit
) {
    val nova = LocalNova.current
    NovaGlassCard(corner = 26.dp, glow = NeonCyan) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 104.dp)
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (identified != null && !listening) {
                CoverArt(identified, Modifier.size(64.dp), corner = 14.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(identified.title, color = nova.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${identified.artist} · ${identified.genre}",
                        color = nova.textDim,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                NovaButton(text = "Play", accent = NeonCyan, onClick = { onPlay(identified) })
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (listening) "Listening…" else "Identify nearby music",
                        color = nova.text,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            listening -> "Analysing the sound around you"
                            !error.isNullOrBlank() -> error
                            else -> "Tap the mic and hold your phone near the speaker"
                        },
                        color = nova.textDim,
                        fontSize = 12.sp
                    )
                    if (listening) {
                        Spacer(Modifier.height(8.dp))
                        ListeningBars()
                    }
                }
                MetalMic(listening = listening, onClick = onMic)
            }
        }
    }
}

@Composable
private fun MetalMic(listening: Boolean, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "mic")
    val ring by pulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "ring"
    )
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.9f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "ringAlpha"
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
        if (listening) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(ring)
                    .border(2.dp, NeonCyan.copy(alpha = ringAlpha), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .border(2.dp, NeonCyan.copy(alpha = 0.35f), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(MetalHi, MetalMid, MetalLo)))
                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = if (listening) "Stop" else "Capture",
                tint = if (listening) NeonCyan else MetalIcon,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun ListeningBars() {
    val pulse = rememberInfiniteTransition(label = "eq")
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(22.dp)
    ) {
        repeat(12) { index ->
            val height by pulse.animateFloat(
                initialValue = 4f,
                targetValue = 22f,
                animationSpec = infiniteRepeatable(
                    tween(800, delayMillis = (index % 3) * 140, easing = LinearEasing),
                    RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NeonCyan)
            )
        }
    }
}

@Composable
private fun ScannerCard(scanning: Boolean, onScan: () -> Unit) {
    val nova = LocalNova.current
    val spin = rememberInfiniteTransition(label = "scan")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    NovaGlassCard(corner = 26.dp, glow = CyberPurple) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Local music scanner", color = nova.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Finds FLAC, MP3, AAC and OGG files on your phone",
                    color = nova.textDim,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .novaRaised(nova, 24.dp, 4.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onScan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Scan files",
                    tint = NeonCyan,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(if (scanning) angle else 0f)
                )
            }
        }
    }
}

@Composable
fun SpecialTrackCard(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNova.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier = modifier
            .width(168.dp)
            .then(if (pressed) Modifier.novaInset(nova, 24.dp) else Modifier.novaRaised(nova, 24.dp, 5.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(144.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            CoverArt(track, Modifier.fillMaxSize(), corner = 16.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.33f), Color.Transparent),
                            center = Offset(40f, 24f),
                            radius = 180f
                        )
                    )
            )
            VinylMark(modifier = Modifier.align(Alignment.BottomEnd))
        }
        Spacer(Modifier.height(10.dp))
        Text(track.title, color = nova.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(track.artist, color = nova.textDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun VinylMark(modifier: Modifier = Modifier) {
    Canvas(modifier.size(90.dp).offset(x = 30.dp, y = 30.dp)) {
        drawCircle(Color(0xFF111111))
        repeat(7) { index ->
            drawCircle(
                color = if (index % 2 == 0) Color(0xFF1D1D27) else Color(0xFF111111),
                radius = size.minDimension / 2f * (0.86f - index * 0.09f),
                style = Stroke(width = 2.5f)
            )
        }
        drawCircle(NeonCyan, radius = size.minDimension * 0.12f)
    }
}

@Composable
fun SmartPlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, subtitle, icon, gradient) = when (playlist.id) {
        "smart_favorites" -> Quad("Liked songs", playlist.description, Icons.Default.Favorite, Brush.linearGradient(listOf(NeonMagenta, CyberPurple)))
        "smart_recently_played" -> Quad("Recently played", "Pick up again", Icons.Default.History, Brush.linearGradient(listOf(NeonCyan, RecentPlaylistEnd)))
        else -> Quad(playlist.name, playlist.description, Icons.Default.MusicNote, Brush.linearGradient(listOf(CyberPurple, NeonCyan)))
    }
    SmartPlaylistCard(title, subtitle, icon, gradient, onClick, modifier)
}

private data class Quad(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val gradient: Brush
)

@Composable
fun SmartPlaylistCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x44000000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TrackListItem(
    track: Track,
    onPlay: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNova.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArt(
            track,
            Modifier
                .size(54.dp)
                .novaRaised(nova, 14.dp, 3.dp),
            corner = 14.dp
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, color = nova.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${track.artist} · ${track.album}",
                color = nova.textDim,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val context = LocalContext.current
        Icon(
            Icons.Default.Favorite,
            contentDescription = "Favourite",
            tint = if (track.isFavorite) NeonMagenta else nova.textDim.copy(alpha = 0.45f),
            modifier = Modifier
                .size(40.dp)
                .clickable {
                    onFavorite()
                    val message = if (track.isFavorite) "Removed from liked songs" else "Added to liked songs"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                .padding(10.dp)
        )
    }
}

@Composable
private fun CoverArt(track: Track, modifier: Modifier, corner: androidx.compose.ui.unit.Dp) {
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(genreBrush(track.genre)),
        contentAlignment = Alignment.Center
    ) {
        if (!track.artworkUrl.isNullOrBlank()) {
            Image(
                painter = rememberAsyncImagePainter(track.artworkUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.85f))
        }
    }
}

private fun genreBrush(genre: String): Brush {
    val pair = when (genre.lowercase()) {
        "synthwave" -> NeonMagenta to Color(0xFF6D28D9)
        "lofi" -> NeonCyan to Color(0xFF1E3A8A)
        "electronic" -> BrassAmber to Color(0xFFBE185D)
        "techno" -> CyberPurple to Color(0xFF0F172A)
        "ambient" -> Color(0xFF34D399) to Color(0xFF312E81)
        else -> NeonCyan to CyberPurple
    }
    return Brush.linearGradient(listOf(pair.first, pair.second))
}
