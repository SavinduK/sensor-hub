package com.example.data.model

data class BluetoothAdapterStatus(
    val isAvailable: Boolean = false,
    val isEnabled: Boolean = false,
    val name: String = "Unknown",
    val address: String = "Hidden by Android",
    val isLe2MPhySupported: Boolean = false,
    val isLeCodedPhySupported: Boolean = false,
    val isLeExtendedAdvertisingSupported: Boolean = false,
    val isLePeriodicAdvertisingSupported: Boolean = false,
    val isMultipleAdvertisementSupported: Boolean = false,
    val isOffloadedFilteringSupported: Boolean = false,
    val isOffloadedScanBatchingSupported: Boolean = false
)

data class BleDiscoveredDevice(
    val address: String,
    val name: String,
    val rssiDbm: Int,
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val packetCount: Int,
    val serviceUuids: List<String>,
    val manufacturerId: Int?,
    val manufacturerName: String?,
    val manufacturerDataHex: String,
    val serviceDataHex: String,
    val isConnectable: Boolean?,
    val txPowerLevel: Int?,
    val estimatedDistanceMeters: Double,
    val isStale: Boolean
)

data class ClassicBondedDevice(
    val name: String,
    val address: String,
    val bondState: String,
    val deviceType: String,
    val majorDeviceClass: String
)

data class BluetoothScanState(
    val isScanning: Boolean = false,
    val discoveredDevices: List<BleDiscoveredDevice> = emptyList(),
    val bondedDevices: List<ClassicBondedDevice> = emptyList(),
    val statusMessage: String = "Idle - press Scan BLE Devices to begin",
    val scanRemainingSeconds: Int = 0
)
