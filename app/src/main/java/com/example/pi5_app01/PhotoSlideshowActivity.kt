package com.example.pi5_app01

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
    
    // Directory picker launcher with callback
    var directorySelectedCallback: ((Uri) -> Unit)? = null
    
    val directoryPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            // Take persistable URI permission
            try {
                contentResolver.takePersistableUriPermission(
                    selectedUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                // Invoke callback if set
                directorySelectedCallback?.invoke(selectedUri)
            } catch (e: Exception) {
                e.printStackTrace()
                // Even if permission fails, try to use the URI
                directorySelectedCallback?.invoke(selectedUri)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Hide system bars for full screen
        hideSystemBars()
        
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
                var onDirectorySelectedCallback: ((Uri) -> Unit)? = null
                
                PhotoSlideshowScreen(
                    onPickDirectory = {
                        directoryPickerLauncher.launch(null)
                    },
                    getPathFromUri = { uri -> getPathFromUri(uri) },
                    onDirectorySelected = { uri ->
                        onDirectorySelectedCallback?.invoke(uri)
                    },
                    setDirectoryCallback = { callback ->
                        directorySelectedCallback = callback
                        onDirectorySelectedCallback = callback
                    },
                    onToggleSystemUI = { show ->
                        if (show) {
                            showSystemBars()
                        } else {
                            hideSystemBars()
                        }
                    }
                )
            }
        }
    }
    
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    
    private fun showSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
    }

    override fun onDestroy() {
        super.onDestroy()
        slideshowRunnable?.let { handler.removeCallbacks(it) }
    }
    
    // Helper function to convert URI to file path
    fun getPathFromUri(uri: Uri): String? {
        var result: String? = null
        try {
            if (DocumentsContract.isDocumentUri(this, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":")
                if (split.size >= 2) {
                    val type = split[0]
                    val path = split[1]
                    
                    if ("primary".equals(type, ignoreCase = true)) {
                        result = "/storage/emulated/0/$path"
                    } else {
                        result = "/storage/$type/$path"
                    }
                }
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {
                // Try to get display name or data path
                var cursor: Cursor? = null
                try {
                    val projection = arrayOf(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    )
                    cursor = contentResolver.query(uri, projection, null, null, null)
                    cursor?.let {
                        if (it.moveToFirst()) {
                            val displayNameIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                            if (displayNameIndex >= 0) {
                                val displayName = it.getString(displayNameIndex)
                                // Return a readable path
                                result = displayName
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    cursor?.close()
                }
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                result = uri.path
            }
            
            // If we still don't have a result, return a readable representation
            if (result == null) {
                // Extract folder name from URI for display
                val uriString = uri.toString()
                val segments = uriString.split("/")
                if (segments.isNotEmpty()) {
                    result = segments.lastOrNull() ?: "Selected Folder"
                } else {
                    result = "Selected Folder"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            result = "Selected Folder" // Fallback
        }
        return result
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

// Helper function to load photos from a directory path (for traditional file paths)
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

// Helper function to load photos from a URI (for Storage Access Framework)
fun loadPhotosFromUri(context: Context, uri: Uri): List<Uri> {
    val photoUris = mutableListOf<Uri>()
    try {
        val documentFile = DocumentFile.fromTreeUri(context, uri)
        documentFile?.listFiles()?.forEach { file ->
            if (file.isFile) {
                val mimeType = file.type ?: ""
                val fileName = file.name ?: ""
                val extension = fileName.substringAfterLast('.', "").lowercase()
                
                if (extension in listOf("jpg", "jpeg", "png", "gif", "webp") ||
                    mimeType.startsWith("image/")) {
                    file.uri?.let { photoUris.add(it) }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return photoUris.sortedBy { it.toString() }
}

@Composable
fun PhotoSlideshowScreen(
    onPickDirectory: () -> Unit = {},
    getPathFromUri: (Uri) -> String? = { null },
    onDirectorySelected: (Uri) -> Unit = {},
    setDirectoryCallback: ((Uri) -> Unit) -> Unit = {},
    onToggleSystemUI: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activity = context as? PhotoSlideshowActivity
    
    var showSettings by remember { mutableStateOf(true) }
    var photoPath by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var intervalSeconds by remember { mutableStateOf(5) }
    var transitionStyle by remember { mutableStateOf(TransitionStyle.FADE) }
    var photos by remember { mutableStateOf<List<File>>(emptyList()) }
    var photoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var currentIndex by remember { mutableIntStateOf(0) }
    
    // Set up callback for directory selection
    LaunchedEffect(Unit) {
        setDirectoryCallback { uri ->
            // Store the URI for direct access
            photoUri = uri
            // Also convert to path for display
            val path = activity?.getPathFromUri(uri) ?: getPathFromUri(uri)
            path?.let {
                photoPath = it
            }
        }
    }
    
    // Load settings
    LaunchedEffect(Unit) {
        val prefs = context.dataStore.data.first()
        photoPath = prefs[stringPreferencesKey("photo_path")] ?: ""
        intervalSeconds = prefs[stringPreferencesKey("interval")]?.toIntOrNull() ?: 5
        transitionStyle = TransitionStyle.valueOf(
            prefs[stringPreferencesKey("transition")] ?: "FADE"
        )
        
        // Try to load photos from saved path
        if (photoPath.isNotEmpty()) {
            val loadedPhotos = loadPhotos(photoPath)
            if (loadedPhotos != null && loadedPhotos.isNotEmpty()) {
                photos = loadedPhotos
                showSettings = false
            }
        }
    }
    
    if (showSettings) {
        SettingsScreen(
            photoPath = photoPath,
            intervalSeconds = intervalSeconds,
            transitionStyle = transitionStyle,
            onPhotoPathChange = { newPath ->
                photoPath = newPath
            },
            onPhotoUriChange = { uri ->
                photoUri = uri
            },
            onIntervalChange = { intervalSeconds = it },
            onTransitionChange = { transitionStyle = it },
            onPickDirectory = {
                activity?.directoryPickerLauncher?.launch(null) ?: onPickDirectory()
            },
            getPathFromUri = { uri ->
                activity?.getPathFromUri(uri) ?: getPathFromUri(uri)
            },
            onStart = {
                scope.launch {
                    // Save settings
                    context.dataStore.edit { prefs ->
                        prefs[stringPreferencesKey("photo_path")] = photoPath
                        prefs[stringPreferencesKey("interval")] = intervalSeconds.toString()
                        prefs[stringPreferencesKey("transition")] = transitionStyle.name
                    }
                }
                
                // Load photos - try URI first (for SAF), then fallback to path
                val loadedPhotoUris = if (photoUri != null) {
                    loadPhotosFromUri(context, photoUri!!)
                } else {
                    emptyList()
                }
                
                // Also try loading from path as fallback
                val loadedPhotos = if (loadedPhotoUris.isEmpty() && photoPath.isNotEmpty()) {
                    loadPhotos(photoPath)
                } else {
                    null
                }
                
                // Update state immediately to trigger recomposition
                if (loadedPhotoUris.isNotEmpty()) {
                    photoUris = loadedPhotoUris
                    photos = emptyList() // Clear file-based photos
                } else if (loadedPhotos != null && loadedPhotos.isNotEmpty()) {
                    photos = loadedPhotos
                    photoUris = emptyList() // Clear URI-based photos
                } else {
                    // No photos found - show slideshow anyway with empty list
                    photos = emptyList()
                    photoUris = emptyList()
                }
                // Always hide settings to show slideshow
                showSettings = false
                // Hide system bars when slideshow starts
                onToggleSystemUI(false)
            }
        )
    } else {
        SlideshowView(
            photos = photos,
            photoUris = photoUris,
            currentIndex = currentIndex,
            onIndexChange = { currentIndex = it },
            intervalSeconds = intervalSeconds,
            transitionStyle = transitionStyle,
            onBackToSettings = { 
                showSettings = true
                onToggleSystemUI(true) // Show system UI when going back to settings
            },
            onToggleSystemUI = onToggleSystemUI
        )
    }
}

@Composable
fun SettingsScreen(
    photoPath: String,
    intervalSeconds: Int,
    transitionStyle: TransitionStyle,
    onPhotoPathChange: (String) -> Unit,
    onPhotoUriChange: (Uri) -> Unit = {},
    onIntervalChange: (Int) -> Unit,
    onTransitionChange: (TransitionStyle) -> Unit,
    onPickDirectory: () -> Unit,
    getPathFromUri: (Uri) -> String?,
    onStart: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? PhotoSlideshowActivity
    
    // Set up callback to receive directory selection - set it immediately
    // Use DisposableEffect to ensure cleanup and proper setup
    DisposableEffect(activity, onPhotoPathChange) {
        val callback: (Uri) -> Unit = { uri ->
            // Store the URI for direct access
            onPhotoUriChange(uri)
            // Also convert to path for display
            val path = activity?.getPathFromUri(uri) ?: getPathFromUri(uri)
            val displayPath = if (path != null && path.isNotEmpty()) {
                path
            } else {
                // Fallback: show folder name or URI
                try {
                    val uriString = uri.toString()
                    val segments = uriString.split("/")
                    segments.lastOrNull()?.takeIf { it.isNotEmpty() } 
                        ?: "Selected Folder"
                } catch (e: Exception) {
                    "Selected Folder"
                }
            }
            onPhotoPathChange(displayPath)
        }
        
        // Set the callback immediately
        activity?.directorySelectedCallback = callback
        
        // Cleanup on dispose
        onDispose {
            // Optionally clear callback, but we might want to keep it
            // activity?.directorySelectedCallback = null
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Photo Path Selection - Now opens file picker
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                onPickDirectory()
                // We'll handle the result via the activity's launcher
            }
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
        
        // Add spacing before button
        Spacer(modifier = Modifier.height(24.dp))
        
        // Start Button - scrollable, always accessible
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            enabled = photoPath.isNotEmpty()
        ) {
            Text("Start Slideshow", fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
        
        // Extra bottom padding for better scrolling
        Spacer(modifier = Modifier.height(16.dp))
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
    photoUris: List<Uri>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    intervalSeconds: Int,
    transitionStyle: TransitionStyle,
    onBackToSettings: () -> Unit,
    onToggleSystemUI: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var showSettingsButton by remember { mutableStateOf(true) }
    var systemUIVisible by remember { mutableStateOf(false) }
    var lastTapTime by remember { mutableStateOf(0L) }
    
    // Determine which list to use and total count
    val totalPhotos = if (photoUris.isNotEmpty()) photoUris.size else photos.size
    val hasPhotos = totalPhotos > 0
    
    // Auto-advance slideshow
    LaunchedEffect(currentIndex, intervalSeconds, totalPhotos) {
        if (hasPhotos) {
            kotlinx.coroutines.delay(intervalSeconds * 1000L)
            onIndexChange((currentIndex + 1) % totalPhotos)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPhotos) {
            if (photoUris.isNotEmpty()) {
                // Use URI-based photos
                PhotoWithTransitionUri(
                    photoUri = photoUris[currentIndex],
                    transitionStyle = transitionStyle,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Use File-based photos
                PhotoWithTransition(
                    photo = photos[currentIndex],
                    transitionStyle = transitionStyle,
                    modifier = Modifier.fillMaxSize()
                )
            }
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
    
    // Double tap to toggle system UI, single tap to toggle settings button
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        systemUIVisible = !systemUIVisible
                        onToggleSystemUI(systemUIVisible)
                    },
                    onTap = {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastTapTime < 300) {
                            // Double tap detected
                            systemUIVisible = !systemUIVisible
                            onToggleSystemUI(systemUIVisible)
                        } else {
                            // Single tap
                            showSettingsButton = !showSettingsButton
                        }
                        lastTapTime = currentTime
                    }
                )
            }
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
            AnimatedContent(
                targetState = photo,
                transitionSpec = {
                    fadeIn(animationSpec = tween(1000)) togetherWith
                    fadeOut(animationSpec = tween(1000))
                },
                modifier = modifier
            ) { targetPhoto ->
                // Animate rotation from 0 to 360 when photo changes
                // Using remember with key to reset animation for each new photo
                val rotation by remember(targetPhoto) {
                    mutableFloatStateOf(0f)
                }
                
                val animatedRotation by animateFloatAsState(
                    targetValue = 360f,
                    animationSpec = tween(1000, easing = LinearEasing),
                    label = "rotation"
                )
                
                PhotoImage(
                    photo = targetPhoto,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = animatedRotation
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
fun PhotoImageUri(photoUri: Uri, modifier: Modifier = Modifier) {
    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(LocalContext.current)
            .data(photoUri)
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
fun PhotoWithTransitionUri(
    photoUri: Uri,
    transitionStyle: TransitionStyle,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    when (transitionStyle) {
        TransitionStyle.FADE -> {
            AnimatedContent(
                targetState = photoUri,
                transitionSpec = {
                    fadeIn(animationSpec = tween(1000)) togetherWith
                    fadeOut(animationSpec = tween(1000))
                },
                modifier = modifier
            ) { targetUri ->
                PhotoImageUri(photoUri = targetUri, modifier = Modifier.fillMaxSize())
            }
        }
        TransitionStyle.SLIDE_LEFT -> {
            AnimatedContent(
                targetState = photoUri,
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
            ) { targetUri ->
                PhotoImageUri(photoUri = targetUri, modifier = Modifier.fillMaxSize())
            }
        }
        TransitionStyle.SLIDE_RIGHT -> {
            AnimatedContent(
                targetState = photoUri,
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
            ) { targetUri ->
                PhotoImageUri(photoUri = targetUri, modifier = Modifier.fillMaxSize())
            }
        }
        TransitionStyle.SLIDE_UP -> {
            AnimatedContent(
                targetState = photoUri,
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
            ) { targetUri ->
                PhotoImageUri(photoUri = targetUri, modifier = Modifier.fillMaxSize())
            }
        }
        TransitionStyle.SLIDE_DOWN -> {
            AnimatedContent(
                targetState = photoUri,
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
            ) { targetUri ->
                PhotoImageUri(photoUri = targetUri, modifier = Modifier.fillMaxSize())
            }
        }
        TransitionStyle.ROTATE -> {
            AnimatedContent(
                targetState = photoUri,
                transitionSpec = {
                    fadeIn(animationSpec = tween(1000)) togetherWith
                    fadeOut(animationSpec = tween(1000))
                },
                modifier = modifier
            ) { targetUri ->
                val rotation by animateFloatAsState(
                    targetValue = 360f,
                    animationSpec = remember(targetUri) {
                        tween(1000, easing = LinearEasing)
                    },
                    label = "rotation"
                )
                
                PhotoImageUri(
                    photoUri = targetUri,
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
fun ClockOverlay(modifier: Modifier = Modifier) {
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    var currentTime by remember { mutableStateOf(Date()) }
    
    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime = Date()
        }
    }
    
    // Single common glassmorphic background container
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
        Column(
            horizontalAlignment = Alignment.End
        ) {
            // Date - plain text, no individual backgrounds
            Text(
                text = dateFormat.format(currentTime),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Time - plain text, no individual backgrounds
            Text(
                text = timeFormat.format(currentTime),
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge
            )
        }
    }
}

@Composable
fun GlassmorphicBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.25f),
                        Color.White.copy(alpha = 0.2f)
                    )
                )
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        content()
    }
}
