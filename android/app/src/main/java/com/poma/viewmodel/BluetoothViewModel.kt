package com.poma.viewmodel

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isConnected: Boolean,
    val deviceType: DeviceType,
    val bondState: Int
)

enum class DeviceType {
    HEADPHONES,
    PIXEL_BUDS_PRO,
    PIXEL_BUDS,
    SPEAKER,
    PHONE,
    UNKNOWN
}

data class BluetoothState(
    val isBluetoothEnabled: Boolean = false,
    val hasBluetoothPermission: Boolean = false,
    val connectedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val pairedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val isScanning: Boolean = false,
    val error: String? = null
)

class BluetoothViewModel : ViewModel() {
    
    private val _bluetoothState = MutableStateFlow(BluetoothState())
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()
    
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothReceiver: BroadcastReceiver? = null
    private var context: Context? = null
    
    companion object {
        private const val TAG = "BluetoothViewModel"
        
        // Pixel Buds Pro identifying patterns
        private val PIXEL_BUDS_PRO_PATTERNS = listOf(
            "Pixel Buds Pro",
            "Google Pixel Buds Pro"
        )
        
        // General Pixel Buds patterns
        private val PIXEL_BUDS_PATTERNS = listOf(
            "Pixel Buds",
            "Google Pixel Buds"
        )
        
        // Audio device class codes
        private const val AUDIO_VIDEO_HEADPHONES = 0x0404
        private const val AUDIO_VIDEO_PORTABLE_AUDIO = 0x041C
        private const val AUDIO_VIDEO_WEARABLE_HEADSET = 0x0404
    }
    
    fun initializeBluetooth(context: Context) {
        this.context = context
        
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        
        checkBluetoothState()
        setupBluetoothReceiver(context)
        if (hasBluetoothPermissions(context)) {
            scanForDevices()
        }
    }
    
    private fun checkBluetoothState() {
        val hasPermission = context?.let { hasBluetoothPermissions(it) } ?: false
        val isEnabled = bluetoothAdapter?.isEnabled ?: false
        
        _bluetoothState.value = _bluetoothState.value.copy(
            isBluetoothEnabled = isEnabled,
            hasBluetoothPermission = hasPermission
        )
    }
    
