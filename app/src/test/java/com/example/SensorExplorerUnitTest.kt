package com.example

import android.hardware.Sensor
import com.example.data.model.SensorLogEntry
import com.example.util.DistanceEstimator
import com.example.util.SensorCategory
import com.example.util.SensorUnits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorExplorerUnitTest {

    @Test
    fun sensorUnits_correctUnitLabelsForHardwareSensors() {
        assertEquals("m/s²", SensorUnits.getUnitLabel(Sensor.TYPE_ACCELEROMETER))
        assertEquals("rad/s", SensorUnits.getUnitLabel(Sensor.TYPE_GYROSCOPE))
        assertEquals("µT", SensorUnits.getUnitLabel(Sensor.TYPE_MAGNETIC_FIELD))
        assertEquals("lux", SensorUnits.getUnitLabel(Sensor.TYPE_LIGHT))
        assertEquals("hPa", SensorUnits.getUnitLabel(Sensor.TYPE_PRESSURE))
        assertEquals("cm", SensorUnits.getUnitLabel(Sensor.TYPE_PROXIMITY))
        assertEquals("°C", SensorUnits.getUnitLabel(Sensor.TYPE_AMBIENT_TEMPERATURE))
        assertEquals("%", SensorUnits.getUnitLabel(Sensor.TYPE_RELATIVE_HUMIDITY))
        assertEquals("steps", SensorUnits.getUnitLabel(Sensor.TYPE_STEP_COUNTER))
    }

    @Test
    fun sensorUnits_correctCategories() {
        assertEquals(SensorCategory.MOTION, SensorUnits.getCategory(Sensor.TYPE_ACCELEROMETER))
        assertEquals(SensorCategory.MOTION, SensorUnits.getCategory(Sensor.TYPE_GYROSCOPE))
        assertEquals(SensorCategory.POSITION, SensorUnits.getCategory(Sensor.TYPE_MAGNETIC_FIELD))
        assertEquals(SensorCategory.POSITION, SensorUnits.getCategory(Sensor.TYPE_PROXIMITY))
        assertEquals(SensorCategory.ENVIRONMENT, SensorUnits.getCategory(Sensor.TYPE_LIGHT))
        assertEquals(SensorCategory.ENVIRONMENT, SensorUnits.getCategory(Sensor.TYPE_PRESSURE))
        assertEquals(SensorCategory.HEALTH, SensorUnits.getCategory(Sensor.TYPE_HEART_RATE))
    }

    @Test
    fun sensorUnits_formattingValues() {
        assertEquals("1234", SensorUnits.formatSensorValue(1234f, "steps"))
        assertEquals("9.807", SensorUnits.formatSensorValue(9.80665f, "m/s²"))
        assertEquals("1013.3", SensorUnits.formatSensorValue(1013.25f, "hPa"))
    }


    @Test
    fun distanceEstimator_calculationMatchesPathLossFormula() {
        // At exact reference RSSI (e.g. -40 dBm at 1m), distance should be exactly 1.0 meter
        val distAt1m = DistanceEstimator.estimateDistance(
            rssiDbm = -40,
            refRssiAt1m = -40,
            pathLossExponent = 2.0
        )
        assertEquals(1.0, distAt1m, 0.05)

        // Drop of 20 dB with n = 2.0 corresponds to 10x distance (10 meters)
        // exponent = (-40 - (-60)) / (10 * 2) = 20 / 20 = 1.0 -> 10^1 = 10.0m
        val distAt10m = DistanceEstimator.estimateDistance(
            rssiDbm = -60,
            refRssiAt1m = -40,
            pathLossExponent = 2.0
        )
        assertEquals(10.0, distAt10m, 0.1)
    }

    @Test
    fun distanceEstimator_edgeCasesAndCaps() {
        // Invalid RSSI > 0 or 0 returns 0.0
        assertEquals(0.0, DistanceEstimator.estimateDistance(0, -40, 2.0), 0.001)
        assertEquals(0.0, DistanceEstimator.estimateDistance(10, -40, 2.0), 0.001)

        // Extremely low signal capped at 150m
        val veryFar = DistanceEstimator.estimateDistance(-120, -40, 2.0)
        assertTrue(veryFar <= 150.0)

        // Extremely high signal capped at 0.1m
        val veryClose = DistanceEstimator.estimateDistance(-10, -50, 2.0)
        assertTrue(veryClose >= 0.1)
    }

    @Test
    fun distanceEstimator_proximityBands() {
        assertEquals("Immediate (< 0.5m)", DistanceEstimator.getProximityBand(0.3))
        assertEquals("Near (0.5 - 3m)", DistanceEstimator.getProximityBand(1.5))
        assertEquals("Mid-range (3 - 15m)", DistanceEstimator.getProximityBand(8.0))
        assertEquals("Far (> 15m)", DistanceEstimator.getProximityBand(25.0))
    }

    @Test
    fun sensorLogEntry_toCsvRowFormattedCorrectly() {
        val entry = SensorLogEntry(
            id = 1L,
            timestampEpochMs = 1700000000000L,
            timestampFormatted = "2023-11-14 12:00:00.000",
            sensorName = "BMI160 Accelerometer",
            sensorType = "Accelerometer",
            valuesFormatted = "X: 0.12, Y: 9.81, Z: 0.05",
            rawValues = floatArrayOf(0.12f, 9.81f, 0.05f),
            unit = "m/s²",
            accuracy = "High (3)",
            rateHz = 50.0f
        )

        val csv = entry.toCsvRow()
        assertTrue(csv.contains("1700000000000"))
        assertTrue(csv.contains("\"BMI160 Accelerometer\""))
        assertTrue(csv.contains("\"Accelerometer\""))
        assertTrue(csv.contains("\"0.12;9.81;0.05\""))
        assertTrue(csv.contains("\"m/s²\""))
        assertTrue(csv.contains("50.0"))
    }
}
