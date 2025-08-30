package com.poma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import com.poma.viewmodel.AuthViewModel
import com.poma.viewmodel.VoiceRecordingViewModel
import com.poma.viewmodel.PlaybackViewModel
import com.poma.viewmodel.SpotifyViewModel
import com.poma.viewmodel.MediaSessionViewModel
import com.poma.utils.PermissionUtils
import com.poma.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    voiceRecordingViewModel: VoiceRecordingViewModel = viewModel(),
    playbackViewModel: PlaybackViewModel = viewModel(),
    spotifyViewModel: SpotifyViewModel = viewModel(),
    mediaSessionViewModel: MediaSessionViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val recordingState by voiceRecordingViewModel.recordingState.collectAsState()
    val playbackState by playbackViewModel.playbackState.collectAsState()
    val spotifyState by spotifyViewModel.spotifyState.collectAsState()
    val mediaSessionState by mediaSessionViewModel.mediaSessionState.collectAsState()
    
    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("HomeScreen", "RECORD_AUDIO permission granted")
        } else {
            android.util.Log.d("HomeScreen", "RECORD_AUDIO permission denied")
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // Top bar with logout and settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.navigate("settings") }
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFFB3B3B3) // Spotify gray
                )
            }
            
            IconButton(
                onClick = { authViewModel.signOut(context) }
            ) {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = Color(0xFFB3B3B3) // Spotify gray
                )
            }
        }
        
        // System Media Status - MediaSessionManager Integration with Background Image
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1ED760) // Spotify绿色
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Background image if available
                mediaSessionState.currentMediaInfo?.albumArtUri?.let { albumArt ->
                    android.util.Log.d("HomeScreen", "Attempting to load image: $albumArt")
                    AsyncImage(
                        model = albumArt,
                        contentDescription = "Album Art",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp) // 增加高度从120dp到160dp
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop,
                        onError = { error ->
                            android.util.Log.e("HomeScreen", "Failed to load image: $albumArt", error.result.throwable)
                        },
                        onSuccess = { 
                            android.util.Log.d("HomeScreen", "Successfully loaded image: $albumArt") 
                        }
                    )
                    // Dark overlay for text readability with Spotify colors
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF191414).copy(alpha = 0.2f), // 顶部浅Spotify深灰
                                        Color(0xFF121212).copy(alpha = 0.8f)  // 底部深Spotify黑
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                    )
                } ?: run {
                    // Fallback Spotify-themed background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp) // 增加高度
                            .background(
                                when {
                                    mediaSessionState.currentMediaInfo?.isPlaying == true -> {
                                        // 播放中：使用渐变Spotify绿色
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF1ED760).copy(alpha = 0.3f),
                                                Color(0xFF191414)
                                            )
                                        )
                                    }
                                    mediaSessionState.currentMediaInfo != null -> {
                                        // 暂停中：深灰色调
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF191414),
                                                Color(0xFF121212)
                                            )
                                        )
                                    }
                                    !mediaSessionState.isListenerEnabled -> {
                                        // 需要权限：温和橙色提示
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFFFF6B35).copy(alpha = 0.2f),
                                                Color(0xFF191414)
                                            )
                                        )
                                    }
                                    else -> {
                                        // 默认：纯Spotify深灰
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF191414),
                                                Color(0xFF121212)
                                            )
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                    )
                }
                
                // Content overlay
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                when {
                    !mediaSessionState.isListenerEnabled -> {
                        // Need permission setup
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎵 Enable Media Detection",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                ),
                                color = Color.Black // 在绿色背景上使用黑色文字
                            )
                            Button(
                                onClick = { 
                                    mediaSessionViewModel.openNotificationListenerSettings(context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Settings")
                            }
                        }
                        Text(
                            text = "Grant notification access to detect media playback",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black.copy(alpha = 0.7f), // 在绿色背景上使用半透明黑色
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    mediaSessionState.currentMediaInfo != null -> {
                        // Show current media info with real-time updates
                        val mediaInfo = mediaSessionState.currentMediaInfo!!
                        
                        // Real-time position update
                        var currentPosition by remember { mutableStateOf(mediaInfo.position) }
                        LaunchedEffect(mediaInfo.isPlaying) {
                            if (mediaInfo.isPlaying) {
                                while (mediaInfo.isPlaying) {
                                    kotlinx.coroutines.delay(1000L) // Update every second
                                    currentPosition += 1000L // Add 1 second
                                    if (currentPosition >= mediaInfo.duration) {
                                        currentPosition = mediaInfo.duration
                                        break
                                    }
                                }
                            } else {
                                currentPosition = mediaInfo.position // Reset to actual position when paused
                            }
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (mediaInfo.isPlaying) "🎵" else "⏸️",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Show episode/track title
                                Text(
                                    text = mediaInfo.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                    ),
                                    color = if (mediaInfo.albumArtUri != null) Color.White else Color.Black,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    lineHeight = 20.sp
                                )
                                // Show podcast/app name  
                                Text(
                                    text = "${mediaInfo.artist} • ${getAppName(mediaInfo.packageName)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (mediaInfo.albumArtUri != null) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                // Show progress bar if duration available
                                if (mediaInfo.duration > 0 && currentPosition >= 0) {
                                    val progressPercent = (currentPosition.toFloat() / mediaInfo.duration.toFloat()).coerceIn(0f, 1f)
                                    LinearProgressIndicator(
                                        progress = progressPercent,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        color = if (mediaInfo.albumArtUri != null) Color.White else Color.Black,
                                        trackColor = if (mediaInfo.albumArtUri != null) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.2f)
                                    )
                                    // Show time info with improved formatting
                                    val currentTime = formatTimeWithHours(currentPosition)
                                    val totalTime = formatTimeWithHours(mediaInfo.duration)
                                    Text(
                                        text = "$currentTime / $totalTime",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (mediaInfo.albumArtUri != null) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // No media playing but permission granted
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎵 No media playing",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                ),
                                color = Color.Black.copy(alpha = 0.7f) // 在绿色背景上使用半透明黑色
                            )
                            TextButton(
                                onClick = { mediaSessionViewModel.refreshMediaSession() }
                            ) {
                                Text("Refresh")
                            }
                        }
                    }
                }
                
                    // Error display
                    mediaSessionState.error?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (mediaSessionState.currentMediaInfo?.albumArtUri != null) Color.White else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        // Main content - centered
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "POMA",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF1ED760), // Spotify green
                    textAlign = TextAlign.Center
                )
            
                Spacer(modifier = Modifier.height(8.dp))
            
                Text(
                    text = "Podcast Bookmarks",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFB3B3B3), // Spotify gray
                    textAlign = TextAlign.Center
                )
            
                Spacer(modifier = Modifier.height(48.dp))
            
                // Voice Recording Button (Press & Hold)
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .size(if (recordingState.isRecording) 110.dp else 100.dp)
                            .border(
                                width = if (recordingState.isRecording) 3.dp else 2.dp,
                                color = if (recordingState.isRecording) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    Color(0xFF1ED760), // Spotify绿色边框
                                shape = CircleShape
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        android.util.Log.d("HomeScreen", "Press detected, checking permissions...")
                                        val hasPermission = PermissionUtils.hasRecordAudioPermission(context)
                                    
                                        if (hasPermission) {
                                            val pressStartTime = System.currentTimeMillis()
                                            android.util.Log.d("HomeScreen", "Permission granted, starting recording")
                                            voiceRecordingViewModel.startRecording(context)
                                            tryAwaitRelease()
                                            val pressDuration = System.currentTimeMillis() - pressStartTime
                                            android.util.Log.d("HomeScreen", "Press duration: ${pressDuration}ms")
                                            
                                            if (pressDuration < 1000) {
                                                // Quick tap under 1 second - create quick bookmark
                                                android.util.Log.d("HomeScreen", "Quick tap detected (${pressDuration}ms), creating quick bookmark")
                                                voiceRecordingViewModel.stopRecording() // Stop recording immediately
                                                voiceRecordingViewModel.createQuickBookmark()
                                            } else {
                                                // Long press over 1 second - process voice recording
                                                android.util.Log.d("HomeScreen", "Long press detected (${pressDuration}ms), processing voice recording")
                                                val recordingPath = voiceRecordingViewModel.stopRecording()
                                                recordingPath?.let { path ->
                                                    android.util.Log.d("HomeScreen", "Uploading recording: $path")
                                                    voiceRecordingViewModel.uploadRecording(path)
                                                }
                                            }
                                        } else {
                                            android.util.Log.d("HomeScreen", "Permission not granted, requesting RECORD_AUDIO permission")
                                            permissionLauncher.launch(PermissionUtils.RECORD_AUDIO_PERMISSION)
                                            tryAwaitRelease()
                                        }
                                    }
                                )
                            },
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = if (recordingState.isRecording) 
                                MaterialTheme.colorScheme.error 
                            else 
                                Color(0xFF121212) // Spotify黑色背景
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = if (recordingState.isRecording) 8.dp else 6.dp
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = com.poma.R.drawable.logo),
                                contentDescription = if (recordingState.isRecording) "Recording" else "Record",
                                modifier = Modifier.size(48.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            
                Spacer(modifier = Modifier.height(16.dp))
            
                Text(
                    text = when {
                        recordingState.isRecording -> "🔴 Recording..."
                        recordingState.isUploading -> "📤 Creating Bookmark..."
                        recordingState.uploadSuccess -> "✅ Bookmark Created!"
                        else -> "Press & Hold to Record"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        recordingState.isRecording -> MaterialTheme.colorScheme.error
                        recordingState.isUploading -> MaterialTheme.colorScheme.primary
                        recordingState.uploadSuccess -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center
                )
            
                Spacer(modifier = Modifier.height(32.dp))
            
                Button(
                    onClick = { navController.navigate("bookmarks") },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(48.dp)
                ) {
                    Text(
                        "My Bookmarks",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
        
    // Success handling - show success message (no auto-navigation)
    LaunchedEffect(recordingState.uploadSuccess) {
        if (recordingState.uploadSuccess) {
            android.util.Log.d("HomeScreen", "Voice bookmark created successfully!")
            // Reset the success state after showing the message
            kotlinx.coroutines.delay(3000) // Show success message for 3 seconds
            voiceRecordingViewModel.resetUploadSuccess()
        }
    }
        
    // Error handling
    recordingState.error?.let { error ->
        LaunchedEffect(error) {
            android.util.Log.e("HomeScreen", "Recording/Upload error: $error")
            voiceRecordingViewModel.clearError()
        }
    }
    
    // Handle Spotify OAuth flow
    LaunchedEffect(spotifyState.authUrl) {
        spotifyState.authUrl?.let { authUrl ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                context.startActivity(intent)
                spotifyViewModel.clearAuthUrl()
            } catch (e: Exception) {
                android.util.Log.e("HomeScreen", "Failed to open Spotify auth URL", e)
            }
        }
    }
    
    // Periodic media session refresh when enabled - balanced for battery life
    LaunchedEffect(mediaSessionState.isListenerEnabled) {
        if (mediaSessionState.isListenerEnabled) {
            while (mediaSessionState.isListenerEnabled) {
                mediaSessionViewModel.refreshMediaSession()
                // Conservative refresh rate to preserve battery
                kotlinx.coroutines.delay(10000L) // 10 seconds - sufficient for podcast bookmarking
            }
        }
    }
    
    // Handle MediaSession errors
    mediaSessionState.error?.let { error ->
        LaunchedEffect(error) {
            android.util.Log.e("HomeScreen", "MediaSession error: $error")
            kotlinx.coroutines.delay(3000)
            mediaSessionViewModel.clearError()
        }
    }
}

// Helper function to get friendly app name from package name
private fun getAppName(packageName: String): String {
    return when {
        packageName.contains("spotify") -> "Spotify"
        packageName.contains("podcast") -> "Podcasts"
        packageName.contains("youtube") -> "YouTube"
        packageName.contains("google.android.apps.podcasts") -> "Google Podcasts"
        packageName.contains("pocketcasts") -> "Pocket Casts"
        packageName.contains("overcast") -> "Overcast"
        else -> packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
    }
}

// Helper function to format time from milliseconds
private fun formatTime(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "${minutes}:${remainingSeconds.toString().padStart(2, '0')}"
}

// Helper function to format time with hours support (hours:minutes:seconds)
private fun formatTimeWithHours(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes}:${seconds.toString().padStart(2, '0')}"
    }
}