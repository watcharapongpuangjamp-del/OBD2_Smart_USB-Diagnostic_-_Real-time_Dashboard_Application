package com.example.obd

object OBD2Parser {
    fun parseRPM(hex: String): Int {
        // Response format: 41 0C AA BB
        val bytes = hex.split(" ").filter { it.length == 2 }
        if (bytes.size >= 4 && bytes[0] == "41" && bytes[1] == "0C") {
            val a = bytes[2].toInt(16)
            val b = bytes[3].toInt(16)
            return ((a * 256) + b) / 4
        }
        return 0
    }

    fun parseSpeed(hex: String): Int {
        // Response format: 41 0D AA
        val bytes = hex.split(" ").filter { it.length == 2 }
        if (bytes.size >= 3 && bytes[0] == "41" && bytes[1] == "0D") {
            return bytes[2].toInt(16)
        }
        return 0
    }

    fun parseCoolantTemp(hex: String): Int {
        // Response format: 41 05 AA
        val bytes = hex.split(" ").filter { it.length == 2 }
        if (bytes.size >= 3 && bytes[0] == "41" && bytes[1] == "05") {
            return bytes[2].toInt(16) - 40
        }
        return 0
    }

    fun parseEngineLoad(hex: String): Float {
        // Response format: 41 04 AA
        val bytes = hex.split(" ").filter { it.length == 2 }
        if (bytes.size >= 3 && bytes[0] == "41" && bytes[1] == "04") {
            val a = bytes[2].toInt(16)
            return (a * 100f) / 255f
        }
        return 0f
    }

    fun parseDTCs(hex: String): List<String> {
        // Response format: 43 01 07 01 00 ... (example)
        // 43 is response to 03
        val dtcs = mutableListOf<String>()
        val bytes = hex.replace(" ", "").chunked(2)
        if (bytes.isEmpty() || (bytes[0] != "43" && bytes[0] != "47")) return emptyList()

        // Skip first byte (response mode)
        for (i in 1 until bytes.size step 2) {
            if (i + 1 >= bytes.size) break
            val byte1 = bytes[i]
            val byte2 = bytes[i + 1]
            
            if (byte1 == "00" && byte2 == "00") continue
            
            val dtc = decodeDtc(byte1, byte2)
            if (dtc.isNotEmpty()) dtcs.add(dtc)
        }
        return dtcs.distinct()
    }

    private fun decodeDtc(byte1: String, byte2: String): String {
        val b1 = byte1.toInt(16)
        val firstChar = when (b1 shr 6) {
            0 -> "P"
            1 -> "C"
            2 -> "B"
            3 -> "U"
            else -> ""
        }
        val secondChar = (b1 shr 4 and 0x03).toString()
        val thirdChar = (b1 and 0x0F).toString(16).uppercase()
        val fourthFifth = byte2.uppercase()
        return "$firstChar$secondChar$thirdChar$fourthFifth"
    }
}
