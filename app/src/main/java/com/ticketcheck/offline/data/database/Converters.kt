package com.ticketcheck.offline.data.database

import androidx.room.TypeConverter
import com.ticketcheck.offline.data.entities.ScanResult
import com.ticketcheck.offline.data.entities.TicketStatus

class Converters {
    @TypeConverter
    fun fromTicketStatus(value: TicketStatus): String = value.name

    @TypeConverter
    fun toTicketStatus(value: String): TicketStatus = TicketStatus.valueOf(value)

    @TypeConverter
    fun fromScanResult(value: ScanResult): String = value.name

    @TypeConverter
    fun toScanResult(value: String): ScanResult = ScanResult.valueOf(value)
}
