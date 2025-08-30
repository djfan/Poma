package com.poma.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.content.ComponentName
import android.media.session.MediaSessionManager
import android.media.MediaMetadata
import com.poma.service.MediaSessionListenerService
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poma.viewmodel.BookmarksViewModel
import com.poma.viewmodel.BookmarkItem
import com.poma.R
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

enum class SortType {
    TIME_DESC, EPISODE_NAME
}

@Composable
fun BookmarksScreen(
    navController: NavController,
    bookmarksViewModel: BookmarksViewModel = viewModel()
) {
    val bookmarksState by bookmarksViewModel.bookmarksState.collectAsState()
    var sortType by remember { mutableStateOf(SortType.TIME_DESC) }
    var filteredEpisode by remember { mutableStateOf<String?>(null) }
    
    // Filter and sort bookmarks
    val displayedBookmarks = remember(bookmarksState.bookmarks, sortType, filteredEpisode) {
        val filtered = if (filteredEpisode != null) {
            bookmarksState.bookmarks.filter { it.episode_name == filteredEpisode }
        } else {
            bookmarksState.bookmarks
        }
        
        when (sortType) {
            SortType.TIME_DESC -> {
                if (filteredEpisode != null) {
                    // When filtering by episode, sort by timestamp for chronological order
                    filtered.sortedBy { it.timestamp_ms }
                } else {
                    filtered.sortedByDescending { it.created_at }
                }
            }
            SortType.EPISODE_NAME -> filtered.sortedBy { it.episode_name }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (filteredEpisode != null) "FILTERED EPISODE" else "MY BOOKMARKS",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(8.dp)
                )
                if (filteredEpisode != null) {
                    Text(
                        text = filteredEpisode!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    TextButton(
                        onClick = { filteredEpisode = null },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text("Show All", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            
            // Sort toggle button
            TextButton(
                onClick = {
                    sortType = when (sortType) {
                        SortType.TIME_DESC -> SortType.EPISODE_NAME
                        SortType.EPISODE_NAME -> SortType.TIME_DESC
                    }
                }
            ) {
                Text(
                    text = when (sortType) {
                        SortType.TIME_DESC -> if (filteredEpisode != null) "Timeline" else "Time ↓"
                        SortType.EPISODE_NAME -> "A-Z"
                    },
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
        
        when {
            bookmarksState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            bookmarksState.error != null -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error: ${bookmarksState.error}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { bookmarksViewModel.refreshBookmarks() }) {
                        Text("Retry")
                    }
                }
            }
            displayedBookmarks.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No bookmarks yet.\nRecord your first voice note!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn {
                    items(displayedBookmarks.size) { index ->
                        val bookmark = displayedBookmarks[index]
                        RealBookmarkCard(
                            bookmark = bookmark,
                            context = LocalContext.current,
                            onDelete = { bookmarksViewModel.deleteBookmark(bookmark.id) },
                            onEdit = { bookmarksViewModel.editBookmark(bookmark.id, it) },
                            onFilter = { episodeName -> filteredEpisode = episodeName }
                        )
                    }
                }
            }
        }
    }
}

data class FakeBookmark(
    val podcastName: String,
    val episodeTitle: String,
    val timestamp: String,
    val note: String,
    val date: String
)

@Composable
fun RealBookmarkCard(
    bookmark: BookmarkItem, 
    context: Context,
    onDelete: () -> Unit,
    onEdit: (String) -> Unit,
    onFilter: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val maxSwipeDistance = with(density) { 
        // Use screen width for maximum swipe distance (100%)
        configuration.screenWidthDp.dp.toPx()
    }
    
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clipToBounds()
    ) {
        // Background actions (shown when swiped)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right swipe backgrounds
            if (animatedOffsetX > 0) {
                val swipeWidth = (animatedOffsetX / density.density).dp
                val swipeRatio = animatedOffsetX / maxSwipeDistance
                
                when {
                    swipeRatio > 0.5f -> {
                        // Right swipe second half - Delete (red)
                        Box(
                            modifier = Modifier
                                .width(swipeWidth)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.error),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    }
                    swipeRatio > 0.2f -> {
                        // Right swipe first half - Edit (blue)
                        Box(
                            modifier = Modifier
                                .width(swipeWidth)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        }
                    }
                    else -> {
                        // Small right swipe - no action yet
                        Box(
                            modifier = Modifier
                                .width(swipeWidth)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Left swipe backgrounds
            if (animatedOffsetX < 0) {
                val swipeWidth = ((-animatedOffsetX) / density.density).dp
                val swipeRatio = (-animatedOffsetX) / maxSwipeDistance
                
                when {
                    swipeRatio > 0.5f -> {
                        // Left swipe >50% - Filter same episode (purple)
                        Box(
                            modifier = Modifier
                                .width(swipeWidth)
                                .fillMaxHeight()
                                .background(Color(0xFF9C27B0)), // Purple
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Filter Episode",
                                tint = Color.White
                            )
                        }
                    }
                    swipeRatio > 0.2f -> {
                        // Left swipe 20-50% - Jump to Spotify (green)
                        Box(
                            modifier = Modifier
                                .width(swipeWidth)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.tertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Jump to Spotify",
                                tint = Color.White
                            )
                        }
                    }
                    else -> {
                        // Small left swipe - no action yet
                        Box(
                            modifier = Modifier
                                .width(swipeWidth)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }
            }
        }
        
        // Main card content
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = (animatedOffsetX / density.density).dp)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        if (!isDragging) {
                            isDragging = true
                            android.util.Log.d("BookmarksScreen", "Horizontal drag started")
                        }
                        
                        val newOffset = offsetX + delta
                        offsetX = newOffset.coerceIn(-maxSwipeDistance, maxSwipeDistance)
                    },
                    onDragStarted = {
                        android.util.Log.d("BookmarksScreen", "Drag started")
                    },
                    onDragStopped = { velocity ->
                        android.util.Log.d("BookmarksScreen", "Horizontal drag ended, offsetX: $offsetX, velocity: $velocity")
                        isDragging = false
                        
                        when {
                            // Left swipe >50% - Filter same episode
                            offsetX < -maxSwipeDistance * 0.5f -> {
                                android.util.Log.d("BookmarksScreen", "Left swipe >50% - Filter same episode (offsetX: $offsetX, threshold: ${-maxSwipeDistance * 0.5f})")
                                onFilter(bookmark.episode_name)
                            }
                            // Left swipe 20-50% - Jump to Spotify 
                            offsetX < -maxSwipeDistance * 0.2f -> {
                                android.util.Log.d("BookmarksScreen", "Left swipe 20-50% - Jump to Spotify (offsetX: $offsetX, threshold: ${-maxSwipeDistance * 0.2f})")
                                if (!bookmark.media_id.isNullOrEmpty() && 
                                    bookmark.source_app_package?.contains("spotify") == true) {
                                    android.util.Log.d("BookmarksScreen", "Jumping to Spotify: ${bookmark.media_id}")
                                    jumpToSpotify(context, bookmark.media_id!!, bookmark.timestamp_ms)
                                } else {
                                    android.util.Log.d("BookmarksScreen", "No valid Spotify content for jump - media_id: ${bookmark.media_id}, source_app_package: ${bookmark.source_app_package}")
                                }
                            }
                            // Right swipe second half - Delete (>50%)
                            offsetX > maxSwipeDistance * 0.5f -> {
                                android.util.Log.d("BookmarksScreen", "Right swipe second half - Delete (offsetX: $offsetX, threshold: ${maxSwipeDistance * 0.5f})")
                                showDeleteDialog = true
                            }
                            // Right swipe first half - Edit (20%-50%)
                            offsetX > maxSwipeDistance * 0.2f -> {
                                android.util.Log.d("BookmarksScreen", "Right swipe first half - Edit (offsetX: $offsetX, threshold: ${maxSwipeDistance * 0.2f})")
                                showEditDialog = true
                            }
                            else -> {
                                android.util.Log.d("BookmarksScreen", "Insufficient horizontal swipe distance (offsetX: $offsetX)")
                            }
                        }
                        offsetX = 0f
                    }
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = bookmark.podcast_name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = bookmark.episode_name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🎙️ ${formatPlaybackPosition(bookmark.timestamp_ms)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatDate(bookmark.created_at),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Show source app and long press hint
            if (!bookmark.media_id.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "📱 ${getAppName(bookmark.source_app_package ?: "")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Show transcript text if available
            bookmark.transcript_text?.let { transcript ->
                if (transcript.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"$transcript\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            }
        }
    }
    
    // Edit Dialog
    if (showEditDialog) {
        var editedText by remember { mutableStateOf(bookmark.transcript_text ?: "") }
        
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Note") },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEdit(editedText)
                        showEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Bookmark") },
            text = { Text("Are you sure you want to delete this bookmark? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Helper functions for jump-back functionality  
private fun jumpToSpotify(context: Context, mediaId: String, timestampMs: Long) {
    try {
        // Subtract 10 seconds for better context (user usually notes after hearing content)
        val adjustedTimestampMs = if (timestampMs > 10000) timestampMs - 10000 else 0
        Log.d("BookmarksScreen", "Attempting to control Spotify playback with mediaId: $mediaId at original timestamp: $timestampMs, adjusted: $adjustedTimestampMs")
        
        // First try to control Spotify via MediaController (background playback)
        if (controlSpotifyPlayback(context, mediaId, adjustedTimestampMs)) {
            Log.d("BookmarksScreen", "Successfully controlled Spotify in background")
            return
        }
        
        // Fallback to opening Spotify app if MediaController doesn't work
        Log.d("BookmarksScreen", "MediaController failed, falling back to opening Spotify app")
        
        // Try Spotify URI with timestamp parameter
        if (mediaId.startsWith("spotify:episode:")) {
            val timestampSeconds = adjustedTimestampMs / 1000
            val spotifyUriWithTime = "$mediaId?t=$timestampSeconds"
            val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUriWithTime))
            spotifyIntent.setPackage("com.spotify.music")
            
            if (spotifyIntent.resolveActivity(context.packageManager) != null) {
                Log.d("BookmarksScreen", "Opening Spotify app with adjusted timestamp: $spotifyUriWithTime")
                context.startActivity(spotifyIntent)
                return
            }
        }
        
        // Final fallback: Try web player with timestamp
        val episodeId = mediaId.removePrefix("spotify:episode:")
        val timestampSeconds = adjustedTimestampMs / 1000  
        val webUrlWithTime = "https://open.spotify.com/episode/$episodeId?t=$timestampSeconds"
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrlWithTime))
        
        Log.d("BookmarksScreen", "Opening Spotify web player with adjusted timestamp: $webUrlWithTime")
        context.startActivity(webIntent)
        
    } catch (e: Exception) {
        Log.e("BookmarksScreen", "Failed to control Spotify", e)
    }
}

private fun controlSpotifyPlayback(context: Context, mediaId: String, timestampMs: Long): Boolean {
    return try {
        val mediaManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val activeSessions = mediaManager.getActiveSessions(ComponentName(context, MediaSessionListenerService::class.java))
        
        // Find Spotify session
        val spotifySession = activeSessions.find { 
            it.packageName == "com.spotify.music" 
        }
        
        if (spotifySession != null) {
            Log.d("BookmarksScreen", "Found active Spotify session, attempting to control playback")
            
            // Get the MediaController for Spotify
            val mediaController = spotifySession
            val transportControls = mediaController.transportControls
            
            // If the media ID matches what's currently playing, just seek to position
            val currentMetadata = mediaController.metadata
            val currentMediaId = currentMetadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)
            
            if (currentMediaId == mediaId) {
                Log.d("BookmarksScreen", "Same episode already loaded, seeking to timestamp: ${timestampMs}ms")
                transportControls.seekTo(timestampMs)
                transportControls.play()
                true
            } else {
                Log.d("BookmarksScreen", "Different episode, current: $currentMediaId, requested: $mediaId")
                // For different episodes, we can't directly load them via MediaController
                // This would require Spotify's own API or intent-based approach
                false
            }
        } else {
            Log.d("BookmarksScreen", "No active Spotify session found")
            false
        }
    } catch (e: Exception) {
        Log.e("BookmarksScreen", "Error controlling Spotify playback", e)
        false
    }
}

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

private fun formatPlaybackPosition(timestampMs: Long): String {
    val totalSeconds = timestampMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes}:${seconds.toString().padStart(2, '0')}"
    }
}

private fun formatTimestamp(timestampMs: Long): String {
    val date = Date(timestampMs)
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(date)
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString.substring(0, 10) // fallback to date part
    }
}