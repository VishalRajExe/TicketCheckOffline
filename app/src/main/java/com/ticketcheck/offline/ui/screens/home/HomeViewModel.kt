package com.ticketcheck.offline.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketcheck.offline.data.entities.EventEntity
import com.ticketcheck.offline.data.repository.TicketRepository
import com.ticketcheck.offline.domain.models.DashboardStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: TicketRepository) : ViewModel() {

    private val _stats = MutableStateFlow(DashboardStats())
    val stats: StateFlow<DashboardStats> = _stats

    private val _event = MutableStateFlow<EventEntity?>(null)
    val event: StateFlow<EventEntity?> = _event

    init {
        viewModelScope.launch {
            repository.observeCurrentEvent().collect { _event.value = it }
        }
        refresh()
        // Recompute stats whenever tickets or history change.
        viewModelScope.launch {
            combine(repository.observeTickets(), repository.observeScanHistory()) { _, _ -> Unit }
                .collect { refresh() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _stats.value = repository.getDashboardStats()
        }
    }
}
