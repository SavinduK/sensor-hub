package com.example.data.model

data class SensorLogEntry(
    val id: Long,
    val timestampEpochMs: Long,
    val timestampFormatted: String,
    val sensorName: String,
    val sensorType: String,
    val valuesFormatted: String,
    val rawValues: FloatArray,
    val unit: String,
    val accuracy: String,
    val rateHz: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SensorLogEntry
        return id == other.id &&
                timestampEpochMs == other.timestampEpochMs &&
                rawValues.contentEquals(other.rawValues)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + timestampEpochMs.hashCode()
        result = 31 * result + rawValues.contentHashCode()
        return result
    }

    fun toCsvRow(): String {
        val valuesJoined = rawValues.joinToString(";") { it.toString() }
        val escapedName = "\"${sensorName.replace("\"", "\"\"")}\""
        val escapedType = "\"${sensorType.replace("\"", "\"\"")}\""
        return "$timestampEpochMs,\"$timestampFormatted\",$escapedName,$escapedType,\"$valuesJoined\",\"$unit\",\"$accuracy\",${String.format(java.util.Locale.US, "%.1f", rateHz)}"
    }

    companion object {
        const val CSV_HEADER = "timestamp_epoch_ms,timestamp_iso,sensor_name,sensor_type,values_semicolon_separated,unit,accuracy,rate_hz"
    }
}
