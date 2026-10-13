package com.example.data.repository

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.view.Display
import androidx.core.content.ContextCompat
import com.example.data.model.CameraSummary
import com.example.data.model.DeviceFullInfo
import com.example.data.model.GnssTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DeviceInfoRepository(private val context: Context) {

    private val _deviceInfo = MutableStateFlow<DeviceFullInfo?>(null)
    val deviceInfo: StateFlow<DeviceFullInfo?> = _deviceInfo.asStateFlow()

    init {
        refreshDeviceInfo()
    }

    @SuppressLint("MissingPermission")
    fun refreshDeviceInfo() {
        try {
            // Battery info from sticky broadcast
            val batteryIntent = try {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            } catch (_: Exception) {
                null
            }

            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

            val statusInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val batteryStatus = when (statusInt) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "Full (100%)"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
                else -> "Battery Power"
            }

            val chargePlug = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
            val powerSource = when (chargePlug) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Qi / Inductive"
                else -> "Battery Power"
            }

            val healthInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
            val batteryHealth = when (healthInt) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Normal"
            }

            val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            val tempRaw = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempCelsius = tempRaw / 10.0f
            val tech = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

            // Memory info
            val memInfo = ActivityManager.MemoryInfo()
            try {
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                actManager?.getMemoryInfo(memInfo)
            } catch (_: Exception) {}

            // Storage info
            var totalStorage = 0L
            var availStorage = 0L
            try {
                val statFs = StatFs(Environment.getDataDirectory().path)
                totalStorage = statFs.blockCountLong * statFs.blockSizeLong
                availStorage = statFs.availableBlocksLong * statFs.blockSizeLong
            } catch (_: Exception) {}

            // Display info safely accessed via resources
            val displayMetrics = context.resources.displayMetrics
            val widthPx = displayMetrics.widthPixels
            val heightPx = displayMetrics.heightPixels
            val densityDpi = displayMetrics.densityDpi

            var refreshRate = 60f
            var isHdr = false
            var isWideColor = false

            try {
                val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                val defaultDisplay = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
                if (defaultDisplay != null) {
                    refreshRate = defaultDisplay.refreshRate
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        isHdr = defaultDisplay.isHdr
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        isWideColor = defaultDisplay.isWideColorGamut
                    }
                }
            } catch (_: Exception) {}

            // Camera inventory via CameraManager (no permissions needed for characteristics)
            val cameraList = inspectCameras()

            // GNSS info
            var gpsEnabled = false
            var netEnabled = false
            var hasLocPerm = false
            var lastLoc: Location? = null

            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (locationManager != null) {
                    gpsEnabled = try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }
                    netEnabled = try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false }

                    hasLocPerm = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasLocPerm) {
                        try {
                            lastLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        } catch (_: SecurityException) {}
                    }
                }
            } catch (_: Exception) {}

            val gnss = GnssTelemetry(
                isGpsEnabled = gpsEnabled,
                isNetworkEnabled = netEnabled,
                permissionGranted = hasLocPerm,
                latitude = lastLoc?.latitude,
                longitude = lastLoc?.longitude,
                altitudeMeters = if (lastLoc?.hasAltitude() == true) lastLoc.altitude else null,
                accuracyMeters = if (lastLoc?.hasAccuracy() == true) lastLoc.accuracy else null,
                speedMps = if (lastLoc?.hasSpeed() == true) lastLoc.speed else null,
                bearingDegrees = if (lastLoc?.hasBearing() == true) lastLoc.bearing else null,
                timeFormatted = lastLoc?.time?.let { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(it)) },
                provider = lastLoc?.provider
            )

            _deviceInfo.value = DeviceFullInfo(
                manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
                model = Build.MODEL,
                brand = Build.BRAND,
                device = Build.DEVICE,
                board = Build.BOARD,
                hardware = Build.HARDWARE,
                androidVersion = Build.VERSION.RELEASE,
                apiLevel = Build.VERSION.SDK_INT,
                securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A",
                buildId = Build.DISPLAY ?: Build.ID,
                cpuCores = Runtime.getRuntime().availableProcessors(),
                supportedAbis = Build.SUPPORTED_ABIS.toList(),
                totalRamBytes = memInfo.totalMem,
                availableRamBytes = memInfo.availMem,
                isLowMemory = memInfo.lowMemory,
                lowMemoryThresholdBytes = memInfo.threshold,
                totalStorageBytes = totalStorage,
                availableStorageBytes = availStorage,
                displayWidthPx = widthPx,
                displayHeightPx = heightPx,
                displayDensityDpi = densityDpi,
                refreshRateHz = refreshRate,
                isHdrSupported = isHdr,
                isWideColorGamut = isWideColor,
                batteryPercent = batteryPct,
                batteryStatus = batteryStatus,
                powerSource = powerSource,
                batteryHealth = batteryHealth,
                batteryVoltageMv = voltageMv,
                batteryTempCelsius = tempCelsius,
                batteryTechnology = tech,
                cameras = cameraList,
                gnss = gnss
            )
        } catch (_: Exception) {
            // Guarantee fallback state so ViewModel construction never fails
            if (_deviceInfo.value == null) {
                _deviceInfo.value = DeviceFullInfo(
                    manufacturer = Build.MANUFACTURER,
                    model = Build.MODEL,
                    brand = Build.BRAND,
                    device = Build.DEVICE,
                    board = Build.BOARD,
                    hardware = Build.HARDWARE,
                    androidVersion = Build.VERSION.RELEASE,
                    apiLevel = Build.VERSION.SDK_INT,
                    securityPatch = "N/A",
                    buildId = Build.ID,
                    cpuCores = Runtime.getRuntime().availableProcessors(),
                    supportedAbis = Build.SUPPORTED_ABIS.toList(),
                    totalRamBytes = 0L,
                    availableRamBytes = 0L,
                    isLowMemory = false,
                    lowMemoryThresholdBytes = 0L,
                    totalStorageBytes = 0L,
                    availableStorageBytes = 0L,
                    displayWidthPx = 1080,
                    displayHeightPx = 2400,
                    displayDensityDpi = 420,
                    refreshRateHz = 60f,
                    isHdrSupported = false,
                    isWideColorGamut = false,
                    batteryPercent = 100,
                    batteryStatus = "Battery Power",
                    powerSource = "Battery Power",
                    batteryHealth = "Good",
                    batteryVoltageMv = 4000,
                    batteryTempCelsius = 25f,
                    batteryTechnology = "Li-ion",
                    cameras = emptyList(),
                    gnss = GnssTelemetry()
                )
            }
        }
    }

    private fun inspectCameras(): List<CameraSummary> {
        val cameraManager = try {
            context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        } catch (_: Exception) {
            null
        } ?: return emptyList()

        val list = mutableListOf<CameraSummary>()

        try {
            val cameraIds = cameraManager.cameraIdList
            for (id in cameraIds) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facingInt = chars.get(CameraCharacteristics.LENS_FACING)
                val facing = when (facingInt) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "Front (Selfie)"
                    CameraCharacteristics.LENS_FACING_BACK -> "Back (Main)"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "External USB"
                    else -> "Unknown"
                }

                val levelInt = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                val hwLevel = when (levelInt) {
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                    CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3 (Pro)"
                    else -> "Unknown"
                }

                val flash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

                val activeArraySize = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                val megapixels = if (activeArraySize != null) {
                    (activeArraySize.width().toLong() * activeArraySize.height().toLong()) / 1_000_000f
                } else 0f

                val physicalSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val sizeMm = if (physicalSize != null) {
                    String.format(Locale.US, "%.2f × %.2f mm", physicalSize.width, physicalSize.height)
                } else "N/A"

                list.add(
                    CameraSummary(
                        id = id,
                        facing = facing,
                        megapixels = megapixels,
                        hardwareLevel = hwLevel,
                        hasFlash = flash,
                        sensorSizeMm = sizeMm
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
