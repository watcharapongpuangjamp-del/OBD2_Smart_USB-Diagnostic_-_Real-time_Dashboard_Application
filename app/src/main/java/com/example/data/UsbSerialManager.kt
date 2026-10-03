package com.example.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException

class UsbSerialManager(private val context: Context) {
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var serialPort: UsbSerialPort? = null
    private val ACTION_USB_PERMISSION = "com.example.USB_PERMISSION"

    suspend fun findAndConnect(): Boolean = withContext(Dispatchers.IO) {
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (drivers.isEmpty()) return@withContext false

        val driver = drivers.first()
        val device = driver.device
        
        if (!usbManager.hasPermission(device)) {
            requestPermission(device)
            return@withContext false
        }

        val connection = usbManager.openDevice(device) ?: return@withContext false
        
        try {
            val port = driver.ports.first()
            port.open(connection)
            port.setParameters(38400, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            serialPort = port
            return@withContext true
        } catch (e: IOException) {
            Log.e("UsbSerialManager", "Error opening port", e)
            return@withContext false
        }
    }

    private fun requestPermission(device: UsbDevice) {
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0, Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE
        )
        usbManager.requestPermission(device, permissionIntent)
    }

    suspend fun sendCommand(command: String): String = withContext(Dispatchers.IO) {
        val port = serialPort ?: return@withContext "ERROR: NO PORT"
        try {
            val data = (command + "\r").toByteArray()
            port.write(data, 1000)
            
            val buffer = ByteArray(1024)
            var result = ""
            var retries = 0
            while (retries < 10) {
                val len = port.read(buffer, 500)
                if (len > 0) {
                    val chunk = String(buffer, 0, len)
                    result += chunk
                    if (result.contains(">")) break
                }
                delay(50)
                retries++
            }
            return@withContext result.trim().replace(">", "").trim()
        } catch (e: IOException) {
            Log.e("UsbSerialManager", "Write/Read error", e)
            return@withContext "ERROR: ${e.message}"
        }
    }

    fun disconnect() {
        try {
            serialPort?.close()
        } catch (e: IOException) {
            Log.e("UsbSerialManager", "Error closing port", e)
        }
        serialPort = null
    }

    fun isConnected(): Boolean = serialPort != null && serialPort!!.isOpen
}
