package com.poma.viewmodel

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Header

data class VoiceRecordingState(
    val isRecording: Boolean = false,
    val recordingPath: String? = null,
    val error: String? = null,
    val duration: Long = 0L,
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean = false
)

// API interface for voice bookmark creation
interface VoiceBookmarkApi {
    @Multipart
    @POST("api/v1/bookmarks/voice-bookmark")
    suspend fun createVoiceBookmark(
        @Part audio: MultipartBody.Part,
        @Part("media_title") mediaTitle: RequestBody?,
        @Part("media_artist") mediaArtist: RequestBody?,
        @Part("media_id") mediaId: RequestBody?,
        @Part("source_app_package") sourceAppPackage: RequestBody?,
        @Part("album_art_uri") albumArtUri: RequestBody?,
        @Part("timestamp_ms") timestampMs: RequestBody?,
        @Part("duration_ms") durationMs: RequestBody?,
        @Header("Authorization") token: String
    ): retrofit2.Response<BookmarkResponse>
}

// Response model for bookmark
data class BookmarkResponse(
    val id: Int,
    val podcast_name: String,
    val episode_name: String,
    val timestamp_ms: Long,
    val duration_ms: Int?,
    val spotify_episode_id: String?,
    val podcast_cover_url: String?,
    val audio_file_path: String?,
    val transcript_text: String?,
    val user_note: String?,
    val ai_summary: String?,
    val created_at: String
)

class VoiceRecordingViewModel : ViewModel() {
    
    private val _recordingState = MutableStateFlow(VoiceRecordingState())
    val recordingState: StateFlow<VoiceRecordingState> = _recordingState.asStateFlow()
    
