package com.example.data

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class TelemetryLogger(context: Context) {
    private val logDir = File(context.filesDir, "logs")
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    init {
        if (!logDir.exists()) logDir.mkdirs()
    }

    fun createLogFile(): File {
        val fileName = "telemetry_${dateFormat.format(Date())}.csv"
        val file = File(logDir, fileName)
        FileWriter(file).use {
            it.append("timestamp,rpm,speed,coolant_temp,engine_load,voltage\n")
        }
        return file
    }

    fun logData(file: File, data: Map<String, String>) {
        FileWriter(file, true).use {
            val timestamp = System.currentTimeMillis()
            it.append("${timestamp},${data["rpm"]},${data["speed"]},${data["coolant"]},${data["load"]},${data["voltage"]}\n")
        }
    }
}
