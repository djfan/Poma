package com.poma.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class SpotifyState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val authUrl: String? = null,
    val error: String? = null,
    val currentTrack: SpotifyTrack? = null
)

data class SpotifyTrack(
    val isPlaying: Boolean,
    val trackName: String,
    val artistName: String,
    val episodeName: String?,
    val podcastName: String?,
    val progressMs: Long?,
    val durationMs: Long?
)

// API Models
data class SpotifyAuthResponse(
    val auth_url: String
)

data class SpotifyCallbackRequest(
    val code: String
)

data class SpotifyTokenResponse(
    val access_token: String,
    val message: String
)

data class SpotifyPlaybackResponse(
    val is_playing: Boolean,
    val progress_ms: Int,
    val episode_id: String,
    val episode_name: String,
    val duration_ms: Int,
    val podcast_name: String,
    val podcast_publisher: String?,
    val episode_uri: String,
    val release_date: String?
)

interface SpotifyApi {
    @GET("api/v1/spotify/auth-url")
    suspend fun getAuthUrl(): retrofit2.Response<SpotifyAuthResponse>
    
    @POST("api/v1/spotify/callback")
    suspend fun handleCallback(
        @Body request: SpotifyCallbackRequest,
        @Header("Authorization") token: String
    ): retrofit2.Response<SpotifyTokenResponse>
    
    @GET("api/v1/spotify/current-playback")
    suspend fun getCurrentPlayback(
        @Header("Authorization") token: String
    ): retrofit2.Response<SpotifyPlaybackResponse?>
}

class SpotifyViewModel : ViewModel() {
    
    private val _spotifyState = MutableStateFlow(SpotifyState())
    val spotifyState: StateFlow<SpotifyState> = _spotifyState.asStateFlow()
    
    private val apiService: SpotifyApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://localhost:8001/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SpotifyApi::class.java)
    }
    
    companion object {
        private const val TAG = "SpotifyViewModel"
    }
    
    fun initiateSpotifyConnection() {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "Initiating Spotify connection...")
                
                _spotifyState.value = _spotifyState.value.copy(
                    isConnecting = true,
                    error = null
                )
                
                val response = apiService.getAuthUrl()
                
                if (response.isSuccessful) {
                    val authUrl = response.body()?.auth_url
                    android.util.Log.d(TAG, "Got auth URL: $authUrl")
                    _spotifyState.value = _spotifyState.value.copy(
                        isConnecting = false,
                        authUrl = authUrl
                    )
                } else {
                    android.util.Log.e(TAG, "Failed to get auth URL: ${response.code()}")
                    _spotifyState.value = _spotifyState.value.copy(
                        isConnecting = false,
                        error = "Failed to get Spotify auth URL"
                    )
                }
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error initiating Spotify connection", e)
                _spotifyState.value = _spotifyState.value.copy(
                    isConnecting = false,
                    error = "Network error: ${e.message}"
                )
            }
        }
    }
    
    fun handleSpotifyCallback(code: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "Handling Spotify callback with code: ${code.take(20)}...")
                
                _spotifyState.value = _spotifyState.value.copy(
                    isConnecting = true,
                    error = null
                )
                
                val token = TokenManager.getToken()
                if (token == null) {
                    _spotifyState.value = _spotifyState.value.copy(
                        isConnecting = false,
                        error = "Authentication required"
                    )
                    return@launch
                }
                
                val request = SpotifyCallbackRequest(code)
                val response = apiService.handleCallback(request, "Bearer $token")
                
                if (response.isSuccessful) {
                    android.util.Log.d(TAG, "Spotify connected successfully!")
                    _spotifyState.value = _spotifyState.value.copy(
                        isConnecting = false,
                        isConnected = true,
                        authUrl = null
                    )
                    
                    // Start checking current playback
                    checkCurrentPlayback()
                } else {
                    android.util.Log.e(TAG, "Failed to connect Spotify: ${response.code()}")
                    _spotifyState.value = _spotifyState.value.copy(
                        isConnecting = false,
                        error = "Failed to connect Spotify account"
                    )
                }
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error handling Spotify callback", e)
                _spotifyState.value = _spotifyState.value.copy(
                    isConnecting = false,
                    error = "Connection failed: ${e.message}"
                )
            }
        }
    }
    
    fun checkCurrentPlayback() {
        viewModelScope.launch {
            try {
                val token = TokenManager.getToken()
                if (token == null) return@launch
                
                val response = apiService.getCurrentPlayback("Bearer $token")
                
                if (response.isSuccessful) {
                    val playback = response.body()
                    if (playback != null) {
                        _spotifyState.value = _spotifyState.value.copy(
                            currentTrack = SpotifyTrack(
                                isPlaying = playback.is_playing,
                                trackName = playback.episode_name,
                                artistName = playback.podcast_publisher ?: "Spotify",
                                episodeName = playback.episode_name,
                                podcastName = playback.podcast_name,
                                progressMs = playback.progress_ms.toLong(),
                                durationMs = playback.duration_ms.toLong()
                            )
                        )
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error checking current playback", e)
            }
        }
    }
    
    fun clearAuthUrl() {
        _spotifyState.value = _spotifyState.value.copy(authUrl = null)
    }
    
    fun clearError() {
        _spotifyState.value = _spotifyState.value.copy(error = null)
    }
}