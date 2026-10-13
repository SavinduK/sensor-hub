package com.example.util

import android.hardware.Sensor
import android.hardware.SensorManager

enum class SensorCategory(val displayName: String) {
    ALL("All"),
    MOTION("Motion"),
    POSITION("Position"),
    ENVIRONMENT("Environment"),
    HEALTH("Biometrics"),
    SPECIAL("Special")
}

enum class ReportingMode(val displayName: String) {
    CONTINUOUS("Continuous"),
    ON_CHANGE("On-change"),
    ONE_SHOT("One-shot"),
    SPECIAL("Special trigger")
}

object SensorUnits {

    fun getCategory(sensorType: Int): SensorCategory {
        return when (sensorType) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_SIGNIFICANT_MOTION,
            Sensor.TYPE_STEP_DETECTOR,
            Sensor.TYPE_STEP_COUNTER -> SensorCategory.MOTION

            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
            Sensor.TYPE_ORIENTATION,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
            36 /* TYPE_HINGE_ANGLE */ -> SensorCategory.POSITION

            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_RELATIVE_HUMIDITY,
            Sensor.TYPE_TEMPERATURE -> SensorCategory.ENVIRONMENT

            Sensor.TYPE_HEART_RATE,
            Sensor.TYPE_HEART_BEAT,
            34 /* TYPE_LOW_LATENCY_OFFBODY_DETECT */ -> SensorCategory.HEALTH

            else -> SensorCategory.SPECIAL
        }
    }

    fun getTypeName(sensorType: Int): String {
        return when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> "Accelerometer (Uncalibrated)"
            Sensor.TYPE_GRAVITY -> "Gravity"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "Gyroscope (Uncalibrated)"
            Sensor.TYPE_LIGHT -> "Ambient Light"
            Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic Field (Compass)"
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "Magnetic Field (Uncalibrated)"
            Sensor.TYPE_ORIENTATION -> "Orientation (Legacy)"
            Sensor.TYPE_PRESSURE -> "Barometer (Atmospheric Pressure)"
            Sensor.TYPE_PROXIMITY -> "Proximity Sensor"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "Relative Humidity"
            Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector (Fused)"
            Sensor.TYPE_GAME_ROTATION_VECTOR -> "Game Rotation Vector (No Mag)"
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "Geomagnetic Rotation Vector"
            Sensor.TYPE_SIGNIFICANT_MOTION -> "Significant Motion Trigger"
            Sensor.TYPE_STEP_COUNTER -> "Step Counter"
            Sensor.TYPE_STEP_DETECTOR -> "Step Detector"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "Ambient Temperature"
            Sensor.TYPE_TEMPERATURE -> "Device Temperature"
            Sensor.TYPE_HEART_RATE -> "Heart Rate"
            Sensor.TYPE_HEART_BEAT -> "Heart Beat"
            Sensor.TYPE_STATIONARY_DETECT -> "Stationary Detector"
            Sensor.TYPE_MOTION_DETECT -> "Motion Detector"
            Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT -> "Off-body Detect"
            Sensor.TYPE_POSE_6DOF -> "6DOF Pose"
            36 /* TYPE_HINGE_ANGLE */ -> "Hinge Angle (Foldable)"
            37 /* TYPE_HEAD_TRACKER */ -> "Head Tracker"
            38 /* TYPE_ACCELEROMETER_LIMITED_AXES */ -> "Limited Axes Accelerometer"
            39 /* TYPE_GYROSCOPE_LIMITED_AXES */ -> "Limited Axes Gyroscope"
            else -> "Sensor Type #$sensorType"
        }
    }

    fun getUnitLabel(sensorType: Int): String {
        return when (sensorType) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION -> "m/s²"

            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> "rad/s"

            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> "µT"

            Sensor.TYPE_LIGHT -> "lux"
            Sensor.TYPE_PRESSURE -> "hPa"
            Sensor.TYPE_PROXIMITY -> "cm"
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_TEMPERATURE -> "°C"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "%"
            Sensor.TYPE_HEART_RATE -> "bpm"
            Sensor.TYPE_STEP_COUNTER -> "steps"
            Sensor.TYPE_ORIENTATION,
            36 /* TYPE_HINGE_ANGLE */ -> "degrees (°)"
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "unitless quaternion"
            Sensor.TYPE_STEP_DETECTOR,
            Sensor.TYPE_SIGNIFICANT_MOTION,
            Sensor.TYPE_STATIONARY_DETECT,
            Sensor.TYPE_MOTION_DETECT -> "events"
            else -> "raw"
        }
    }

    fun getValueLabels(sensorType: Int): List<String> {
        return when (sensorType) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION -> listOf("X", "Y", "Z")

            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED -> listOf(
                "X (no bias)", "Y (no bias)", "Z (no bias)",
                "Bias X", "Bias Y", "Bias Z"
            )

            Sensor.TYPE_GYROSCOPE -> listOf("Roll X", "Pitch Y", "Yaw Z")
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> listOf(
                "X (no drift)", "Y (no drift)", "Z (no drift)",
                "Drift X", "Drift Y", "Drift Z"
            )

            Sensor.TYPE_MAGNETIC_FIELD -> listOf("X", "Y", "Z")
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> listOf(
                "X (uncal)", "Y (uncal)", "Z (uncal)",
                "Hard Iron X", "Hard Iron Y", "Hard Iron Z"
            )

            Sensor.TYPE_ORIENTATION -> listOf("Azimuth", "Pitch", "Roll")

            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> listOf(
                "x·sin(θ/2)", "y·sin(θ/2)", "z·sin(θ/2)", "cos(θ/2)", "Estimated Acc (rad)"
            )

            Sensor.TYPE_GAME_ROTATION_VECTOR -> listOf(
                "x·sin(θ/2)", "y·sin(θ/2)", "z·sin(θ/2)", "cos(θ/2)"
            )

            Sensor.TYPE_LIGHT -> listOf("Illuminance")
            Sensor.TYPE_PRESSURE -> listOf("Pressure")
            Sensor.TYPE_PROXIMITY -> listOf("Distance")
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_TEMPERATURE -> listOf("Temperature")
            Sensor.TYPE_RELATIVE_HUMIDITY -> listOf("Humidity")
            Sensor.TYPE_HEART_RATE -> listOf("Pulse")
            Sensor.TYPE_STEP_COUNTER -> listOf("Step Count")
            Sensor.TYPE_STEP_DETECTOR -> listOf("Step Trigger")
            Sensor.TYPE_SIGNIFICANT_MOTION -> listOf("Motion Trigger")
            36 /* TYPE_HINGE_ANGLE */ -> listOf("Hinge Angle")
            else -> listOf("Value 0", "Value 1", "Value 2")
        }
    }

    fun getReportingMode(sensor: Sensor): ReportingMode {
        return when (sensor.reportingMode) {
            Sensor.REPORTING_MODE_CONTINUOUS -> ReportingMode.CONTINUOUS
            Sensor.REPORTING_MODE_ON_CHANGE -> ReportingMode.ON_CHANGE
            Sensor.REPORTING_MODE_ONE_SHOT -> ReportingMode.ONE_SHOT
            Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> ReportingMode.SPECIAL
            else -> ReportingMode.CONTINUOUS
        }
    }

    fun getAccuracyLabel(accuracy: Int): String {
        return when (accuracy) {
            SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "High (3)"
            SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "Medium (2)"
            SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "Low (1)"
            SensorManager.SENSOR_STATUS_UNRELIABLE -> "Unreliable (0)"
            SensorManager.SENSOR_STATUS_NO_CONTACT -> "No contact"
            else -> "Unknown ($accuracy)"
        }
    }

    fun formatSensorValue(value: Float, unit: String): String {
        return when {
            unit == "steps" || unit == "events" -> value.toLong().toString()
            kotlin.math.abs(value) >= 1000f -> String.format(java.util.Locale.US, "%.1f", value)
            kotlin.math.abs(value) >= 10f -> String.format(java.util.Locale.US, "%.2f", value)
            kotlin.math.abs(value) >= 1f -> String.format(java.util.Locale.US, "%.3f", value)
            else -> String.format(java.util.Locale.US, "%.4f", value)
        }
    }
}
