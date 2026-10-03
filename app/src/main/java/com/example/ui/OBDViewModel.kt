package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UsbSerialManager
import com.example.obd.OBD2Command
import com.example.obd.OBD2Parser
import com.example.obd.OBD2Protocol
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val commandHistory: List<String> = emptyList()
)

class OBDViewModel(application: Application) : AndroidViewModel(application) {
    private val usbSerialManager = UsbSerialManager(application)
    
    private val _uiState = MutableStateFlow(OBDUiState())
    val uiState: StateFlow<OBDUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

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
                
                // Initialize ELM327
                runOBDCommand(OBD2Command.Reset)
                delay(500)
                runCommand("ATE0", "Echo Off")
                runCommand("ATL0", "Linefeeds Off")
                
                // Set Protocol
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
                startPolling()
            } else {
                _uiState.update { it.copy(connectionStatus = "Device not found or Permission denied") }
            }
        }
    }

    fun disconnect() {
        pollingJob?.cancel()
        usbSerialManager.disconnect()
        _uiState.update { it.copy(isConnected = false, connectionStatus = "Disconnected") }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                if (!usbSerialManager.isConnected()) {
                    disconnect()
                    break
                }

                val rpmRaw = runOBDCommand(OBD2Command.EngineRPM)
                val speedRaw = runOBDCommand(OBD2Command.VehicleSpeed)
                val tempRaw = runOBDCommand(OBD2Command.CoolantTemp)
                val loadRaw = runOBDCommand(OBD2Command.EngineLoad)
                val voltRaw = runOBDCommand(OBD2Command.BatteryVoltage)

                _uiState.update { 
                    it.copy(
                        rpm = OBD2Parser.parseRPM(rpmRaw),
                        speed = OBD2Parser.parseSpeed(speedRaw),
                        coolantTemp = OBD2Parser.parseCoolantTemp(tempRaw),
                        engineLoad = OBD2Parser.parseEngineLoad(loadRaw),
                        voltage = voltRaw
                    )
                }
                delay(200) // Poll every 200ms
            }
        }
    }

    fun scanDTCs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val stored = runOBDCommand(OBD2Command.StoredDTCs)
            val pending = runOBDCommand(OBD2Command.PendingDTCs)
            
            val dtcs = (OBD2Parser.parseDTCs(stored) + OBD2Parser.parseDTCs(pending)).distinct()
            _uiState.update { it.copy(dtcs = dtcs, isScanning = false) }
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
}