    private fun hasBluetoothPermissions(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12+ requires BLUETOOTH_CONNECT permission
            ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Pre-Android 12 uses BLUETOOTH permission
            ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun setupBluetoothReceiver(context: Context) {
        if (bluetoothReceiver != null) return
        
        bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        checkBluetoothState()
                    }
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            Log.d(TAG, "Device connected: ${it.name}")
                            refreshDeviceList()
                        }
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            Log.d(TAG, "Device disconnected: ${it.name}")
                            refreshDeviceList()
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        
        context.registerReceiver(bluetoothReceiver, filter)
    }
    
    fun scanForDevices() {
        viewModelScope.launch {
            try {
                _bluetoothState.value = _bluetoothState.value.copy(isScanning = true, error = null)
                
                val context = this@BluetoothViewModel.context
                if (context == null || bluetoothAdapter == null) {
                    _bluetoothState.value = _bluetoothState.value.copy(
                        isScanning = false,
                        error = "Bluetooth not initialized"
                    )
                    return@launch
                }
                
                if (!hasBluetoothPermissions(context)) {
                    _bluetoothState.value = _bluetoothState.value.copy(
                        isScanning = false,
                        error = "Bluetooth permissions required"
                    )
                    return@launch
                }
                
                // Get paired devices
                val pairedDevices = bluetoothAdapter!!.bondedDevices?.mapNotNull { device ->
                    createBluetoothDeviceInfo(device, false)
                } ?: emptyList()
                
                // Get connected audio devices
                val connectedDevices = getConnectedAudioDevices()
                
                _bluetoothState.value = _bluetoothState.value.copy(
                    pairedDevices = pairedDevices,
                    connectedDevices = connectedDevices,
                    isScanning = false
                )
                
                Log.d(TAG, "Found ${pairedDevices.size} paired devices, ${connectedDevices.size} connected")
                
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception during device scan", e)
                _bluetoothState.value = _bluetoothState.value.copy(
                    isScanning = false,
                    error = "Permission denied: ${e.message}"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error during device scan", e)
                _bluetoothState.value = _bluetoothState.value.copy(
                    isScanning = false,
                    error = "Scan failed: ${e.message}"
                )
            }
        }
    }
    
    private fun getConnectedAudioDevices(): List<BluetoothDeviceInfo> {
        val context = this.context ?: return emptyList()
        val bluetoothAdapter = this.bluetoothAdapter ?: return emptyList()
        
        return try {
            if (!hasBluetoothPermissions(context)) return emptyList()
            
            val connectedDevices = mutableListOf<BluetoothDeviceInfo>()
            
            // Get A2DP (Advanced Audio Distribution Profile) connected devices
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            
            // Check connected devices for common audio profiles
            val connectedDeviceAddresses = bluetoothAdapter.bondedDevices
                .filter { device ->
                    try {
                        // This is a simplified check - in a real app you'd use BluetoothProfile.ServiceListener
                        val method = device.javaClass.getMethod("isConnected")
                        method.invoke(device) as? Boolean ?: false
                    } catch (e: Exception) {
                        false
                    }
                }
                .mapNotNull { device ->
                    createBluetoothDeviceInfo(device, true)
                }
            
            connectedDevices.addAll(connectedDeviceAddresses)
            connectedDevices
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connected devices", e)
            emptyList()
        }
    }
    
    private fun refreshDeviceList() {
        scanForDevices()
    }
    
    private fun createBluetoothDeviceInfo(device: BluetoothDevice, isConnected: Boolean): BluetoothDeviceInfo? {
        return try {
            val context = this.context ?: return null
            if (!hasBluetoothPermissions(context)) return null
            
            val deviceName = device.name ?: "Unknown Device"
            val deviceType = determineDeviceType(deviceName, device.bluetoothClass?.deviceClass ?: 0)
            
            BluetoothDeviceInfo(
                name = deviceName,
                address = device.address,
                isConnected = isConnected,
                deviceType = deviceType,
                bondState = device.bondState
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied accessing device info", e)
            null
        }
    }
    
    private fun determineDeviceType(name: String, deviceClass: Int): DeviceType {
        return when {
            PIXEL_BUDS_PRO_PATTERNS.any { pattern -> 
                name.contains(pattern, ignoreCase = true) 
            } -> DeviceType.PIXEL_BUDS_PRO
            
            PIXEL_BUDS_PATTERNS.any { pattern -> 
                name.contains(pattern, ignoreCase = true) 
            } -> DeviceType.PIXEL_BUDS
            
            deviceClass == AUDIO_VIDEO_HEADPHONES || 
            deviceClass == AUDIO_VIDEO_WEARABLE_HEADSET ||
            name.contains("headphone", ignoreCase = true) ||
            name.contains("buds", ignoreCase = true) ||
            name.contains("earphone", ignoreCase = true) -> DeviceType.HEADPHONES
            
            deviceClass == AUDIO_VIDEO_PORTABLE_AUDIO ||
            name.contains("speaker", ignoreCase = true) -> DeviceType.SPEAKER
            
            name.contains("phone", ignoreCase = true) -> DeviceType.PHONE
            
            else -> DeviceType.UNKNOWN
        }
    }
    
    fun getPixelBudsProDevice(): BluetoothDeviceInfo? {
        val allDevices = _bluetoothState.value.connectedDevices + _bluetoothState.value.pairedDevices
        return allDevices.firstOrNull { it.deviceType == DeviceType.PIXEL_BUDS_PRO }
    }
    
    fun hasPixelBudsProConnected(): Boolean {
        return _bluetoothState.value.connectedDevices.any { it.deviceType == DeviceType.PIXEL_BUDS_PRO }
    }
    
    override fun onCleared() {
        super.onCleared()
        context?.let { ctx ->
            bluetoothReceiver?.let { receiver ->
                try {
                    ctx.unregisterReceiver(receiver)
                } catch (e: Exception) {
                    Log.e(TAG, "Error unregistering receiver", e)
                }
            }
        }
    }
}