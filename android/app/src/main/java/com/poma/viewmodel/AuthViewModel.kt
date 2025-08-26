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
    
    fun signOut() {
        viewModelScope.launch {
            TokenManager.clearToken()
            _authState.value = AuthState()
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

// TODO: 实现 Token 管理
object TokenManager {
    private var token: String? = null
    
    fun saveToken(token: String) {
        this.token = token
    }
    
    fun getToken(): String? {
        return token
    }
    
    fun clearToken() {
        token = null
    }
    
    fun isTokenValid(token: String): Boolean {
        return token.isNotEmpty()
    }
}