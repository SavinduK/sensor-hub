package com.example.data.model

import com.example.util.ReportingMode
import com.example.util.SensorCategory

enum class SensorStatus(val label: String) {
    ACTIVE("Active"),
    WAITING("Waiting for data"),
    PAUSED("Paused"),
    UNSUPPORTED("Unsupported"),
    PERMISSION_REQUIRED("Permission required")
}

enum class SamplingPreset(val label: String, val delayUs: Int, val description: String) {
    UI_EFFICIENT("UI-efficient (15 Hz)", 66667, "Optimized for smooth UI rendering with low battery impact"),
    NORMAL("Normal (5 Hz)", 200000, "Standard sensor update rate"),
    FAST("Fast (50 Hz)", 20000, "High-frequency tracking for dynamic motion analysis")
}

data class SensorMetadata(
    val id: Int,
    val name: String,
    val vendor: String,
    val version: Int,
    val type: Int,
    val typeName: String,
    val category: SensorCategory,
    val reportingMode: ReportingMode,
    val isWakeUpSensor: Boolean,
    val isDynamicSensor: Boolean,
    val powerMa: Float,
    val resolution: Float,
    val maximumRange: Float,
    val minDelayUs: Int,
    val maxDelayUs: Int,
    val unitLabel: String,
    val valueLabels: List<String>,
    val isContinuous: Boolean,
    val fifoReservedEventCount: Int,
    val fifoMaxEventCount: Int
)

data class SensorUiState(
    val metadata: SensorMetadata,
    val isMonitored: Boolean = false,
    val values: FloatArray = floatArrayOf(),
    val accuracy: Int = 0,
    val lastTimestampNs: Long = 0L,
    val eventCount: Long = 0L,
    val observedRateHz: Float = 0f,
    val history: List<Float> = emptyList(),
    val status: SensorStatus = SensorStatus.WAITING,
    val statusMessage: String = "Idle"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SensorUiState
        return metadata.id == other.metadata.id &&
                isMonitored == other.isMonitored &&
                values.contentEquals(other.values) &&
                accuracy == other.accuracy &&
                lastTimestampNs == other.lastTimestampNs &&
                eventCount == other.eventCount &&
                observedRateHz == other.observedRateHz &&
                status == other.status
    }

    override fun hashCode(): Int {
        var result = metadata.id.hashCode()
        result = 31 * result + isMonitored.hashCode()
        result = 31 * result + values.contentHashCode()
        result = 31 * result + accuracy
        result = 31 * result + lastTimestampNs.hashCode()
        return result
    }
}
