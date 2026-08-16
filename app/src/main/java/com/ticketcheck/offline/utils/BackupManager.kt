package com.ticketcheck.offline.utils

import android.content.Context
import androidx.core.content.FileProvider
import com.ticketcheck.offline.data.entities.EventEntity
import com.ticketcheck.offline.data.entities.ScanHistoryEntity
import com.ticketcheck.offline.data.entities.ScanResult
import com.ticketcheck.offline.data.entities.TicketEntity
import com.ticketcheck.offline.data.entities.TicketStatus
import com.ticketcheck.offline.data.repository.TicketRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports/imports all local data (event, tickets, scan history) as a
 * single JSON file on the device's own storage. No network is used -
 * the file is written to app-external storage and handed to Android's
 * share sheet or file picker, entirely offline.
 */
class BackupManager(private val context: Context, private val repository: TicketRepository) {

    suspend fun exportBackup(): File {
        val event = repository.getCurrentEvent()
        val tickets = repository.getAllTicketsOnce()
        val history = repository.observeScanHistory()
        // one-shot snapshot of history via DAO
        val historyList = repository.historyDao().getAll()

        val root = JSONObject()
        root.put("formatVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())

        event?.let {
            root.put("event", JSONObject().apply {
                put("name", it.name)
                put("eventDate", it.eventDate)
                put("venue", it.venue)
                put("ticketPrefix", it.ticketPrefix)
                put("createdAt", it.createdAt)
            })
        }

        val ticketsArray = JSONArray()
        tickets.forEach { t ->
            ticketsArray.put(JSONObject().apply {
                put("ticketCode", t.ticketCode)
                put("customerName", t.customerName ?: JSONObject.NULL)
                put("ticketType", t.ticketType ?: JSONObject.NULL)
                put("price", t.price ?: JSONObject.NULL)
                put("qrContent", t.qrContent)
                put("status", t.status.name)
                put("createdAt", t.createdAt)
                put("usedAt", t.usedAt ?: JSONObject.NULL)
                put("scanCount", t.scanCount)
                put("lastScanTime", t.lastScanTime ?: JSONObject.NULL)
            })
        }
        root.put("tickets", ticketsArray)

        val historyArray = JSONArray()
        historyList.forEach { h ->
            historyArray.put(JSONObject().apply {
                put("scannedValue", h.scannedValue)
                put("result", h.result.name)
                put("ticketId", h.ticketId ?: JSONObject.NULL)
                put("timestamp", h.timestamp)
            })
        }
        root.put("scanHistory", historyArray)

        val dir = File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
        val prefix = event?.ticketPrefix?.takeIf { it.isNotBlank() } ?: "TICKETCHECK"
        val file = File(dir, "${prefix}_backup_$stamp.json")
        file.writeText(root.toString(2))
        return file
    }

    fun shareUri(file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    data class RestoreResult(val success: Boolean, val ticketsRestored: Int, val error: String? = null)

    suspend fun restoreBackup(file: File): RestoreResult {
        return try {
            val json = JSONObject(file.readText())

            if (json.has("event")) {
                val e = json.getJSONObject("event")
                repository.createOrUpdateEvent(
                    EventEntity(
                        name = e.optString("name", "Restored Event"),
                        eventDate = e.optString("eventDate", ""),
                        venue = e.optString("venue", ""),
                        ticketPrefix = e.optString("ticketPrefix", ""),
                        createdAt = e.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            repository.clearEverything()

            val ticketsArray = json.optJSONArray("tickets") ?: JSONArray()
            val tickets = mutableListOf<TicketEntity>()
            for (i in 0 until ticketsArray.length()) {
                val t = ticketsArray.getJSONObject(i)
                tickets.add(
                    TicketEntity(
                        ticketCode = t.getString("ticketCode"),
                        customerName = t.optStringOrNull("customerName"),
                        ticketType = t.optStringOrNull("ticketType"),
                        price = if (t.isNull("price")) null else t.optDouble("price"),
                        qrContent = t.optString("qrContent", t.getString("ticketCode")),
                        status = TicketStatus.valueOf(t.optString("status", "VALID")),
                        createdAt = t.optLong("createdAt", System.currentTimeMillis()),
                        usedAt = if (t.isNull("usedAt")) null else t.optLong("usedAt"),
                        scanCount = t.optInt("scanCount", 0),
                        lastScanTime = if (t.isNull("lastScanTime")) null else t.optLong("lastScanTime")
                    )
                )
            }
            repository.importTickets(tickets)

            val historyArray = json.optJSONArray("scanHistory") ?: JSONArray()
            for (i in 0 until historyArray.length()) {
                val h = historyArray.getJSONObject(i)
                repository.historyDao().insert(
                    ScanHistoryEntity(
                        scannedValue = h.getString("scannedValue"),
                        result = ScanResult.valueOf(h.optString("result", "INVALID")),
                        ticketId = if (h.isNull("ticketId")) null else h.optLong("ticketId"),
                        timestamp = h.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }

            RestoreResult(success = true, ticketsRestored = tickets.size)
        } catch (e: Exception) {
            RestoreResult(success = false, ticketsRestored = 0, error = e.message ?: "Unknown error while reading backup file.")
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}
