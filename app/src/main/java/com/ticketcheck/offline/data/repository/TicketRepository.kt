package com.ticketcheck.offline.data.repository

import com.ticketcheck.offline.data.dao.EventDao
import com.ticketcheck.offline.data.dao.ScanHistoryDao
import com.ticketcheck.offline.data.dao.TicketDao
import com.ticketcheck.offline.data.entities.EventEntity
import com.ticketcheck.offline.data.entities.ScanHistoryEntity
import com.ticketcheck.offline.data.entities.ScanResult
import com.ticketcheck.offline.data.entities.TicketEntity
import com.ticketcheck.offline.data.entities.TicketStatus
import com.ticketcheck.offline.domain.models.DashboardStats
import com.ticketcheck.offline.domain.models.ScanOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single access point for all ticket / event / scan-history data.
 * Everything here reads and writes only the local Room database -
 * there is no network call anywhere in this class.
 */
class TicketRepository(
    private val ticketDao: TicketDao,
    private val eventDao: EventDao,
    private val scanHistoryDao: ScanHistoryDao
) {
    // Serializes scan processing so two near-simultaneous camera callbacks
    // for the same code can never both "win" the USED transition.
    private val scanMutex = Mutex()

    fun observeTickets(): Flow<List<TicketEntity>> = ticketDao.observeAll()
    fun observeScanHistory(): Flow<List<ScanHistoryEntity>> = scanHistoryDao.observeAll()
    fun observeCurrentEvent(): Flow<EventEntity?> = eventDao.observeCurrentEvent()

    suspend fun getCurrentEvent(): EventEntity? = eventDao.getCurrentEvent()

    suspend fun createOrUpdateEvent(event: EventEntity) {
        eventDao.insert(event)
    }

    suspend fun ticketExists(code: String): Boolean = ticketDao.findByCode(code) != null

    suspend fun addTicket(ticket: TicketEntity): Result<Unit> {
        return try {
            if (ticketExists(ticket.ticketCode)) {
                Result.failure(IllegalStateException("Ticket already exists."))
            } else {
                ticketDao.insert(ticket)
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTicket(ticket: TicketEntity) {
        ticketDao.update(ticket)
    }

    suspend fun deleteTicket(ticket: TicketEntity) {
        ticketDao.delete(ticket)
    }

    data class ImportSummary(val imported: Int, val duplicates: Int, val invalidRows: Int)

    suspend fun importTickets(candidates: List<TicketEntity>): ImportSummary {
        var imported = 0
        var duplicates = 0
        for (t in candidates) {
            if (t.ticketCode.isBlank()) continue
            if (ticketExists(t.ticketCode)) {
                duplicates++
            } else {
                try {
                    ticketDao.insert(t)
                    imported++
                } catch (e: Exception) {
                    duplicates++
                }
            }
        }
        return ImportSummary(imported = imported, duplicates = duplicates, invalidRows = 0)
    }

    suspend fun generateSequentialTickets(prefix: String, start: Int, count: Int, padding: Int) {
        val tickets = (start until start + count).map { n ->
            val code = "$prefix${n.toString().padStart(padding, '0')}"
            TicketEntity(ticketCode = code, qrContent = code)
        }
        ticketDao.insertAll(tickets)
    }

    suspend fun getAllTicketsOnce(): List<TicketEntity> = ticketDao.getAllOnce()

    /**
     * Core verification logic described in the spec:
     *
     *   ticket = findTicket(code)
     *   if null -> INVALID
     *   if USED -> ALREADY_USED
     *   else -> mark USED, record VALID
     *
     * The VALID branch uses a single conditional UPDATE statement
     * (markUsedIfValid) so the "check then mark used" step is atomic -
     * two rapid duplicate camera callbacks for the same code cannot
     * both succeed.
     */
    suspend fun processScan(rawCode: String): ScanOutcome {
        val code = rawCode.trim()
        return scanMutex.withLock {
            val existing = ticketDao.findByCode(code)
            if (existing == null) {
                recordHistory(code, ScanResult.INVALID, null)
                return@withLock ScanOutcome.Invalid(code)
            }

            if (existing.status == TicketStatus.USED) {
                ticketDao.recordDuplicateScan(code, System.currentTimeMillis())
                recordHistory(code, ScanResult.ALREADY_USED, existing.id)
                return@withLock ScanOutcome.AlreadyUsed(existing)
            }

            val now = System.currentTimeMillis()
            val rowsUpdated = ticketDao.markUsedIfValid(code, now)
            return@withLock if (rowsUpdated == 1) {
                val updated = ticketDao.findByCode(code)!!
                recordHistory(code, ScanResult.VALID, updated.id)
                ScanOutcome.Valid(updated)
            } else {
                // Lost the race to another simultaneous callback - treat as already used.
                val current = ticketDao.findByCode(code)!!
                recordHistory(code, ScanResult.ALREADY_USED, current.id)
                ScanOutcome.AlreadyUsed(current)
            }
        }
    }

    private suspend fun recordHistory(code: String, result: ScanResult, ticketId: Long?) {
        scanHistoryDao.insert(ScanHistoryEntity(scannedValue = code, result = result, ticketId = ticketId))
    }

    suspend fun getDashboardStats(): DashboardStats {
        val total = ticketDao.count()
        val used = ticketDao.countByStatus(TicketStatus.USED)
        return DashboardStats(
            totalTickets = total,
            usedTickets = used,
            remainingTickets = total - used,
            validScans = scanHistoryDao.countValid(),
            alreadyUsedScans = scanHistoryDao.countAlreadyUsed(),
            invalidScans = scanHistoryDao.countInvalid()
        )
    }

    suspend fun resetAllUsedStatus() {
        ticketDao.resetAllUsed()
    }

    suspend fun clearScanHistory() {
        scanHistoryDao.deleteAll()
    }

    suspend fun clearEverything() {
        ticketDao.deleteAll()
        scanHistoryDao.deleteAll()
        eventDao.deleteAll()
    }

    fun dao() = ticketDao
    fun historyDao() = scanHistoryDao
    fun eventDaoRef() = eventDao
}
