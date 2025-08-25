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
        viewModelScope.launch {
            try {
                _authState.value = _authState.value.copy(
                    isLoading = true,
                    error = ""
                )
                
                // 调用后端验证 Google Token
                val response = apiService.googleSignIn(idToken)
                
                if (response.isSuccessful) {
                    val loginResponse = response.body()!!
                    
                    // 保存 JWT Token
                    TokenManager.saveToken(loginResponse.accessToken)
                    
                    // 更新状态
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        user = loginResponse.user,
                        error = ""
                    )
                } else {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "登录失败，请稍后重试"
                    )
                }
                
            } catch (e: Exception) {
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

// TODO: 实现 API 服务
class AuthApiService {
    suspend fun googleSignIn(idToken: String): retrofit2.Response<LoginResponse> {
        // 调用后端 API
        TODO("实现 Google 登录 API 调用")
    }
    
    suspend fun getCurrentUser(): retrofit2.Response<User> {
        // 获取当前用户信息
        TODO("实现获取用户信息 API 调用")
    }
}

data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    val user: User
)

// TODO: 实现 Token 管理
object TokenManager {
    fun saveToken(token: String) {
        // 使用 EncryptedSharedPreferences 保存
        TODO("实现 Token 保存")
    }
    
    fun getToken(): String? {
        // 从 EncryptedSharedPreferences 获取
        TODO("实现 Token 获取")
    }
    
    fun clearToken() {
        // 清除保存的 Token
        TODO("实现 Token 清除")
    }
    
    fun isTokenValid(token: String): Boolean {
        // 检查 Token 是否过期
        TODO("实现 Token 验证")
    }
}