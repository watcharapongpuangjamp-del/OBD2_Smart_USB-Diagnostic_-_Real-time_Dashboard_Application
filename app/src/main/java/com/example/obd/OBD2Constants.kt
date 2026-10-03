package com.example.obd

enum class OBD2Protocol(val command: String, val description: String) {
    AUTO("AT SP 0", "Automatic Detection"),
    SAE_J1850_PWM("AT SP 1", "SAE J1850 PWM (41.6 kbaud)"),
    SAE_J1850_VPW("AT SP 2", "SAE J1850 VPW (10.4 kbaud)"),
    ISO_9141_2("AT SP 3", "ISO 9141-2 (5 baud init, 10.4 kbaud)"),
    ISO_14230_4_KWP_5BAUD("AT SP 4", "ISO 14230-4 KWP (5 baud init, 10.4 kbaud)"),
    ISO_14230_4_KWP_FAST("AT SP 5", "ISO 14230-4 KWP (fast init, 10.4 kbaud)"),
    ISO_15765_4_CAN_11_500("AT SP 6", "ISO 15765-4 CAN (11 bit ID, 500 kbaud)"),
    ISO_15765_4_CAN_29_500("AT SP 7", "ISO 15765-4 CAN (29 bit ID, 500 kbaud)"),
    ISO_15765_4_CAN_11_250("AT SP 8", "ISO 15765-4 CAN (11 bit ID, 250 kbaud)"),
    ISO_15765_4_CAN_29_250("AT SP 9", "ISO 15765-4 CAN (29 bit ID, 250 kbaud)"),
    SAE_J1939_CAN("AT SP A", "SAE J1939 CAN (29 bit ID, 250* kbaud)")
}

sealed class OBD2Command(val command: String, val name: String, val unit: String = "", val priority: CommandPriority = CommandPriority.LOW) {
    enum class CommandPriority { HIGH, MEDIUM, LOW }

    // Mode 01 - Live Data
    object EngineRPM : OBD2Command("010C", "Engine RPM", "rpm", CommandPriority.HIGH)
    object VehicleSpeed : OBD2Command("010D", "Vehicle Speed", "km/h", CommandPriority.HIGH)
    object CoolantTemp : OBD2Command("0105", "Coolant Temp", "°C", CommandPriority.LOW)
    object EngineLoad : OBD2Command("0104", "Engine Load", "%", CommandPriority.MEDIUM)
    
    // AT Commands
    object BatteryVoltage : OBD2Command("AT RV", "Battery Voltage", "V", CommandPriority.LOW)
    object ProtocolName : OBD2Command("AT DP", "Current Protocol", "", CommandPriority.LOW)
    object ProtocolNumber : OBD2Command("AT DPN", "Protocol Number", "", CommandPriority.LOW)
    
    // Reset Commands
    object Reset : OBD2Command("AT Z", "Adapter Reset", "", CommandPriority.LOW)
    object WarmStart : OBD2Command("AT WS", "Warm Start", "", CommandPriority.LOW)
    object ClearDTCs : OBD2Command("04", "Clear Trouble Codes", "", CommandPriority.LOW)
    
    // Mode 03 / 07 - DTCs
    object StoredDTCs : OBD2Command("03", "Stored Trouble Codes", "", CommandPriority.LOW)
    object PendingDTCs : OBD2Command("07", "Pending Trouble Codes", "", CommandPriority.LOW)
    
    // Mode 08 - Request Control of On-Board Systems
    object TestECU : OBD2Command("0801", "ECU System Test", "", CommandPriority.LOW)
    
    // Mode 09 - Request Vehicle Info
    object VIN : OBD2Command("0902", "Get VIN", "", CommandPriority.LOW)
    object CalibrationID : OBD2Command("0904", "Calibration ID", "", CommandPriority.LOW)
    
    // Proprietary / Reset Commands (Careful!)
    object ThrottleReset : OBD2Command("31", "Throttle Body Reset", "", CommandPriority.LOW)
    object InjectorAdaptation : OBD2Command("32", "Injector Adaptation", "", CommandPriority.LOW)
}
