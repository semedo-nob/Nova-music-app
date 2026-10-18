package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LocalNova
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.delay

@Composable
fun NovaSplash(onFinished: () -> Unit) {
    val nova = LocalNova.current
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, tween(500))
        delay(900)
        onFinished()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(nova.bg)
            .clickable(onClick = onFinished),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(alpha.value)
        ) {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonCyan, CyberPurple))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color(0xFF1A1727),
                    modifier = Modifier.size(52.dp)
                )
            }
            Spacer(Modifier.height(22.dp))
            Text("Nova", color = nova.text, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Text("Your music", color = nova.textDim, fontSize = 15.sp)
        }
    }
}
