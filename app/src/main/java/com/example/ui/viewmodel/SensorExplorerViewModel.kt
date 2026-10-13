package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BleDiscoveredDevice
import com.example.data.model.BluetoothAdapterStatus
import com.example.data.model.BluetoothScanState
import com.example.data.model.ConnectedWifiInfo
import com.example.data.model.DeviceFullInfo
import com.example.data.model.SamplingPreset
import com.example.data.model.SensorLogEntry
import com.example.data.model.SensorUiState
import com.example.data.model.WifiScanState
import com.example.data.repository.BluetoothRepository
import com.example.data.repository.DeviceInfoRepository
import com.example.data.repository.SensorRepository
import com.example.data.repository.WifiRepository
import com.example.util.DistanceEstimator
import com.example.util.SensorCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.OutputStream

enum class AppTab(val title: String) {
    DASHBOARD("Overview"),
    SENSORS("Sensors"),
    WIFI("Wi-Fi"),
    BLUETOOTH("Bluetooth"),
    DEVICE_INFO("Device"),
    LOGS("Export")
}

enum class SensorSort(val label: String) {
    NAME_ASC("Name (A-Z)"),
    CATEGORY("Category"),
    EVENT_RATE("Event Rate"),
    POWER("Power (mA)")
}

class SensorExplorerViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorRepo = SensorRepository(application)
    private val wifiRepo = WifiRepository(application)
    private val bluetoothRepo = BluetoothRepository(application)
    private val deviceInfoRepo = DeviceInfoRepository(application)

    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Sensor filtering & search
    private val _sensorCategoryFilter = MutableStateFlow(SensorCategory.ALL)
    val sensorCategoryFilter: StateFlow<SensorCategory> = _sensorCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sensorSort = MutableStateFlow(SensorSort.CATEGORY)
    val sensorSort: StateFlow<SensorSort> = _sensorSort.asStateFlow()

    private val _onlyMonitoredFilter = MutableStateFlow(false)
    val onlyMonitoredFilter: StateFlow<Boolean> = _onlyMonitoredFilter.asStateFlow()

    // Pass-through flows from repos
    val allSensorsMap = sensorRepo.sensorsState
    val rawLogs: StateFlow<List<SensorLogEntry>> = sensorRepo.rawLogs
    val samplingPreset: StateFlow<SamplingPreset> = sensorRepo.currentPreset
    val isAllPaused: StateFlow<Boolean> = sensorRepo.isAllPaused

    val connectedWifi: StateFlow<ConnectedWifiInfo> = wifiRepo.connectedInfo
    val wifiScanState: StateFlow<WifiScanState> = wifiRepo.scanState

    val bluetoothAdapterStatus: StateFlow<BluetoothAdapterStatus> = bluetoothRepo.adapterStatus
    val bluetoothScanState: StateFlow<BluetoothScanState> = bluetoothRepo.scanState

    val deviceInfo: StateFlow<DeviceFullInfo?> = deviceInfoRepo.deviceInfo

    // Distance calibration settings
    private val _wifiRefRssi = MutableStateFlow(DistanceEstimator.DEFAULT_WIFI_REF_RSSI)
    val wifiRefRssi: StateFlow<Int> = _wifiRefRssi.asStateFlow()

    private val _wifiPathLossN = MutableStateFlow(DistanceEstimator.DEFAULT_WIFI_PATH_LOSS_N)
    val wifiPathLossN: StateFlow<Double> = _wifiPathLossN.asStateFlow()

    private val _bleRefRssi = MutableStateFlow(DistanceEstimator.DEFAULT_BLE_REF_RSSI)
    val bleRefRssi: StateFlow<Int> = _bleRefRssi.asStateFlow()

    private val _blePathLossN = MutableStateFlow(DistanceEstimator.DEFAULT_BLE_PATH_LOSS_N)
    val blePathLossN: StateFlow<Double> = _blePathLossN.asStateFlow()

    // Filtered sensors list for UI
    val filteredSensors: StateFlow<List<SensorUiState>> = combine(
        allSensorsMap,
        _sensorCategoryFilter,
        _searchQuery,
        _sensorSort,
        _onlyMonitoredFilter
    ) { map, cat, query, sort, onlyMonitored ->
        var list = map.values.toList()

        if (cat != SensorCategory.ALL) {
            list = list.filter { it.metadata.category == cat }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.metadata.name.lowercase().contains(q) ||
                        it.metadata.typeName.lowercase().contains(q) ||
                        it.metadata.vendor.lowercase().contains(q)
            }
        }

        if (onlyMonitored) {
            list = list.filter { it.isMonitored }
        }

        when (sort) {
            SensorSort.NAME_ASC -> list.sortedBy { it.metadata.name }
            SensorSort.CATEGORY -> list.sortedWith(compareBy({ it.metadata.category.ordinal }, { it.metadata.name }))
            SensorSort.EVENT_RATE -> list.sortedByDescending { it.observedRateHz }
            SensorSort.POWER -> list.sortedByDescending { it.metadata.powerMa }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setCategoryFilter(category: SensorCategory) {
        _sensorCategoryFilter.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSensorSort(sort: SensorSort) {
        _sensorSort.value = sort
    }

    fun toggleOnlyMonitored() {
        _onlyMonitoredFilter.value = !_onlyMonitoredFilter.value
    }

    fun toggleSensorMonitoring(sensorId: Int) {
        val currentState = allSensorsMap.value[sensorId] ?: return
        if (currentState.isMonitored) {
            sensorRepo.stopMonitoringSensor(sensorId)
        } else {
            sensorRepo.startMonitoringSensor(sensorId)
        }
    }

    fun startAllSensors() {
        sensorRepo.startMonitoringAll()
    }

    fun stopAllSensors() {
        sensorRepo.stopMonitoringAll()
    }

    fun togglePauseAll() {
        sensorRepo.togglePauseAll()
    }

    fun setSamplingPreset(preset: SamplingPreset) {
        sensorRepo.setSamplingPreset(preset)
    }

    fun clearLogs() {
        sensorRepo.clearLogs()
    }

    fun exportCsvToStream(outputStream: OutputStream): Boolean {
        return try {
            val csv = sensorRepo.generateCsvContent()
            outputStream.bufferedWriter().use { it.write(csv) }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun refreshWifi() {
        wifiRepo.refreshConnectedWifi()
    }

    fun startWifiScan() {
        wifiRepo.refRssiAt1m = _wifiRefRssi.value
        wifiRepo.pathLossExponent = _wifiPathLossN.value
        wifiRepo.startScan()
    }

    fun refreshBluetooth() {
        bluetoothRepo.refreshAdapterStatus()
        bluetoothRepo.refreshBondedDevices()
    }

    fun startBleScan() {
        bluetoothRepo.refRssiAt1m = _bleRefRssi.value
        bluetoothRepo.pathLossExponent = _blePathLossN.value
        bluetoothRepo.startBleScan()
    }

    fun stopBleScan() {
        bluetoothRepo.stopBleScan()
    }

    fun startClassicDiscovery() {
        bluetoothRepo.startClassicDiscovery()
    }

    fun refreshDeviceInfo() {
        deviceInfoRepo.refreshDeviceInfo()
    }

    fun updateWifiCalibration(refRssi: Int, n: Double) {
        _wifiRefRssi.value = refRssi
        _wifiPathLossN.value = n
        wifiRepo.refRssiAt1m = refRssi
        wifiRepo.pathLossExponent = n
    }

    fun updateBleCalibration(refRssi: Int, n: Double) {
        _bleRefRssi.value = refRssi
        _blePathLossN.value = n
        bluetoothRepo.refRssiAt1m = refRssi
        bluetoothRepo.pathLossExponent = n
    }

    override fun onCleared() {
        super.onCleared()
        sensorRepo.cleanup()
        wifiRepo.cleanup()
        bluetoothRepo.cleanup()
    }
}
