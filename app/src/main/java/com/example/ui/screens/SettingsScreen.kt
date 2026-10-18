package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.components.AuthModalDialog
import com.example.ui.components.LocalNova
import com.example.ui.components.NovaButton
import com.example.ui.components.NovaFader
import com.example.ui.components.NovaGem
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.NovaLedMeter
import com.example.ui.components.NovaRow
import com.example.ui.components.NovaSection
import com.example.ui.components.NovaSegmented
import com.example.ui.components.NovaSkeuoSwitch
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.viewmodel.MusicViewModel

private val EqPresets = mapOf(
    "Flat" to listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f),
    "Bass Boost" to listOf(0.9f, 0.75f, 0.5f, 0.45f, 0.4f),
    "Cyberpunk" to listOf(0.8f, 0.45f, 0.6f, 0.85f, 0.9f),
    "Vocal" to listOf(0.35f, 0.55f, 0.85f, 0.7f, 0.45f),
    "Electronic" to listOf(0.8f, 0.55f, 0.45f, 0.7f, 0.85f)
)
private val EqBands = listOf("60", "250", "1k", "4k", "12k")

@Composable
fun SettingsScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val nova = LocalNova.current
    val currentUser by viewModel.currentUser.collectAsState()
    val amoled by viewModel.amoledTheme.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val accentColor by viewModel.accentColorName.collectAsState()
    val crossfade by viewModel.crossfadeEnabled.collectAsState()
    val scanCount by viewModel.scannedCount.collectAsState()
    val eqPreset by viewModel.eqPreset.collectAsState()

    var showAuthModal by remember { mutableStateOf(false) }
    var currentCacheSize by remember { mutableStateOf(viewModel.getAppCacheSize()) }
    var audioNormalization by remember { mutableStateOf(true) }

    val bands = EqPresets[eqPreset] ?: EqPresets.getValue("Cyberpunk")
    val cacheMb = currentCacheSize.substringBefore(" ").toFloatOrNull() ?: 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Settings",
                color = nova.text,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = "Sound, look and storage",
                color = nova.textDim,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 0.dp)
            )
        }

        item {
            val user = currentUser
            NovaGlassCard(corner = 26.dp, glow = NeonCyan, onClick = { showAuthModal = true }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(Brush.sweepGradient(listOf(NeonCyan, CyberPurple, NeonMagenta, NeonCyan)))
                            .padding(4.dp)
                    ) {
                        if (!user?.photoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(user?.photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(NeonCyan, CyberPurple))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                            }
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user?.displayName ?: "Guest",
                                color = nova.text,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (user?.isPremium == true) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "VIP",
                                    color = NeonMagenta,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NeonMagenta.copy(alpha = 0.22f))
                                        .border(1.dp, NeonMagenta.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = user?.email ?: "Tap to sign in",
                            color = nova.textDim,
                            fontSize = 13.sp
                        )
                        Text(
                            text = when (user?.provider) {
                                "google.com" -> "Synced with Google"
                                null -> "Not signed in"
                                else -> "Signed in on this device"
                            },
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Manage account",
                        tint = nova.textDim,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        item {
            NovaSection(
                title = "Audio",
                accent = NeonCyan,
                footer = "Processed on this device. Supports 24-bit FLAC."
            ) {
                NovaRow(
                    title = "Equalizer",
                    subtitle = "Pick a preset or watch the bands move",
                    icon = Icons.Default.GraphicEq,
                    accent = NeonCyan,
                    divider = false
                )
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.verticalGradient(listOf(nova.raisedLight.copy(alpha = 0.35f), Color.Transparent)))
                        .border(1.dp, nova.glassEdgeBottom, RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    bands.forEachIndexed { index, level ->
                        NovaFader(level = level, accent = NeonCyan, label = EqBands[index])
                    }
                }
                NovaSegmented(
                    options = EqPresets.keys.toList(),
                    selected = if (eqPreset in EqPresets) eqPreset else "Cyberpunk",
                    accent = NeonCyan,
                    onSelect = { preset ->
                        viewModel.selectEqPreset(preset)
                        Toast.makeText(context, "Equalizer set to $preset", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp)
                        .fillMaxWidth()
                )
                NovaRow(
                    title = "Gapless crossfade",
                    subtitle = "Overlap tracks by 3 seconds",
                    icon = Icons.Default.AllInclusive,
                    accent = CyberPurple,
                    divider = false,
                    trailing = {
                        NovaSkeuoSwitch(
                            checked = crossfade,
                            onCheckedChange = { viewModel.toggleCrossfade() },
                            accent = CyberPurple
                        )
                    }
                )
                NovaRow(
                    title = "ReplayGain",
                    subtitle = "Keep volume even across tracks",
                    icon = Icons.Default.Tune,
                    accent = NeonCyan,
                    divider = false,
                    trailing = {
                        NovaSkeuoSwitch(
                            checked = audioNormalization,
                            onCheckedChange = {
                                audioNormalization = it
                                Toast.makeText(context, if (it) "ReplayGain on" else "ReplayGain off", Toast.LENGTH_SHORT).show()
                            },
                            accent = NeonCyan
                        )
                    }
                )
            }
        }

        item {
            NovaSection(
                title = "Appearance",
                accent = NeonCyan,
                footer = "Accent colour tints controls, glow and meters across the app."
            ) {
                NovaRow(
                    title = "Dark theme",
                    subtitle = "Deep plum interface",
                    icon = Icons.Default.DarkMode,
                    accent = NeonCyan,
                    divider = true,
                    trailing = {
                        NovaSkeuoSwitch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) },
                            accent = NeonCyan
                        )
                    }
                )
                NovaRow(
                    title = "AMOLED black",
                    subtitle = "True black pixels to save battery on OLED",
                    icon = Icons.Default.Brightness2,
                    accent = CyberPurple,
                    divider = false,
                    trailing = {
                        NovaSkeuoSwitch(
                            checked = amoled,
                            onCheckedChange = { viewModel.setAmoledTheme(it) },
                            accent = CyberPurple
                        )
                    }
                )
                Text(
                    text = "Accent colour",
                    color = nova.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NovaGem(
                        label = "Blue",
                        c1 = Color(0xFF5CE1E6),
                        c2 = Color(0xFF0E7490),
                        selected = accentColor == "Gemini Blue",
                        onClick = { viewModel.setAccentColorName("Gemini Blue") },
                        modifier = Modifier.weight(1f)
                    )
                    NovaGem(
                        label = "Purple",
                        c1 = Color(0xFFA78BFA),
                        c2 = Color(0xFF5B21B6),
                        selected = accentColor == "Cyber Purple",
                        onClick = { viewModel.setAccentColorName("Cyber Purple") },
                        modifier = Modifier.weight(1f)
                    )
                    NovaGem(
                        label = "Pink",
                        c1 = Color(0xFFFB7185),
                        c2 = Color(0xFF9F1239),
                        selected = accentColor == "Neon Pink",
                        onClick = { viewModel.setAccentColorName("Neon Pink") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            NovaSection(
                title = "Storage",
                accent = NeonCyan,
                footer = "Cached stream files are safe to clear. Your library stays put."
            ) {
                NovaRow(
                    title = "Audio cache",
                    subtitle = "Using $currentCacheSize",
                    icon = Icons.Default.Delete,
                    accent = NeonMagenta,
                    divider = false,
                    trailing = {
                        NovaButton(
                            text = "Clear",
                            accent = NeonMagenta,
                            onClick = {
                                viewModel.clearAppCache()
                                currentCacheSize = "0.0 MB"
                                Toast.makeText(context, "Cache cleared! Storage recovered.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
                NovaLedMeter(
                    ratio = (cacheMb / 100f).coerceIn(0f, 1f),
                    accent = NeonCyan,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
                NovaRow(
                    title = "Rescan media",
                    subtitle = if (scanCount != null) "$scanCount audio files indexed" else "Library loaded with Nova Originals",
                    icon = Icons.Default.Refresh,
                    accent = NeonCyan,
                    divider = false,
                    trailing = {
                        NovaButton(
                            text = "Scan",
                            accent = NeonCyan,
                            icon = Icons.Default.Sync,
                            onClick = {
                                viewModel.scanLocalTracks()
                                Toast.makeText(context, "Scanning local tracks...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
            }
        }

        item {
            NovaSection(title = "About", accent = NeonCyan) {
                NovaRow(
                    title = "Nova Music",
                    subtitle = "Holographic Edition",
                    icon = Icons.Default.MusicNote,
                    accent = NeonCyan,
                    divider = true
                )
                NovaRow(
                    title = "Built with",
                    subtitle = "Native Android, Jetpack Compose and Material 3",
                    icon = Icons.Default.Code,
                    accent = CyberPurple,
                    divider = true
                )
                NovaRow(
                    title = "Intelligence",
                    subtitle = "Gemini 3.1 Pro and 3.5 Flash",
                    icon = Icons.Default.AutoAwesome,
                    accent = NeonMagenta,
                    divider = false
                )
            }
        }
    }

    if (showAuthModal) {
        AuthModalDialog(
            viewModel = viewModel,
            onDismiss = { showAuthModal = false }
        )
    }
}
