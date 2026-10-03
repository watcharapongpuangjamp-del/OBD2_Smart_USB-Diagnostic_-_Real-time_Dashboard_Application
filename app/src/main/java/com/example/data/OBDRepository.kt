package com.example.data

import com.example.data.db.ScanResult
import com.example.data.db.ScanResultDao
import kotlinx.coroutines.flow.Flow

class OBDRepository(private val scanResultDao: ScanResultDao) {
    val allScanResults: Flow<List<ScanResult>> = scanResultDao.getAllScanResults()

    suspend fun saveScanResult(protocol: String, dtcs: List<String>) {
        if (dtcs.isNotEmpty()) {
            val result = ScanResult(
                protocol = protocol,
                dtcs = dtcs.joinToString(",")
            )
            scanResultDao.insertScanResult(result)
        }
    }

    suspend fun clearHistory() {
        scanResultDao.deleteAllResults()
    }

    suspend fun deleteScan(id: Int) {
        scanResultDao.deleteResultById(id)
    }
}
