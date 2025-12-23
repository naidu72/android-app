package com.example.pi5_app01

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.pi5_app01.ui.theme.Pi5app01Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// DataStore for settings
val Context.dataStore by preferencesDataStore(name = "slideshow_settings")

class PhotoSlideshowActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var slideshowRunnable: Runnable? = null
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, continue
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Request permission if needed
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        }
        
        setContent {
            Pi5app01Theme {
                PhotoSlideshowScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        slideshowRunnable?.let { handler.removeCallbacks(it) }
    }
}

enum class TransitionStyle {
    FADE,
    SLIDE_LEFT,
    SLIDE_RIGHT,
    SLIDE_UP,
    SLIDE_DOWN,
    ROTATE
}

@Composable
fun PhotoSlideshowScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var showSettings by remember { mutableStateOf(true) }
    var photoPath by remember { mutableStateOf("") }
    var intervalSeconds by remember { mutableStateOf(5) }
    var transitionStyle by remember { mutableStateOf(TransitionStyle.FADE) }
    var photos by remember { mutableStateOf<List<File>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    
    // Load settings
    LaunchedEffect(Unit) {
        val prefs = context.dataStore.data.first()
        photoPath = prefs[stringPreferencesKey("photo_path")] ?: ""
        intervalSeconds = prefs[stringPreferencesKey("interval")]?.toIntOrNull() ?: 5
        transitionStyle = TransitionStyle.valueOf(
            prefs[stringPreferencesKey("transition")] ?: "FADE"
        )
        
        if (photoPath.isNotEmpty()) {
            loadPhotos(photoPath)?.let {
                photos = it
                showSettings = false
            }
        }
    }
    
    if (showSettings) {
        SettingsScreen(
            photoPath = photoPath,
            intervalSeconds = intervalSeconds,
            transitionStyle = transitionStyle,
            onPhotoPathChange = { photoPath = it },
            onIntervalChange = { intervalSeconds = it },
            onTransitionChange = { transitionStyle = it },
            onStart = {
                scope.launch {
                    // Save settings
                    context.dataStore.edit { prefs ->
                        prefs[stringPreferencesKey("photo_path")] = photoPath
                        prefs[stringPreferencesKey("interval")] = intervalSeconds.toString()
                        prefs[stringPreferencesKey("transition")] = transitionStyle.name
                    }
                    
                    // Load photos
                    loadPhotos(photoPath)?.let {
                        photos = it
                        showSettings = false
                    }
                }
            }
        )
    } else {
        SlideshowView(
            photos = photos,
            currentIndex = currentIndex,
            onIndexChange = { currentIndex = it },
            intervalSeconds = intervalSeconds,
            transitionStyle = transitionStyle,
            onBackToSettings = { showSettings = true }
        )
    }
}

@Composable
fun SettingsScreen(
    photoPath: String,
    intervalSeconds: Int,
    transitionStyle: TransitionStyle,
    onPhotoPathChange: (String) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onTransitionChange: (TransitionStyle) -> Unit,
    onStart: () -> Unit
) {
    val context = LocalContext.current
    var showPathDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Photo Slideshow Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Photo Path Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showPathDialog = true }
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Photo Folder",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (photoPath.isEmpty()) "Tap to select folder" else photoPath,
                    fontSize = 14.sp,
                    color = if (photoPath.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                           else MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Interval Selection
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Change Interval: ${intervalSeconds} seconds",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3, 5, 10, 15, 30).forEach { seconds ->
                        FilterChip(
                            selected = intervalSeconds == seconds,
                            onClick = { onIntervalChange(seconds) },
                            label = { Text("${seconds}s") }
                        )
                    }
                }
            }
        }
        
        // Transition Style Selection
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Transition Style",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransitionStyle.values().forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTransitionChange(style) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = transitionStyle == style,
                                onClick = { onTransitionChange(style) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = style.name.replace("_", " ").lowercase()
                                    .replaceFirstChar { it.uppercase() },
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Start Button
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            enabled = photoPath.isNotEmpty()
        ) {
            Text("Start Slideshow", fontSize = 18.sp, modifier = Modifier.padding(8.dp))
        }
    }
    
    if (showPathDialog) {
        PathInputDialog(
            currentPath = photoPath,
            onPathEntered = {
                onPhotoPathChange(it)
                showPathDialog = false
            },
            onDismiss = { showPathDialog = false }
        )
    }
}

