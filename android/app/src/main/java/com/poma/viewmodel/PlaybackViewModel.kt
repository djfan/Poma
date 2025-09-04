package com.poma.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Data classes for playback status
data class PlaybackState(
    val isPlaying: Boolean = false,
    val trackName: String = "No music playing",
    val artistName: String = "",
    val albumName: String = "",
    val spotifyAvailable: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

// API interface for playback status
interface PlaybackApi {
    @GET("api/v1/bookmarks/current-playback")
    suspend fun getCurrentPlayback(): retrofit2.Response<PlaybackResponse>
}

// Response model
data class PlaybackResponse(
    val is_playing: Boolean,
    val track_name: String,
    val artist_name: String,
    val album_name: String,
    val spotify_available: Boolean
)

class PlaybackViewModel : ViewModel() {
    
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()
    
    // API client
    private val apiService: PlaybackApi 
        get() = Retrofit.Builder()
            .baseUrl(com.poma.config.ApiConfig.getBaseUrl())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlaybackApi::class.java)
    
    companion object {
        private const val TAG = "PlaybackViewModel"
        private const val REFRESH_INTERVAL_MS = 10000L // 10 seconds
    }
    
    init {
        startPeriodicRefresh()
    }
    
    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                refreshPlaybackStatus()
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }
    
    fun refreshPlaybackStatus() {
        viewModelScope.launch {
            try {
                _playbackState.value = _playbackState.value.copy(
                    isLoading = true,
                    error = null
                )
                
                val response = apiService.getCurrentPlayback()
                
                if (response.isSuccessful) {
                    val playback = response.body()
                    if (playback != null) {
                        _playbackState.value = PlaybackState(
                            isPlaying = playback.is_playing,
                            trackName = playback.track_name,
                            artistName = playback.artist_name,
                            albumName = playback.album_name,
                            spotifyAvailable = playback.spotify_available,
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    _playbackState.value = _playbackState.value.copy(
                        isLoading = false,
                        error = "Failed to get playback status"
                    )
                }
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error getting playback status", e)
                _playbackState.value = _playbackState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}