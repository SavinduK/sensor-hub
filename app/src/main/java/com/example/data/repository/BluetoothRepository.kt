package com.example.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.ParcelUuid
import com.example.data.model.BleDiscoveredDevice
import com.example.data.model.BluetoothAdapterStatus
import com.example.data.model.BluetoothScanState
import com.example.data.model.ClassicBondedDevice
import com.example.util.DistanceEstimator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class BluetoothRepository(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _adapterStatus = MutableStateFlow(BluetoothAdapterStatus())
    val adapterStatus: StateFlow<BluetoothAdapterStatus> = _adapterStatus.asStateFlow()

    private val _scanState = MutableStateFlow(BluetoothScanState())
    val scanState: StateFlow<BluetoothScanState> = _scanState.asStateFlow()

    private val discoveredDevicesMap = ConcurrentHashMap<String, BleDiscoveredDevice>()
    private var scanCallback: ScanCallback? = null
    private var scanCountdownJob: Job? = null
    private var classicDiscoveryReceiver: BroadcastReceiver? = null

    var refRssiAt1m = DistanceEstimator.DEFAULT_BLE_REF_RSSI
    var pathLossExponent = DistanceEstimator.DEFAULT_BLE_PATH_LOSS_N

    init {
        refreshAdapterStatus()
        refreshBondedDevices()
    }

    @SuppressLint("MissingPermission")
    fun refreshAdapterStatus() {
        if (bluetoothAdapter == null) {
            _adapterStatus.value = BluetoothAdapterStatus(
                isAvailable = false,
                isEnabled = false,
                name = "No Bluetooth Hardware",
                address = "N/A"
            )
            return
        }

        try {
            val isEnabled = try { bluetoothAdapter.isEnabled } catch (_: Exception) { false }
            val name = try {
                bluetoothAdapter.name ?: "Bluetooth Device"
            } catch (_: Exception) {
                "Permission Required"
            }

            val address = try {
                val addr = bluetoothAdapter.address
                if (addr == "02:00:00:00:00:00") "Hidden by Android" else (addr ?: "Hidden")
            } catch (_: Exception) {
                "Hidden for privacy"
            }

            val isLe2M = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { bluetoothAdapter.isLe2MPhySupported } catch (_: Exception) { false }
            } else false

            val isCoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { bluetoothAdapter.isLeCodedPhySupported } catch (_: Exception) { false }
            } else false

            val isExtAdv = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { bluetoothAdapter.isLeExtendedAdvertisingSupported } catch (_: Exception) { false }
            } else false

            val isPerAdv = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { bluetoothAdapter.isLePeriodicAdvertisingSupported } catch (_: Exception) { false }
            } else false

            val isMultiAdv = try { bluetoothAdapter.isMultipleAdvertisementSupported } catch (_: Exception) { false }
            val isOffloadedFilter = try { bluetoothAdapter.isOffloadedFilteringSupported } catch (_: Exception) { false }
            val isOffloadedBatch = try { bluetoothAdapter.isOffloadedScanBatchingSupported } catch (_: Exception) { false }

            _adapterStatus.value = BluetoothAdapterStatus(
                isAvailable = true,
                isEnabled = isEnabled,
                name = name,
                address = address,
                isLe2MPhySupported = isLe2M,
                isLeCodedPhySupported = isCoded,
                isLeExtendedAdvertisingSupported = isExtAdv,
                isLePeriodicAdvertisingSupported = isPerAdv,
                isMultipleAdvertisementSupported = isMultiAdv,
                isOffloadedFilteringSupported = isOffloadedFilter,
                isOffloadedScanBatchingSupported = isOffloadedBatch
            )
        } catch (_: Exception) {
            _adapterStatus.value = BluetoothAdapterStatus(
                isAvailable = true,
                isEnabled = false,
                name = "Permission Required",
                address = "Hidden"
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshBondedDevices() {
        if (bluetoothAdapter == null) {
            _scanState.update { it.copy(bondedDevices = emptyList()) }
            return
        }

        try {
            val isEnabled = try { bluetoothAdapter.isEnabled } catch (_: Exception) { false }
            if (!isEnabled) {
                _scanState.update { it.copy(bondedDevices = emptyList()) }
                return
            }

            val bonded = bluetoothAdapter.bondedDevices ?: emptySet()
            val list = bonded.map { dev ->
                val typeStr = when (dev.type) {
                    BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic (BR/EDR)"
                    BluetoothDevice.DEVICE_TYPE_LE -> "Low Energy (LE)"
                    BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual Mode (BR/EDR/LE)"
                    else -> "Unknown Type"
                }

                val majorClass = when (dev.bluetoothClass?.majorDeviceClass) {
                    BluetoothClass.Device.Major.AUDIO_VIDEO -> "Audio / Headphones / Speaker"
                    BluetoothClass.Device.Major.COMPUTER -> "Computer / Laptop"
                    BluetoothClass.Device.Major.PHONE -> "Phone / Tablet"
                    BluetoothClass.Device.Major.WEARABLE -> "Wearable / Watch"
                    BluetoothClass.Device.Major.PERIPHERAL -> "Peripheral (Mouse/Keyboard)"
                    else -> "Miscellaneous / Other"
                }

                ClassicBondedDevice(
                    name = dev.name ?: "Unnamed Device",
                    address = dev.address ?: "Hidden",
                    bondState = "Paired",
                    deviceType = typeStr,
                    majorDeviceClass = majorClass
                )
            }.sortedBy { it.name }

            _scanState.update { it.copy(bondedDevices = list) }
        } catch (_: Exception) {
            _scanState.update { it.copy(bondedDevices = emptyList()) }
        }
    }


    @SuppressLint("MissingPermission")
    fun startBleScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _scanState.update { it.copy(statusMessage = "Bluetooth is disabled or unavailable") }
            return
        }

        val scanner: BluetoothLeScanner? = bluetoothAdapter.bluetoothLeScanner
        if (scanner == null) {
            _scanState.update { it.copy(statusMessage = "BluetoothLeScanner unavailable") }
            return
        }

        stopBleScan()
        discoveredDevicesMap.clear()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setLegacy(false)
                }
            }
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                if (result == null) return
                handleScanResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { handleScanResult(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                val errorMsg = when (errorCode) {
                    SCAN_FAILED_ALREADY_STARTED -> "Scan already active"
                    SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed with Bluetooth stack"
                    SCAN_FAILED_INTERNAL_ERROR -> "Bluetooth driver internal error"
                    SCAN_FAILED_FEATURE_UNSUPPORTED -> "Power mode or filter unsupported by hardware"
                    else -> "BLE Scan Error #$errorCode"
                }
                _scanState.update {
                    it.copy(
                        isScanning = false,
                        statusMessage = errorMsg,
                        scanRemainingSeconds = 0
                    )
                }
            }
        }

        try {
            scanner.startScan(null, settings, scanCallback)
            _scanState.update {
                it.copy(
                    isScanning = true,
                    statusMessage = "Scanning for nearby BLE advertisements (30s foreground window)...",
                    scanRemainingSeconds = 30,
                    discoveredDevices = emptyList()
                )
            }

            // Start countdown timer
            scanCountdownJob?.cancel()
            scanCountdownJob = scope.launch {
                for (remaining in 30 downTo 1) {
                    _scanState.update { it.copy(scanRemainingSeconds = remaining) }
                    delay(1000)
                    updateStaleStates()
                }
                stopBleScan()
                _scanState.update {
                    it.copy(
                        statusMessage = "Scan completed. Found ${discoveredDevicesMap.size} unique BLE devices."
                    )
                }
            }
        } catch (e: Exception) {
            _scanState.update {
                it.copy(
                    isScanning = false,
                    statusMessage = "Failed to launch scan: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val address = device.address ?: "Unknown-Addr-${result.hashCode()}"
        val record = result.scanRecord

        val rawName = try {
            record?.deviceName ?: device.name
        } catch (_: SecurityException) {
            null
        }
        val name = if (!rawName.isNullOrBlank()) rawName else "Unknown BLE Device"

        val rssi = result.rssi
        val now = System.currentTimeMillis()

        val uuids: List<String> = record?.serviceUuids?.mapNotNull { it.uuid?.toString() } ?: emptyList()

        // Manufacturer specific data
        var mfgId: Int? = null
        var mfgHex = ""
        var mfgName: String? = null
        record?.manufacturerSpecificData?.let { sparseArray ->
            if (sparseArray.size() > 0) {
                mfgId = sparseArray.keyAt(0)
                val bytes = sparseArray.valueAt(0)
                if (bytes != null) {
                    mfgHex = bytesToHex(bytes)
                }
                mfgName = getManufacturerName(mfgId!!)
            }
        }

        // Service data
        val serviceDataHex = record?.serviceData?.entries?.firstOrNull()?.let {
            "${it.key.uuid}: ${bytesToHex(it.value)}"
        } ?: ""

        val isConnectable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            result.isConnectable
        } else null

        val txPower = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val tx = result.txPower
            if (tx != ScanResult.TX_POWER_NOT_PRESENT) tx else null
        } else null

        val estimatedDistance = DistanceEstimator.estimateDistance(
            rssiDbm = rssi,
            refRssiAt1m = refRssiAt1m,
            pathLossExponent = pathLossExponent
        )

        val existing = discoveredDevicesMap[address]
        val packetCount = (existing?.packetCount ?: 0) + 1
        val firstSeen = existing?.firstSeenMs ?: now

        val updatedDevice = BleDiscoveredDevice(
            address = address,
            name = name,
            rssiDbm = rssi,
            firstSeenMs = firstSeen,
            lastSeenMs = now,
            packetCount = packetCount,
            serviceUuids = uuids,
            manufacturerId = mfgId,
            manufacturerName = mfgName,
            manufacturerDataHex = mfgHex,
            serviceDataHex = serviceDataHex,
            isConnectable = isConnectable,
            txPowerLevel = txPower,
            estimatedDistanceMeters = estimatedDistance,
            isStale = false
        )

        discoveredDevicesMap[address] = updatedDevice

        _scanState.update {
            it.copy(
                discoveredDevices = discoveredDevicesMap.values.sortedByDescending { dev -> dev.lastSeenMs }
            )
        }
    }

    private fun updateStaleStates() {
        val now = System.currentTimeMillis()
        var changed = false
        discoveredDevicesMap.forEach { (addr, dev) ->
            val isNowStale = (now - dev.lastSeenMs) > 12000L
            if (dev.isStale != isNowStale) {
                discoveredDevicesMap[addr] = dev.copy(isStale = isNowStale)
                changed = true
            }
        }
        if (changed) {
            _scanState.update {
                it.copy(
                    discoveredDevices = discoveredDevicesMap.values.sortedByDescending { dev -> dev.lastSeenMs }
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopBleScan() {
        scanCountdownJob?.cancel()
        scanCountdownJob = null

        val scanner = bluetoothAdapter?.bluetoothLeScanner
        if (scanner != null && scanCallback != null) {
            try {
                scanner.stopScan(scanCallback)
            } catch (_: Exception) {}
        }
        scanCallback = null

        _scanState.update {
            it.copy(
                isScanning = false,
                scanRemainingSeconds = 0
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startClassicDiscovery() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return

        registerClassicReceiverIfNeeded()
        try {
            bluetoothAdapter.startDiscovery()
            _scanState.update {
                it.copy(statusMessage = "Classic Bluetooth discovery initiated (inquiry mode)...")
            }
        } catch (_: SecurityException) {
            _scanState.update {
                it.copy(statusMessage = "BLUETOOTH_SCAN permission required for classic discovery")
            }
        }
    }

    private fun registerClassicReceiverIfNeeded() {
        if (classicDiscoveryReceiver != null) return

        classicDiscoveryReceiver = object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (BluetoothDevice.ACTION_FOUND == action) {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    if (device != null) {
                        val addr = device.address ?: return
                        val name = try { device.name ?: "Classic Device" } catch (_: SecurityException) { "Device" }
                        val now = System.currentTimeMillis()
                        val existing = discoveredDevicesMap[addr]
                        val packetCount = (existing?.packetCount ?: 0) + 1

                        val dev = BleDiscoveredDevice(
                            address = addr,
                            name = name,
                            rssiDbm = if (rssi != Short.MIN_VALUE.toInt()) rssi else -75,
                            firstSeenMs = existing?.firstSeenMs ?: now,
                            lastSeenMs = now,
                            packetCount = packetCount,
                            serviceUuids = emptyList(),
                            manufacturerId = null,
                            manufacturerName = null,
                            manufacturerDataHex = "Classic BR/EDR inquiry",
                            serviceDataHex = "",
                            isConnectable = true,
                            txPowerLevel = null,
                            estimatedDistanceMeters = DistanceEstimator.estimateDistance(
                                rssiDbm = rssi,
                                refRssiAt1m = refRssiAt1m,
                                pathLossExponent = pathLossExponent
                            ),
                            isStale = false
                        )
                        discoveredDevicesMap[addr] = dev
                        _scanState.update {
                            it.copy(discoveredDevices = discoveredDevicesMap.values.sortedByDescending { d -> d.lastSeenMs })
                        }
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                    _scanState.update {
                        it.copy(statusMessage = "Classic discovery finished.")
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(classicDiscoveryReceiver, filter)
    }

    private fun bytesToHex(bytes: ByteArray, maxLen: Int = 24): String {
        val hexChars = "0123456789ABCDEF"
        val limit = minOf(bytes.size, maxLen)
        val sb = StringBuilder(limit * 3)
        for (i in 0 until limit) {
            val octet = bytes[i].toInt() and 0xFF
            sb.append(hexChars[octet shr 4])
            sb.append(hexChars[octet and 0x0F])
            if (i < limit - 1) sb.append(' ')
        }
        if (bytes.size > maxLen) {
            sb.append("… (+${bytes.size - maxLen} bytes)")
        }
        return sb.toString()
    }

    private fun getManufacturerName(companyId: Int): String {
        return when (companyId) {
            0x004C -> "Apple Inc."
            0x0006 -> "Microsoft"
            0x00E0 -> "Google"
            0x0075 -> "Samsung Electronics"
            0x0059 -> "Nordic Semiconductor"
            0x000D -> "Texas Instruments"
            0x000F -> "Broadcom"
            0x0002 -> "Intel Corp."
            0x0087 -> "Garmin International"
            0x00D2 -> "Dialog Semiconductor"
            0x0157 -> "Anhui Huami (Xiaomi/Amazfit)"
            0x0171 -> "Amazon.com Services"
            0x038F -> "Xiaomi Inc."
            0x01AB -> "Tile Inc."
            else -> "Company ID: 0x${companyId.toString(16).uppercase().padStart(4, '0')}"
        }
    }

    fun cleanup() {
        stopBleScan()
        classicDiscoveryReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
            classicDiscoveryReceiver = null
        }
    }
}
