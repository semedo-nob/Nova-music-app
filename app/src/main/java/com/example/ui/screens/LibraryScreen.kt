package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Style
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.Playlist
import com.example.data.model.RecognizedTrack
import com.example.data.model.Track
import com.example.ui.SongActions
import com.example.ui.components.LocalNova
import com.example.ui.components.NovaButton
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.novaInset
import com.example.ui.components.novaRaised
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.viewmodel.MusicViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MetalHi = Color(0xFFF4F4F8)
private val MetalMid = Color(0xFFB4B2C2)
private val MetalLo = Color(0xFF6B6980)

@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onOpenMixer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val songs by viewModel.allTracks.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState()
    val recognizedTracks by viewModel.recognizedTracks.collectAsState()
    val artists by viewModel.allArtists.collectAsState()
    val albums by viewModel.allAlbums.collectAsState()
    val genres by viewModel.allGenres.collectAsState()
    val isListening by viewModel.isShazamListening.collectAsState()
    val identified by viewModel.shazamResult.collectAsState()
    val captureError by viewModel.shazamError.collectAsState()

    val context = LocalContext.current
    val nova = LocalNova.current
    val tabNames = listOf("Songs", "Playlists", "Recognized", "Artists", "Albums", "Genres")

    var libraryTab by remember { mutableStateOf(0) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showListeningDialog by remember { mutableStateOf(false) }
    var playlistNameInput by remember { mutableStateOf("") }
    var playlistDescInput by remember { mutableStateOf("") }
    var selecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var newPlaylistForSelection by remember { mutableStateOf("") }
    var pendingDeleteIds by remember { mutableStateOf(listOf<String>()) }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && pendingDeleteIds.isNotEmpty()) {
            viewModel.removeTracks(pendingDeleteIds)
            Toast.makeText(context, "Deleted ${pendingDeleteIds.size} songs", Toast.LENGTH_SHORT).show()
            selecting = false
            selectedIds = emptySet()
        }
        pendingDeleteIds = emptyList()
    }

    var activeFilterType by remember { mutableStateOf<String?>(null) }
    var activeFilterValue by remember { mutableStateOf<String?>(null) }
    var activeFilterPlaylistId by remember { mutableStateOf<String?>(null) }

    val playlistSongsFlow = remember(activeFilterPlaylistId) {
        if (activeFilterPlaylistId != null) viewModel.getTracksInPlaylist(activeFilterPlaylistId!!)
        else kotlinx.coroutines.flow.flowOf(emptyList())
    }
    val playlistSongs by playlistSongsFlow.collectAsState(initial = emptyList())

    val filteredSongs = remember(songs, activeFilterType, activeFilterValue, playlistSongs) {
        when (activeFilterType) {
            "ARTIST" -> songs.filter { it.artist == activeFilterValue }
            "ALBUM" -> songs.filter { it.album == activeFilterValue }
            "GENRE" -> songs.filter { it.genre == activeFilterValue }
            "PLAYLIST" -> playlistSongs
            else -> songs
        }
    }
    val filtering = activeFilterType != null

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.startRecognition()
            showListeningDialog = true
        } else {
            viewModel.onRecognitionPermissionDenied()
            Toast.makeText(context, "Microphone permission is required to identify music", Toast.LENGTH_SHORT).show()
        }
    }

    fun startListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            viewModel.startRecognition()
            showListeningDialog = true
        } else {
            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(showListeningDialog, isListening, identified, captureError) {
        if (!showListeningDialog || isListening) return@LaunchedEffect
        val found = identified
        if (found != null) {
            showListeningDialog = false
            libraryTab = 2
            activeFilterType = null
            activeFilterValue = null
            activeFilterPlaylistId = null
            Toast.makeText(context, "Identified: ${found.title}", Toast.LENGTH_SHORT).show()
            viewModel.clearShazam()
        } else if (!captureError.isNullOrBlank()) {
            showListeningDialog = false
            Toast.makeText(context, captureError, Toast.LENGTH_SHORT).show()
            viewModel.clearShazam()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 24.dp, bottom = 16.dp)
                .defaultMinSize(minHeight = 52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Library",
                    color = nova.text,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp
                )
                Text(
                    text = if (selecting) "${selectedIds.size} selected" else if (filtering) activeFilterValue ?: "Browse your music" else "Browse your music",
                    color = nova.textDim,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(210.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (selecting) {
                    ToolButton(Icons.Default.Close, "Cancel selection") {
                        selecting = false
                        selectedIds = emptySet()
                    }
                } else {
                    ToolButton(Icons.Default.GraphicEq, "DJ mixer", onClick = onOpenMixer)
                    ToolButton(Icons.Default.Hearing, "Identify music", onClick = { startListening() })
                    if (filtering || libraryTab == 0) {
                        ToolButton(Icons.Default.CheckCircle, "Select songs") { selecting = true }
                    }
                    if (filtering) {
                        ToolButton(Icons.AutoMirrored.Filled.ArrowBack, "Back") {
                            activeFilterType = null
                            activeFilterValue = null
                            activeFilterPlaylistId = null
                            selecting = false
                            selectedIds = emptySet()
                        }
                    } else if (libraryTab == 1) {
                        ToolButton(Icons.Default.Add, "Create playlist") { showCreatePlaylistDialog = true }
                    }
                }
            }
        }

        if (!filtering) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .novaInset(nova, 18.dp)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(tabNames.size) { index ->
                    val selected = libraryTab == index
                    Text(
                        text = tabNames[index],
                        color = if (selected) NeonCyan else nova.textDim,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .then(if (selected) Modifier.novaRaised(nova, 13.dp, 3.dp) else Modifier)
                            .clip(RoundedCornerShape(13.dp))
                            .clickable { libraryTab = index }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (selecting) {
            val actionSongs = if (filtering) filteredSongs else songs
            val chosen = actionSongs.filter { it.id in selectedIds }
            val customPlaylist = activeFilterPlaylistId?.takeIf { filtering && !it.startsWith("smart_") }
            SelectionActions(
                enabled = chosen.isNotEmpty(),
                canRemoveFromPlaylist = customPlaylist != null,
                onShare = { SongActions.share(context, chosen) },
                onQueue = {
                    viewModel.addTracksToQueue(chosen)
                    Toast.makeText(context, "Added ${chosen.size} to the queue", Toast.LENGTH_SHORT).show()
                    selecting = false
                    selectedIds = emptySet()
                },
                onPlaylist = { showAddToPlaylist = true },
                onRemove = {
                    customPlaylist?.let { viewModel.removeTracksFromPlaylist(it, chosen.map { track -> track.id }) }
                    Toast.makeText(context, "Removed from playlist", Toast.LENGTH_SHORT).show()
                    selecting = false
                    selectedIds = emptySet()
                },
                onDelete = { showDeleteConfirm = true }
            )
            Spacer(Modifier.height(8.dp))
        }

        Box(modifier = Modifier.weight(1f)) {
            if (filtering) {
                if (filteredSongs.isEmpty()) {
                    LibraryEmptyState("tracks in this playlist")
                } else {
                    SelectableSongColumn(
                        tracks = filteredSongs,
                        selecting = selecting,
                        selectedIds = selectedIds,
                        onToggle = { track ->
                            selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id
                        },
                        onStartSelection = { track ->
                            selecting = true
                            selectedIds = selectedIds + track.id
                        },
                        onPlay = { track -> viewModel.playTrack(track, filteredSongs) },
                        onFavorite = { viewModel.toggleFavorite(it) }
                    )
                }
            } else when (libraryTab) {
                0 -> if (songs.isEmpty()) {
                    LibraryEmptyState("songs")
                } else {
                    SelectableSongColumn(
                        tracks = songs,
                        selecting = selecting,
                        selectedIds = selectedIds,
                        onToggle = { track ->
                            selectedIds = if (track.id in selectedIds) selectedIds - track.id else selectedIds + track.id
                        },
                        onStartSelection = { track ->
                            selecting = true
                            selectedIds = selectedIds + track.id
                        },
                        onPlay = { track -> viewModel.playTrack(track, songs) },
                        onFavorite = { viewModel.toggleFavorite(it) }
                    )
                }
                1 -> {
                    val sorted = playlists.sortedByDescending { it.isSmart }
                    if (sorted.isEmpty()) {
                        LibraryEmptyState("playlists", "Create playlist") { showCreatePlaylistDialog = true }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(sorted, key = { it.id }) { playlist ->
                                PlaylistItem(
                                    playlist = playlist,
                                    onClick = {
                                        activeFilterType = "PLAYLIST"
                                        activeFilterValue = playlistLabel(playlist).first
                                        activeFilterPlaylistId = playlist.id
                                    },
                                    onDelete = {
                                        viewModel.deletePlaylist(playlist.id)
                                        Toast.makeText(context, "Playlist deleted", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
                2 -> if (recognizedTracks.isEmpty()) {
                    LibraryEmptyState("recognized songs", "Identify nearby music") { startListening() }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recognizedTracks, key = { it.id }) { track ->
                            RecognizedTrackItem(
                                track = track,
                                onPlay = { viewModel.playRecognizedTrack(track) },
                                onDelete = { viewModel.deleteRecognizedTrack(track) }
                            )
                        }
                    }
                }
                3 -> if (artists.isEmpty()) LibraryEmptyState("artists") else CategoryColumn(artists, "Artist", Icons.Default.Person) {
                    activeFilterType = "ARTIST"
                    activeFilterValue = it
                }
                4 -> if (albums.isEmpty()) LibraryEmptyState("albums") else CategoryColumn(albums, "Album", Icons.Default.Album) {
                    activeFilterType = "ALBUM"
                    activeFilterValue = it
                }
                else -> if (genres.isEmpty()) LibraryEmptyState("genres") else CategoryColumn(genres, "Genre", Icons.Default.Style) {
                    activeFilterType = "GENRE"
                    activeFilterValue = it
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        Dialog(onDismissRequest = { showCreatePlaylistDialog = false }) {
            NovaGlassCard(corner = 22.dp, modifier = Modifier.widthIn(max = 360.dp).padding(8.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("New playlist", color = nova.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    InsetField(playlistNameInput, "Playlist name") { playlistNameInput = it }
                    Spacer(Modifier.height(12.dp))
                    InsetField(playlistDescInput, "Description (optional)") { playlistDescInput = it }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Cancel",
                            color = nova.textDim,
                            modifier = Modifier
                                .clickable { showCreatePlaylistDialog = false }
                                .padding(10.dp)
                        )
                        NovaButton(
                            text = "Create",
                            accent = NeonCyan,
                            onClick = {
                                if (playlistNameInput.isNotBlank()) {
                                    viewModel.createPlaylist(playlistNameInput.trim(), playlistDescInput.trim())
                                    playlistNameInput = ""
                                    playlistDescInput = ""
                                    showCreatePlaylistDialog = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showListeningDialog) {
        Dialog(onDismissRequest = {
            viewModel.cancelRecognition()
            showListeningDialog = false
        }) {
            NovaGlassCard(corner = 22.dp, modifier = Modifier.widthIn(max = 360.dp)) {
                val pulse by rememberInfiniteTransition(label = "listen").animateFloat(
                    initialValue = 1f,
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                    label = "listenScale"
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Listening…", color = nova.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Hold your phone near the music",
                        color = nova.textDim,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(22.dp))
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .scale(pulse)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(MetalHi, MetalMid, MetalLo)))
                            .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }

    if (showAddToPlaylist) {
        val actionSongs = if (filtering) filteredSongs else songs
        val chosen = actionSongs.filter { it.id in selectedIds }
        val customPlaylists = playlists.filter { !it.isSmart }
        Dialog(onDismissRequest = { showAddToPlaylist = false }) {
            NovaGlassCard(corner = 22.dp, modifier = Modifier.widthIn(max = 360.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Add to playlist", color = nova.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    InsetField(newPlaylistForSelection, "New playlist name") { newPlaylistForSelection = it }
                    Spacer(Modifier.height(10.dp))
                    NovaButton(
                        text = "Create and add",
                        accent = NeonCyan,
                        onClick = {
                            if (newPlaylistForSelection.isNotBlank() && chosen.isNotEmpty()) {
                                viewModel.createPlaylistWithTracks(newPlaylistForSelection.trim(), "", chosen.map { it.id })
                                newPlaylistForSelection = ""
                                showAddToPlaylist = false
                                selecting = false
                                selectedIds = emptySet()
                                Toast.makeText(context, "Playlist created", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    if (customPlaylists.isEmpty()) {
                        Text("Or create one above.", color = nova.textDim, fontSize = 13.sp)
                    } else {
                        LazyColumn(modifier = Modifier.height(220.dp)) {
                            items(customPlaylists, key = { it.id }) { playlist ->
                                Text(
                                    playlist.name,
                                    color = nova.text,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addTracksToPlaylist(playlist.id, chosen.map { it.id })
                                            showAddToPlaylist = false
                                            selecting = false
                                            selectedIds = emptySet()
                                            Toast.makeText(context, "Added to ${playlist.name}", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        val actionSongs = if (filtering) filteredSongs else songs
        val chosen = actionSongs.filter { it.id in selectedIds }
        val onDevice = chosen.count { it.path.startsWith("content://") }
        Dialog(onDismissRequest = { showDeleteConfirm = false }) {
            NovaGlassCard(corner = 22.dp, modifier = Modifier.widthIn(max = 360.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Delete ${chosen.size} songs?", color = nova.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (onDevice > 0) "Songs stored on this device are deleted after you confirm. This cannot be undone."
                        else "These songs will be removed from your library.",
                        color = nova.textDim,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Cancel",
                            color = nova.textDim,
                            modifier = Modifier.clickable { showDeleteConfirm = false }.padding(10.dp)
                        )
                        NovaButton(
                            text = "Delete",
                            accent = NeonMagenta,
                            onClick = {
                                showDeleteConfirm = false
                                val waiting = SongActions.prepareDeviceDelete(
                                    context = context,
                                    tracks = chosen,
                                    launchConsent = { deleteLauncher.launch(it) },
                                    removeNow = { viewModel.removeTracks(it) }
                                )
                                pendingDeleteIds = waiting
                                if (waiting.isEmpty()) {
                                    selecting = false
                                    selectedIds = emptySet()
                                    Toast.makeText(context, "Removed from library", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableSongColumn(
    tracks: List<Track>,
    selecting: Boolean,
    selectedIds: Set<String>,
    onToggle: (Track) -> Unit,
    onStartSelection: (Track) -> Unit,
    onPlay: (Track) -> Unit,
    onFavorite: (String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
        items(tracks, key = { it.id }) { track ->
            TrackListItem(
                track = track,
                selected = track.id in selectedIds,
                onPlay = {
                    if (selecting) onToggle(track) else onPlay(track)
                },
                onLongPress = { onStartSelection(track) },
                onFavorite = { onFavorite(track.id) }
            )
        }
    }
}

@Composable
private fun SelectionActions(
    enabled: Boolean,
    canRemoveFromPlaylist: Boolean,
    onShare: () -> Unit,
    onQueue: () -> Unit,
    onPlaylist: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit
) {
    val nova = LocalNova.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .novaInset(nova, 16.dp)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SelectionAction("Share", Icons.Default.Share, enabled, onShare)
        SelectionAction("Queue", Icons.Default.QueueMusic, enabled, onQueue)
        SelectionAction("Playlist", Icons.Default.Add, enabled, onPlaylist)
        if (canRemoveFromPlaylist) {
            SelectionAction("Remove", Icons.Default.Close, enabled, onRemove)
        }
        SelectionAction("Delete", Icons.Default.Delete, enabled, onDelete)
    }
}

@Composable
private fun SelectionAction(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val nova = LocalNova.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) NeonCyan else nova.textDim, modifier = Modifier.size(18.dp))
        Text(label, color = if (enabled) nova.text else nova.textDim, fontSize = 11.sp)
    }
}

@Composable
private fun CategoryColumn(
    names: List<String>,
    subtitle: String,
    icon: ImageVector,
    onClick: (String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
        items(names) { name ->
            CategoryListItem(title = name, subtitle = subtitle, icon = icon, onClick = { onClick(name) })
        }
    }
}

@Composable
private fun ToolButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val nova = LocalNova.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(44.dp)
            .then(if (pressed) Modifier.novaInset(nova, 22.dp) else Modifier.novaRaised(nova, 22.dp, 4.dp))
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = NeonCyan, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun InsetField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    val nova = LocalNova.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = nova.text, fontSize = 15.sp),
        cursorBrush = SolidColor(NeonCyan),
        modifier = Modifier
            .fillMaxWidth()
            .novaInset(nova, 14.dp)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = nova.textDim, fontSize = 15.sp)
                inner()
            }
        }
    )
}

@Composable
fun LibraryEmptyState(
    type: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val nova = LocalNova.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Inbox, contentDescription = null, tint = nova.textDim.copy(alpha = 0.6f), modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text("No $type yet.", color = nova.textDim, fontSize = 15.sp, textAlign = TextAlign.Center)
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            NovaButton(text = actionText, accent = NeonCyan, onClick = onAction)
        }
    }
}

@Composable
fun PlaylistItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNova.current
    val (label, detail) = playlistLabel(playlist)
    val (icon, tint) = playlistIcon(playlist)
    NovaGlassCard(corner = 22.dp, onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .novaInset(nova, 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = nova.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    detail.ifBlank { "Custom playlist" },
                    color = nova.textDim,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!playlist.isSmart) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = NeonMagenta,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onDelete)
                        .padding(10.dp)
                )
            }
        }
    }
}

private fun playlistLabel(playlist: Playlist): Pair<String, String> = when (playlist.id) {
    "smart_favorites" -> "Liked songs" to "Everything you starred"
    "smart_recently_played" -> "Recently played" to "Pick up where you left off"
    "smart_most_played" -> "Most played" to "Your heavy rotation"
    else -> playlist.name to playlist.description
}

private fun playlistIcon(playlist: Playlist): Pair<ImageVector, Color> = when (playlist.id) {
    "smart_favorites" -> Icons.Default.Favorite to NeonMagenta
    "smart_recently_played" -> Icons.Default.History to NeonCyan
    "smart_most_played" -> Icons.Default.Headset to CyberPurple
    else -> Icons.Default.QueueMusic to NeonCyan
}

@Composable
fun CategoryListItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNova.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .novaRaised(nova, 24.dp, 4.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = nova.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = nova.textDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun RecognizedTrackItem(
    track: RecognizedTrack,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNova.current
    val whenText = remember(track.recognizedAt) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(track.recognizedAt))
    }
    NovaGlassCard(corner = 22.dp, onClick = onPlay, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .novaRaised(nova, 14.dp, 3.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(NeonCyan, CyberPurple)))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, color = nova.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${track.artist} · ${track.album}",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Identified $whenText", color = nova.textDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = NeonCyan,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onPlay)
                    .padding(10.dp)
            )
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove",
                tint = nova.textDim,
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onDelete)
                    .padding(10.dp)
            )
        }
    }
}
