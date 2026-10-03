package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UsbSerialManager
import com.example.data.AiRepository
import com.example.data.OBDRepository
import com.example.data.PerformanceManager
import com.example.data.PollingScheduler
import com.example.data.TelemetryLogger
import com.example.data.db.AppDatabase
import com.example.data.db.ScanResult
import com.example.obd.OBD2Command
import com.example.obd.OBD2Parser
import com.example.obd.OBD2Protocol
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OBDUiState(
    val isConnected: Boolean = false,
    val connectionStatus: String = "Disconnected",
    val rpm: Int = 0,
    val speed: Int = 0,
    val coolantTemp: Int = 0,
    val engineLoad: Float = 0f,
    val voltage: String = "0.0V",
    val currentProtocol: String = "None",
    val dtcs: List<String> = emptyList(),
    val isScanning: Boolean = false,
    val commandHistory: List<String> = emptyList(),
    val scanHistory: List<ScanResult> = emptyList(),
    val aiAdvice: Map<String, String> = emptyMap(),
    val isAnalyzingAi: Boolean = false,
    val accelerationTime: Double = 0.0,
    val isLogging: Boolean = false
)

class OBDViewModel(application: Application) : AndroidViewModel(application) {
    private val usbSerialManager = UsbSerialManager(application)
    private val repository = OBDRepository(AppDatabase.getDatabase(application).scanResultDao())
    private val aiRepository = AiRepository()
    private val telemetryLogger = TelemetryLogger(application)
    private val performanceManager = PerformanceManager()
    private val scheduler = PollingScheduler { command -> runOBDCommand(command) }
    
    private var logFile: java.io.File? = null
    
    private val _uiState = MutableStateFlow(OBDUiState())
    val uiState: StateFlow<OBDUiState> = combine(_uiState, repository.allScanResults) { state, history ->
        state.copy(scanHistory = history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OBDUiState())

    override fun onCleared() {
        super.onCleared()
        scheduler.stop()
    }

    private suspend fun runCommand(command: String, name: String = ""): String {
        val response = usbSerialManager.sendCommand(command)
        val logEntry = ">> $command ${if (name.isNotEmpty()) "($name)" else ""}\n<< $response"
        _uiState.update { state ->
            val newHistory = (state.commandHistory + logEntry).takeLast(50)
            state.copy(commandHistory = newHistory)
        }
        return response
    }

    private suspend fun runOBDCommand(command: OBD2Command): String {
        return runCommand(command.command, command.name)
    }

    fun connect(protocol: OBD2Protocol = OBD2Protocol.AUTO) {
        viewModelScope.launch {
            _uiState.update { it.copy(connectionStatus = "Searching...", commandHistory = emptyList()) }
            if (usbSerialManager.findAndConnect()) {
                _uiState.update { it.copy(connectionStatus = "Initializing ELM327...") }
                
                runOBDCommand(OBD2Command.Reset)
                delay(500)
                runCommand("ATE0", "Echo Off")
                runCommand("ATL0", "Linefeeds Off")
                
                _uiState.update { it.copy(connectionStatus = "Setting Protocol: ${protocol.description}...") }
                runCommand(protocol.command, "Set Protocol")
                
                val protocolResponse = runOBDCommand(OBD2Command.ProtocolName)
                
                _uiState.update { 
                    it.copy(
                        isConnected = true, 
                        connectionStatus = "Connected",
                        currentProtocol = protocolResponse
                    ) 
                }
                
                scheduler.addTasks(listOf(
                    OBD2Command.EngineRPM,
                    OBD2Command.VehicleSpeed,
                    OBD2Command.CoolantTemp,
                    OBD2Command.EngineLoad,
                    OBD2Command.BatteryVoltage
                ))
                scheduler.start(viewModelScope) { command, response ->
                    _uiState.update { state ->
                        val newState = when (command) {
                            OBD2Command.EngineRPM -> state.copy(rpm = OBD2Parser.parseRPM(response))
                            OBD2Command.VehicleSpeed -> {
                                val speed = OBD2Parser.parseSpeed(response)
                                val accel = performanceManager.update(speed)
                                if (accel != null) state.copy(speed = speed, accelerationTime = accel)
                                else state.copy(speed = speed)
                            }
                            OBD2Command.CoolantTemp -> state.copy(coolantTemp = OBD2Parser.parseCoolantTemp(response))
                            OBD2Command.EngineLoad -> state.copy(engineLoad = OBD2Parser.parseEngineLoad(response))
                            OBD2Command.BatteryVoltage -> state.copy(voltage = response)
                            else -> state
                        }
                        
                        if (_uiState.value.isLogging && logFile != null) {
                            telemetryLogger.logData(logFile!!, mapOf(
                                "rpm" to newState.rpm.toString(),
                                "speed" to newState.speed.toString(),
                                "coolant" to newState.coolantTemp.toString(),
                                "load" to newState.engineLoad.toString(),
                                "voltage" to newState.voltage
                            ))
                        }
                        newState
                    }
                }
            } else {
                _uiState.update { it.copy(connectionStatus = "Device not found or Permission denied") }
            }
        }
    }

    fun disconnect() {
        scheduler.stop()
        usbSerialManager.disconnect()
        _uiState.update { it.copy(isConnected = false, connectionStatus = "Disconnected") }
    }

    fun scanDTCs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val stored = runOBDCommand(OBD2Command.StoredDTCs)
            val pending = runOBDCommand(OBD2Command.PendingDTCs)
            
            val dtcs = (OBD2Parser.parseDTCs(stored) + OBD2Parser.parseDTCs(pending)).distinct()
            _uiState.update { it.copy(dtcs = dtcs, isScanning = false) }
            
            // Save to database
            if (dtcs.isNotEmpty()) {
                repository.saveScanResult(_uiState.value.currentProtocol, dtcs)
            }
        }
    }

    fun clearDTCs() {
        viewModelScope.launch {
            runOBDCommand(OBD2Command.ClearDTCs)
            scanDTCs()
        }
    }

    fun resetAdapter() {
        viewModelScope.launch {
            runOBDCommand(OBD2Command.Reset)
            delay(1000)
            connect()
        }
    }

    fun clearHistory() {
        _uiState.update { it.copy(commandHistory = emptyList()) }
    }

    fun deleteScan(id: Int) {
        viewModelScope.launch {
            repository.deleteScan(id)
        }
    }

    fun clearScanHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun getAiAdvice(dtc: String) {
        if (_uiState.value.aiAdvice.containsKey(dtc)) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingAi = true) }
            val advice = aiRepository.analyzeDtc(dtc)
            _uiState.update { state ->
                state.copy(
                    aiAdvice = state.aiAdvice + (dtc to advice),
                    isAnalyzingAi = false
                )
            }
        }
    }

    fun startPerformanceTest() {
        performanceManager.startTest()
    }

    fun toggleLogging() {
        if (_uiState.value.isLogging) {
            logFile = null
            _uiState.update { it.copy(isLogging = false) }
        } else {
            logFile = telemetryLogger.createLogFile()
            _uiState.update { it.copy(isLogging = true) }
        }
    }

    fun performService(command: OBD2Command) {
        viewModelScope.launch {
            runOBDCommand(command)
        }
    }
}
