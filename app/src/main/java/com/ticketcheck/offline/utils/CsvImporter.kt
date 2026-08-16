package com.ticketcheck.offline.utils

import com.ticketcheck.offline.data.entities.TicketEntity

/**
 * Parses plain TXT (one code per line) or simple CSV
 * (ticketCode,customerName,ticketType,price) content into TicketEntity
 * candidates. All parsing happens in memory, offline.
 */
object CsvImporter {

    data class ParseResult(val tickets: List<TicketEntity>, val invalidRows: Int)

    fun parse(content: String): ParseResult {
        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) return ParseResult(emptyList(), 0)

        val looksLikeCsv = lines.first().contains(",") ||
            lines.any { it.split(",").size > 1 }

        val tickets = mutableListOf<TicketEntity>()
        var invalidRows = 0

        // Skip an obvious header row like "ticketCode,customerName,..."
        val dataLines = if (looksLikeCsv && lines.first().lowercase().startsWith("ticketcode")) {
            lines.drop(1)
        } else lines

        for (line in dataLines) {
            if (looksLikeCsv) {
                val parts = line.split(",").map { it.trim() }
                val code = parts.getOrNull(0).orEmpty()
                if (code.isBlank()) {
                    invalidRows++
                    continue
                }
                val name = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
                val type = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
                val price = parts.getOrNull(3)?.toDoubleOrNull()
                tickets.add(
                    TicketEntity(
                        ticketCode = code,
                        customerName = name,
                        ticketType = type,
                        price = price,
                        qrContent = code
                    )
                )
            } else {
                tickets.add(TicketEntity(ticketCode = line, qrContent = line))
            }
        }
        return ParseResult(tickets, invalidRows)
    }
}
