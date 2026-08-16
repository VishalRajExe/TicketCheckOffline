package com.ticketcheck.offline.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketcheck.offline.data.entities.ScanHistoryEntity
import com.ticketcheck.offline.data.entities.ScanResult
import com.ticketcheck.offline.data.repository.TicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class HistoryFilter { ALL, VALID, ALREADY_USED, INVALID }

class ScanHistoryViewModel(private val repository: TicketRepository) : ViewModel() {
    private val all = MutableStateFlow<List<ScanHistoryEntity>>(emptyList())
    private val filter = MutableStateFlow(HistoryFilter.ALL)
    val currentFilter: StateFlow<HistoryFilter> = filter

    val visible: StateFlow<List<ScanHistoryEntity>> = MutableStateFlow(emptyList<ScanHistoryEntity>()).also { state ->
        viewModelScope.launch {
            combine(all, filter) { entries, f ->
                entries.filter {
                    when (f) {
                        HistoryFilter.ALL -> true
                        HistoryFilter.VALID -> it.result == ScanResult.VALID
                        HistoryFilter.ALREADY_USED -> it.result == ScanResult.ALREADY_USED
                        HistoryFilter.INVALID -> it.result == ScanResult.INVALID
                    }
                }
            }.collect { state.value = it }
        }
    }

    init {
        viewModelScope.launch { repository.observeScanHistory().collect { all.value = it } }
    }

    fun setFilter(f: HistoryFilter) { filter.value = f }
}
