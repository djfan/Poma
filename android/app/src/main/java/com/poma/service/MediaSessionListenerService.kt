package com.poma.service

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData

/**
 * MediaSessionListenerService - System-level media session monitoring
 * 
 * This service listens to system-level media sessions to extract podcast/audio metadata
 * without requiring external API calls. This provides unified online/offline logic
 * and universal compatibility with any media app.
 */
class MediaSessionListenerService : NotificationListenerService() {
    
    companion object {
        private const val TAG = "MediaSessionListener"
        
        // Singleton instance for accessing from ViewModels
        private var _instance: MediaSessionListenerService? = null
        val instance: MediaSessionListenerService? get() = _instance
        
        // LiveData for current media session state
        private val _currentMediaInfo = MutableLiveData<MediaSessionInfo?>()
        val currentMediaInfo: LiveData<MediaSessionInfo?> = _currentMediaInfo
    }
    
    private lateinit var mediaSessionManager: MediaSessionManager
    private var activeControllers = mutableListOf<MediaController>()
    
    override fun onCreate() {
        super.onCreate()
        _instance = this
        
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        setupMediaSessionMonitoring()
        
        Log.d(TAG, "MediaSessionListenerService created and initialized")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        _instance = null
        cleanupControllers()
        Log.d(TAG, "MediaSessionListenerService destroyed")
    }
    
