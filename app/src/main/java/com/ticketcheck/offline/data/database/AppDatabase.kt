package com.ticketcheck.offline.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ticketcheck.offline.data.dao.EventDao
import com.ticketcheck.offline.data.dao.ScanHistoryDao
import com.ticketcheck.offline.data.dao.TicketDao
import com.ticketcheck.offline.data.entities.EventEntity
import com.ticketcheck.offline.data.entities.ScanHistoryEntity
import com.ticketcheck.offline.data.entities.TicketEntity

/**
 * 100% local Room database. No network, no cloud sync, no backend of any kind.
 * This file on the scanning phone's storage is the single source of truth.
 */
@Database(
    entities = [TicketEntity::class, EventEntity::class, ScanHistoryEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ticketDao(): TicketDao
    abstract fun eventDao(): EventDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ticketcheck.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
