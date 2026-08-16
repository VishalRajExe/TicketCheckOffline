package com.ticketcheck.offline

import com.ticketcheck.offline.data.entities.TicketEntity
import com.ticketcheck.offline.data.entities.TicketStatus
import com.ticketcheck.offline.data.repository.TicketRepository
import com.ticketcheck.offline.domain.models.ScanOutcome
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests the core scan() business logic described in the spec:
 *   null -> INVALID
 *   USED -> ALREADY_USED
 *   VALID -> mark USED, return VALID
 * plus the "app restart" and "duplicate camera detection" guarantees.
 */
class TicketVerificationTest {

    private lateinit var ticketDao: FakeTicketDao
    private lateinit var eventDao: FakeEventDao
    private lateinit var historyDao: FakeScanHistoryDao
    private lateinit var repository: TicketRepository

    @Before
    fun setUp() {
        ticketDao = FakeTicketDao()
        eventDao = FakeEventDao()
        historyDao = FakeScanHistoryDao()
        repository = TicketRepository(ticketDao, eventDao, historyDao)
    }

    @Test
    fun `valid ticket becomes used on first scan`() = runBlocking {
        repository.addTicket(TicketEntity(ticketCode = "SISH01", qrContent = "SISH01"))

        val outcome = repository.processScan("SISH01")

        assertTrue(outcome is ScanOutcome.Valid)
        val ticket = ticketDao.findByCode("SISH01")!!
        assertEquals(TicketStatus.USED, ticket.status)
        assertNotNull(ticket.usedAt)
    }

    @Test
    fun `scanning an already used ticket does not change its status`() = runBlocking {
        repository.addTicket(TicketEntity(ticketCode = "SISH01", qrContent = "SISH01"))
        repository.processScan("SISH01") // first scan -> USED
        val usedAtAfterFirstScan = ticketDao.findByCode("SISH01")!!.usedAt

        val outcome = repository.processScan("SISH01") // duplicate scan

        assertTrue(outcome is ScanOutcome.AlreadyUsed)
        val ticket = ticketDao.findByCode("SISH01")!!
        assertEquals(TicketStatus.USED, ticket.status)
        assertEquals(usedAtAfterFirstScan, ticket.usedAt) // unchanged
    }

    @Test
    fun `scanning an unregistered code is invalid and does not create a ticket`() = runBlocking {
        val outcome = repository.processScan("ABC999")

        assertTrue(outcome is ScanOutcome.Invalid)
        assertNull(ticketDao.findByCode("ABC999"))
        assertEquals(0, ticketDao.count())
    }

    @Test
    fun `rapid duplicate scans of the same code only mark it used once`() = runBlocking {
        repository.addTicket(TicketEntity(ticketCode = "SISH04", qrContent = "SISH04"))

        // Simulate the camera analyzer firing several near-simultaneous callbacks.
        val outcomes = kotlinx.coroutines.coroutineScope {
            (1..5).map { async { repository.processScan("SISH04") } }.awaitAll()
        }

        val validCount = outcomes.count { it is ScanOutcome.Valid }
        val alreadyUsedCount = outcomes.count { it is ScanOutcome.AlreadyUsed }

        assertEquals(1, validCount)
        assertEquals(4, alreadyUsedCount)
        assertEquals(TicketStatus.USED, ticketDao.findByCode("SISH04")!!.status)
    }

    @Test
    fun `ticket status survives being read again after a scan (simulated app restart)`() = runBlocking {
        repository.addTicket(TicketEntity(ticketCode = "SISH01", qrContent = "SISH01"))
        repository.processScan("SISH01")

        // A "restart" just means re-querying the same underlying storage;
        // since our fake DAO holds the state, re-reading it simulates that.
        val reReadTicket = ticketDao.findByCode("SISH01")
        assertEquals(TicketStatus.USED, reReadTicket?.status)
    }

    @Test
    fun `duplicate ticket codes are rejected when adding manually`() = runBlocking {
        repository.addTicket(TicketEntity(ticketCode = "SISH01", qrContent = "SISH01"))
        val result = repository.addTicket(TicketEntity(ticketCode = "SISH01", qrContent = "SISH01"))

        assertTrue(result.isFailure)
    }

    @Test
    fun `sequential ticket generation pads codes correctly`() = runBlocking {
        repository.generateSequentialTickets("SISH", 1, 10, 2)
        val all = ticketDao.getAllOnce().map { it.ticketCode }.sorted()

        assertTrue(all.contains("SISH01"))
        assertTrue(all.contains("SISH10"))
        assertEquals(10, all.size)
    }

    @Test
    fun `backup export and restore round trip preserves ticket statuses`() = runBlocking {
        repository.addTicket(TicketEntity(ticketCode = "SISH01", qrContent = "SISH01"))
        repository.addTicket(TicketEntity(ticketCode = "SISH02", qrContent = "SISH02"))
        repository.processScan("SISH01") // SISH01 -> USED

        val snapshot = ticketDao.getAllOnce()

        repository.clearEverything()
        assertEquals(0, repository.getAllTicketsOnce().size)

        repository.importTickets(snapshot)

        val restored = ticketDao.findByCode("SISH01")!!
        assertEquals(TicketStatus.USED, restored.status)
        val restoredUnused = ticketDao.findByCode("SISH02")!!
        assertEquals(TicketStatus.VALID, restoredUnused.status)
    }

    @Test
    fun `resetting used status makes tickets scannable again`() = runBlocking {
        repository.addTicket(TicketEntity(ticketCode = "SISH01", qrContent = "SISH01"))
        repository.processScan("SISH01")
        assertEquals(TicketStatus.USED, ticketDao.findByCode("SISH01")!!.status)

        repository.resetAllUsedStatus()

        assertEquals(TicketStatus.VALID, ticketDao.findByCode("SISH01")!!.status)
        val outcome = repository.processScan("SISH01")
        assertTrue(outcome is ScanOutcome.Valid)
    }
}
