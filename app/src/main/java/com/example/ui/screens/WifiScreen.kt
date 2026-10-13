package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Button
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
import com.example.data.model.WifiAccessPoint
import com.example.ui.components.PathLossConfigDialog
import com.example.ui.components.SignalBar
import com.example.ui.theme.SensorAmber
import com.example.ui.theme.SensorGreen
import com.example.ui.theme.TechCyanPrimary
import com.example.ui.viewmodel.SensorExplorerViewModel
import com.example.util.DistanceEstimator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WifiScreen(
    viewModel: SensorExplorerViewModel,
    modifier: Modifier = Modifier
) {
    val connectedWifi by viewModel.connectedWifi.collectAsState()
    val scanState by viewModel.wifiScanState.collectAsState()
    val refRssi by viewModel.wifiRefRssi.collectAsState()
    val pathLossN by viewModel.wifiPathLossN.collectAsState()

    var showCalibrationDialog by remember { mutableStateOf(false) }

    if (showCalibrationDialog) {
        PathLossConfigDialog(
            title = "Wi-Fi Distance Calibration",
            initialRefRssi = refRssi,
            initialExponent = pathLossN,
            onDismiss = { showCalibrationDialog = false },
            onSave = { r, n -> viewModel.updateWifiCalibration(r, n) }
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
                        text = "Wi-Fi Telemetry & Scanning",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Connected network parameters & nearby 802.11 beacons",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.refreshWifi() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh connected Wi-Fi")
                }
            }
        }

        // Section A: Connected Network Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("connected_wifi_card"),
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
                            Icon(Icons.Default.Router, contentDescription = null, tint = TechCyanPrimary)
                            Text(
                                text = "Active Wi-Fi Connection",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (connectedWifi.isConnected) {
                            SignalBar(rssiDbm = connectedWifi.rssiDbm)
                        } else {
                            Text(
                                text = "DISCONNECTED",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SensorAmber
                            )
                        }
                    }

                    if (connectedWifi.isConnected) {
                        Text(
                            text = connectedWifi.ssid,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

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
                            InfoRow("BSSID (Router MAC)", connectedWifi.bssid)
                            InfoRow("Device IP Address", connectedWifi.ipAddress)
                            InfoRow("Subnet Mask", connectedWifi.subnetMask)
                            InfoRow("Default Gateway", connectedWifi.gateway)
                            InfoRow("DNS Servers", if (connectedWifi.dnsServers.isNotEmpty()) connectedWifi.dnsServers.joinToString(", ") else "N/A")
                            InfoRow("Frequency Band", "${connectedWifi.band} (${connectedWifi.frequencyMhz} MHz)")
                            InfoRow("Wi-Fi Channel", if (connectedWifi.channel > 0) "Ch ${connectedWifi.channel}" else "N/A")
                            InfoRow("Link Speed", "${connectedWifi.linkSpeedMbps} Mbps")
                            InfoRow("Standard", connectedWifi.wifiStandard)
                            InfoRow("Signal Strength", "${connectedWifi.rssiDbm} dBm (Level ${connectedWifi.signalLevel}/4)")
                            InfoRow("Status", connectedWifi.connectionState)
                        }
                    } else {
                        Text(
                            text = "Device is not currently connected to any Wi-Fi access point.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Section C: Wi-Fi RTT Hardware Card
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wi-Fi RTT (802.11mc FTM Ranging)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    if (scanState.rttSupported) SensorGreen.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (scanState.rttSupported) "SUPPORTED" else "UNSUPPORTED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = if (scanState.rttSupported) SensorGreen else Color.Gray
                            )
                        }
                    }
                    Text(
                        text = scanState.rttStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Wi-Fi Round Trip Time (RTT) measures nanosecond time-of-flight to compatible access points for sub-meter positioning without signal-strength guessing.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Section B: Scan & Nearby APs Controls
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
                            text = "Nearby Access Points (${scanState.accessPoints.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        OutlinedButton(
                            onClick = { showCalibrationDialog = true },
                            modifier = Modifier.testTag("wifi_calibration_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Calibrate Model", fontSize = 11.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.startWifiScan() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_wifi_scan_button"),
                            enabled = !scanState.isScanning
                        ) {
                            if (scanState.isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scanning...")
                            } else {
                                Icon(Icons.Default.WifiFind, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Wi-Fi Scan")
                            }
                        }
                    }

                    // Scan status & throttling notice
                    Text(
                        text = scanState.scanMessage,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = if (scanState.isThrottledNotice) SensorAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (scanState.lastScanTimeMs > 0L) {
                        val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(scanState.lastScanTimeMs))
                        Text(
                            text = "Last scan executed at $formattedTime · Scan count: ${scanState.scanCount}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Access Points List
        if (scanState.accessPoints.isNotEmpty()) {
            items(scanState.accessPoints, key = { it.bssid }) { ap ->
                AccessPointCard(ap = ap)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccessPointCard(ap: WifiAccessPoint) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ap_card_${ap.bssid}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                        text = ap.ssid,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = ap.bssid,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                SignalBar(rssiDbm = ap.rssiDbm)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TagPill(if (ap.isFresh) "Fresh" else "Cached", if (ap.isFresh) SensorGreen else Color.Gray)
                TagPill("Ch ${ap.channel} (${ap.frequencyMhz} MHz)", TechCyanPrimary)
                TagPill(ap.channelWidth, MaterialTheme.colorScheme.secondary)
            }

            Text(
                text = "Security: ${ap.capabilities}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Rough distance estimate with disclaimer
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
                        text = "${DistanceEstimator.formatDistance(ap.estimatedDistanceMeters)} · ${DistanceEstimator.getProximityBand(ap.estimatedDistanceMeters)}",
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
