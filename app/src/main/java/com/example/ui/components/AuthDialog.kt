package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.NeonCyan
import com.example.ui.viewmodel.MusicViewModel
import java.io.File

@Composable
fun AuthModalDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val nova = LocalNova.current
    val user by viewModel.currentUser.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    var name by remember(user?.id) { mutableStateOf(if (isLoggedIn) user?.displayName.orEmpty() else "") }
    var photoPath by remember(user?.id) { mutableStateOf(user?.photoUrl) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            photoPath = copyProfilePhoto(context, uri) ?: photoPath
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        NovaGlassCard(corner = 26.dp, glow = NeonCyan, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isLoggedIn) name.ifBlank { "Your profile" } else "Your name",
                    color = nova.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isLoggedIn) "This is how Nova addresses you" else "Add a photo and the name you want to use",
                    color = nova.textDim,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(NeonCyan, CyberPurple)))
                        .clickable {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (!photoPath.isNullOrBlank()) {
                        AsyncImage(
                            model = profileImageModel(photoPath),
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }
                Text(
                    "Photo",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = TextStyle(color = nova.text, fontSize = 16.sp),
                    cursorBrush = SolidColor(NeonCyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .novaInset(nova, 14.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    decorationBox = { inner ->
                        Box {
                            if (name.isEmpty()) Text("Preferred name", color = nova.textDim, fontSize = 16.sp)
                            inner()
                        }
                    }
                )
                Spacer(Modifier.height(18.dp))
                NovaButton(
                    text = if (isLoggedIn) "Save" else "Continue",
                    accent = NeonCyan,
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Enter the name you want to use", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.saveProfile(name, photoPath)
                            onDismiss()
                        }
                    }
                )
                if (isLoggedIn) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sign out",
                        color = nova.textDim,
                        modifier = Modifier
                            .clickable {
                                viewModel.signOut()
                                onDismiss()
                            }
                            .padding(10.dp)
                    )
                }
            }
        }
    }
}

private fun profileImageModel(path: String?): Any? {
    if (path.isNullOrBlank()) return null
    return if (path.startsWith("/")) File(path) else path
}

private fun copyProfilePhoto(context: android.content.Context, uri: Uri): String? {
    return try {
        val dest = File(context.filesDir, "profile.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        dest.absolutePath
    } catch (_: Exception) {
        null
    }
}
