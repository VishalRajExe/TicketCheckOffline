package com.ticketcheck.offline.ui.screens.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketcheck.offline.data.entities.TicketEntity
import com.ticketcheck.offline.data.repository.TicketRepository
import com.ticketcheck.offline.utils.CsvImporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ManageUiMessage(val text: String, val isError: Boolean = false)

class ManageTicketsViewModel(private val repository: TicketRepository) : ViewModel() {

    private val _message = MutableStateFlow<ManageUiMessage?>(null)
    val message: StateFlow<ManageUiMessage?> = _message

    fun clearMessage() { _message.value = null }

    fun generateSequential(prefix: String, start: Int, count: Int, padding: Int) {
        viewModelScope.launch {
            if (count <= 0 || count > 20000) {
                _message.value = ManageUiMessage("Enter a valid ticket count.", isError = true)
                return@launch
            }
            repository.generateSequentialTickets(prefix.uppercase(), start, count, padding)
            _message.value = ManageUiMessage("Generated $count tickets.")
        }
    }

    fun addManualTicket(code: String, name: String?, type: String?, price: Double?) {
        viewModelScope.launch {
            if (code.isBlank()) {
                _message.value = ManageUiMessage("Ticket code is required.", isError = true)
                return@launch
            }
            val result = repository.addTicket(
                TicketEntity(
                    ticketCode = code.trim(),
                    customerName = name?.takeIf { it.isNotBlank() },
                    ticketType = type?.takeIf { it.isNotBlank() },
                    price = price,
                    qrContent = code.trim()
                )
            )
            _message.value = if (result.isSuccess) {
                ManageUiMessage("Ticket $code added.")
            } else {
                ManageUiMessage(result.exceptionOrNull()?.message ?: "Ticket already exists.", isError = true)
            }
        }
    }

    fun importFromText(content: String) {
        viewModelScope.launch {
            val parsed = CsvImporter.parse(content)
            val summary = repository.importTickets(parsed.tickets)
            _message.value = ManageUiMessage(
                "Imported: ${summary.imported}   Duplicates: ${summary.duplicates}   Invalid rows: ${parsed.invalidRows}"
            )
        }
    }
}
