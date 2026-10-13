package com.example.data.model

data class CameraSummary(
    val id: String,
    val facing: String,
    val megapixels: Float,
    val hardwareLevel: String,
    val hasFlash: Boolean,
    val sensorSizeMm: String
)

data class GnssTelemetry(
    val isGpsEnabled: Boolean = false,
    val isNetworkEnabled: Boolean = false,
    val permissionGranted: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitudeMeters: Double? = null,
    val accuracyMeters: Float? = null,
    val speedMps: Float? = null,
    val bearingDegrees: Float? = null,
    val timeFormatted: String? = null,
    val provider: String? = null
)

data class DeviceFullInfo(
    // OS & Platform
    val manufacturer: String,
    val model: String,
    val brand: String,
    val device: String,
    val board: String,
    val hardware: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val buildId: String,

    // CPU & Memory
    val cpuCores: Int,
    val supportedAbis: List<String>,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val isLowMemory: Boolean,
    val lowMemoryThresholdBytes: Long,

    // Storage
    val totalStorageBytes: Long,
    val availableStorageBytes: Long,

    // Display
    val displayWidthPx: Int,
    val displayHeightPx: Int,
    val displayDensityDpi: Int,
    val refreshRateHz: Float,
    val isHdrSupported: Boolean,
    val isWideColorGamut: Boolean,

    // Battery
    val batteryPercent: Int,
    val batteryStatus: String,
    val powerSource: String,
    val batteryHealth: String,
    val batteryVoltageMv: Int,
    val batteryTempCelsius: Float,
    val batteryTechnology: String,

    // Cameras
    val cameras: List<CameraSummary>,

    // GNSS
    val gnss: GnssTelemetry
)
