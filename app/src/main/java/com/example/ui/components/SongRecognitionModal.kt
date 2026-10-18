package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.RecognizedTrack
import com.example.playback.recognition.RecognitionState
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongRecognitionModal(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val recognitionState by viewModel.recognitionState.collectAsState()
    val recognizedHistory by viewModel.recognizedTracks.collectAsState()
    var showHistory by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecognition()
        } else {
            viewModel.onRecognitionPermissionDenied()
        }
    }

    fun checkAndStart() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startRecognition()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Dialog(
        onDismissRequest = {
            viewModel.cancelRecognition()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD90E0E14)) // Dim backdrop with frosted blur effect
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val nova = LocalNova.current
            val panel = RoundedCornerShape(32.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .novaRaised(nova, 32.dp, 6.dp)
                    .clip(panel)
                    .background(Brush.verticalGradient(listOf(nova.glassTop, nova.surface.copy(alpha = 0.92f), nova.glassBottom)))
                    .border(1.dp, Brush.verticalGradient(listOf(nova.glassEdgeTop, nova.glassEdgeBottom)), panel)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x2600E5FF))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hearing,
                                    contentDescription = "Acoustic Recognition",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Nova SoundSense",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnDarkText
                                )
                                Text(
                                    text = "Shazam Acoustic Music Discovery",
                                    fontSize = 11.sp,
                                    color = OnDarkTextSecondary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // History Toggle
                            IconButton(
                                onClick = { showHistory = !showHistory },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (showHistory) NeonCyan.copy(alpha = 0.2f) else Color(0x1AFFFFFF))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "History",
                                    tint = if (showHistory) NeonCyan else OnDarkTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            // Close Button
                            IconButton(
                                onClick = {
                                    viewModel.cancelRecognition()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1AFFFFFF))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = OnDarkTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (showHistory) {
                        // History View
                        RecognizedHistoryContent(
                            history = recognizedHistory,
                            onPlay = { track ->
                                viewModel.playRecognizedTrack(track)
                                onDismiss()
                            },
                            onDelete = { track -> viewModel.deleteRecognizedTrack(track) },
                            onClearAll = { viewModel.clearRecognizedHistory() },
                            onBackToScan = { showHistory = false }
                        )
                    } else {
                        // Active Recognition Flow Content
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            when (val state = recognitionState) {
                                is RecognitionState.Idle -> {
                                    IdleScanState(onStartScan = { checkAndStart() })
                                }
                                is RecognitionState.Listening -> {
                                    ListeningScanState(
                                        amplitude = state.amplitude,
                                        onCancel = { viewModel.cancelRecognition() }
                                    )
                                }
                                is RecognitionState.Matching -> {
                                    MatchingScanState()
                                }
                                is RecognitionState.Found -> {
                                    FoundTrackState(
                                        track = state.track,
                                        onPlay = {
                                            viewModel.playRecognizedTrack(state.track)
                                            onDismiss()
                                        },
                                        onScanAgain = { checkAndStart() }
                                    )
                                }
                                is RecognitionState.NoMatch -> {
                                    NoMatchScanState(
                                        reason = state.reason,
                                        onRetry = { checkAndStart() }
                                    )
                                }
                                is RecognitionState.PermissionDenied -> {
                                    PermissionDeniedState(
                                        onRequestPermission = { checkAndStart() }
                                    )
                                }
                                is RecognitionState.Error -> {
                                    ErrorScanState(
                                        message = state.message,
                                        onRetry = { checkAndStart() }
                                    )
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
private fun IdleScanState(onStartScan: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "idle_glow")
        val ringScale by infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ringScale"
        )

        Box(
            modifier = Modifier
                .size(170.dp)
                .scale(ringScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0x3300E5FF),
                            Color(0x10FF3D9A),
                            Color.Transparent
                        )
                    )
                )
                .border(2.dp, Brush.sweepGradient(listOf(NeonCyan, NeonMagenta, NeonCyan)), CircleShape)
                .clickable { onStartScan() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFFF4F4F8), Color(0xFFB4B2C2), Color(0xFF6B6980))))
                    .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Identify Song",
                    tint = NeonCyan,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Tap Radar to Identify Music",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = OnDarkText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Hold your phone near audio to identify track title, artist, album, and high-res artwork instantly.",
            fontSize = 13.sp,
            color = OnDarkTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        NeumorphicButton(
            onClick = onStartScan,
            isAccent = true,
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF1A1727), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Listen nearby", color = Color(0xFF1A1727), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ListeningScanState(
    amplitude: Float,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        val dynamicScale = 1.0f + (amplitude * 0.45f)

        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Concentric animated radar waves
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(dynamicScale)
                    .clip(CircleShape)
                    .background(NeonCyan.copy(alpha = 0.15f * (0.3f + amplitude * 0.7f)))
                    .border(1.5.dp, NeonCyan.copy(alpha = 0.4f), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(135.dp)
                    .scale(1.0f + (amplitude * 0.25f))
                    .clip(CircleShape)
                    .background(NeonMagenta.copy(alpha = 0.2f))
                    .border(1.5.dp, NeonMagenta.copy(alpha = 0.5f), CircleShape)
            )

            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevatedStatic)
                    .border(2.dp, NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Listening",
                    tint = NeonCyan,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Listening to Nearby Music...",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Capturing acoustic frequencies & calculating soundwave fingerprint",
            fontSize = 12.sp,
            color = OnDarkTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, GlassBorderColor),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OnDarkTextSecondary)
        ) {
            Text("Cancel", fontSize = 13.sp)
        }
    }
}

