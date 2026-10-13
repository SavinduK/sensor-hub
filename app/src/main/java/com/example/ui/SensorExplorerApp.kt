package com.example.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.BluetoothScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceInfoScreen
import com.example.ui.screens.LogsScreen
import com.example.ui.screens.SensorsScreen
import com.example.ui.screens.WifiScreen
import com.example.ui.theme.TechCyanPrimary
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.SensorExplorerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorExplorerApp(
    viewModel: SensorExplorerViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsState()

    // Handle back button: return to Dashboard tab if on another tab
    BackHandler(enabled = currentTab != AppTab.DASHBOARD) {
        viewModel.selectTab(AppTab.DASHBOARD)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sensor Explorer",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                val navItems = listOf(
                    Triple(AppTab.DASHBOARD, "Overview", Icons.Default.Dashboard),
                    Triple(AppTab.SENSORS, "Sensors", Icons.Default.Sensors),
                    Triple(AppTab.WIFI, "Wi-Fi", Icons.Default.Wifi),
                    Triple(AppTab.BLUETOOTH, "Bluetooth", Icons.Default.Bluetooth),
                    Triple(AppTab.DEVICE_INFO, "Device", Icons.Default.Memory),
                    Triple(AppTab.LOGS, "Export", Icons.Default.Download)
                )

                navItems.forEach { (tab, label, icon) ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = TechCyanPrimary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                AppTab.SENSORS -> SensorsScreen(viewModel = viewModel)
                AppTab.WIFI -> WifiScreen(viewModel = viewModel)
                AppTab.BLUETOOTH -> BluetoothScreen(viewModel = viewModel)
                AppTab.DEVICE_INFO -> DeviceInfoScreen(viewModel = viewModel)
                AppTab.LOGS -> LogsScreen(viewModel = viewModel)
            }
        }
    }
}
