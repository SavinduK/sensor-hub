package com.example.util

import kotlin.math.pow

object DistanceEstimator {

    const val DEFAULT_WIFI_REF_RSSI = -40 // dBm measured at 1 meter
    const val DEFAULT_WIFI_PATH_LOSS_N = 2.7 // Indoor path-loss exponent (typically 2.0 - 3.5)

    const val DEFAULT_BLE_REF_RSSI = -59 // dBm measured at 1 meter (standard iBeacon default)
    const val DEFAULT_BLE_PATH_LOSS_N = 2.4 // Indoor path-loss exponent

    /**
     * Calculates estimated distance in meters using Log-Distance Path Loss Model:
     * RSSI = RSSI_0 - 10 * n * log10(d)
     * d = 10 ^ ((RSSI_0 - RSSI) / (10 * n))
     *
     * @param rssiDbm measured signal strength in dBm
     * @param refRssiAt1m reference signal strength at 1 meter distance (dBm)
     * @param pathLossExponent environment path loss exponent n (2.0 = free space, 2.5-3.5 = indoor)
     * @return estimated distance in meters, capped to reasonable limits
     */
    fun estimateDistance(
        rssiDbm: Int,
        refRssiAt1m: Int,
        pathLossExponent: Double
    ): Double {
        if (rssiDbm == 0 || rssiDbm > 0) return 0.0
        val exponent = (refRssiAt1m - rssiDbm) / (10.0 * pathLossExponent)
        val distance = 10.0.pow(exponent)
        return distance.coerceIn(0.1, 150.0)
    }

    fun formatDistance(meters: Double): String {
        return if (meters < 1.0) {
            String.format(java.util.Locale.US, "~%.1f m (< 1 m)", meters)
        } else if (meters < 10.0) {
            String.format(java.util.Locale.US, "~%.1f m", meters)
        } else {
            String.format(java.util.Locale.US, "~%.0f m", meters)
        }
    }

    fun getProximityBand(meters: Double): String {
        return when {
            meters <= 0.0 -> "Unknown"
            meters < 0.5 -> "Immediate (< 0.5m)"
            meters < 3.0 -> "Near (0.5 - 3m)"
            meters < 15.0 -> "Mid-range (3 - 15m)"
            else -> "Far (> 15m)"
        }
    }

    const val DISCLAIMER = "Uncalibrated radio path-loss estimate. Actual distance varies widely due to physical obstacles, reflections, human body absorption, and antenna directionality. For precise distance, Wi-Fi RTT (802.11mc/az) or Bluetooth Channel Sounding hardware is required."
}
