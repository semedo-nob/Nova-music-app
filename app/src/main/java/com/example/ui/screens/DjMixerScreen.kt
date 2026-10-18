package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Track
import com.example.ui.components.LocalNova
import com.example.ui.components.NovaButton
import com.example.ui.components.NovaGlassCard
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun DjMixerScreen(
    viewModel: MusicViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nova = LocalNova.current
    val songs by viewModel.allTracks.collectAsState()
    val deckA by viewModel.currentTrack.collectAsState()
    val deckAPlaying by viewModel.isPlaying.collectAsState()
    val deckAPosition by viewModel.currentPosition.collectAsState()
    val deckB by viewModel.mixerDeckBTrack.collectAsState()
    val deckBPlaying by viewModel.mixerDeckBPlaying.collectAsState()
    val deckBPosition by viewModel.mixerDeckBPosition.collectAsState()
    val crossfader by viewModel.mixerCrossfader.collectAsState()
    val gainA by viewModel.mixerDeckAGain.collectAsState()
    val gainB by viewModel.mixerDeckBGain.collectAsState()
    var pickingDeck by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(nova.bg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("DJ Mixer", color = nova.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Two decks, one crossfader", color = nova.textDim, fontSize = 13.sp)
            }
            Icon(
                Icons.Default.Close,
                contentDescription = "Close mixer",
                tint = nova.text,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onClose)
                    .padding(8.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        DeckCard(
            label = "Deck A",
            accent = NeonCyan,
            track = deckA,
            playing = deckAPlaying,
            positionMs = deckAPosition,
            gain = gainA,
            onGain = viewModel::setMixerDeckAGain,
            onPlay = viewModel::playPause,
            onCue = viewModel::cueMixerDeckA,
            onLoad = { pickingDeck = "A" }
        )
        Spacer(Modifier.height(12.dp))
        DeckCard(
            label = "Deck B",
            accent = CyberPurple,
            track = deckB,
            playing = deckBPlaying,
            positionMs = deckBPosition,
            gain = gainB,
            onGain = viewModel::setMixerDeckBGain,
            onPlay = viewModel::toggleMixerDeckB,
            onCue = viewModel::cueMixerDeckB,
            onLoad = { pickingDeck = "B" }
        )
        Spacer(Modifier.height(18.dp))
        Text("Crossfader", color = nova.text, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("A", color = NeonCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
            Slider(
                value = crossfader,
                onValueChange = viewModel::setMixerCrossfader,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = nova.text,
                    activeTrackColor = CyberPurple,
                    inactiveTrackColor = NeonCyan
                )
            )
            Text("B", color = CyberPurple, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
    }

    val deck = pickingDeck
    if (deck != null) {
        Dialog(onDismissRequest = { pickingDeck = null }) {
            NovaGlassCard(corner = 22.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Load deck $deck", color = nova.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    if (songs.isEmpty()) {
                        Text("Scan your library before loading a deck.", color = nova.textDim)
                    } else {
                        LazyColumn(modifier = Modifier.height(360.dp)) {
                            items(songs, key = { it.id }) { track ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (deck == "A") viewModel.loadMixerDeckA(track) else viewModel.loadMixerDeckB(track)
                                            pickingDeck = null
                                        }
                                        .padding(vertical = 10.dp)
                                ) {
                                    Text(track.title, color = nova.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(track.artist, color = nova.textDim, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckCard(
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    track: Track?,
    playing: Boolean,
    positionMs: Long,
    gain: Float,
    onGain: (Float) -> Unit,
    onPlay: () -> Unit,
    onCue: () -> Unit,
    onLoad: () -> Unit
) {
    val nova = LocalNova.current
    NovaGlassCard(corner = 22.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                track?.title ?: "No song loaded",
                color = nova.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track?.artist ?: "Load a song from your library",
                color = nova.textDim,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(formatClock(positionMs), color = nova.textDim, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = track != null, onClick = onPlay)
                        .padding(6.dp)
                ) {
                    Icon(
                        if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play",
                        tint = if (track == null) nova.textDim else accent
                    )
                }
                NovaButton(text = "Cue", accent = accent, onClick = { if (track != null) onCue() })
                NovaButton(text = "Load", accent = accent, onClick = onLoad)
            }
            Text("Gain", color = nova.textDim, fontSize = 12.sp)
            Slider(
                value = gain,
                onValueChange = onGain,
                colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
            )
        }
    }
}

private fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0L)
    return "%d:%02d".format(total / 60, total % 60)
}
