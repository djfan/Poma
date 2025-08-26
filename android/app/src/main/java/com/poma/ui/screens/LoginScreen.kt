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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.poma.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
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
                    authViewModel.setError("ID Token 获取失败")
                }
            } catch (e: ApiException) {
                // 处理错误
                android.util.Log.e("GoogleSignIn", "Google Sign-In failed with status code: ${e.statusCode}", e)
                authViewModel.setError("Google 登录失败: ${e.statusCode} - ${e.message}")
            }
        } else {
            android.util.Log.w("GoogleSignIn", "Result not OK: ${result.resultCode}")
        }
    }
    
    // 配置 Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("882585452174-e4ehsoof2jm9ccs3olh16t5gk9mtokkq.apps.googleusercontent.com")
            .requestEmail()
            .requestProfile()
            .build()
    }
    
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }
    
    // 测试日志
    LaunchedEffect(Unit) {
        android.util.Log.e("LoginScreen", "=== POMA LoginScreen composed! ===")
    }
    
    // 监听登录状态，成功后跳转
    LaunchedEffect(authState.isLoggedIn) {
        android.util.Log.d("LoginScreen", "Auth state changed: isLoggedIn=${authState.isLoggedIn}")
        if (authState.isLoggedIn) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo 占位符
        Card(
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎧",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }
        
        Text(
            text = "欢迎使用 Poma",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "播客版 Kindle Highlights\n用耳机一键记录你的想法",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Google 登录按钮
        Button(
            onClick = {
                android.util.Log.e("LoginScreen", "=== POMA Google Sign-In button clicked! ===")
                val signInIntent = googleSignInClient.signInIntent
                launcher.launch(signInIntent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !authState.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (authState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google 图标占位符
                    Text(
                        text = "🔍",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "使用 Google 账号登录",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 邮箱登录按钮（暂未实现）
        OutlinedButton(
            onClick = { /* TODO: 实现邮箱登录 */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = false,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("📧 邮箱密码登录（即将推出）")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 错误信息显示
        if (authState.error.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = authState.error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "登录即表示您同意我们的服务条款和隐私政策",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "v1.0.11-debug (Build 12) - ADB Port Forwarding",
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outline
        )
    }
}