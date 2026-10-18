package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.auth.UserProfile
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel

@Composable
fun AuthModalDialog(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    var authMode by remember { mutableStateOf(0) } // 0: Google / Fast, 1: Email Sign In, 2: Sign Up, 3: Edit Profile
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var bioInput by remember { mutableStateOf(currentUser?.bio ?: "") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val nova = LocalNova.current
        val panel = RoundedCornerShape(28.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .novaRaised(nova, 28.dp, 6.dp)
                .clip(panel)
                .background(Brush.verticalGradient(listOf(nova.glassTop, nova.surface.copy(alpha = 0.94f), nova.glassBottom)))
                .border(1.dp, Brush.verticalGradient(listOf(nova.glassEdgeTop, nova.glassEdgeBottom)), panel)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isLoggedIn && authMode != 3) "Cyber ID Deck" else "Nova Authorization",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnDarkText
                        )
                        Text(
                            text = "Global SharedPreferences Credentials",
                            fontSize = 11.sp,
                            color = SoftCyan
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = OnDarkText, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // If already logged in and not in edit mode, display User Profile Dashboard
                if (isLoggedIn && authMode != 3) {
                    val user = currentUser
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // User Avatar with VIP Halo
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 2.dp,
                                    brush = Brush.sweepGradient(listOf(GeminiBlue, CyberPurple, NeonPink, GeminiBlue)),
                                    shape = CircleShape
                                )
                                .padding(4.dp)
                                .clip(CircleShape)
                        ) {
                            if (!user?.photoUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(user?.photoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "User Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(listOf(GeminiBlue, CyberPurple))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = user?.displayName ?: "Nova Pilot",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnDarkText
                        )
                        Text(
                            text = user?.email ?: "pilot@novamusic.space",
                            fontSize = 12.sp,
                            color = OnDarkTextSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Provider Badge & VIP status
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x2238BDF8))
                                    .border(1.dp, GeminiBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (user?.provider == "google.com") Icons.Default.AccountCircle else Icons.Default.Email,
                                        contentDescription = null,
                                        tint = GeminiBlue,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (user?.provider == "google.com") "Google Account" else "Email Auth",
                                        fontSize = 10.sp,
                                        color = GeminiBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonPink.copy(alpha = 0.2f))
                                    .border(1.dp, NeonPink.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (user?.isPremium == true) "VIP Cyber Hi-Res" else "Standard Tier",
                                    fontSize = 10.sp,
                                    color = NeonPink,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bio Quote
                        if (!user?.bio.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x18000000))
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "\"${user?.bio}\"",
                                    fontSize = 12.sp,
                                    color = OnDarkTextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Premium VIP Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x15FFFFFF))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Lossless 24-bit Flac & DSP", color = OnDarkText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Saved globally in SharedPreferences", color = OnDarkTextSecondary, fontSize = 10.sp)
                            }
                            Switch(
                                checked = user?.isPremium == true,
                                onCheckedChange = { viewModel.toggleVipStatus(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonPink,
                                    checkedTrackColor = NeonPink.copy(alpha = 0.3f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons: Edit Profile & Sign Out
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NeumorphicButton(
                                onClick = {
                                    nameInput = user?.displayName ?: ""
                                    bioInput = user?.bio ?: ""
                                    authMode = 3
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = SoftCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Profile", color = SoftCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            NeumorphicButton(
                                onClick = {
                                    viewModel.signOut()
                                    Toast.makeText(context, "Signed out. Session cleared from SharedPreferences.", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = NeonPink, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out", color = NeonPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (authMode == 3) {
                    // Edit Profile Screen
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = nova.hairline,
                                focusedTextColor = nova.text,
                                unfocusedTextColor = nova.text,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = nova.textDim,
                                cursorColor = NeonCyan
                            )
                        )

                        OutlinedTextField(
                            value = bioInput,
                            onValueChange = { bioInput = it },
                            label = { Text("Status / Bio") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = nova.hairline,
                                focusedTextColor = nova.text,
                                unfocusedTextColor = nova.text,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = nova.textDim,
                                cursorColor = NeonCyan
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextButton(
                                onClick = { authMode = 0 },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = OnDarkTextSecondary)
                            }

                            NeumorphicButton(
                                onClick = {
                                    if (nameInput.isNotBlank()) {
                                        viewModel.updateProfile(nameInput, bioInput)
                                        Toast.makeText(context, "Profile updated in SharedPreferences! 🚀", Toast.LENGTH_SHORT).show()
                                        authMode = 0
                                    }
                                },
                                isAccent = true,
                                modifier = Modifier.weight(1.5f),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    // Sign In / Sign Up Interface
                    IOSSegmentedControl(
                        items = listOf("Google Auth", "Email Sign In", "New Account"),
                        selectedIndex = authMode,
                        onItemSelected = { authMode = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    when (authMode) {
                        0 -> {
                            // Google Fast Sign In
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Sign in using your Google Account for instant cloud-synced playlists, AI presets, and audio preferences.",
                                    fontSize = 12.sp,
                                    color = OnDarkTextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Google Sign-in Button with G Logo
                                NeumorphicButton(
                                    onClick = {
                                        // Sign in with Google user credentials
                                        val googleName = "Alex Vance"
                                        val googleEmail = "alex.vance@gmail.com"
                                        val avatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&q=80"
                                        viewModel.signInWithGoogle(googleName, googleEmail, avatar)
                                        Toast.makeText(context, "Signed in as $googleEmail via Google! Stored to SharedPreferences.", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    isAccent = false,
                                    contentPadding = PaddingValues(vertical = 14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GTranslate,
                                        contentDescription = null,
                                        tint = SoftCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Continue with Google",
                                        color = OnDarkText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Alternative Quick Google Account Option
                                NeumorphicButton(
                                    onClick = {
                                        val googleName = "Elena Rostova"
                                        val googleEmail = "elena.sound@gmail.com"
                                        val avatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&q=80"
                                        viewModel.signInWithGoogle(googleName, googleEmail, avatar)
                                        Toast.makeText(context, "Switched to $googleEmail! Stored to SharedPreferences.", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = GeminiBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Switch to Elena (Google)", color = GeminiBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        1 -> {
                            // Email Sign In
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Email Address") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = nova.hairline,
                                        focusedTextColor = nova.text,
                                        unfocusedTextColor = nova.text,
                                        focusedLabelColor = NeonCyan,
                                        unfocusedLabelColor = nova.textDim,
                                        cursorColor = NeonCyan
                                    )
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text("Password") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonCyan,
                                        unfocusedBorderColor = nova.hairline,
                                        focusedTextColor = nova.text,
                                        unfocusedTextColor = nova.text,
                                        focusedLabelColor = NeonCyan,
                                        unfocusedLabelColor = nova.textDim,
                                        cursorColor = NeonCyan
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                NeumorphicButton(
                                    onClick = {
                                        if (emailInput.isNotBlank()) {
                                            viewModel.signInWithEmail(emailInput)
                                            Toast.makeText(context, "Welcome back! Credentials saved globally in SharedPreferences.", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    isAccent = true,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Sign In to Terminal", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        2 -> {
                            // New Account Sign Up
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Full Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberPurple,
                                        unfocusedBorderColor = nova.hairline,
                                        focusedTextColor = nova.text,
                                        unfocusedTextColor = nova.text,
                                        focusedLabelColor = CyberPurple,
                                        unfocusedLabelColor = nova.textDim,
                                        cursorColor = CyberPurple
                                    )
                                )

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Email Address") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberPurple,
                                        unfocusedBorderColor = nova.hairline,
                                        focusedTextColor = nova.text,
                                        unfocusedTextColor = nova.text,
                                        focusedLabelColor = CyberPurple,
                                        unfocusedLabelColor = nova.textDim,
                                        cursorColor = CyberPurple
                                    )
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text("Create Password") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = CyberPurple,
                                        unfocusedBorderColor = nova.hairline,
                                        focusedTextColor = nova.text,
                                        unfocusedTextColor = nova.text,
                                        focusedLabelColor = CyberPurple,
                                        unfocusedLabelColor = nova.textDim,
                                        cursorColor = CyberPurple
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                NeumorphicButton(
                                    onClick = {
                                        if (nameInput.isNotBlank() && emailInput.isNotBlank()) {
                                            viewModel.signUpWithEmail(nameInput, emailInput, "Holographic audio explorer.")
                                            Toast.makeText(context, "Account registered! Saved to SharedPreferences globally.", Toast.LENGTH_LONG).show()
                                            onDismiss()
                                        } else {
                                            Toast.makeText(context, "Please fill in your name and email", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    isAccent = true,
                                    glowColor = CyberPurple,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Create Holographic Account", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
