package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.PatientRecord
import com.example.data.PatientRepository
import com.example.iot.EcgWaveGenerator
import com.example.iot.Esp32Client
import com.example.iot.Esp32Telemetry
import com.example.ui.components.TempUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PatientRepository
    private val esp32Client = Esp32Client()

    init {
        val db = AppDatabase.getInstance(application)
        repository = PatientRepository(db.patientRecordDao())
    }

    val records: StateFlow<List<PatientRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _telemetry = MutableStateFlow(
        Esp32Telemetry(
            heartRate = 75,
            spo2 = 98,
            temperature = 36.8f,
            fallDetected = false,
            fallSeverity = "NONE",
            ecgSample = 0.0f,
            batteryLevel = 94,
            status = "SIMULATION"
        )
    )
    val telemetry: StateFlow<Esp32Telemetry> = _telemetry.asStateFlow()

    private val _recentHeartRates = MutableStateFlow(listOf(72, 73, 74, 75, 76, 75, 74, 76, 75, 77, 75))
    val recentHeartRates: StateFlow<List<Int>> = _recentHeartRates.asStateFlow()

    private val sampleCount = 90
    private val _ecgSamples = MutableStateFlow<List<Float>>(List(sampleCount) { 0f })
    val ecgSamples: StateFlow<List<Float>> = _ecgSamples.asStateFlow()

    private val _sweepIndex = MutableStateFlow(0)
    val sweepIndex: StateFlow<Int> = _sweepIndex.asStateFlow()

    private val _isSimulating = MutableStateFlow(true)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _esp32Url = MutableStateFlow(Esp32Client.DEFAULT_ESP32_URL)
    val esp32Url: StateFlow<String> = _esp32Url.asStateFlow()

    private val _isEsp32Connected = MutableStateFlow(false)
    val isEsp32Connected: StateFlow<Boolean> = _isEsp32Connected.asStateFlow()

    private val _lastPingStatus = MutableStateFlow<String?>(null)
    val lastPingStatus: StateFlow<String?> = _lastPingStatus.asStateFlow()

    private val _pollingIntervalMs = MutableStateFlow(1000L)
    val pollingIntervalMs: StateFlow<Long> = _pollingIntervalMs.asStateFlow()

    private val _tempUnit = MutableStateFlow(TempUnit.CELSIUS)
    val tempUnit: StateFlow<TempUnit> = _tempUnit.asStateFlow()

    private val _lastFallTimestamp = MutableStateFlow<Long?>(null)
    val lastFallTimestamp: StateFlow<Long?> = _lastFallTimestamp.asStateFlow()

    private val _patientName = MutableStateFlow("John Doe")
    val patientName: StateFlow<String> = _patientName.asStateFlow()

    private val _patientId = MutableStateFlow("PT-8092")
    val patientId: StateFlow<String> = _patientId.asStateFlow()

    private var telemetryJob: Job? = null
    private var ecgAnimationJob: Job? = null
    private var ecgPhase = 0f
    private var periodicLogCounter = 0

    init {
        startEcgLoop()
        startTelemetryLoop()
    }

    private fun startEcgLoop() {
        ecgAnimationJob?.cancel()
        ecgAnimationJob = viewModelScope.launch(Dispatchers.Default) {
            val stepDelay = 35L
            while (isActive) {
                val hr = _telemetry.value.heartRate.coerceIn(40, 180)
                // Cycle frequency in Hz = hr / 60. Increment per step:
                val phaseIncrement = (hr / 60.0f) * (stepDelay / 1000.0f)
                ecgPhase = (ecgPhase + phaseIncrement) % 1.0f

                val newSample = EcgWaveGenerator.getSample(ecgPhase)
                val currentList = _ecgSamples.value.toMutableList()
                val idx = _sweepIndex.value % sampleCount
                if (idx < currentList.size) {
                    currentList[idx] = newSample
                }
                _ecgSamples.value = currentList
                _sweepIndex.value = (idx + 1) % sampleCount

                delay(stepDelay)
            }
        }
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                if (_isSimulating.value) {
                    // Simulation mode: realistic bio-variation
                    val currentHr = _telemetry.value.heartRate
                    val deltaHr = Random.nextInt(-2, 3)
                    val nextHr = (currentHr + deltaHr).coerceIn(68, 88)

                    val nextSpo2 = if (Random.nextFloat() < 0.15f) {
                        Random.nextInt(97, 100)
                    } else _telemetry.value.spo2

                    val nextTemp = 36.6f + (Random.nextFloat() * 0.4f)

                    val updated = _telemetry.value.copy(
                        heartRate = nextHr,
                        spo2 = nextSpo2,
                        temperature = nextTemp,
                        batteryLevel = (_telemetry.value.batteryLevel - (if (Random.nextFloat() < 0.02f) 1 else 0)).coerceAtLeast(15),
                        status = "SIMULATION ACTIVE"
                    )
                    _telemetry.value = updated
                    updateHrHistory(nextHr)

                } else {
                    // Real ESP32 HTTP fetch
                    val result = esp32Client.fetchTelemetry(_esp32Url.value)
                    if (result.isSuccess) {
                        val data = result.getOrThrow()
                        _telemetry.value = data
                        _isEsp32Connected.value = true
                        _lastPingStatus.value = "Connected (${data.status})"
                        updateHrHistory(data.heartRate)

                        if (data.fallDetected && !_telemetry.value.fallDetected) {
                            triggerFallAlert(data.fallSeverity)
                        }
                    } else {
                        _isEsp32Connected.value = false
                        _lastPingStatus.value = "ESP32 Error: ${result.exceptionOrNull()?.localizedMessage}"
                    }
                }

                // Periodic auto-log to Room database every 10 cycles (~10-15s)
                periodicLogCounter++
                if (periodicLogCounter >= 10) {
                    periodicLogCounter = 0
                    logCurrentReading()
                }

                delay(_pollingIntervalMs.value)
            }
        }
    }

    private fun updateHrHistory(hr: Int) {
        val list = _recentHeartRates.value.toMutableList()
        list.add(hr)
        if (list.size > 20) list.removeAt(0)
        _recentHeartRates.value = list
    }

    fun setEsp32Config(url: String, intervalMs: Long, enableSimulation: Boolean) {
        _esp32Url.value = url.trim()
        _pollingIntervalMs.value = intervalMs
        _isSimulating.value = enableSimulation
        startTelemetryLoop()
    }

    fun testPing(url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _lastPingStatus.value = "Pinging $url..."
            val result = esp32Client.fetchTelemetry(url)
            if (result.isSuccess) {
                val data = result.getOrThrow()
                _isEsp32Connected.value = true
                _lastPingStatus.value = "Ping Success! HR=${data.heartRate} BPM, SpO2=${data.spo2}%, Battery=${data.batteryLevel}%"
            } else {
                _isEsp32Connected.value = false
                _lastPingStatus.value = "Ping Failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun toggleTempUnit() {
        _tempUnit.value = if (_tempUnit.value == TempUnit.CELSIUS) TempUnit.FAHRENHEIT else TempUnit.CELSIUS
    }

    fun triggerFallAlert(severity: String = "IMPACT") {
        val now = System.currentTimeMillis()
        _lastFallTimestamp.value = now
        _telemetry.value = _telemetry.value.copy(
            fallDetected = true,
            fallSeverity = severity
        )

        // Haptic buzzer feedback for emergency alert
        vibrateEmergency()

        // Immediate log to Room database
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertRecord(
                PatientRecord(
                    patientName = _patientName.value,
                    patientId = _patientId.value,
                    timestamp = now,
                    heartRate = _telemetry.value.heartRate,
                    spo2 = _telemetry.value.spo2,
                    temperature = _telemetry.value.temperature,
                    fallDetected = true,
                    fallSeverity = severity,
                    ecgRhythm = "Emergency Fall Trigger",
                    source = if (_isSimulating.value) "SIMULATION" else "ESP32",
                    notes = "Emergency fall impact detected by wearable accelerometer"
                )
            )
        }
    }

    fun dismissFallAlert() {
        _telemetry.value = _telemetry.value.copy(
            fallDetected = false,
            fallSeverity = "NONE"
        )
    }

    fun simulateFallAlert() {
        triggerFallAlert("SIMULATED_IMPACT")
    }

    private fun logCurrentReading() {
        val t = _telemetry.value
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertRecord(
                PatientRecord(
                    patientName = _patientName.value,
                    patientId = _patientId.value,
                    timestamp = System.currentTimeMillis(),
                    heartRate = t.heartRate,
                    spo2 = t.spo2,
                    temperature = t.temperature,
                    fallDetected = t.fallDetected,
                    fallSeverity = t.fallSeverity,
                    ecgRhythm = "Normal Sinus Rhythm",
                    source = if (_isSimulating.value) "SIMULATION" else "ESP32",
                    notes = "Routine telemetry check"
                )
            )
        }
    }

    fun importCsvData(csvText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val parsed = repository.parseCsv(csvText, _patientName.value, _patientId.value)
            if (parsed.isNotEmpty()) {
                repository.insertRecords(parsed)
            }
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
        }
    }

    private fun vibrateEmergency() {
        try {
            val context = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 300, 150, 300, 150, 400)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(600)
            }
        } catch (_: Exception) {
            // Ignore if vibration unavailable
        }
    }
}
