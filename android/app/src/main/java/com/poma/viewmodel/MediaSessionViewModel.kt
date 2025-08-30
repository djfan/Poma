package com.poma.viewmodel

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.poma.service.MediaSessionListenerService
import com.poma.service.MediaSessionInfo

data class MediaSessionState(
    val isListenerEnabled: Boolean = false,
    val currentMediaInfo: MediaSessionInfo? = null,
    val isCheckingPermission: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for managing MediaSessionManager integration
 * 
 * This ViewModel provides system-level media session monitoring capabilities,
 * allowing the app to extract podcast metadata without external API calls.
 */
class MediaSessionViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "MediaSessionViewModel"
    }
    
    private val _mediaSessionState = MutableStateFlow(MediaSessionState())
    val mediaSessionState: StateFlow<MediaSessionState> = _mediaSessionState.asStateFlow()
    
    init {
        // Check initial permission state
        checkNotificationListenerPermission()
        
        // Observe media info changes from service
        observeMediaSessionService()
        
        Log.d(TAG, "MediaSessionViewModel initialized")
    }
    
    private fun observeMediaSessionService() {
        viewModelScope.launch {
            MediaSessionListenerService.currentMediaInfo.observeForever { mediaInfo ->
                Log.d(TAG, "Media info updated: $mediaInfo")
                _mediaSessionState.value = _mediaSessionState.value.copy(
                    currentMediaInfo = mediaInfo
                )
            }
        }
    }
    
    /**
     * Check if NotificationListenerService permission is granted
     */
    fun checkNotificationListenerPermission() {
        viewModelScope.launch {
            _mediaSessionState.value = _mediaSessionState.value.copy(isCheckingPermission = true)
            
            try {
                val service = MediaSessionListenerService.instance
                val hasPermission = service?.hasNotificationPermission() ?: false
                
                Log.d(TAG, "Notification listener permission: $hasPermission")
                
                _mediaSessionState.value = _mediaSessionState.value.copy(
                    isListenerEnabled = hasPermission,
                    isCheckingPermission = false,
                    error = if (!hasPermission) "Notification access required for media session monitoring" else null
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error checking notification permission", e)
                _mediaSessionState.value = _mediaSessionState.value.copy(
                    isListenerEnabled = false,
                    isCheckingPermission = false,
                    error = "Error checking permissions: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Open notification listener settings for user to grant permission
     */
    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            Log.d(TAG, "Opened notification listener settings")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening notification listener settings", e)
            _mediaSessionState.value = _mediaSessionState.value.copy(
                error = "Could not open settings: ${e.message}"
            )
        }
    }
    
    /**
     * Get current media information for immediate use (e.g., voice bookmark creation)
     */
    fun getCurrentMediaInfo(): MediaSessionInfo? {
        val currentInfo = _mediaSessionState.value.currentMediaInfo
        Log.d(TAG, "Getting current media info: $currentInfo")
        return currentInfo
    }
    
    /**
     * Create bookmark data from current media session
     * This replaces the need for Spotify API calls
     */
    fun createBookmarkFromCurrentMedia(): Triple<String, String, Long>? {
        val mediaInfo = getCurrentMediaInfo()
        return if (mediaInfo?.isPlaying == true) {
            val bookmarkData = mediaInfo.toBookmarkData()
            Log.d(TAG, "Created bookmark data from media session: $bookmarkData")
            bookmarkData
        } else {
            Log.d(TAG, "No active media session for bookmark creation")
            null
        }
    }
    
    /**
     * Check if current media appears to be podcast content
     */
    fun isPodcastCurrentlyPlaying(): Boolean {
        return getCurrentMediaInfo()?.isPodcastContent() ?: false
    }
    
    /**
     * Get detailed media session information for debugging
     */
    fun getMediaSessionDebugInfo(): String {
        val mediaInfo = getCurrentMediaInfo()
        return if (mediaInfo != null) {
            """
            === MediaSession Debug Info ===
            ${mediaInfo}
            
            Podcast Detection: ${mediaInfo.isPodcastContent()}
            Bookmark Data: ${mediaInfo.toBookmarkData()}
            """.trimIndent()
        } else {
            "No active media session"
        }
    }
    
    /**
     * Clear any error state
     */
    fun clearError() {
        _mediaSessionState.value = _mediaSessionState.value.copy(error = null)
    }
    
    /**
     * Force refresh media session state
     */
    fun refreshMediaSession() {
        Log.d(TAG, "Refreshing media session state")
        checkNotificationListenerPermission()
        
        // Force update from service
        val service = MediaSessionListenerService.instance
        val currentInfo = service?.getCurrentMediaInfoSync()
        
        _mediaSessionState.value = _mediaSessionState.value.copy(
            currentMediaInfo = currentInfo
        )
    }
}