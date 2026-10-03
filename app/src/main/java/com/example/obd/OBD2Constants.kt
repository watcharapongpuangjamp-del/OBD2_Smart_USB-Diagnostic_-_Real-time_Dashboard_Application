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

sealed class OBD2Command(val command: String, val name: String, val unit: String = "") {
    // Mode 01 - Live Data
    object EngineRPM : OBD2Command("010C", "Engine RPM", "rpm")
    object VehicleSpeed : OBD2Command("010D", "Vehicle Speed", "km/h")
    object CoolantTemp : OBD2Command("0105", "Coolant Temp", "°C")
    object EngineLoad : OBD2Command("0104", "Engine Load", "%")
    
    // AT Commands
    object BatteryVoltage : OBD2Command("AT RV", "Battery Voltage", "V")
    object ProtocolName : OBD2Command("AT DP", "Current Protocol")
    object ProtocolNumber : OBD2Command("AT DPN", "Protocol Number")
    
    // Reset Commands
    object Reset : OBD2Command("AT Z", "Adapter Reset")
    object WarmStart : OBD2Command("AT WS", "Warm Start")
    object ClearDTCs : OBD2Command("04", "Clear Trouble Codes")
    
    // Mode 03 / 07 - DTCs
    object StoredDTCs : OBD2Command("03", "Stored Trouble Codes")
    object PendingDTCs : OBD2Command("07", "Pending Trouble Codes")
}
