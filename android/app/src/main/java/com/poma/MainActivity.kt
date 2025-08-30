package com.poma

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.poma.ui.navigation.PomaNavigation
import com.poma.ui.theme.PomaTheme
import com.poma.viewmodel.TokenManager
import com.poma.viewmodel.SpotifyViewModel

class MainActivity : ComponentActivity() {
    
    companion object {
        var spotifyCallbackHandler: ((String) -> Unit)? = null
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.e("MainActivity", "=== POMA MainActivity onCreate called! ===")
        
        // 初始化 TokenManager
        TokenManager.init(this)
        
        // Handle Spotify OAuth callback
        handleSpotifyIntent(intent)
        
        setContent {
            PomaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PomaNavigation()
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSpotifyIntent(intent)
    }
    
    private fun handleSpotifyIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        if (data != null && data.scheme == "poma" && data.host == "spotify") {
            val code = data.getQueryParameter("code")
            if (code != null) {
                android.util.Log.d("MainActivity", "Received Spotify auth code: ${code.take(20)}...")
                spotifyCallbackHandler?.invoke(code)
            } else {
                android.util.Log.e("MainActivity", "No auth code in Spotify callback")
            }
        }
    }
}