@Composable
fun PathInputDialog(
    currentPath: String,
    onPathEntered: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pathText by remember { mutableStateOf(currentPath) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Photo Folder Path") },
        text = {
            Column {
                Text("Example: /storage/emulated/0/Pictures or /sdcard/DCIM/Camera")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pathText,
                    onValueChange = { pathText = it },
                    label = { Text("Folder Path") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onPathEntered(pathText) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SlideshowView(
    photos: List<File>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    intervalSeconds: Int,
    transitionStyle: TransitionStyle,
    onBackToSettings: () -> Unit
) {
    val context = LocalContext.current
    var showSettingsButton by remember { mutableStateOf(true) }
    
    // Auto-advance slideshow
    LaunchedEffect(currentIndex, intervalSeconds) {
        if (photos.isNotEmpty()) {
            kotlinx.coroutines.delay(intervalSeconds * 1000L)
            onIndexChange((currentIndex + 1) % photos.size)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (photos.isNotEmpty()) {
            PhotoWithTransition(
                photo = photos[currentIndex],
                transitionStyle = transitionStyle,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No photos found",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
        
        // Clock overlay (top right)
        ClockOverlay(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(24.dp)
        )
        
        // Settings button (top left, appears on tap)
        if (showSettingsButton) {
            FloatingActionButton(
                onClick = onBackToSettings,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text("⚙️", fontSize = 20.sp)
            }
        }
    }
    
    // Hide/show settings button on tap
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { showSettingsButton = !showSettingsButton }
    )
}

@Composable
fun PhotoWithTransition(
    photo: File,
    transitionStyle: TransitionStyle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    when (transitionStyle) {
        TransitionStyle.FADE -> {
            AnimatedContent(
                targetState = photo,
                transitionSpec = {
                    fadeIn(animationSpec = tween(1000)) togetherWith
                    fadeOut(animationSpec = tween(1000))
                },
                modifier = modifier
            ) { targetPhoto ->
                PhotoImage(photo = targetPhoto, modifier = Modifier.fillMaxSize())
            }
        }
        TransitionStyle.SLIDE_LEFT -> {
            AnimatedContent(
                targetState = photo,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(1000)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(1000)
                    )
                },
                modifier = modifier
            ) { targetPhoto ->
                PhotoImage(photo = targetPhoto, modifier = Modifier.fillMaxSize())
            }
        }
        TransitionStyle.SLIDE_RIGHT -> {
            AnimatedContent(
                targetState = photo,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(1000)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(1000)
                    )
                },
                modifier = modifier
            ) { targetPhoto ->
                PhotoImage(photo = targetPhoto, modifier = Modifier.fillMaxSize())
            }
        }
        TransitionStyle.SLIDE_UP -> {
            AnimatedContent(
                targetState = photo,
                transitionSpec = {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(1000)
                    ) togetherWith slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(1000)
                    )
                },
                modifier = modifier
            ) { targetPhoto ->
                PhotoImage(photo = targetPhoto, modifier = Modifier.fillMaxSize())
            }
        }
        TransitionStyle.SLIDE_DOWN -> {
            AnimatedContent(
                targetState = photo,
                transitionSpec = {
                    slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = tween(1000)
                    ) togetherWith slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(1000)
                    )
                },
                modifier = modifier
            ) { targetPhoto ->
                PhotoImage(photo = targetPhoto, modifier = Modifier.fillMaxSize())
            }
        }
        TransitionStyle.ROTATE -> {
            var rotation by remember { mutableFloatStateOf(0f) }
            
            LaunchedEffect(photo) {
                rotation = 0f
                animateFloatAsState(
                    targetValue = 360f,
                    animationSpec = tween(1000, easing = LinearEasing)
                )
            }
            
            AnimatedContent(
                targetState = photo,
                transitionSpec = {
                    fadeIn(animationSpec = tween(1000)) togetherWith
                    fadeOut(animationSpec = tween(1000))
                },
                modifier = modifier
            ) { targetPhoto ->
                PhotoImage(
                    photo = targetPhoto,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = rotation
                        }
                )
            }
        }
    }
}

@Composable
fun PhotoImage(photo: File, modifier: Modifier = Modifier) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(photo)
            .build()
    )
    
    Image(
        painter = painter,
        contentDescription = "Slideshow photo",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}

@Composable
fun ClockOverlay(modifier: Modifier = Modifier) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var currentTime by remember { mutableStateOf(Date()) }
    
    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime = Date()
        }
    }
    
    // iOS 26 glass style clock with glassmorphism effect
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.2f)
                    )
                )
            )
            .padding(horizontal = 28.dp, vertical = 18.dp)
    ) {
        Text(
            text = timeFormat.format(currentTime),
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineLarge
        )
    }
}

fun loadPhotos(path: String): List<File>? {
    return try {
        val directory = File(path)
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()
                ?.filter { it.isFile && it.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "webp") }
                ?.sortedBy { it.name }
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

