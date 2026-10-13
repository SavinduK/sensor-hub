package com.example.data.repository

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.rtt.WifiRttManager
import android.os.Build
import android.os.SystemClock
import com.example.data.model.ConnectedWifiInfo
import com.example.data.model.WifiAccessPoint
import com.example.data.model.WifiScanState
import com.example.util.DistanceEstimator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.net.Inet4Address
import java.net.NetworkInterface

class WifiRepository(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _connectedInfo = MutableStateFlow(ConnectedWifiInfo())
    val connectedInfo: StateFlow<ConnectedWifiInfo> = _connectedInfo.asStateFlow()

    private val _scanState = MutableStateFlow(WifiScanState())
    val scanState: StateFlow<WifiScanState> = _scanState.asStateFlow()

    private var scanReceiver: BroadcastReceiver? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // Scan tracking to communicate Android's 4-scans-per-2-minutes throttle policy
    private var scanTimestamps = mutableListOf<Long>()

    var refRssiAt1m = DistanceEstimator.DEFAULT_WIFI_REF_RSSI
    var pathLossExponent = DistanceEstimator.DEFAULT_WIFI_PATH_LOSS_N

    init {
        checkRttSupport()
        refreshConnectedWifi()
        registerNetworkCallback()
    }

    private fun checkRttSupport() {
        val hasRttFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)
        val rttManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE) as? WifiRttManager
        } else null

        val isRttAvailable = rttManager?.isAvailable ?: false

        _scanState.update { current ->
            current.copy(
                rttSupported = hasRttFeature && isRttAvailable,
                rttStatusMessage = when {
                    !hasRttFeature -> "Unsupported on this device (Hardware lacks 802.11mc FTM support)"
                    !isRttAvailable -> "Hardware supports RTT, but Wi-Fi RTT service is currently unavailable or disabled"
                    else -> "Wi-Fi RTT (802.11mc FTM Ranging) is supported by device hardware"
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshConnectedWifi() {
        if (wifiManager == null || connectivityManager == null) {
            _connectedInfo.value = ConnectedWifiInfo(connectionState = "Wi-Fi hardware unavailable")
            return
        }

        try {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
            val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            if (!isWifi) {
                _connectedInfo.value = ConnectedWifiInfo(
                    isConnected = false,
                    connectionState = "Disconnected (No active Wi-Fi connection)"
                )
                return
            }

            val wifiInfo: WifiInfo? = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    capabilities?.transportInfo as? WifiInfo ?: wifiManager.connectionInfo
                } else {
                    wifiManager.connectionInfo
                }
            } catch (_: Exception) {
                null
            }

            val linkProperties = activeNetwork?.let { connectivityManager.getLinkProperties(it) }

            // IP Address
            var ip = "N/A"
            linkProperties?.linkAddresses?.forEach { linkAddress ->
                if (linkAddress.address is Inet4Address) {
                    ip = linkAddress.address.hostAddress ?: "N/A"
                }
            }
            if (ip == "N/A") {
                ip = getLocalIpv4()
            }

            // Gateway & DNS
            val gateway = linkProperties?.routes?.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress ?: "N/A"
            val dnsServers = linkProperties?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList()

            val rawSsid = wifiInfo?.ssid?.trim('\"') ?: "Connected Wi-Fi"
            val ssid = if (rawSsid == "<unknown ssid>" || rawSsid.isBlank()) "Connected Wi-Fi (Permission needed for SSID)" else rawSsid
            val rawBssid = wifiInfo?.bssid ?: "N/A"
            val bssid = if (rawBssid == "02:00:00:00:00:00") "Hidden for privacy" else rawBssid

            val rssi = wifiInfo?.rssi ?: -100
            val linkSpeed = wifiInfo?.linkSpeed ?: 0
            val freq = wifiInfo?.frequency ?: 0
            val networkId = wifiInfo?.networkId ?: -1

            val band = when {
                freq in 2400..2495 -> "2.4 GHz"
                freq in 5150..5895 -> "5 GHz"
                freq in 5925..7125 -> "6 GHz (Wi-Fi 6E/7)"
                freq > 58000 -> "60 GHz (WiGig)"
                else -> if (freq > 0) "$freq MHz" else "N/A"
            }

            val channel = frequencyToChannel(freq)
            val standard = getWifiStandardString(wifiInfo)
            val signalLevel = calculateSignalLevel(rssi)

            _connectedInfo.value = ConnectedWifiInfo(
                isConnected = true,
                ssid = ssid,
                bssid = bssid,
                ipAddress = ip,
                linkSpeedMbps = linkSpeed,
                rssiDbm = rssi,
                frequencyMhz = freq,
                wifiStandard = standard,
                channel = channel,
                band = band,
                gateway = gateway,
                dnsServers = dnsServers,
                subnetMask = getSubnetMask(linkProperties),
                networkId = networkId,
                signalLevel = signalLevel,
                connectionState = "Connected and active"
            )
        } catch (e: Exception) {
            _connectedInfo.value = ConnectedWifiInfo(
                isConnected = false,
                connectionState = "Wi-Fi details unavailable: ${e.localizedMessage ?: "Permission required"}"
            )
        }
    }

    private fun getWifiStandardString(wifiInfo: WifiInfo?): String {
        if (wifiInfo == null) return "Unknown"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return when (wifiInfo.wifiStandard) {
                ScanResult.WIFI_STANDARD_11AX -> "Wi-Fi 6 / 6E (802.11ax)"
                ScanResult.WIFI_STANDARD_11AC -> "Wi-Fi 5 (802.11ac)"
                ScanResult.WIFI_STANDARD_11N -> "Wi-Fi 4 (802.11n)"
                ScanResult.WIFI_STANDARD_LEGACY -> "Legacy (802.11a/b/g)"
                8 /* WIFI_STANDARD_11BE on Android 14+ */ -> "Wi-Fi 7 (802.11be)"
                else -> "Wi-Fi standard #${wifiInfo.wifiStandard}"
            }
        }
        return "Standard details (Requires API 30+)"
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (wifiManager == null) {
            _scanState.update { it.copy(scanMessage = "Wi-Fi Manager unavailable") }
            return
        }

        val now = System.currentTimeMillis()
        // Check Android scan throttling: filter timestamps within last 2 minutes (120,000 ms)
        scanTimestamps.removeAll { now - it > 120000L }
        val isThrottled = scanTimestamps.size >= 4

        if (isThrottled) {
            val oldest = scanTimestamps.minOrNull() ?: now
            val waitSec = ((120000L - (now - oldest)) / 1000).coerceAtLeast(1)
            _scanState.update {
                it.copy(
                    isThrottledNotice = true,
                    scanMessage = "Android scan throttle active (max 4 scans per 2 min in foreground). Cached results shown. Retry in ${waitSec}s."
                )
            }
            // Fetch cached results anyway!
            loadExistingScanResults(isFresh = false)
            return
        }

        scanTimestamps.add(now)

        registerScanReceiverIfNeeded()

        _scanState.update {
            it.copy(
                isScanning = true,
                isThrottledNotice = false,
                scanMessage = "Initiating scan through WifiManager..."
            )
        }

        try {
            val success = wifiManager.startScan()
            if (!success) {
                _scanState.update {
                    it.copy(
                        isScanning = false,
                        scanMessage = "startScan() returned false. Android system may have throttled or rejected the request. Displaying cached results."
                    )
                }
                loadExistingScanResults(isFresh = false)
            }
        } catch (e: Exception) {
            _scanState.update {
                it.copy(
                    isScanning = false,
                    scanMessage = "Scan exception: ${e.localizedMessage ?: "Unknown error"}. Displaying cached results."
                )
            }
            loadExistingScanResults(isFresh = false)
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadExistingScanResults(isFresh: Boolean) {
        if (wifiManager == null) return
        val results: List<ScanResult> = try {
            wifiManager.scanResults ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val nowBootUs = SystemClock.elapsedRealtime() * 1000L

        val accessPoints = results.map { sr ->
            val bssid = sr.BSSID ?: "N/A"
            val rawSsid = sr.SSID ?: ""
            val ssid = if (rawSsid.isBlank()) "[Hidden SSID]" else rawSsid
            val rssi = sr.level
            val freq = sr.frequency
            val channel = frequencyToChannel(freq)
            val channelWidth = formatChannelWidth(sr.channelWidth)
            val capabilities = sr.capabilities ?: "Open"
            val timestamp = sr.timestamp

            // If timestamp was within 30 seconds of boot clock, it's fresh
            val ageUs = nowBootUs - timestamp
            val fresh = isFresh && ageUs < 30_000_000L

            val estimatedDist = DistanceEstimator.estimateDistance(
                rssiDbm = rssi,
                refRssiAt1m = refRssiAt1m,
                pathLossExponent = pathLossExponent
            )

            WifiAccessPoint(
                bssid = bssid,
                ssid = ssid,
                rssiDbm = rssi,
                frequencyMhz = freq,
                channel = channel,
                channelWidth = channelWidth,
                capabilities = capabilities,
                timestampUs = timestamp,
                isFresh = fresh,
                signalLevel = calculateSignalLevel(rssi),
                estimatedDistanceMeters = estimatedDist
            )
        }.sortedByDescending { it.rssiDbm }

        _scanState.update {
            it.copy(
                isScanning = false,
                lastScanTimeMs = System.currentTimeMillis(),
                scanCount = it.scanCount + 1,
                scanMessage = "Discovered ${accessPoints.size} access points (${if (isFresh) "Fresh scan" else "Cached scan"}).",
                accessPoints = accessPoints
            )
        }
    }

    private fun registerScanReceiverIfNeeded() {
        if (scanReceiver != null) return

        scanReceiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    loadExistingScanResults(isFresh = updated)
                }
            }
        }

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(scanReceiver, filter)
    }

    private fun registerNetworkCallback() {
        if (connectivityManager == null) return
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                refreshConnectedWifi()
            }

            override fun onLost(network: Network) {
                refreshConnectedWifi()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                refreshConnectedWifi()
            }
        }
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
        } catch (_: Exception) {
        }
    }

    private fun frequencyToChannel(freq: Int): Int {
        return when {
            freq == 2484 -> 14
            freq in 2412..2472 -> (freq - 2407) / 5
            freq in 5170..5825 -> (freq - 5000) / 5
            freq in 5945..7105 -> (freq - 5940) / 5
            else -> 0
        }
    }

    private fun formatChannelWidth(width: Int): String {
        return when (width) {
            ScanResult.CHANNEL_WIDTH_20MHZ -> "20 MHz"
            ScanResult.CHANNEL_WIDTH_40MHZ -> "40 MHz"
            ScanResult.CHANNEL_WIDTH_80MHZ -> "80 MHz"
            ScanResult.CHANNEL_WIDTH_160MHZ -> "160 MHz"
            ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80+80 MHz"
            5 /* CHANNEL_WIDTH_320MHZ on Android 14+ */ -> "320 MHz (Wi-Fi 7)"
            else -> "Standard"
        }
    }

    private fun calculateSignalLevel(rssi: Int): Int {
        return when {
            rssi >= -55 -> 4
            rssi >= -67 -> 3
            rssi >= -78 -> 2
            rssi >= -88 -> 1
            else -> 0
        }
    }

    private fun getSubnetMask(linkProperties: android.net.LinkProperties?): String {
        val linkAddress = linkProperties?.linkAddresses?.firstOrNull { it.address is Inet4Address }
        val prefix = linkAddress?.prefixLength ?: return "N/A"
        val mask = (0xffffffffL shl (32 - prefix)) and 0xffffffffL
        return "${(mask shr 24) and 0xff}.${(mask shr 16) and 0xff}.${(mask shr 8) and 0xff}.${mask and 0xff} (/$prefix)"
    }

    private fun getLocalIpv4(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "N/A"
            for (intf in interfaces) {
                val addrs = intf.inetAddresses ?: continue
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: "N/A"
                    }
                }
            }
        } catch (_: Exception) {}
        return "N/A"
    }

    fun cleanup() {
        scanReceiver?.let {
            try { context.unregisterReceiver(it) } catch (_: Exception) {}
            scanReceiver = null
        }
        networkCallback?.let {
            try { connectivityManager?.unregisterNetworkCallback(it) } catch (_: Exception) {}
            networkCallback = null
        }
    }
}
