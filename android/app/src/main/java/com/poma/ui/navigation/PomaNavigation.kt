package com.poma.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.poma.ui.screens.HomeScreen
import com.poma.ui.screens.BookmarksScreen
import com.poma.ui.screens.SettingsScreen

@Composable
fun PomaNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController = navController)
        }
        
        composable("bookmarks") {
            BookmarksScreen(navController = navController)
        }
        
        composable("settings") {
            SettingsScreen(navController = navController)
        }
    }
}