    private var mediaRecorder: MediaRecorder? = null
    private var recordingStartTime: Long = 0L
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    
    // API client
    private val apiService: VoiceBookmarkApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://localhost:8001/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(VoiceBookmarkApi::class.java)
    }
    
    companion object {
        private const val TAG = "VoiceRecordingViewModel"
    }
    
    private fun requestAudioFocus(context: Context): Boolean {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { focusChange ->
                    Log.d(TAG, "Audio focus changed: $focusChange")
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            // Stop recording if we lose focus completely
                            if (_recordingState.value.isRecording) {
                                stopRecording()
                            }
                        }
                    }
                }
                .build()
            
            val result = audioManager?.requestAudioFocus(audioFocusRequest!!)
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager?.requestAudioFocus(
                { focusChange ->
                    Log.d(TAG, "Audio focus changed: $focusChange")
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            if (_recordingState.value.isRecording) {
                                stopRecording()
                            }
                        }
                    }
                },
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }
    
    private fun releaseAudioFocus() {
        audioManager?.let { manager ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { request ->
                    manager.abandonAudioFocusRequest(request)
                }
            } else {
                @Suppress("DEPRECATION")
                manager.abandonAudioFocus(null)
            }
        }
        audioManager = null
        audioFocusRequest = null
    }
    
    fun startRecording(context: Context) {
        try {
            Log.d(TAG, "Starting voice recording...")
            
            // Request audio focus to pause other media players (like Spotify)
            if (!requestAudioFocus(context)) {
                Log.w(TAG, "Failed to gain audio focus, but continuing with recording")
            }
            
            // Create recording file
            val recordingDir = File(context.cacheDir, "recordings")
            if (!recordingDir.exists()) {
                recordingDir.mkdirs()
            }
            
            val timestamp = System.currentTimeMillis()
            val recordingFile = File(recordingDir, "voice_note_$timestamp.m4a")
            
            // Initialize MediaRecorder
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFile.absolutePath)
                
                prepare()
                start()
            }
            
            recordingStartTime = System.currentTimeMillis()
            
            _recordingState.value = VoiceRecordingState(
                isRecording = true,
                recordingPath = recordingFile.absolutePath,
                error = null,
                duration = 0L
            )
            
            Log.d(TAG, "Recording started successfully: ${recordingFile.absolutePath}")
            
        } catch (e: IOException) {
            Log.e(TAG, "Failed to start recording", e)
            releaseAudioFocus() // Release focus if recording failed
            _recordingState.value = VoiceRecordingState(
                isRecording = false,
                recordingPath = null,
                error = "Failed to start recording: ${e.message}",
                duration = 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during recording", e)
            releaseAudioFocus() // Release focus if recording failed
            _recordingState.value = VoiceRecordingState(
                isRecording = false,
                recordingPath = null,
                error = "Recording error: ${e.message}",
                duration = 0L
            )
        }
    }
    
    fun stopRecording(): String? {
        return try {
            Log.d(TAG, "Stopping voice recording...")
            
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            val duration = System.currentTimeMillis() - recordingStartTime
            val recordingPath = _recordingState.value.recordingPath
            
            _recordingState.value = VoiceRecordingState(
                isRecording = false,
                recordingPath = recordingPath,
                error = null,
                duration = duration
            )
            
            Log.d(TAG, "Recording stopped successfully. Duration: ${duration}ms, Path: $recordingPath")
            
            // Release audio focus to allow other media players to resume
            releaseAudioFocus()
            
            recordingPath
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            
            // Release audio focus even if stopping failed
            releaseAudioFocus()
            
            _recordingState.value = VoiceRecordingState(
                isRecording = false,
                recordingPath = null,
                error = "Failed to stop recording: ${e.message}",
                duration = 0L
            )
            null
        }
    }
    
    fun clearError() {
        _recordingState.value = _recordingState.value.copy(error = null)
    }
    
    fun resetUploadSuccess() {
        _recordingState.value = _recordingState.value.copy(uploadSuccess = false)
    }
    
    // Quick bookmark without recording - for clicks under 1 second
    fun createQuickBookmark() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Creating quick bookmark without recording...")
                
                _recordingState.value = _recordingState.value.copy(
                    isUploading = true,
                    error = null
                )
                
                // Get authentication token
                val token = TokenManager.getToken()
                if (token == null) {
                    _recordingState.value = _recordingState.value.copy(
                        isUploading = false,
                        uploadSuccess = false,
                        error = "Authentication required"
                    )
                    return@launch
                }
                
                // Get current media info
                val mediaInfo = com.poma.service.MediaSessionListenerService.instance?.getCurrentMediaInfoSync()
                
                Log.d(TAG, "Quick bookmark MediaSession info available: ${mediaInfo != null}")
                if (mediaInfo != null) {
                    Log.d(TAG, "Quick bookmark MediaSession data: ${mediaInfo}")
                }
                
                // Create empty audio part (1 byte placeholder)
                val emptyAudioData = ByteArray(1) { 0 }
                val requestBody = emptyAudioData.toRequestBody("audio/m4a".toMediaTypeOrNull())
                val audioPart = MultipartBody.Part.createFormData(
                    "audio",
                    "quick_bookmark.m4a",
                    requestBody
                )
                
                // Prepare MediaSession data as RequestBody parts
                val mediaTitle = mediaInfo?.title?.toRequestBody("text/plain".toMediaTypeOrNull())
                val mediaArtist = mediaInfo?.artist?.toRequestBody("text/plain".toMediaTypeOrNull())
                val mediaId = mediaInfo?.mediaId?.toRequestBody("text/plain".toMediaTypeOrNull())
                val sourceAppPackage = mediaInfo?.packageName?.toRequestBody("text/plain".toMediaTypeOrNull())
                val albumArtUri = mediaInfo?.albumArtUri?.toRequestBody("text/plain".toMediaTypeOrNull())
                val timestampMs = mediaInfo?.position?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val durationMs = mediaInfo?.duration?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                
                // Call API with MediaSession data and authentication token
                val response = apiService.createVoiceBookmark(
                    audio = audioPart,
                    mediaTitle = mediaTitle,
                    mediaArtist = mediaArtist,
                    mediaId = mediaId,
                    sourceAppPackage = sourceAppPackage,
                    albumArtUri = albumArtUri,
                    timestampMs = timestampMs,
                    durationMs = durationMs,
                    token = "Bearer $token"
                )
                
                if (response.isSuccessful) {
                    val bookmark = response.body()
                    Log.d(TAG, "Quick bookmark created successfully: ${bookmark?.id}")
                    _recordingState.value = _recordingState.value.copy(
                        isUploading = false,
                        uploadSuccess = true,
                        error = null
                    )
                } else {
                    Log.e(TAG, "Failed to create quick bookmark: ${response.code()}")
                    _recordingState.value = _recordingState.value.copy(
                        isUploading = false,
                        uploadSuccess = false,
                        error = "Failed to create bookmark: ${response.code()}"
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating quick bookmark", e)
                _recordingState.value = _recordingState.value.copy(
                    isUploading = false,
                    uploadSuccess = false,
                    error = "Quick bookmark failed: ${e.message}"
                )
            }
        }
    }
    
    fun uploadRecording(recordingPath: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting voice bookmark upload...")
                
                _recordingState.value = _recordingState.value.copy(
                    isUploading = true,
                    error = null
                )
                
                val audioFile = File(recordingPath)
                if (!audioFile.exists()) {
                    throw Exception("Audio file not found: $recordingPath")
                }
                
                val requestBody = audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
                val audioPart = MultipartBody.Part.createFormData(
                    "audio",
                    audioFile.name,
                    requestBody
                )
                
                // Get authentication token
                val token = TokenManager.getToken()
                if (token == null) {
                    _recordingState.value = _recordingState.value.copy(
                        isUploading = false,
                        uploadSuccess = false,
                        error = "Authentication required"
                    )
                    return@launch
                }
                
                // Try to get media session info first (system-level approach)
                val mediaInfo = com.poma.service.MediaSessionListenerService.instance?.getCurrentMediaInfoSync()
                
                Log.d(TAG, "MediaSession info available: ${mediaInfo != null}")
                if (mediaInfo != null) {
                    Log.d(TAG, "MediaSession data: ${mediaInfo}")
                }
                
                // Prepare MediaSession data as RequestBody parts
                val mediaTitle = mediaInfo?.title?.toRequestBody("text/plain".toMediaTypeOrNull())
                val mediaArtist = mediaInfo?.artist?.toRequestBody("text/plain".toMediaTypeOrNull())
                val mediaId = mediaInfo?.mediaId?.toRequestBody("text/plain".toMediaTypeOrNull())
                val sourceAppPackage = mediaInfo?.packageName?.toRequestBody("text/plain".toMediaTypeOrNull())
                val albumArtUri = mediaInfo?.albumArtUri?.toRequestBody("text/plain".toMediaTypeOrNull())
                val timestampMs = mediaInfo?.position?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                val durationMs = mediaInfo?.duration?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
                
                // Call API with MediaSession data and authentication token
                val response = apiService.createVoiceBookmark(
                    audio = audioPart,
                    mediaTitle = mediaTitle,
                    mediaArtist = mediaArtist,
                    mediaId = mediaId,
                    sourceAppPackage = sourceAppPackage,
                    albumArtUri = albumArtUri,
                    timestampMs = timestampMs,
                    durationMs = durationMs,
                    token = "Bearer $token"
                )
                
                if (response.isSuccessful) {
                    val bookmark = response.body()
                    Log.d(TAG, "Voice bookmark created successfully: ${bookmark?.id}")
                    _recordingState.value = _recordingState.value.copy(
                        isUploading = false,
                        uploadSuccess = true,
                        error = null
                    )
                } else {
                    Log.e(TAG, "Failed to create voice bookmark: ${response.code()}")
                    _recordingState.value = _recordingState.value.copy(
                        isUploading = false,
                        uploadSuccess = false,
                        error = "Failed to create bookmark: ${response.code()}"
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading voice bookmark", e)
                _recordingState.value = _recordingState.value.copy(
                    isUploading = false,
                    uploadSuccess = false,
                    error = "Upload failed: ${e.message}"
                )
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        if (_recordingState.value.isRecording) {
            stopRecording()
        }
        // Ensure audio focus is released when ViewModel is cleared
        releaseAudioFocus()
    }
}