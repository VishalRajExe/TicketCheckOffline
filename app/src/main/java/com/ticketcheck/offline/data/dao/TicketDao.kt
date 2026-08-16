package com.ticketcheck.offline.data.dao

import androidx.room.*
import com.ticketcheck.offline.data.entities.TicketEntity
import com.ticketcheck.offline.data.entities.TicketStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {

    @Query("SELECT * FROM tickets ORDER BY ticketCode ASC")
    fun observeAll(): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets ORDER BY ticketCode ASC")
    suspend fun getAllOnce(): List<TicketEntity>

    @Query("SELECT * FROM tickets WHERE ticketCode = :code LIMIT 1")
    suspend fun findByCode(code: String): TicketEntity?

    @Query("SELECT * FROM tickets WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): TicketEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(ticket: TicketEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tickets: List<TicketEntity>): List<Long>

    @Update
    suspend fun update(ticket: TicketEntity)

    @Delete
    suspend fun delete(ticket: TicketEntity)

    @Query("DELETE FROM tickets")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM tickets")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM tickets WHERE status = :status")
    suspend fun countByStatus(status: TicketStatus): Int

    /**
     * Atomically marks a ticket USED only if it is currently VALID.
     * Returns the number of rows updated (0 or 1).
     * This prevents a ticket from being accepted twice due to rapid
     * duplicate scanner callbacks - the check and the write happen
     * as a single database statement instead of separate read+write steps.
     */
    @Query(
        """
        UPDATE tickets
        SET status = 'USED', usedAt = :usedAt, scanCount = scanCount + 1, lastScanTime = :usedAt
        WHERE ticketCode = :code AND status = 'VALID'
        """
    )
    suspend fun markUsedIfValid(code: String, usedAt: Long): Int

    @Query(
        """
        UPDATE tickets
        SET scanCount = scanCount + 1, lastScanTime = :time
        WHERE ticketCode = :code
        """
    )
    suspend fun recordDuplicateScan(code: String, time: Long)

    @Query("UPDATE tickets SET status = 'VALID', usedAt = NULL WHERE status = 'USED'")
    suspend fun resetAllUsed()
}
