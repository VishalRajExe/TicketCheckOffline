package com.ticketcheck.offline.data.dao

import androidx.room.Insert
import androidx.room.Dao
import androidx.room.Query
import com.ticketcheck.offline.data.entities.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    suspend fun getAll(): List<ScanHistoryEntity>

    @Insert
    suspend fun insert(entry: ScanHistoryEntity): Long

    @Query("DELETE FROM scan_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM scan_history WHERE result = 'VALID'")
    suspend fun countValid(): Int

    @Query("SELECT COUNT(*) FROM scan_history WHERE result = 'ALREADY_USED'")
    suspend fun countAlreadyUsed(): Int

    @Query("SELECT COUNT(*) FROM scan_history WHERE result = 'INVALID'")
    suspend fun countInvalid(): Int
}