@Composable
private fun MatchingScanState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(68.dp),
            color = NeonMagenta,
            trackColor = Color(0x3300E5FF),
            strokeWidth = 4.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Matching Song Fingerprint...",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OnDarkText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Querying millions of tracks in music catalog database",
            fontSize = 12.sp,
            color = OnDarkTextSecondary
        )
    }
}

@Composable
private fun FoundTrackState(
    track: RecognizedTrack,
    onPlay: () -> Unit,
    onScanAgain: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
    ) {
        Text(
            text = "✨ SONG IDENTIFIED!",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NeonCyan,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Prominent Artwork Card
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
        ) {
            Image(
                painter = rememberAsyncImagePainter(model = track.artworkUrl),
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = track.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = OnDarkText,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = track.artist,
            fontSize = 15.sp,
            color = NeonCyan,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (track.album.isNotBlank() && track.album != "Unknown Album") {
            Text(
                text = "Album: ${track.album}" + (track.releaseDate?.let { " ($it)" } ?: ""),
                fontSize = 12.sp,
                color = OnDarkTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, GlassBorderColor),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = OnDarkText)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Another", fontSize = 12.sp)
            }

            Button(
                onClick = onPlay,
                modifier = Modifier.weight(1.2f).height(48.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play Track", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun NoMatchScanState(
    reason: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Color(0x22FF3D9A))
                .border(1.5.dp, NeonMagenta.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicOff,
                contentDescription = null,
                tint = NeonMagenta,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "No Song Match Found",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OnDarkText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = reason,
            fontSize = 12.sp,
            color = OnDarkTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PermissionDeniedState(onRequestPermission: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MicOff,
            contentDescription = null,
            tint = NeonMagenta,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Microphone Access Required",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OnDarkText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Nova requires microphone permission to listen to nearby music and generate acoustic fingerprints for identification.",
            fontSize = 12.sp,
            color = OnDarkTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
            Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorScanState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Recognition Error",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OnDarkText
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            fontSize = 12.sp,
            color = OnDarkTextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
            Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecognizedHistoryContent(
    history: List<RecognizedTrack>,
    onPlay: (RecognizedTrack) -> Unit,
    onDelete: (RecognizedTrack) -> Unit,
    onClearAll: () -> Unit,
    onBackToScan: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackToScan) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scan", color = NeonCyan)
            }

            if (history.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Clear All", color = OnDarkTextSecondary, fontSize = 12.sp)
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.HistoryToggleOff, contentDescription = null, tint = OnDarkTextSecondary, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No recognized songs yet", color = OnDarkText, fontWeight = FontWeight.Bold)
                    Text("Identified songs will appear in this history list.", color = OnDarkTextSecondary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { it.id }) { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x1F222232))
                            .border(1.dp, GlassBorderColor, RoundedCornerShape(16.dp))
                            .clickable { onPlay(track) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = track.artworkUrl),
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnDarkText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artist,
                                fontSize = 12.sp,
                                color = NeonCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = dateFormat.format(Date(track.recognizedAt)),
                                fontSize = 10.sp,
                                color = OnDarkTextSecondary
                            )
                        }

                        IconButton(onClick = { onPlay(track) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = NeonCyan)
                        }

                        IconButton(onClick = { onDelete(track) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = OnDarkTextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
