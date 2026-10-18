package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ai.AssistantAction
import com.example.ai.AssistantMessage
import com.example.data.model.Track
import com.example.data.online.OnlineSearchResult
import com.example.ui.components.LocalNova
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.SongRecognitionModal
import com.example.ui.components.novaInset
import com.example.ui.components.novaRaised
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.viewmodel.MusicViewModel

private val MetalHi = Color(0xFFF4F4F8)
private val MetalMid = Color(0xFFB4B2C2)
private val MetalLo = Color(0xFF6B6980)
private val Ink = Color(0xFF1A1727)
private val MicInk = Color(0xFF3A3750)

private val Suggestions = listOf(
    "Play upbeat songs",
    "Relaxing library tracks",
    "Skip this song",
    "Play Kenyan music",
    "Study instrumental",
    "Search online: Sauti Sol",
    "Shuffle library"
)

private val Models = listOf(
    "gemini-3.5-flash" to "Flash",
    "gemini-3.1-pro-preview" to "Pro thinking",
    "gemini-3.1-flash-lite-preview" to "Flash lite"
)

@Composable
fun AIdjScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val assistantMessages by viewModel.assistantMessages.collectAsState()
    val isAssistantThinking by viewModel.isAssistantThinking.collectAsState()
    val isGeneratingPlaylist by viewModel.isGeneratingPlaylist.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    val context = LocalContext.current
    val nova = LocalNova.current
    val listState = rememberLazyListState()

    var textInput by remember { mutableStateOf("") }
    var useSearchGrounding by remember { mutableStateOf(false) }
    var showMoodDialog by remember { mutableStateOf(false) }
    var moodInput by remember { mutableStateOf("") }
    var showRecognitionModal by remember { mutableStateOf(false) }
    var voiceListening by remember { mutableStateOf(false) }

    val modelLabel = Models.firstOrNull { it.first == selectedModel }?.second
        ?: if (selectedModel.contains("lite")) "Flash lite" else if (selectedModel.contains("pro")) "Pro thinking" else "Flash"

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        voiceListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrEmpty()) {
                textInput = ""
                viewModel.sendAssistantRequest(spokenText)
            }
        }
    }

    LaunchedEffect(assistantMessages.size, isAssistantThinking, isGeneratingPlaylist) {
        val extra = (if (isAssistantThinking) 1 else 0) + (if (isGeneratingPlaylist) 1 else 0)
        val last = assistantMessages.size + extra - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    if (showRecognitionModal) {
        SongRecognitionModal(
            viewModel = viewModel,
            onDismiss = { showRecognitionModal = false }
        )
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModel.sendAssistantRequest(trimmed)
        textInput = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 12.dp)) {
            Text(
                text = "Nova AI",
                color = nova.text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Your library controller and music finder",
                color = nova.textDim,
                fontSize = 12.sp
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ActionChip(text = "Identify song", icon = Icons.Default.Mic, onClick = { showRecognitionModal = true })
            ActionChip(
                text = modelLabel,
                accent = NeonMagenta,
                onClick = {
                    val index = Models.indexOfFirst { it.first == selectedModel }.let { if (it < 0) 0 else it }
                    val next = Models[(index + 1) % Models.size]
                    viewModel.setSelectedModel(next.first)
                    Toast.makeText(context, "AI model: ${next.second}", Toast.LENGTH_SHORT).show()
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ActionChip(
                text = "Google grounding",
                lit = useSearchGrounding,
                onClick = { useSearchGrounding = !useSearchGrounding }
            )
            ActionChip(
                text = "Playlist maker",
                accent = NeonMagenta,
                icon = Icons.Default.AutoAwesome,
                onClick = { showMoodDialog = true }
            )
        }
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(assistantMessages, key = { it.id }) { msg ->
                AssistantMessageCard(
                    message = msg,
                    onPlayLocal = { tracks ->
                        viewModel.playTrack(tracks.first(), tracks)
                        Toast.makeText(context, "Playing ${tracks.size} tracks from your library", Toast.LENGTH_SHORT).show()
                    },
                    onPlayOnline = { onlineResult ->
                        viewModel.playOnlineTrack(onlineResult)
                        Toast.makeText(context, "Playing preview: ${onlineResult.title}", Toast.LENGTH_SHORT).show()
                    },
                    onOptionSelected = { option -> viewModel.sendAssistantRequest(option) },
                    onSpeak = { text ->
                        Toast.makeText(context, "Reading aloud…", Toast.LENGTH_SHORT).show()
                        viewModel.speakText(text)
                    }
                )
            }
            if (isAssistantThinking || isGeneratingPlaylist) {
                item {
                    ThinkingBubble(
                        label = if (isGeneratingPlaylist && !isAssistantThinking) "Building your playlist…"
                        else "Checking your library and catalogue…"
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Suggestions) { prompt ->
                ActionChip(text = prompt, onClick = { send(prompt) })
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 100.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BasicTextField(
                value = textInput,
                onValueChange = { textInput = it },
                singleLine = true,
                textStyle = TextStyle(color = nova.text, fontSize = 15.sp),
                cursorBrush = SolidColor(NeonCyan),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send(textInput) }),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .novaInset(nova, 24.dp)
                    .padding(horizontal = 18.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (textInput.isEmpty()) {
                            Text("Ask Nova, e.g. Play Sauti Sol", color = nova.textDim, fontSize = 13.sp)
                        }
                        inner()
                    }
                }
            )
            MetalMic(recording = voiceListening) {
                voiceListening = true
                Toast.makeText(context, "Listening…", Toast.LENGTH_SHORT).show()
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a music request to Nova")
                }
                try {
                    speechLauncher.launch(intent)
                } catch (e: Exception) {
                    voiceListening = false
                    Toast.makeText(context, "Voice input not available, please type", Toast.LENGTH_SHORT).show()
                }
            }
            SendButton(onClick = { send(textInput) })
        }
    }

    if (showMoodDialog) {
        Dialog(onDismissRequest = { showMoodDialog = false }) {
            NovaGlassCard(corner = 22.dp, modifier = Modifier.widthIn(max = 360.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("AI playlist maker", color = nova.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Describe your vibe, mood or activity",
                        color = nova.textDim,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )
                    BasicTextField(
                        value = moodInput,
                        onValueChange = { moodInput = it },
                        singleLine = true,
                        textStyle = TextStyle(color = nova.text, fontSize = 15.sp),
                        cursorBrush = SolidColor(NeonCyan),
                        modifier = Modifier
                            .fillMaxWidth()
                            .novaInset(nova, 14.dp)
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        decorationBox = { inner ->
                            Box {
                                if (moodInput.isEmpty()) {
                                    Text("e.g. Upbeat synthwave for running", color = nova.textDim, fontSize = 15.sp)
                                }
                                inner()
                            }
                        }
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Dismiss",
                            color = nova.textDim,
                            modifier = Modifier
                                .clickable { showMoodDialog = false }
                                .padding(10.dp)
                        )
                        ActionChip(
                            text = "Create playlist",
                            accent = NeonMagenta,
                            onClick = {
                                if (moodInput.isNotBlank()) {
                                    viewModel.generateAiPlaylist(moodInput.trim())
                                    moodInput = ""
                                    showMoodDialog = false
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
private fun ActionChip(
    text: String,
    onClick: () -> Unit,
    accent: Color = NeonCyan,
    icon: ImageVector? = null,
    lit: Boolean = false
) {
    val nova = LocalNova.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .then(if (pressed) Modifier.novaInset(nova, 14.dp) else Modifier.novaRaised(nova, 14.dp, 3.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (lit || text == "Google grounding") {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (lit) accent else Color.Black.copy(alpha = 0.35f))
            )
        }
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
        }
        Text(text, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

@Composable
private fun MetalMic(recording: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(MetalHi, MetalMid, MetalLo)))
            .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.Mic,
            contentDescription = "Voice input",
            tint = if (recording) NeonMagenta else MicInk,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SendButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(if (pressed) 0.94f else 1f)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(NeonCyan, CyberPurple)))
            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Send, contentDescription = "Send", tint = Ink, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ThinkingBubble(label: String) {
    val nova = LocalNova.current
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp)
    Column(horizontalAlignment = Alignment.Start) {
        Text("Nova", color = NeonMagenta, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 3.dp))
        Row(
            modifier = Modifier
                .clip(shape)
                .background(Brush.verticalGradient(listOf(nova.glassTop, nova.glassBottom)))
                .border(1.dp, Brush.verticalGradient(listOf(nova.glassEdgeTop, nova.glassEdgeBottom)), shape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThinkingDots()
            Text(label, color = nova.textDim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ThinkingDots() {
    val phase by rememberInfiniteTransition(label = "dots").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "phase"
    )
    Row {
        repeat(3) { index ->
            val local = (phase + index * 0.2f) % 1f
            val up = if (local < 0.5f) local / 0.5f else (1f - local) / 0.5f
            Box(
                Modifier
                    .padding(end = 3.dp)
                    .offset(y = (2f - up * 5f).dp)
                    .size(6.dp)
                    .alpha(0.25f + 0.75f * up)
                    .clip(CircleShape)
                    .background(NeonCyan)
            )
        }
    }
}

@Composable
fun AssistantMessageCard(
    message: AssistantMessage,
    onPlayLocal: (List<Track>) -> Unit,
    onPlayOnline: (OnlineSearchResult) -> Unit,
    onOptionSelected: (String) -> Unit,
    onSpeak: (String) -> Unit
) {
    val nova = LocalNova.current
    val isUser = message.sender == "user"
    val shape = if (isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 6.dp, bottomStart = 20.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Text(
            text = if (isUser) "You" else "Nova",
            color = if (isUser) NeonCyan else NeonMagenta,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.dp).padding(bottom = 3.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .then(
                    if (isUser) {
                        Modifier
                            .novaRaised(nova, 20.dp, 3.dp)
                            .clip(shape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        lerp(nova.surface, NeonCyan, 0.28f),
                                        lerp(nova.surface, CyberPurple, 0.22f)
                                    )
                                )
                            )
                    } else {
                        Modifier
                            .clip(shape)
                            .background(Brush.verticalGradient(listOf(nova.glassTop, nova.glassBottom)))
                            .border(1.dp, Brush.verticalGradient(listOf(nova.glassEdgeTop, nova.glassEdgeBottom)), shape)
                    }
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(message.text, color = nova.text, fontSize = 13.5f.sp, lineHeight = 20.sp)

            when (val action = message.action) {
                is AssistantAction.PlayLocalTracks -> {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(13.dp))
                            .background(Brush.linearGradient(listOf(NeonCyan, lerp(NeonCyan, CyberPurple, 0.4f))))
                            .clickable { onPlayLocal(action.tracks) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Play all (${action.tracks.size})",
                            color = Ink,
                            fontSize = 12.5f.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                is AssistantAction.LocalNotFoundOfferOnline -> {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Online tracks (30s previews)",
                        color = NeonMagenta,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    action.onlineResults.take(4).forEach { online ->
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .novaInset(nova, 12.dp)
                                .clickable { onPlayOnline(online) }
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = online.artworkUrl,
                                contentDescription = online.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Brush.linearGradient(listOf(NeonCyan, CyberPurple)))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(online.title, color = nova.text, fontSize = 12.5f.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${online.artist} · 30s preview", color = NeonCyan, fontSize = 10.5f.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Default.PlayCircle, contentDescription = "Play", tint = NeonCyan, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                is AssistantAction.ClarificationNeeded -> {
                    action.options.forEach { option ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = option,
                            color = NeonCyan,
                            fontSize = 12.5f.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .novaInset(nova, 13.dp)
                                .clickable { onOptionSelected(option) }
                                .padding(vertical = 9.dp, horizontal = 8.dp)
                        )
                    }
                }
                is AssistantAction.ControlAction -> {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = action.message,
                        color = NeonCyan,
                        fontSize = 10.5f.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan.copy(alpha = 0.16f))
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
                else -> Unit
            }

            if (!isUser) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(nova.text.copy(alpha = 0.07f))
                        .clickable { onSpeak(message.text) }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                    Text("Speak", color = NeonCyan, fontSize = 10.5f.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
