package com.ticketcheck.offline.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TicketStatus { VALID, USED }

@Entity(
    tableName = "tickets",
    indices = [Index(value = ["ticketCode"], unique = true)]
)
data class TicketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticketCode: String,
    val customerName: String? = null,
    val ticketType: String? = null,
    val price: Double? = null,
    val qrContent: String,
    val status: TicketStatus = TicketStatus.VALID,
    val createdAt: Long = System.currentTimeMillis(),
    val usedAt: Long? = null,
    val scanCount: Int = 0,
    val lastScanTime: Long? = null
)
