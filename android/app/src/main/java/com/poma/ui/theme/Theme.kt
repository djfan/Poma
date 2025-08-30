package com.poma.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Spotify-inspired Dark Color Scheme
private val SpotifyDarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = SpotifyBlack,
    secondary = SpotifyGreen,
    onSecondary = SpotifyBlack,
    tertiary = SpotifyGreen,
    onTertiary = SpotifyBlack,
    
    background = SpotifyBlack,
    onBackground = SpotifyWhite,
    
    surface = SpotifyBlack,
    onSurface = SpotifyWhite,
    surfaceVariant = SpotifyDarkGray,
    onSurfaceVariant = SpotifyGray,
    
    error = Color(0xFFCF6679),
    onError = SpotifyBlack,
    
    outline = SpotifyLightGray,
    outlineVariant = SpotifyDarkGray
)

@Composable
fun PomaTheme(
    darkTheme: Boolean = true, // Force dark theme for Spotify style
    content: @Composable () -> Unit
) {
    // Always use Spotify dark color scheme
    val colorScheme = SpotifyDarkColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SpotifyBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}