    private fun setupMediaSessionMonitoring() {
        try {
            val componentName = ComponentName(this, MediaSessionListenerService::class.java)
            
            // Set up listener for active sessions changes
            mediaSessionManager.addOnActiveSessionsChangedListener(
                { controllers ->
                    controllers?.let { controllerList ->
                        Log.d(TAG, "Active sessions changed: ${controllerList.size} active sessions")
                        updateActiveControllers(controllerList)
                    } ?: run {
                        Log.d(TAG, "Active sessions list is null")
                        updateActiveControllers(emptyList())
                    }
                },
                componentName
            )
            
            // Get initial active sessions
            val initialControllers = mediaSessionManager.getActiveSessions(componentName)
            initialControllers?.let { controllerList ->
                Log.d(TAG, "Initial active sessions: ${controllerList.size}")
                updateActiveControllers(controllerList)
            } ?: run {
                Log.d(TAG, "Initial active sessions list is null")
                updateActiveControllers(emptyList())
            }
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception - notification access permission not granted", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up media session monitoring", e)
        }
    }
    
    private fun updateActiveControllers(controllers: List<MediaController>) {
        // Clean up existing controllers
        cleanupControllers()
        
        // Set up new controllers
        activeControllers.clear()
        controllers.forEach { controller ->
            try {
                activeControllers.add(controller)
                setupControllerCallback(controller)
                
                // Immediately extract current media info
                extractMediaInfo(controller)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up controller callback", e)
            }
        }
    }
    
    private fun setupControllerCallback(controller: MediaController) {
        val callback = object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                Log.d(TAG, "Playback state changed: ${state?.state}")
                extractMediaInfo(controller)
            }
            
            override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                Log.d(TAG, "Metadata changed for ${controller.packageName}")
                extractMediaInfo(controller)
            }
        }
        
        controller.registerCallback(callback)
        Log.d(TAG, "Controller callback registered for ${controller.packageName}")
    }
    
    private fun extractMediaInfo(controller: MediaController) {
        try {
            val metadata = controller.metadata
            val playbackState = controller.playbackState
            val packageName = controller.packageName
            
            if (metadata != null && playbackState != null) {
                // Log ALL available metadata fields for complete analysis
                Log.d(TAG, "=== COMPLETE METADATA DUMP for $packageName ===")
                logAllMetadataFields(metadata)
                
                val mediaInfo = MediaSessionInfo(
                    packageName = packageName,
                    title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown Title",
                    artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist",
                    album = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM) ?: "Unknown Album",
                    duration = metadata.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION),
                    position = playbackState.position,
                    playbackState = playbackState.state,
                    isPlaying = playbackState.state == PlaybackState.STATE_PLAYING,
                    albumArtUri = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI),
                    // Deep linking for jump-back functionality
                    mediaId = metadata.getString(android.media.MediaMetadata.METADATA_KEY_MEDIA_ID),
                    // Additional podcast-specific fields
                    episodeDescription = metadata.getString(android.media.MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION),
                    genre = metadata.getString(android.media.MediaMetadata.METADATA_KEY_GENRE)
                )
                
                Log.d(TAG, "Media info extracted: $mediaInfo")
                _currentMediaInfo.postValue(mediaInfo)
                
            } else {
                Log.d(TAG, "No metadata or playback state available for ${packageName}")
                if (playbackState?.state == PlaybackState.STATE_STOPPED) {
                    _currentMediaInfo.postValue(null)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting media info", e)
        }
    }
    
    private fun logAllMetadataFields(metadata: android.media.MediaMetadata) {
        try {
            // String fields
            val stringKeys = listOf(
                android.media.MediaMetadata.METADATA_KEY_TITLE,
                android.media.MediaMetadata.METADATA_KEY_ARTIST,
                android.media.MediaMetadata.METADATA_KEY_ALBUM,
                android.media.MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
                android.media.MediaMetadata.METADATA_KEY_COMPOSER,
                android.media.MediaMetadata.METADATA_KEY_DATE,
                android.media.MediaMetadata.METADATA_KEY_GENRE,
                android.media.MediaMetadata.METADATA_KEY_COMPILATION,
                android.media.MediaMetadata.METADATA_KEY_WRITER,
                android.media.MediaMetadata.METADATA_KEY_DISPLAY_TITLE,
                android.media.MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
                android.media.MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION,
                android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
                android.media.MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
                android.media.MediaMetadata.METADATA_KEY_ART_URI,
                android.media.MediaMetadata.METADATA_KEY_MEDIA_ID,
                android.media.MediaMetadata.METADATA_KEY_MEDIA_URI
            )
            
            stringKeys.forEach { key ->
                val value = metadata.getString(key)
                if (!value.isNullOrEmpty()) {
                    Log.d(TAG, "STRING $key = '$value'")
                }
            }
            
            // Long fields
            val longKeys = listOf(
                android.media.MediaMetadata.METADATA_KEY_DURATION,
                android.media.MediaMetadata.METADATA_KEY_TRACK_NUMBER,
                android.media.MediaMetadata.METADATA_KEY_NUM_TRACKS,
                android.media.MediaMetadata.METADATA_KEY_DISC_NUMBER,
                android.media.MediaMetadata.METADATA_KEY_YEAR,
                android.media.MediaMetadata.METADATA_KEY_USER_RATING
            )
            
            longKeys.forEach { key ->
                val value = metadata.getLong(key)
                if (value != 0L) {
                    Log.d(TAG, "LONG $key = $value")
                }
            }
            
            // Bitmap fields (just check existence)
            val bitmapKeys = listOf(
                android.media.MediaMetadata.METADATA_KEY_ART,
                android.media.MediaMetadata.METADATA_KEY_ALBUM_ART,
                android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON
            )
            
            bitmapKeys.forEach { key ->
                val bitmap = metadata.getBitmap(key)
                if (bitmap != null) {
                    Log.d(TAG, "BITMAP $key = ${bitmap.width}x${bitmap.height}")
                }
            }
            
            Log.d(TAG, "=== END METADATA DUMP ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging metadata fields", e)
        }
    }
    
    private fun cleanupControllers() {
        activeControllers.forEach { controller ->
            try {
                // Note: MediaController.Callback doesn't have explicit unregister in older APIs
                // The callback will be cleaned up when controller is garbage collected
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up controller", e)
            }
        }
        activeControllers.clear()
    }
    
    /**
     * Get current media information synchronously
     * This is useful for immediate access in ViewModels
     */
    fun getCurrentMediaInfoSync(): MediaSessionInfo? {
        return _currentMediaInfo.value
    }
    
    /**
     * Check if notification listener permission is granted
     */
    fun hasNotificationPermission(): Boolean {
        val componentName = ComponentName(this, MediaSessionListenerService::class.java)
        return try {
            mediaSessionManager.getActiveSessions(componentName)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}

/**
 * Data class representing extracted media session information
 */
data class MediaSessionInfo(
    val packageName: String,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // in milliseconds
    val position: Long, // in milliseconds
    val playbackState: Int,
    val isPlaying: Boolean,
    val albumArtUri: String? = null,
    // Deep linking and jump-back functionality
    val mediaId: String? = null, // e.g., "spotify:episode:7v2NyyYIsI9xzqZ0qL21w0"
    // Podcast-specific fields
    val episodeDescription: String? = null,
    val genre: String? = null
) {
    
    /**
     * Convert to user-friendly format for debugging
     */
    override fun toString(): String {
        val minutes = position / 60000
        val seconds = (position % 60000) / 1000
        return """
            App: $packageName
            Title: $title
            Artist: $artist
            Position: ${minutes}:${seconds.toString().padStart(2, '0')}
            Playing: $isPlaying
            Duration: ${duration}ms
            Album: $album
            Genre: $genre
            Description: $episodeDescription
        """.trimIndent()
    }
    
    /**
     * Determine if this appears to be podcast content
     */
    fun isPodcastContent(): Boolean {
        return packageName.contains("podcast", ignoreCase = true) ||
               genre?.contains("podcast", ignoreCase = true) == true ||
               episodeDescription?.isNotEmpty() == true ||
               // Spotify podcast episodes typically have longer descriptions
               (packageName.contains("spotify") && episodeDescription?.length ?: 0 > 100)
    }
    
    /**
     * Convert to format compatible with current bookmark system
     */
    fun toBookmarkData(): Triple<String, String, Long> {
        // Return (podcast_name, episode_name, timestamp_ms)
        return Triple(
            artist.ifEmpty { "Unknown Podcast" },
            title.ifEmpty { "Unknown Episode" },
            position
        )
    }
    
    /**
     * Generate deep link for jumping back to this content
     */
    fun getDeepLink(): String? {
        return when {
            // Spotify provides direct deep links
            packageName.contains("spotify") && !mediaId.isNullOrEmpty() -> {
                mediaId // e.g., "spotify:episode:7v2NyyYIsI9xzqZ0qL21w0"
            }
            // YouTube doesn't provide reliable deep links via MediaSession
            packageName.contains("youtube") -> {
                null // Would require web search or title-based matching
            }
            // Generic podcast apps might provide mediaId
            !mediaId.isNullOrEmpty() -> {
                mediaId
            }
            else -> null
        }
    }
    
    /**
     * Generate web fallback URL for jump-back functionality
     */
    fun getWebFallbackUrl(): String? {
        return when {
            // Spotify web player
            packageName.contains("spotify") && !mediaId.isNullOrEmpty() -> {
                val episodeId = mediaId!!.removePrefix("spotify:episode:")
                "https://open.spotify.com/episode/$episodeId"
            }
            // YouTube search fallback
            packageName.contains("youtube") -> {
                val searchQuery = "$artist $title".replace(" ", "+")
                "https://www.youtube.com/results?search_query=$searchQuery"
            }
            else -> null
        }
    }
}