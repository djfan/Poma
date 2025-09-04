package com.poma.config

import android.content.Context
import android.content.SharedPreferences
import com.poma.R

/**
 * API配置管理
 * 支持开发时在Local和Cloud后端之间切换
 */
object ApiConfig {
    private const val PREFS_NAME = "api_config"
    private const val KEY_USE_LOCAL_BACKEND = "use_local_backend"
    
    private var sharedPrefs: SharedPreferences? = null
    private var context: Context? = null
    
    fun init(context: Context) {
        this.context = context
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private fun getLocalBackendUrl(): String {
        return context?.getString(R.string.local_backend_url) ?: "http://localhost:8001/"
    }
    
    private fun getCloudBackendUrl(): String {
        return context?.getString(R.string.cloud_backend_url) ?: "https://poma-2sxi.onrender.com/"
    }
    
    private fun getDefaultUseLocal(): Boolean {
        return context?.resources?.getBoolean(R.bool.debug_use_local_backend) ?: true
    }
    
    /**
     * 获取当前API Base URL
     */
    fun getBaseUrl(): String {
        return if (isUsingLocalBackend()) {
            getLocalBackendUrl()
        } else {
            getCloudBackendUrl()
        }
    }
    
    /**
     * 获取API v1 URL (for AuthViewModel)
     */
    fun getApiV1Url(): String {
        return "${getBaseUrl()}api/v1/"
    }
    
    /**
     * 是否使用本地后端
     */
    fun isUsingLocalBackend(): Boolean {
        return sharedPrefs?.getBoolean(KEY_USE_LOCAL_BACKEND, getDefaultUseLocal()) ?: getDefaultUseLocal()
    }
    
    /**
     * 设置后端类型
     * @param useLocal true=使用本地后端, false=使用云端后端
     */
    fun setBackendType(useLocal: Boolean) {
        sharedPrefs?.edit()?.putBoolean(KEY_USE_LOCAL_BACKEND, useLocal)?.apply()
    }
    
    /**
     * 切换到本地后端
     */
    fun switchToLocal() {
        setBackendType(true)
    }
    
    /**
     * 切换到云端后端  
     */
    fun switchToCloud() {
        setBackendType(false)
    }
    
    /**
     * 获取当前后端类型描述
     */
    fun getCurrentBackendDescription(): String {
        return if (isUsingLocalBackend()) {
            "Local Development (${getLocalBackendUrl()})"
        } else {
            "Cloud Production (${getCloudBackendUrl()})"
        }
    }
    
    /**
     * 是否显示开发者选项 (总是显示，便于开发)
     */
    fun shouldShowDeveloperOptions(): Boolean {
        return true // Always show for now
    }
}