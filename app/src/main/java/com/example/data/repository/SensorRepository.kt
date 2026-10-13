package com.example.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.data.model.SamplingPreset
import com.example.data.model.SensorLogEntry
import com.example.data.model.SensorMetadata
import com.example.data.model.SensorStatus
import com.example.data.model.SensorUiState
import com.example.util.ReportingMode
import com.example.util.SensorUnits
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

class SensorRepository(private val context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val _sensorsState = MutableStateFlow<Map<Int, SensorUiState>>(emptyMap())
    val sensorsState: StateFlow<Map<Int, SensorUiState>> = _sensorsState.asStateFlow()

    private val _rawLogs = MutableStateFlow<List<SensorLogEntry>>(emptyList())
    val rawLogs: StateFlow<List<SensorLogEntry>> = _rawLogs.asStateFlow()

    private val _currentPreset = MutableStateFlow(SamplingPreset.UI_EFFICIENT)
    val currentPreset: StateFlow<SamplingPreset> = _currentPreset.asStateFlow()

    private val _isAllPaused = MutableStateFlow(false)
    val isAllPaused: StateFlow<Boolean> = _isAllPaused.asStateFlow()

    // Internal tracking structures
    private val sensorObjectMap = mutableMapOf<Int, Sensor>()
    private val registeredListeners = ConcurrentHashMap<Int, SensorEventListener>()

    // For rate calculation & UI throttling
    private class SensorRuntimeData(
        val sensor: Sensor,
        val metadata: SensorMetadata
    ) {
        var isMonitored = false
        var values = floatArrayOf()
        var accuracy = 0
        var lastTimestampNs = 0L
        var eventCount = 0L
        var lastCalcEventCount = 0L
        var lastCalcTimeMs = System.currentTimeMillis()
        var currentRateHz = 0f
        val history = ArrayDeque<Float>(32)
        var status = SensorStatus.WAITING
        var statusMessage = "Waiting for data"
    }

    private val runtimeDataMap = ConcurrentHashMap<Int, SensorRuntimeData>()
    private val logIdGenerator = AtomicLong(0)
    private val maxLogEntries = 600

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var uiUpdateJob: Job? = null

    init {
        discoverSensors()
        startUiUpdateLoop()
    }

    private fun discoverSensors() {
        val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        val initialMap = mutableMapOf<Int, SensorUiState>()

        allSensors.forEachIndexed { index, sensor ->
            // Use index + 1 as unique key if sensor.id is 0 or duplicate
            val uniqueId = index + 1
            sensorObjectMap[uniqueId] = sensor

            val category = SensorUnits.getCategory(sensor.type)
            val typeName = SensorUnits.getTypeName(sensor.type)
            val unitLabel = SensorUnits.getUnitLabel(sensor.type)
            val valueLabels = SensorUnits.getValueLabels(sensor.type)
            val reportingMode = SensorUnits.getReportingMode(sensor)
            val isContinuous = reportingMode == ReportingMode.CONTINUOUS || reportingMode == ReportingMode.ON_CHANGE

            val metadata = SensorMetadata(
                id = uniqueId,
                name = sensor.name ?: "Unknown Sensor",
                vendor = sensor.vendor ?: "Unknown Vendor",
                version = sensor.version,
                type = sensor.type,
                typeName = typeName,
                category = category,
                reportingMode = reportingMode,
                isWakeUpSensor = sensor.isWakeUpSensor,
                isDynamicSensor = sensor.isDynamicSensor,
                powerMa = sensor.power,
                resolution = sensor.resolution,
                maximumRange = sensor.maximumRange,
                minDelayUs = sensor.minDelay,
                maxDelayUs = sensor.maxDelay,
                unitLabel = unitLabel,
                valueLabels = valueLabels,
                isContinuous = isContinuous,
                fifoReservedEventCount = sensor.fifoReservedEventCount,
                fifoMaxEventCount = sensor.fifoMaxEventCount
            )

            val runtime = SensorRuntimeData(sensor, metadata)
            runtimeDataMap[uniqueId] = runtime

            initialMap[uniqueId] = SensorUiState(
                metadata = metadata,
                isMonitored = false,
                status = if (isContinuous) SensorStatus.WAITING else SensorStatus.ACTIVE,
                statusMessage = if (isContinuous) "Idle - tap to monitor" else "Event-based sensor"
            )
        }

        _sensorsState.value = initialMap
    }

    private fun startUiUpdateLoop() {
        uiUpdateJob?.cancel()
        uiUpdateJob = scope.launch {
            while (isActive) {
                delay(65) // ~15 FPS throttled UI emission to prevent Compose stuttering
                if (_isAllPaused.value) continue

                val now = System.currentTimeMillis()
                val updatedState = mutableMapOf<Int, SensorUiState>()

                runtimeDataMap.forEach { (id, runtime) ->
                    if (runtime.isMonitored) {
                        // Recalculate rate every 1 second
                        val timeElapsedSec = (now - runtime.lastCalcTimeMs) / 1000f
                        if (timeElapsedSec >= 1.0f) {
                            val eventsDelta = runtime.eventCount - runtime.lastCalcEventCount
                            runtime.currentRateHz = eventsDelta / timeElapsedSec
                            runtime.lastCalcEventCount = runtime.eventCount
                            runtime.lastCalcTimeMs = now
                        }

                        updatedState[id] = SensorUiState(
                            metadata = runtime.metadata,
                            isMonitored = true,
                            values = runtime.values.clone(),
                            accuracy = runtime.accuracy,
                            lastTimestampNs = runtime.lastTimestampNs,
                            eventCount = runtime.eventCount,
                            observedRateHz = runtime.currentRateHz,
                            history = runtime.history.toList(),
                            status = runtime.status,
                            statusMessage = runtime.statusMessage
                        )
                    } else {
                        val current = _sensorsState.value[id]
                        if (current != null) {
                            updatedState[id] = current
                        }
                    }
                }

                if (updatedState.isNotEmpty()) {
                    _sensorsState.value = updatedState
                }
            }
        }
    }

    fun startMonitoringSensor(sensorId: Int) {
        val runtime = runtimeDataMap[sensorId] ?: return
        if (runtime.isMonitored) return

        val sensor = sensorObjectMap[sensorId] ?: return
        val delayUs = _currentPreset.value.delayUs

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || _isAllPaused.value) return

                val vals = event.values.clone()
                runtime.values = vals
                runtime.accuracy = event.accuracy
                runtime.lastTimestampNs = event.timestamp
                runtime.eventCount++
                runtime.status = SensorStatus.ACTIVE
                runtime.statusMessage = "Active"

                // Compute sparkline magnitude / primary value
                val sparkVal = if (vals.isNotEmpty()) {
                    if (vals.size >= 3 && runtime.metadata.category == com.example.util.SensorCategory.MOTION) {
                        // Vector magnitude for 3D motion: sqrt(x^2 + y^2 + z^2)
                        sqrt((vals[0] * vals[0] + vals[1] * vals[1] + vals[2] * vals[2]).toDouble()).toFloat()
                    } else {
                        vals[0]
                    }
                } else 0f

                synchronized(runtime.history) {
                    if (runtime.history.size >= 32) {
                        runtime.history.removeFirst()
                    }
                    runtime.history.add(sparkVal)
                }

                // Append to raw logs periodically (every 5th event or all if rate is low)
                if (runtime.eventCount % 4L == 0L || runtime.currentRateHz < 5f) {
                    appendLog(runtime, vals, event.accuracy)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                runtime.accuracy = accuracy
            }
        }

        val registered = sensorManager.registerListener(listener, sensor, delayUs)
        if (registered) {
            registeredListeners[sensorId] = listener
            runtime.isMonitored = true
            runtime.status = SensorStatus.WAITING
            runtime.statusMessage = "Registered (awaiting event)"
        } else {
            runtime.status = SensorStatus.UNSUPPORTED
            runtime.statusMessage = "Registration failed (hardware rejected)"
        }

        _sensorsState.update { current ->
            current.toMutableMap().apply {
                this[sensorId] = this[sensorId]?.copy(
                    isMonitored = runtime.isMonitored,
                    status = runtime.status,
                    statusMessage = runtime.statusMessage
                ) ?: SensorUiState(metadata = runtime.metadata, isMonitored = runtime.isMonitored)
            }
        }
    }

    fun stopMonitoringSensor(sensorId: Int) {
        val listener = registeredListeners.remove(sensorId)
        if (listener != null) {
            sensorManager.unregisterListener(listener)
        }

        val runtime = runtimeDataMap[sensorId] ?: return
        runtime.isMonitored = false
        runtime.status = SensorStatus.PAUSED
        runtime.statusMessage = "Stopped"
        runtime.currentRateHz = 0f

        _sensorsState.update { current ->
            current.toMutableMap().apply {
                this[sensorId] = this[sensorId]?.copy(
                    isMonitored = false,
                    status = SensorStatus.PAUSED,
                    statusMessage = "Stopped",
                    observedRateHz = 0f
                ) ?: SensorUiState(metadata = runtime.metadata, isMonitored = false)
            }
        }
    }

    fun startMonitoringAll() {
        runtimeDataMap.keys.forEach { sensorId ->
            val runtime = runtimeDataMap[sensorId]
            if (runtime != null && runtime.metadata.isContinuous) {
                startMonitoringSensor(sensorId)
            }
        }
        _isAllPaused.value = false
    }

    fun stopMonitoringAll() {
        registeredListeners.keys.toList().forEach { sensorId ->
            stopMonitoringSensor(sensorId)
        }
    }

    fun togglePauseAll() {
        val paused = !_isAllPaused.value
        _isAllPaused.value = paused
    }

    fun setSamplingPreset(preset: SamplingPreset) {
        _currentPreset.value = preset
        // Re-register active continuous listeners with new delay
        val activeIds = registeredListeners.keys.toList()
        activeIds.forEach { id ->
            val listener = registeredListeners.remove(id)
            if (listener != null) {
                sensorManager.unregisterListener(listener)
            }
            val sensor = sensorObjectMap[id]
            val runtime = runtimeDataMap[id]
            if (sensor != null && runtime != null) {
                val newListener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event == null || _isAllPaused.value) return
                        val vals = event.values.clone()
                        runtime.values = vals
                        runtime.accuracy = event.accuracy
                        runtime.lastTimestampNs = event.timestamp
                        runtime.eventCount++
                        runtime.status = SensorStatus.ACTIVE
                        runtime.statusMessage = "Active"

                        val sparkVal = if (vals.isNotEmpty()) {
                            if (vals.size >= 3 && runtime.metadata.category == com.example.util.SensorCategory.MOTION) {
                                sqrt((vals[0] * vals[0] + vals[1] * vals[1] + vals[2] * vals[2]).toDouble()).toFloat()
                            } else vals[0]
                        } else 0f

                        synchronized(runtime.history) {
                            if (runtime.history.size >= 32) runtime.history.removeFirst()
                            runtime.history.add(sparkVal)
                        }

                        if (runtime.eventCount % 4L == 0L || runtime.currentRateHz < 5f) {
                            appendLog(runtime, vals, event.accuracy)
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                        runtime.accuracy = accuracy
                    }
                }
                sensorManager.registerListener(newListener, sensor, preset.delayUs)
                registeredListeners[id] = newListener
            }
        }
    }

    private fun appendLog(runtime: SensorRuntimeData, vals: FloatArray, accuracy: Int) {
        val now = System.currentTimeMillis()
        val iso = synchronized(dateFormat) { dateFormat.format(Date(now)) }
        val formattedVals = vals.mapIndexed { idx, v ->
            val label = runtime.metadata.valueLabels.getOrElse(idx) { "Val $idx" }
            "$label: ${SensorUnits.formatSensorValue(v, runtime.metadata.unitLabel)}"
        }.joinToString(", ")

        val entry = SensorLogEntry(
            id = logIdGenerator.incrementAndGet(),
            timestampEpochMs = now,
            timestampFormatted = iso,
            sensorName = runtime.metadata.name,
            sensorType = runtime.metadata.typeName,
            valuesFormatted = formattedVals,
            rawValues = vals,
            unit = runtime.metadata.unitLabel,
            accuracy = SensorUnits.getAccuracyLabel(accuracy),
            rateHz = runtime.currentRateHz
        )

        _rawLogs.update { current ->
            val list = current.toMutableList()
            if (list.size >= maxLogEntries) {
                list.removeAt(0)
            }
            list.add(entry)
            list
        }
    }

    fun clearLogs() {
        _rawLogs.value = emptyList()
    }

    fun generateCsvContent(): String {
        val logs = _rawLogs.value
        val sb = StringBuilder()
        sb.append(SensorLogEntry.CSV_HEADER).append("\n")
        logs.forEach { entry ->
            sb.append(entry.toCsvRow()).append("\n")
        }
        return sb.toString()
    }

    fun cleanup() {
        uiUpdateJob?.cancel()
        registeredListeners.values.forEach { sensorManager.unregisterListener(it) }
        registeredListeners.clear()
    }
}
