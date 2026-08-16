package com.ticketcheck.offline

import com.ticketcheck.offline.data.dao.EventDao
import com.ticketcheck.offline.data.dao.ScanHistoryDao
import com.ticketcheck.offline.data.dao.TicketDao
import com.ticketcheck.offline.data.entities.EventEntity
import com.ticketcheck.offline.data.entities.ScanHistoryEntity
import com.ticketcheck.offline.data.entities.TicketEntity
import com.ticketcheck.offline.data.entities.TicketStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow

/**
 * In-memory fakes so the repository's scan logic can be unit tested on
 * the JVM without a real Room/SQLite/Android dependency.
 */
class FakeTicketDao : TicketDao {
    private val tickets = linkedMapOf<String, TicketEntity>()
    private var nextId = 1L
    private val flow = MutableStateFlow<List<TicketEntity>>(emptyList())

    private fun publish() { flow.value = tickets.values.toList() }

    override fun observeAll(): Flow<List<TicketEntity>> = flow
    override suspend fun getAllOnce(): List<TicketEntity> = tickets.values.toList()
    override suspend fun findByCode(code: String): TicketEntity? = tickets[code]
    override suspend fun findById(id: Long): TicketEntity? = tickets.values.firstOrNull { it.id == id }

    override suspend fun insert(ticket: TicketEntity): Long {
        if (tickets.containsKey(ticket.ticketCode)) throw IllegalStateException("duplicate")
        val withId = ticket.copy(id = nextId++)
        tickets[ticket.ticketCode] = withId
        publish()
        return withId.id
    }

    override suspend fun insertAll(tickets_: List<TicketEntity>): List<Long> {
        val ids = mutableListOf<Long>()
        for (t in tickets_) {
            if (!tickets.containsKey(t.ticketCode)) {
                val withId = t.copy(id = nextId++)
                tickets[t.ticketCode] = withId
                ids.add(withId.id)
            }
        }
        publish()
        return ids
    }

    override suspend fun update(ticket: TicketEntity) {
        tickets[ticket.ticketCode] = ticket
        publish()
    }

    override suspend fun delete(ticket: TicketEntity) {
        tickets.remove(ticket.ticketCode)
        publish()
    }

    override suspend fun deleteAll() { tickets.clear(); publish() }
    override suspend fun count(): Int = tickets.size
    override suspend fun countByStatus(status: TicketStatus): Int = tickets.values.count { it.status == status }

    override suspend fun markUsedIfValid(code: String, usedAt: Long): Int {
        val existing = tickets[code] ?: return 0
        if (existing.status != TicketStatus.VALID) return 0
        tickets[code] = existing.copy(status = TicketStatus.USED, usedAt = usedAt, scanCount = existing.scanCount + 1, lastScanTime = usedAt)
        publish()
        return 1
    }

    override suspend fun recordDuplicateScan(code: String, time: Long) {
        val existing = tickets[code] ?: return
        tickets[code] = existing.copy(scanCount = existing.scanCount + 1, lastScanTime = time)
        publish()
    }

    override suspend fun resetAllUsed() {
        tickets.replaceAll { _, t -> if (t.status == TicketStatus.USED) t.copy(status = TicketStatus.VALID, usedAt = null) else t }
        publish()
    }
}

class FakeEventDao : EventDao {
    private var current: EventEntity? = null
    private val flow = MutableStateFlow<EventEntity?>(null)
    override fun observeCurrentEvent(): Flow<EventEntity?> = flow
    override suspend fun getCurrentEvent(): EventEntity? = current
    override suspend fun insert(event: EventEntity): Long { current = event; flow.value = event; return 1 }
    override suspend fun update(event: EventEntity) { current = event; flow.value = event }
    override suspend fun deleteAll() { current = null; flow.value = null }
}

class FakeScanHistoryDao : ScanHistoryDao {
    val entries = mutableListOf<ScanHistoryEntity>()
    private var nextId = 1L
    private val flow = MutableStateFlow<List<ScanHistoryEntity>>(emptyList())
    override fun observeAll(): Flow<List<ScanHistoryEntity>> = flow
    override suspend fun getAll(): List<ScanHistoryEntity> = entries.toList()
    override suspend fun insert(entry: ScanHistoryEntity): Long {
        val withId = entry.copy(id = nextId++)
        entries.add(withId)
        flow.value = entries.toList()
        return withId.id
    }
    override suspend fun deleteAll() { entries.clear(); flow.value = emptyList() }
    override suspend fun countValid(): Int = entries.count { it.result.name == "VALID" }
    override suspend fun countAlreadyUsed(): Int = entries.count { it.result.name == "ALREADY_USED" }
    override suspend fun countInvalid(): Int = entries.count { it.result.name == "INVALID" }
}
