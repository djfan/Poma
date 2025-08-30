package com.poma.ui.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.poma.ui.screens.HomeScreen
import com.poma.ui.screens.BookmarksScreen
import com.poma.ui.screens.SettingsScreen
import com.poma.ui.screens.LoginScreen
import com.poma.viewmodel.AuthViewModel
import com.poma.viewmodel.SpotifyViewModel
import com.poma.MainActivity

@Composable
fun PomaNavigation() {
    val navController = rememberNavController()
    val sharedAuthViewModel: AuthViewModel = viewModel()
    val sharedSpotifyViewModel: SpotifyViewModel = viewModel()
    val authState by sharedAuthViewModel.authState.collectAsState()
    
    // Set up Spotify OAuth callback handler
    LaunchedEffect(Unit) {
        MainActivity.spotifyCallbackHandler = { code ->
            sharedSpotifyViewModel.handleSpotifyCallback(code)
        }
    }
    
    // 根据认证状态自动导航
    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            navController.navigate("home") {
                popUpTo(0) { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(navController = navController, authViewModel = sharedAuthViewModel)
        }
        
        composable("home") {
            HomeScreen(
                navController = navController, 
                authViewModel = sharedAuthViewModel,
                spotifyViewModel = sharedSpotifyViewModel
            )
        }
        
        composable("bookmarks") {
            BookmarksScreen(navController = navController)
        }
        
        composable("settings") {
            SettingsScreen(navController = navController)
        }
    }
}