package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SamplingPreset
import com.example.ui.components.SignalBar
import com.example.ui.theme.SensorAmber
import com.example.ui.theme.SensorGreen
import com.example.ui.theme.SensorRed
import com.example.ui.theme.TechCyanPrimary
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.SensorExplorerViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: SensorExplorerViewModel,
    modifier: Modifier = Modifier
) {
    val allSensors by viewModel.allSensorsMap.collectAsState()
    val activeSensorsCount = allSensors.values.count { it.isMonitored }
    val isPaused by viewModel.isAllPaused.collectAsState()
    val samplingPreset by viewModel.samplingPreset.collectAsState()
    val connectedWifi by viewModel.connectedWifi.collectAsState()
    val bleScanState by viewModel.bluetoothScanState.collectAsState()
    val bleAdapter by viewModel.bluetoothAdapterStatus.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "System Telemetry Overview",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Live monitoring of device hardware sensors, Wi-Fi signals, and Bluetooth beacons",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Metrics Grid (2x2)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Sensors Discovered",
                    value = "${allSensors.size}",
                    subText = "$activeSensorsCount active streams",
                    icon = Icons.Default.Sensors,
                    iconColor = TechCyanPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectTab(AppTab.SENSORS) }
                )
                MetricCard(
                    title = "Wi-Fi Status",
                    value = if (connectedWifi.isConnected) "${connectedWifi.rssiDbm} dBm" else "Disconnected",
                    subText = if (connectedWifi.isConnected) connectedWifi.ssid else "No connection",
                    icon = Icons.Default.Wifi,
                    iconColor = if (connectedWifi.isConnected) SensorGreen else SensorAmber,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectTab(AppTab.WIFI) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Bluetooth / BLE",
                    value = if (bleAdapter.isEnabled) "${bleScanState.discoveredDevices.size} BLE" else "Disabled",
                    subText = if (bleAdapter.isEnabled) "${bleScanState.bondedDevices.size} paired devices" else "Adapter off",
                    icon = Icons.Default.Bluetooth,
                    iconColor = if (bleAdapter.isEnabled) TechCyanPrimary else Color.Gray,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectTab(AppTab.BLUETOOTH) }
                )
                MetricCard(
                    title = "Battery & Health",
                    value = "${deviceInfo?.batteryPercent ?: 0}%",
                    subText = "${deviceInfo?.batteryStatus ?: "Checking"} · ${deviceInfo?.batteryTempCelsius ?: 0f}°C",
                    icon = Icons.Default.BatteryChargingFull,
                    iconColor = SensorGreen,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.selectTab(AppTab.DEVICE_INFO) }
                )
            }
        }

        // Global Sensor Stream Controller
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sensor_controller_card"),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Sensor Stream Controls",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (activeSensorsCount > 0) "$activeSensorsCount sensors currently running" else "All continuous listeners paused",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isPaused) {
                            Box(
                                modifier = Modifier
                                    .background(SensorAmber.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "GLOBAL PAUSE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = SensorAmber
                                )
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startAllSensors() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_all_sensors_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start All")
                        }

                        OutlinedButton(
                            onClick = { viewModel.stopAllSensors() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stop_all_sensors_button")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop All")
                        }

                        FilledTonalButton(
                            onClick = { viewModel.togglePauseAll() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pause_toggle_button")
                        ) {
                            Icon(
                                if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPaused) "Resume" else "Pause")
                        }
                    }

                    // Sampling Delay Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Sampling Frequency Preset:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SamplingPreset.entries.forEach { preset ->
                                val selected = samplingPreset == preset
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setSamplingPreset(preset) },
                                    label = { Text(preset.label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                        Text(
                            text = samplingPreset.description,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Live Wi-Fi quick card
        item {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectTab(AppTab.WIFI) },
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, tint = TechCyanPrimary)
                            Text(
                                text = "Wi-Fi Telemetry",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (connectedWifi.isConnected) {
                            SignalBar(rssiDbm = connectedWifi.rssiDbm)
                        }
                    }

                    if (connectedWifi.isConnected) {
                        Text(
                            text = "${connectedWifi.ssid} · ${connectedWifi.band} (${connectedWifi.frequencyMhz} MHz)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "IP: ${connectedWifi.ipAddress} · Speed: ${connectedWifi.linkSpeedMbps} Mbps · ${connectedWifi.wifiStandard}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Not connected to any Wi-Fi network. Tap to inspect Wi-Fi scanner and nearby access points.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Quick Bluetooth Card
        item {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectTab(AppTab.BLUETOOTH) },
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = TechCyanPrimary)
                            Text(
                                text = "Bluetooth & BLE Engine",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Text(
                            text = if (bleAdapter.isEnabled) "ACTIVE" else "DISABLED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (bleAdapter.isEnabled) SensorGreen else SensorRed
                        )
                    }

                    Text(
                        text = "Adapter: ${bleAdapter.name} · ${bleScanState.discoveredDevices.size} nearby BLE beacons discovered",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Device Specs Summary Card
        item {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectTab(AppTab.DEVICE_INFO) },
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = TechCyanPrimary)
                        Text(
                            text = "Device Platform Specs",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    deviceInfo?.let { dev ->
                        Text(
                            text = "${dev.manufacturer} ${dev.model} · Android ${dev.androidVersion} (API ${dev.apiLevel})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "CPU: ${dev.cpuCores} cores · RAM: ${dev.availableRamBytes / 1024 / 1024} MB free / ${dev.totalRamBytes / 1024 / 1024} MB total · Display: ${dev.displayWidthPx}x${dev.displayHeightPx} @ ${dev.refreshRateHz.toInt()}Hz",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subText: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = iconColor
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
