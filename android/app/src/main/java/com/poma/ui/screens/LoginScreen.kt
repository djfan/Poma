package com.poma.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.poma.R
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.poma.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    
    // Google Sign-In launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                android.util.Log.d("GoogleSignIn", "Account: ${account.email}")
                account.idToken?.let { idToken ->
                    android.util.Log.d("GoogleSignIn", "ID Token received: ${idToken.take(50)}...")
                    authViewModel.signInWithGoogle(idToken)
                } ?: run {
                    android.util.Log.e("GoogleSignIn", "ID Token is null!")
                    authViewModel.setError("Authentication failed")
                }
            } catch (e: ApiException) {
                // Handle error
                android.util.Log.e("GoogleSignIn", "Google Sign-In failed with status code: ${e.statusCode}", e)
                authViewModel.setError("Google Sign-In failed: ${e.statusCode}")
            }
        } else {
            android.util.Log.w("GoogleSignIn", "Result not OK: ${result.resultCode}")
        }
    }
    
    // Configure Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.google_client_id))
            .requestEmail()
            .requestProfile()
            .build()
    }
    
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }
    
    // Debug logging
    LaunchedEffect(Unit) {
        android.util.Log.e("LoginScreen", "=== POMA LoginScreen composed! ===")
    }
    
    // Debug auth state changes
    LaunchedEffect(authState.isLoggedIn) {
        android.util.Log.d("LoginScreen", "Auth state changed: isLoggedIn=${authState.isLoggedIn}")
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // POMA Logo
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "POMA Logo",
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 32.dp)
        )
        
        Text(
            text = "P O M A",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                letterSpacing = 8.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            ),
            color = androidx.compose.ui.graphics.Color(0xFF1ED760), // Spotify green
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Podcast Bookmarks",
            style = MaterialTheme.typography.bodyLarge.copy(
fontWeight = FontWeight.Normal
            ),
            textAlign = TextAlign.Center,
            color = androidx.compose.ui.graphics.Color(0xFFB3B3B3) // Spotify gray
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Google Sign-In button
        Button(
            onClick = {
                android.util.Log.e("LoginScreen", "=== POMA Google Sign-In button clicked! ===")
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            },
            modifier = Modifier
                .size(64.dp),
            enabled = !authState.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = androidx.compose.ui.graphics.Color(0xFF1ED760) // Spotify green
            ),
            shape = RoundedCornerShape(32.dp)
        ) {
            if (authState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                // Simple Google G logo
                Text(
                    text = "G",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.Black
                    )
                )
            }
        }
        
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Error display
        if (authState.error.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color(0xFFCF6679) // Error red
                )
            ) {
                Text(
                    text = authState.error,
                    modifier = Modifier.padding(16.dp),
                    color = androidx.compose.ui.graphics.Color.Black,
                    style = MaterialTheme.typography.bodyMedium.copy(
        fontWeight = FontWeight.Normal
                    )
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "By signing in, you agree to our Terms of Service",
            style = MaterialTheme.typography.labelSmall.copy(
fontWeight = FontWeight.Normal
            ),
            textAlign = TextAlign.Center,
            color = androidx.compose.ui.graphics.Color(0xFFB3B3B3) // Spotify gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "v3.0.0-production (Build 46) - Cloud Ready",
            style = MaterialTheme.typography.labelSmall.copy(
fontWeight = FontWeight.Normal
            ),
            textAlign = TextAlign.Center,
            color = androidx.compose.ui.graphics.Color(0xFF535353) // Spotify light gray
        )
    }
}