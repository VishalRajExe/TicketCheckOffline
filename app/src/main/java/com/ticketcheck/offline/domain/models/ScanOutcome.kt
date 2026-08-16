package com.ticketcheck.offline.domain.models

import com.ticketcheck.offline.data.entities.TicketEntity

/**
 * Result of processing a single scanned QR value. This is what the
 * scanner UI renders - it never exposes raw database access to the
 * person holding the phone at the door.
 */
sealed class ScanOutcome {
    data class Valid(val ticket: TicketEntity) : ScanOutcome()
    data class AlreadyUsed(val ticket: TicketEntity) : ScanOutcome()
    data class Invalid(val scannedCode: String) : ScanOutcome()
}

/** Simple counters shown on the dashboard. */
data class DashboardStats(
    val totalTickets: Int = 0,
    val usedTickets: Int = 0,
    val remainingTickets: Int = 0,
    val validScans: Int = 0,
    val alreadyUsedScans: Int = 0,
    val invalidScans: Int = 0
)
