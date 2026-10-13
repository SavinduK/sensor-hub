package com.example.data.model

data class ConnectedWifiInfo(
    val isConnected: Boolean = false,
    val ssid: String = "Not connected",
    val bssid: String = "N/A",
    val ipAddress: String = "N/A",
    val linkSpeedMbps: Int = 0,
    val rssiDbm: Int = -100,
    val frequencyMhz: Int = 0,
    val wifiStandard: String = "N/A",
    val channel: Int = 0,
    val band: String = "N/A",
    val gateway: String = "N/A",
    val dnsServers: List<String> = emptyList(),
    val subnetMask: String = "N/A",
    val networkId: Int = -1,
    val signalLevel: Int = 0,
    val connectionState: String = "Disconnected"
)

data class WifiAccessPoint(
    val bssid: String,
    val ssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val channel: Int,
    val channelWidth: String,
    val capabilities: String,
    val timestampUs: Long,
    val isFresh: Boolean,
    val signalLevel: Int,
    val estimatedDistanceMeters: Double
)

data class WifiScanState(
    val isScanning: Boolean = false,
    val lastScanTimeMs: Long = 0L,
    val scanCount: Int = 0,
    val scanMessage: String = "Tap 'Start Wi-Fi Scan' to discover nearby APs",
    val isThrottledNotice: Boolean = false,
    val accessPoints: List<WifiAccessPoint> = emptyList(),
    val rttSupported: Boolean = false,
    val rttStatusMessage: String = "Checking Wi-Fi RTT ranging capability..."
)
