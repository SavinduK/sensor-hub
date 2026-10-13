package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BleDiscoveredDevice
import com.example.data.model.ClassicBondedDevice
import com.example.ui.components.PathLossConfigDialog
import com.example.ui.components.SignalBar
import com.example.ui.theme.SensorAmber
import com.example.ui.theme.SensorGreen
import com.example.ui.theme.SensorRed
import com.example.ui.theme.TechCyanPrimary
import com.example.ui.viewmodel.SensorExplorerViewModel
import com.example.util.DistanceEstimator

@Composable
fun BluetoothScreen(
    viewModel: SensorExplorerViewModel,
    modifier: Modifier = Modifier
) {
    val adapterStatus by viewModel.bluetoothAdapterStatus.collectAsState()
    val scanState by viewModel.bluetoothScanState.collectAsState()
    val refRssi by viewModel.bleRefRssi.collectAsState()
    val pathLossN by viewModel.blePathLossN.collectAsState()

    var showCalibrationDialog by remember { mutableStateOf(false) }
    var showClassicDevices by remember { mutableStateOf(false) }

    if (showCalibrationDialog) {
        PathLossConfigDialog(
            title = "Bluetooth BLE Distance Calibration",
            initialRefRssi = refRssi,
            initialExponent = pathLossN,
            onDismiss = { showCalibrationDialog = false },
            onSave = { r, n -> viewModel.updateBleCalibration(r, n) }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bluetooth & BLE Engine",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Hardware adapter state, BLE advertisements, and paired devices",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.refreshBluetooth() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Bluetooth status")
                }
            }
        }

        // Adapter Status Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bluetooth_adapter_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                text = "Adapter Hardware Status",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    if (adapterStatus.isEnabled) SensorGreen.copy(alpha = 0.15f) else SensorRed.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (adapterStatus.isEnabled) "ENABLED" else "DISABLED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = if (adapterStatus.isEnabled) SensorGreen else SensorRed
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        InfoRow("Device Name", adapterStatus.name)
                        InfoRow("MAC Address", adapterStatus.address)
                        InfoRow("LE 2M PHY (High Speed)", if (adapterStatus.isLe2MPhySupported) "Supported" else "Unsupported")
                        InfoRow("LE Coded PHY (Long Range)", if (adapterStatus.isLeCodedPhySupported) "Supported" else "Unsupported")
                        InfoRow("Extended Advertising", if (adapterStatus.isLeExtendedAdvertisingSupported) "Supported" else "Unsupported")
                        InfoRow("Periodic Advertising", if (adapterStatus.isLePeriodicAdvertisingSupported) "Supported" else "Unsupported")
                        InfoRow("Multiple Advertising", if (adapterStatus.isMultipleAdvertisementSupported) "Supported" else "Unsupported")
                        InfoRow("Offloaded Filtering", if (adapterStatus.isOffloadedFilteringSupported) "Supported" else "Unsupported")
                        InfoRow("Hardware Batching", if (adapterStatus.isOffloadedScanBatchingSupported) "Supported" else "Unsupported")
                    }
                }
            }
        }

        // BLE Scanning Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nearby BLE Beacons (${scanState.discoveredDevices.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        OutlinedButton(
                            onClick = { showCalibrationDialog = true },
                            modifier = Modifier.testTag("ble_calibration_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Calibrate Model", fontSize = 11.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (scanState.isScanning) {
                            Button(
                                onClick = { viewModel.stopBleScan() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("stop_ble_scan_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop Scan (${scanState.scanRemainingSeconds}s)")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.startBleScan() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("start_ble_scan_button"),
                                enabled = adapterStatus.isEnabled
                            ) {
                                Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start BLE Scan (30s)")
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.startClassicDiscovery() },
                            modifier = Modifier.testTag("start_classic_discovery_button"),
                            enabled = adapterStatus.isEnabled
                        ) {
                            Text("Classic Discovery", fontSize = 11.sp)
                        }
                    }

                    Text(
                        text = scanState.statusMessage,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section C: Classic Bonded / Paired Devices Card
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClassicDevices = !showClassicDevices },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = TechCyanPrimary)
                            Text(
                                text = "Paired Devices (${scanState.bondedDevices.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Icon(
                            imageVector = if (showClassicDevices) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(
                        visible = showClassicDevices,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        if (scanState.bondedDevices.isEmpty()) {
                            Text(
                                text = "No bonded Bluetooth devices found.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                scanState.bondedDevices.forEach { dev ->
                                    BondedDeviceItem(dev = dev)
                                }
                            }
                        }
                    }
                }
            }
        }

        // BLE Discovered Devices List
        if (scanState.discoveredDevices.isNotEmpty()) {
            items(scanState.discoveredDevices, key = { it.address }) { dev ->
                BleDeviceCard(dev = dev)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BleDeviceCard(dev: BleDiscoveredDevice) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ble_card_${dev.address}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (dev.isStale)
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.surface
        )
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dev.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = dev.address,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SignalBar(rssiDbm = dev.rssiDbm)
            }

            // Tags row: Activity, Packets, Connectable
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TagPill(
                    if (dev.isStale) "Stale (> 12s)" else "Active beacon",
                    if (dev.isStale) Color.Gray else SensorGreen
                )
                TagPill("${dev.packetCount} packets rx", TechCyanPrimary)
                if (dev.isConnectable != null) {
                    TagPill(
                        if (dev.isConnectable) "Connectable" else "Non-connectable",
                        MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // Manufacturer Specific Data
            if (dev.manufacturerDataHex.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "MANUFACTURER DATA: ${dev.manufacturerName ?: "Unknown"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = TechCyanPrimary
                    )
                    Text(
                        text = dev.manufacturerDataHex,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Service UUIDs
            if (dev.serviceUuids.isNotEmpty()) {
                Text(
                    text = "Services: ${dev.serviceUuids.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            // Distance estimate row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Estimated Proximity (Path Loss)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${DistanceEstimator.formatDistance(dev.estimatedDistanceMeters)} · ${DistanceEstimator.getProximityBand(dev.estimatedDistanceMeters)}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TechCyanPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .background(SensorAmber.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Low Confidence",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = SensorAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun BondedDeviceItem(dev: ClassicBondedDevice) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = dev.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${dev.majorDeviceClass} · ${dev.deviceType}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = dev.address,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TagPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = color
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
