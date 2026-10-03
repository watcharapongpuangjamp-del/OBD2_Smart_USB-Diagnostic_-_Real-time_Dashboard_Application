package com.example.data

class PerformanceManager {
    private var startTime: Long = 0L
    private var isTesting = false
    private var result: Double = 0.0

    fun startTest() {
        startTime = 0L
        isTesting = true
        result = 0.0
    }

    fun update(speed: Int): Double? {
        if (!isTesting) return null
        
        if (speed > 0 && startTime == 0L) {
            startTime = System.currentTimeMillis()
        }
        
        if (speed >= 100 && startTime != 0L) {
            val endTime = System.currentTimeMillis()
            isTesting = false
            result = (endTime - startTime) / 1000.0
            return result
        }
        return null
    }
}
