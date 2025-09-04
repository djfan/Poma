package com.poma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.net.Uri
import com.poma.viewmodel.SpotifyViewModel
import com.poma.viewmodel.BluetoothViewModel
import com.poma.viewmodel.DeviceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    spotifyViewModel: SpotifyViewModel = viewModel(),
    bluetoothViewModel: BluetoothViewModel = viewModel()
) {
    val context = LocalContext.current
    val bluetoothState by bluetoothViewModel.bluetoothState.collectAsState()
    
    // Initialize Bluetooth when the screen loads
    LaunchedEffect(Unit) {
        bluetoothViewModel.initializeBluetooth(context)
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        // Bluetooth Devices Section
        item {
            BluetoothDevicesCard(
                bluetoothState = bluetoothState,
                onRefresh = { bluetoothViewModel.scanForDevices() }
            )
        }
        
        // Headphone Controls Section
        item {
            HeadphoneControlsCard(bluetoothState = bluetoothState)
        }
        
        // Quick Tap Configuration
        item {
            QuickTapCard()
        }
        
        // Spotify Integration
        item {
            SpotifyIntegrationCard(spotifyViewModel = spotifyViewModel)
        }
    }
}

@Composable
fun BluetoothDevicesCard(
    bluetoothState: com.poma.viewmodel.BluetoothState,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Bluetooth",
                        tint = if (bluetoothState.isBluetoothEnabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bluetooth Devices",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                
                if (bluetoothState.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    TextButton(onClick = onRefresh) {
                        Text("Scan")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            when {
                !bluetoothState.isBluetoothEnabled -> {
                    Text(
                        text = "⚠️ Bluetooth is disabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Enable Bluetooth to detect headphones",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                !bluetoothState.hasBluetoothPermission -> {
                    Text(
                        text = "⚠️ Bluetooth permissions required",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Grant Bluetooth permissions to detect devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                bluetoothState.error != null -> {
                    Text(
                        text = "❌ ${bluetoothState.error}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                bluetoothState.connectedDevices.isEmpty() && bluetoothState.pairedDevices.isEmpty() -> {
                    Text(
                        text = "No Bluetooth devices found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Make sure your headphones are paired and connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                else -> {
                    // Connected devices
                    if (bluetoothState.connectedDevices.isNotEmpty()) {
                        Text(
                            text = "📱 Connected",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        bluetoothState.connectedDevices.forEach { device ->
                            DeviceItem(device = device, isConnected = true)
                        }
                        
                        if (bluetoothState.pairedDevices.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    // Paired devices
                    if (bluetoothState.pairedDevices.isNotEmpty()) {
                        Text(
                            text = "🔗 Paired",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        bluetoothState.pairedDevices.take(3).forEach { device ->
                            DeviceItem(device = device, isConnected = false)
                        }
                        
                        if (bluetoothState.pairedDevices.size > 3) {
                            Text(
                                text = "+ ${bluetoothState.pairedDevices.size - 3} more devices",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 24.dp, top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceItem(
    device: com.poma.viewmodel.BluetoothDeviceInfo,
    isConnected: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (device.deviceType) {
                DeviceType.PIXEL_BUDS_PRO, DeviceType.PIXEL_BUDS, DeviceType.HEADPHONES -> Icons.Default.PlayArrow
                else -> Icons.Default.Settings
            },
            contentDescription = null,
            tint = when {
                device.deviceType == DeviceType.PIXEL_BUDS_PRO -> Color(0xFF1ED760) // Spotify green
                isConnected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (device.deviceType == DeviceType.PIXEL_BUDS_PRO) 
                        FontWeight.Medium else FontWeight.Normal
                )
            )
            
            val statusText = when {
                device.deviceType == DeviceType.PIXEL_BUDS_PRO -> "✨ Pixel Buds Pro - Optimized"
                device.deviceType == DeviceType.PIXEL_BUDS -> "Pixel Buds"
                device.deviceType == DeviceType.HEADPHONES -> "Audio Device"
                else -> "Bluetooth Device"
            }
            
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isConnected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Connected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun HeadphoneControlsCard(bluetoothState: com.poma.viewmodel.BluetoothState) {
    val hasPixelBudsProConnected = bluetoothState.connectedDevices.any { 
        it.deviceType == DeviceType.PIXEL_BUDS_PRO 
    }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Headphone Controls",
                    tint = if (hasPixelBudsProConnected) 
                        Color(0xFF1ED760) 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Headphone Controls",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (hasPixelBudsProConnected) {
                Text(
                    text = "🎧 Pixel Buds Pro Detected",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF1ED760)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Available media button controls:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "• Single tap (play/pause): Quick bookmark",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Double tap (next track): Start recording",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Triple tap (prev track): Stop recording",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "💡 Native gestures preserved:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Long press left: Noise cancellation toggle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Long press right: Google Assistant",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "⚠️ Media controls work alongside Spotify without interfering",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Connect Pixel Buds Pro for optimized controls",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Other Bluetooth headphones may work with basic media button support",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuickTapCard() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📱",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Quick Tap (Pixel 7a)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Set up Quick Tap gesture:",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "1. Go to Settings > System > Gestures",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "2. Select 'Quick Tap to start actions'",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "3. Choose 'Open app' → Select 'Poma'",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "4. Double-tap phone back to quick bookmark",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "💡 Quick Tap will create instant bookmarks without recording",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF1ED760)
            )
        }
    }
}

@Composable
fun SpotifyIntegrationCard(spotifyViewModel: SpotifyViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎵",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Spotify Integration",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Connect Spotify for enhanced bookmark features:",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "• Direct jump to bookmarked timestamp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Rich episode metadata and artwork",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "• Seamless podcast discovery",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = { spotifyViewModel.initiateSpotifyConnection() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Connect Spotify Account")
            }
        }
        
        // Developer Options - Backend Switching
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            var isUsingLocal by remember { mutableStateOf(com.poma.config.ApiConfig.isUsingLocalBackend()) }
            
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = "Backend Config",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Backend Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Current: ${if (isUsingLocal) "Local Development" else "Cloud Production"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = {
                            com.poma.config.ApiConfig.switchToLocal()
                            isUsingLocal = true
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isUsingLocal) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                    ) {
                        Text("Local Dev", color = if (isUsingLocal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                    }
                    
                    OutlinedButton(
                        onClick = {
                            com.poma.config.ApiConfig.switchToCloud()
                            isUsingLocal = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (!isUsingLocal) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                    ) {
                        Text("Cloud Prod", color = if (!isUsingLocal) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}