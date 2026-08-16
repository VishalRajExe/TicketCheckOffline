package com.ticketcheck.offline.ui.screens.ticketlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketcheck.offline.data.entities.TicketEntity
import com.ticketcheck.offline.data.entities.TicketStatus
import com.ticketcheck.offline.data.repository.TicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class TicketFilter { ALL, VALID, USED }

class TicketListViewModel(private val repository: TicketRepository) : ViewModel() {

    private val allTickets = MutableStateFlow<List<TicketEntity>>(emptyList())
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(TicketFilter.ALL)

    val visibleTickets: StateFlow<List<TicketEntity>> = combine(allTickets, query, filter) { tickets, q, f ->
        tickets
            .filter { f == TicketFilter.ALL || (f == TicketFilter.VALID && it.status == TicketStatus.VALID) || (f == TicketFilter.USED && it.status == TicketStatus.USED) }
            .filter {
                q.isBlank() ||
                    it.ticketCode.contains(q, ignoreCase = true) ||
                    (it.customerName?.contains(q, ignoreCase = true) == true)
            }
    }.let { flow ->
        val state = MutableStateFlow<List<TicketEntity>>(emptyList())
        viewModelScope.launch { flow.collect { state.value = it } }
        state
    }

    val currentFilter: StateFlow<TicketFilter> = filter

    init {
        viewModelScope.launch {
            repository.observeTickets().collect { allTickets.value = it }
        }
    }

    fun setQuery(q: String) { query.value = q }
    fun setFilter(f: TicketFilter) { filter.value = f }

    fun deleteTickets(ids: Set<Long>) {
        viewModelScope.launch {
            val toDelete = allTickets.value.filter { it.id in ids }
            toDelete.forEach { repository.deleteTicket(it) }
        }
    }
}
