package com.poma.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Body

data class BookmarkItem(
    val id: Int,
    val podcast_name: String,
    val episode_name: String,
    val timestamp_ms: Long,
    val duration_ms: Long?,
    val spotify_episode_id: String?,
    val podcast_cover_url: String?,
    val audio_file_path: String?,
    val transcript_text: String?,
    val user_note: String?,
    val ai_summary: String?,
    val created_at: String,
    // New MediaSession fields for deep linking
    val media_id: String? = null,
    val source_app_package: String? = null,
    val album_art_uri: String? = null
)

data class BookmarksState(
    val bookmarks: List<BookmarkItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class UpdateBookmarkRequest(
    val transcript_text: String
)

interface BookmarksApi {
    @GET("api/v1/bookmarks/")
    suspend fun getBookmarks(@Header("Authorization") token: String): retrofit2.Response<List<BookmarkItem>>
    
    @DELETE("api/v1/bookmarks/{id}")
    suspend fun deleteBookmark(@Header("Authorization") token: String, @Path("id") id: Int): retrofit2.Response<Unit>
    
    @PUT("api/v1/bookmarks/{id}")
    suspend fun updateBookmark(@Header("Authorization") token: String, @Path("id") id: Int, @Body request: UpdateBookmarkRequest): retrofit2.Response<BookmarkItem>
}

class BookmarksViewModel : ViewModel() {
    
    private val _bookmarksState = MutableStateFlow(BookmarksState())
    val bookmarksState: StateFlow<BookmarksState> = _bookmarksState.asStateFlow()
    
    // API client
    private val apiService: BookmarksApi by lazy {
        Retrofit.Builder()
            .baseUrl("http://localhost:8001/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BookmarksApi::class.java)
    }
    
    companion object {
        private const val TAG = "BookmarksViewModel"
    }
    
    init {
        loadBookmarks()
    }
    
    fun loadBookmarks() {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "Loading bookmarks...")
                
                _bookmarksState.value = _bookmarksState.value.copy(
                    isLoading = true,
                    error = null
                )
                
                // Get authentication token
                val token = TokenManager.getToken()
                if (token == null) {
                    _bookmarksState.value = _bookmarksState.value.copy(
                        isLoading = false,
                        error = "Authentication required"
                    )
                    return@launch
                }
                
                val response = apiService.getBookmarks("Bearer $token")
                
                if (response.isSuccessful) {
                    val bookmarks = response.body() ?: emptyList()
                    android.util.Log.d(TAG, "Loaded ${bookmarks.size} bookmarks for current user")
                    _bookmarksState.value = BookmarksState(
                        bookmarks = bookmarks,
                        isLoading = false,
                        error = null
                    )
                } else {
                    android.util.Log.e(TAG, "Failed to load bookmarks: ${response.code()}")
                    _bookmarksState.value = _bookmarksState.value.copy(
                        isLoading = false,
                        error = "Failed to load bookmarks: ${response.code()}"
                    )
                }
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error loading bookmarks", e)
                _bookmarksState.value = _bookmarksState.value.copy(
                    isLoading = false,
                    error = "Error loading bookmarks: ${e.message}"
                )
            }
        }
    }
    
    fun refreshBookmarks() {
        loadBookmarks()
    }
    
    fun deleteBookmark(bookmarkId: Int) {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "Deleting bookmark $bookmarkId...")
                
                val token = TokenManager.getToken()
                if (token == null) {
                    android.util.Log.e(TAG, "No authentication token")
                    return@launch
                }
                
                val response = apiService.deleteBookmark("Bearer $token", bookmarkId)
                
                if (response.isSuccessful) {
                    android.util.Log.d(TAG, "Bookmark $bookmarkId deleted successfully")
                    // Remove from local state
                    _bookmarksState.value = _bookmarksState.value.copy(
                        bookmarks = _bookmarksState.value.bookmarks.filter { it.id != bookmarkId }
                    )
                } else {
                    android.util.Log.e(TAG, "Failed to delete bookmark: ${response.code()}")
                }
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error deleting bookmark", e)
            }
        }
    }
    
    fun editBookmark(bookmarkId: Int, newText: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "Editing bookmark $bookmarkId...")
                
                val token = TokenManager.getToken()
                if (token == null) {
                    android.util.Log.e(TAG, "No authentication token")
                    return@launch
                }
                
                val request = UpdateBookmarkRequest(transcript_text = newText)
                val response = apiService.updateBookmark("Bearer $token", bookmarkId, request)
                
                if (response.isSuccessful) {
                    android.util.Log.d(TAG, "Bookmark $bookmarkId updated successfully")
                    // Update local state
                    val updatedBookmarks = _bookmarksState.value.bookmarks.map { bookmark ->
                        if (bookmark.id == bookmarkId) {
                            bookmark.copy(transcript_text = newText)
                        } else {
                            bookmark
                        }
                    }
                    _bookmarksState.value = _bookmarksState.value.copy(bookmarks = updatedBookmarks)
                } else {
                    android.util.Log.e(TAG, "Failed to update bookmark: ${response.code()}")
                }
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error updating bookmark", e)
            }
        }
    }
}