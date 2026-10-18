package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.LocalNova
import com.example.ui.components.MiniPlayer
import com.example.ui.components.NovaBackdrop
import com.example.ui.components.NovaDark
import com.example.ui.components.NovaLight
import com.example.ui.components.NowPlayingScreen
import com.example.ui.components.novaRaised
import com.example.ui.screens.AIdjScreen
import com.example.ui.screens.DjMixerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NovaSplash
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MusicViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                CompositionLocalProvider(LocalNova provides if (isDarkMode) NovaDark else NovaLight) {
                    var showSplash by rememberSaveable { mutableStateOf(true) }
                    if (showSplash) {
                        NovaSplash(onFinished = { showSplash = false })
                    } else {
                        NovaAppFrame(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NovaAppFrame(viewModel: MusicViewModel = viewModel()) {
    val context = LocalContext.current
    val pages = listOf("home", "library", "aidj", "settings")
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    val currentTrack by viewModel.currentTrack.collectAsState()
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var showMixer by remember { mutableStateOf(false) }

    // Android 13+ Notification Permission Handling for Native Lock-Screen and Shade Media Controls
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                context,
                "Notification permission is recommended for persistent media playback controls on the lock screen and notification shade.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (status != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Accelerometer Shake Detection to Skip Tracks
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShakeTime = 0L

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                // Use the net acceleration force excluding gravity
                val gravity = 9.81f
                val force = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                val diff = Math.abs(force - gravity)
                val now = System.currentTimeMillis()

                // A loose threshold was skipping tracks from ordinary movement, which felt like playback stopping.
                if (diff > 18.0f && viewModel.isPlaying.value) {
                    if (now - lastShakeTime > 2500) {
                        lastShakeTime = now
                        viewModel.next()
                        Toast.makeText(context, "Shake Detected: Skipping to next track! 📳", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NovaBackdrop(accent = NeonCyan, modifier = Modifier.fillMaxSize())

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent, // transparent to let glowing backdrops blend
            bottomBar = {
                // Floating navigation and MiniPlayer container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(), // protects from bottom system navigation pill
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Mini Player docked at bottom
                    AnimatedVisibility(
                        visible = currentTrack != null && !isPlayerExpanded && !showMixer,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        MiniPlayer(
                            viewModel = viewModel,
                            onExpand = { isPlayerExpanded = true }
                        )
                    }

                    PageControl(
                        count = pages.size,
                        selected = pagerState.currentPage,
                        onSelect = { page ->
                            scope.launch { pagerState.animateScrollToPage(page) }
                        }
                    )

                    FloatingNavBar(
                        activeRoute = pages[pagerState.currentPage],
                        onNavigate = { route ->
                            val page = pages.indexOf(route)
                            if (page >= 0) scope.launch { pagerState.animateScrollToPage(page) }
                        }
                    )
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = pages.lastIndex,
                userScrollEnabled = !isPlayerExpanded && !showMixer,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) { page ->
                when (page) {
                    0 -> HomeScreen(viewModel = viewModel)
                    1 -> LibraryScreen(
                        viewModel = viewModel,
                        onOpenMixer = {
                            isPlayerExpanded = false
                            showMixer = true
                        }
                    )
                    2 -> AIdjScreen(viewModel = viewModel)
                    else -> SettingsScreen(viewModel = viewModel)
                }
            }
        }

        // Expanded full-screen "Now Playing" Slider
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            NowPlayingScreen(
                viewModel = viewModel,
                onMinimize = { isPlayerExpanded = false }
            )
        }

        AnimatedVisibility(
            visible = showMixer,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            DjMixerScreen(
                viewModel = viewModel,
                onClose = {
                    viewModel.closeMixer()
                    showMixer = false
                }
            )
        }
    }
}

@Composable
fun FloatingNavBar(
    activeRoute: String,
    onNavigate: (String) -> Unit
) {
    val nova = LocalNova.current
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(68.dp)
            .novaRaised(nova, corner = 34.dp, depth = 8.dp)
            .clip(RoundedCornerShape(34.dp))
            .background(Brush.verticalGradient(listOf(nova.glassTop, nova.glassBottom)))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(nova.glassEdgeTop, nova.glassEdgeBottom)),
                shape = RoundedCornerShape(34.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val items = listOf(
                NavBarItem("home", "Home", Icons.Default.Home),
                NavBarItem("library", "Library", Icons.Default.LibraryMusic),
                NavBarItem("aidj", "AI DJ", Icons.Default.AutoAwesome),
                NavBarItem("settings", "Settings", Icons.Default.Settings)
            )

            items.forEach { item ->
                val active = activeRoute == item.route

                val activeBackgroundModifier = if (active) Modifier.novaRaised(nova, 20.dp, 3.dp) else Modifier

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .then(activeBackgroundModifier)
                        .border(
                            width = if (active) 1.dp else 0.dp,
                            brush = if (active) Brush.verticalGradient(listOf(Color(0x55FFFFFF), NeonCyan.copy(alpha = 0.35f))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { onNavigate(item.route) }
                        .padding(vertical = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (active) NeonCyan else OnDarkTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            fontSize = 10.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) NeonCyan else nova.textDim
                        )
                    }
                }
            }
        }
    }
}

data class NavBarItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun PageControl(
    count: Int,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { index ->
            val active = index == selected
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (active) 18.dp else 7.dp, 7.dp)
                    .clip(CircleShape)
                    .background(if (active) NeonCyan else OnDarkTextSecondary.copy(alpha = 0.45f))
                    .clickable { onSelect(index) }
            )
        }
    }
}
