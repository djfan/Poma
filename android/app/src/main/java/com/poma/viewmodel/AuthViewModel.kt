package com.poma.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val user: User? = null,
    val error: String = ""
)

data class User(
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String?
)

class AuthViewModel : ViewModel() {
    
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val apiService = AuthApiService() // TODO: 注入依赖
    
    init {
        // 启动时检查已保存的 token
        // checkAuthStatus() // Temporarily disabled to debug connection issues
    }
    
    fun signInWithGoogle(idToken: String) {
        android.util.Log.d("AuthViewModel", "signInWithGoogle called with token: ${idToken.take(50)}...")
        viewModelScope.launch {
            try {
                _authState.value = _authState.value.copy(
                    isLoading = true,
                    error = ""
                )
                
                // 调用后端验证 Google Token
                android.util.Log.d("AuthViewModel", "Sending request to backend...")
                val response = apiService.googleSignIn(idToken)
                
                if (response.isSuccessful) {
                    android.util.Log.d("AuthViewModel", "Backend response successful!")
                    val loginResponse = response.body()!!
                    
                    // 保存 JWT Token
                    TokenManager.saveToken(loginResponse.access_token)
                    
                    // 更新状态
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = loginResponse.user,
                        error = ""
                    )
                } else {
                    android.util.Log.e("AuthViewModel", "Backend response failed: ${response.code()}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "登录失败，请稍后重试"
                    )
                }
                
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Network error occurred", e)
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "网络错误: ${e.message}"
                )
            }
        }
    }
    
    fun signOut(context: android.content.Context? = null) {
        viewModelScope.launch {
            // 清除本地 token
            TokenManager.clearToken()
            
            // 清除 Google Sign-In 状态
            context?.let { ctx ->
                try {
                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                        com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                    ).build()
                    val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(ctx, gso)
                    googleSignInClient.signOut()
                    android.util.Log.d("AuthViewModel", "Google Sign-In cleared")
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "Error clearing Google Sign-In", e)
                }
            }
            
            // 重置状态
            _authState.value = AuthState()
            android.util.Log.d("AuthViewModel", "User signed out successfully")
        }
    }
    
    fun setError(message: String) {
        _authState.value = _authState.value.copy(error = message)
    }
    
    fun clearError() {
        _authState.value = _authState.value.copy(error = "")
    }
    
    fun checkAuthStatus() {
        viewModelScope.launch {
            val token = TokenManager.getToken()
            if (token != null && TokenManager.isTokenValid(token)) {
                // Token 有效，获取用户信息
                try {
                    val userResponse = apiService.getCurrentUser()
                    if (userResponse.isSuccessful) {
                        _authState.value = _authState.value.copy(
                            isLoggedIn = true,
                            user = userResponse.body()
                        )
                    } else {
                        // Token 可能已过期，清除
                        TokenManager.clearToken()
                    }
                } catch (e: Exception) {
                    TokenManager.clearToken()
                }
            }
        }
    }
}

// API 服务
class AuthApiService {
    private val baseUrl = "http://localhost:8001/api/v1/"
    private val retrofit = retrofit2.Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
        .build()
    
    private val api = retrofit.create(AuthApi::class.java)
    
    suspend fun googleSignIn(idToken: String): retrofit2.Response<LoginResponse> {
        val request = GoogleSignInRequest(idToken)
        return api.googleSignIn(request)
    }
    
    suspend fun getCurrentUser(): retrofit2.Response<User> {
        val token = TokenManager.getToken()
        return api.getCurrentUser("Bearer $token")
    }
}

interface AuthApi {
    @retrofit2.http.POST("auth/google")
    suspend fun googleSignIn(@retrofit2.http.Body request: GoogleSignInRequest): retrofit2.Response<LoginResponse>
    
    @retrofit2.http.GET("auth/me")  
    suspend fun getCurrentUser(@retrofit2.http.Header("Authorization") token: String): retrofit2.Response<User>
}

data class GoogleSignInRequest(
    val id_token: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val user: User
)

// Token 持久化管理 - 使用 SharedPreferences
object TokenManager {
    private const val PREFS_NAME = "poma_auth_prefs"
    private const val TOKEN_KEY = "auth_token"
    private const val TOKEN_EXPIRY_KEY = "token_expiry"
    
    private var context: android.content.Context? = null
    
    fun init(context: android.content.Context) {
        this.context = context.applicationContext
    }
    
    fun saveToken(token: String) {
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            prefs.edit()
                .putString(TOKEN_KEY, token)
                .putLong(TOKEN_EXPIRY_KEY, System.currentTimeMillis() + (24 * 60 * 60 * 1000)) // 24小时
                .apply()
            android.util.Log.d("TokenManager", "Token saved with 24h expiry")
        }
    }
    
    fun getToken(): String? {
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val token = prefs.getString(TOKEN_KEY, null)
            val expiry = prefs.getLong(TOKEN_EXPIRY_KEY, 0)
            
            if (token != null && System.currentTimeMillis() < expiry) {
                android.util.Log.d("TokenManager", "Valid token found")
                return token
            } else if (token != null) {
                android.util.Log.d("TokenManager", "Token expired, clearing")
                clearToken()
            }
        }
        return null
    }
    
    fun clearToken() {
        context?.let { ctx ->
            val prefs = ctx.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            prefs.edit()
                .remove(TOKEN_KEY)
                .remove(TOKEN_EXPIRY_KEY)
                .apply()
            android.util.Log.d("TokenManager", "Token cleared")
        }
    }
    
    fun isTokenValid(token: String): Boolean {
        return token.isNotEmpty() && getToken() == token
    }
}