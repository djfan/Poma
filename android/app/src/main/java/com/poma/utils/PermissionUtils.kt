package com.poma.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object PermissionUtils {
    
    const val RECORD_AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
    
    fun hasRecordAudioPermission(context: Context): Boolean {
        val result = ContextCompat.checkSelfPermission(
            context,
            RECORD_AUDIO_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
        android.util.Log.d("PermissionUtils", "hasRecordAudioPermission result: $result")
        return result
    }
    
    fun requestRecordAudioPermission(
        launcher: ActivityResultLauncher<String>
    ) {
        launcher.launch(RECORD_AUDIO_PERMISSION)
    }
}