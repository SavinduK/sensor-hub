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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceFullInfo
import com.example.ui.theme.SensorAmber
import com.example.ui.theme.SensorGreen
import com.example.ui.theme.TechCyanPrimary
import com.example.ui.viewmodel.SensorExplorerViewModel
import java.util.Locale

@Composable
fun DeviceInfoScreen(
    viewModel: SensorExplorerViewModel,
    modifier: Modifier = Modifier
) {
    val deviceInfo by viewModel.deviceInfo.collectAsState()

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
                        text = "Device Specifications",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Hardware architecture, SoC, display, battery, and cameras",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.refreshDeviceInfo() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Device Info")
                }
            }
        }

        if (deviceInfo == null) {
            item {
                Text("Loading hardware specifications...")
            }
        } else {
            val dev = deviceInfo!!

            // Platform & Build
            item {
                SpecCard(
                    title = "Platform & OS Build",
                    icon = Icons.Default.Smartphone
                ) {
                    SpecRow("Device Model", "${dev.manufacturer} ${dev.model} (${dev.device})")
                    SpecRow("Brand / Product", "${dev.brand} · ${dev.board}")
                    SpecRow("Hardware Platform", dev.hardware)
                    SpecRow("Android OS Version", "Android ${dev.androidVersion} (API level ${dev.apiLevel})")
                    SpecRow("Security Patch Level", dev.securityPatch)
                    SpecRow("Build ID", dev.buildId)
                }
            }

            // SoC, CPU & RAM
            item {
                SpecCard(
                    title = "Processor & Memory Architecture",
                    icon = Icons.Default.Memory
                ) {
                    SpecRow("CPU Architecture", "${dev.cpuCores} active execution cores")
                    SpecRow("Supported ABIs", dev.supportedAbis.joinToString(", "))
                    SpecRow(
                        "Total Physical RAM",
                        "${String.format(Locale.US, "%.2f", dev.totalRamBytes / (1024.0 * 1024.0 * 1024.0))} GB (${dev.totalRamBytes / 1024 / 1024} MB)"
                    )
                    SpecRow(
                        "Available RAM",
                        "${String.format(Locale.US, "%.2f", dev.availableRamBytes / (1024.0 * 1024.0 * 1024.0))} GB (${dev.availableRamBytes / 1024 / 1024} MB)"
                    )
                    SpecRow("Low Memory Flag", if (dev.isLowMemory) "WARNING: Low RAM condition" else "Normal")
                    SpecRow("Kernel Low RAM Threshold", "${dev.lowMemoryThresholdBytes / 1024 / 1024} MB")
                }
            }

            // Display
            item {
                SpecCard(
                    title = "Display Panel Characteristics",
                    icon = Icons.Default.Tv
                ) {
                    SpecRow("Resolution (Physical)", "${dev.displayWidthPx} × ${dev.displayHeightPx} px")
                    SpecRow("Screen Density", "${dev.displayDensityDpi} dpi")
                    SpecRow("Refresh Rate", "${String.format(Locale.US, "%.1f", dev.refreshRateHz)} Hz")
                    SpecRow("HDR Rendering", if (dev.isHdrSupported) "Supported (High Dynamic Range)" else "SDR (Standard)")
                    SpecRow("Wide Color Gamut", if (dev.isWideColorGamut) "Supported (DCI-P3)" else "Standard sRGB")
                }
            }

            // Battery
            item {
                SpecCard(
                    title = "Battery & Power Subsystem",
                    icon = Icons.Default.BatteryChargingFull
                ) {
                    SpecRow("Battery Level", "${dev.batteryPercent}%")
                    SpecRow("Charging State", dev.batteryStatus)
                    SpecRow("Power Source", dev.powerSource)
                    SpecRow("Battery Health", dev.batteryHealth)
                    SpecRow("Terminal Voltage", "${dev.batteryVoltageMv} mV (${String.format(Locale.US, "%.2f", dev.batteryVoltageMv / 1000f)} V)")
                    SpecRow("Internal Temperature", "${String.format(Locale.US, "%.1f", dev.batteryTempCelsius)} °C")
                    SpecRow("Chemistry", dev.batteryTechnology)
                }
            }

            // Storage
            item {
                SpecCard(
                    title = "Internal Storage Volume",
                    icon = Icons.Default.Storage
                ) {
                    val totalGb = dev.totalStorageBytes / (1024.0 * 1024.0 * 1024.0)
                    val availGb = dev.availableStorageBytes / (1024.0 * 1024.0 * 1024.0)
                    val usedGb = totalGb - availGb
                    val usedPercent = if (totalGb > 0) (usedGb / totalGb).toFloat() else 0f

                    SpecRow("Total Data Partition", "${String.format(Locale.US, "%.2f", totalGb)} GB")
                    SpecRow("Free Available Space", "${String.format(Locale.US, "%.2f", availGb)} GB")
                    SpecRow("Used Space", "${String.format(Locale.US, "%.2f", usedGb)} GB (${(usedPercent * 100).toInt()}%)")

                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { usedPercent.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (usedPercent > 0.9f) SensorAmber else TechCyanPrimary
                    )
                }
            }

            // Camera Inventory
            item {
                SpecCard(
                    title = "Camera Hardware Inventory (${dev.cameras.size})",
                    icon = Icons.Default.CameraAlt
                ) {
                    if (dev.cameras.isEmpty()) {
                        Text(
                            text = "No cameras detected on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        dev.cameras.forEach { cam ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Camera #${cam.id}: ${cam.facing}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", cam.megapixels)} MP",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = TechCyanPrimary
                                    )
                                }
                                Text(
                                    text = "HW Capability Level: ${cam.hardwareLevel} · Flash: ${if (cam.hasFlash) "Yes" else "No"} · Sensor Size: ${cam.sensorSizeMm}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            // GNSS Telemetry
            item {
                SpecCard(
                    title = "GNSS Positioning Framework",
                    icon = Icons.Default.LocationOn
                ) {
                    SpecRow("GPS Hardware Provider", if (dev.gnss.isGpsEnabled) "Enabled in Settings" else "Disabled")
                    SpecRow("Network Location Provider", if (dev.gnss.isNetworkEnabled) "Enabled in Settings" else "Disabled")
                    SpecRow("Location Permission", if (dev.gnss.permissionGranted) "Granted" else "Not granted")

                    if (dev.gnss.latitude != null && dev.gnss.longitude != null) {
                        SpecRow("Last Coordinates", "${String.format(Locale.US, "%.5f", dev.gnss.latitude)}, ${String.format(Locale.US, "%.5f", dev.gnss.longitude)}")
                        SpecRow("Estimated Accuracy", if (dev.gnss.accuracyMeters != null) "± ${String.format(Locale.US, "%.1f", dev.gnss.accuracyMeters)} m" else "N/A")
                        SpecRow("Altitude", if (dev.gnss.altitudeMeters != null) "${String.format(Locale.US, "%.1f", dev.gnss.altitudeMeters)} m MSL" else "N/A")
                        SpecRow("Fix Timestamp", dev.gnss.timeFormatted ?: "N/A")
                        SpecRow("Provider Source", dev.gnss.provider ?: "N/A")
                    } else {
                        Text(
                            text = "No active GNSS fix queried. Location tracking is on-demand only.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
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
private fun SpecCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = TechCyanPrimary)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            content()
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
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
