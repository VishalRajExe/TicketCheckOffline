package com.ticketcheck.offline.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ScanResult { VALID, ALREADY_USED, INVALID }

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scannedValue: String,
    val result: ScanResult,
    val ticketId: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)
