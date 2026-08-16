package com.ticketcheck.offline.ui.screens.ticketdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketcheck.offline.data.entities.TicketEntity
import com.ticketcheck.offline.data.repository.TicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TicketDetailViewModel(private val repository: TicketRepository) : ViewModel() {
    private val _ticket = MutableStateFlow<TicketEntity?>(null)
    val ticket: StateFlow<TicketEntity?> = _ticket

    fun load(id: Long) {
        viewModelScope.launch {
            _ticket.value = repository.dao().findById(id)
        }
    }

    fun updateTicket(customerName: String, ticketType: String, price: Double?) {
        val current = _ticket.value ?: return
        val updated = current.copy(
            customerName = customerName.ifBlank { null },
            ticketType = ticketType.ifBlank { null },
            price = price
        )
        viewModelScope.launch {
            repository.updateTicket(updated)
            _ticket.value = updated
        }
    }

    fun deleteTicket(onDone: () -> Unit) {
        val current = _ticket.value ?: return
        viewModelScope.launch {
            repository.deleteTicket(current)
            onDone()
        }
    }
}
