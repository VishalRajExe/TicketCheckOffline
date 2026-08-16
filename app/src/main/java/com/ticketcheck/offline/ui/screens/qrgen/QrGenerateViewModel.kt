package com.ticketcheck.offline.ui.screens.qrgen

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketcheck.offline.data.entities.TicketEntity
import com.ticketcheck.offline.data.repository.TicketRepository
import com.ticketcheck.offline.qr.QrCodeGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class QrItem(val ticket: TicketEntity, val bitmap: Bitmap)

data class QrGenState(
    val items: List<QrItem> = emptyList(),
    val notFound: Boolean = false
)

class QrGenerateViewModel(private val repository: TicketRepository) : ViewModel() {
    private val _state = MutableStateFlow(QrGenState())
    val state: StateFlow<QrGenState> = _state

    fun load(ticketCode: String?) {
        viewModelScope.launch {
            if (ticketCode.isNullOrBlank()) {
                _state.value = QrGenState()
                return@launch
            }
            val ticket = repository.dao().findByCode(ticketCode)
            if (ticket == null) {
                _state.value = QrGenState(notFound = true)
            } else {
                val item = QrItem(ticket, QrCodeGenerator.generate(ticket.ticketCode))
                _state.value = QrGenState(items = listOf(item))
            }
        }
    }

    fun loadMultiple(codes: List<String>) {
        viewModelScope.launch {
            val items = codes.mapNotNull { code ->
                val ticket = repository.dao().findByCode(code)
                ticket?.let { QrItem(it, QrCodeGenerator.generate(it.ticketCode)) }
            }
            _state.value = QrGenState(items = items)
        }
    }
}
