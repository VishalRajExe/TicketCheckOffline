package com.ticketcheck.offline.ui.screens.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketcheck.offline.data.repository.TicketRepository
import com.ticketcheck.offline.utils.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class BackupUiEvent {
    data class ExportSuccess(val file: File) : BackupUiEvent()
    data class RestoreDone(val ticketsRestored: Int) : BackupUiEvent()
    data class Error(val message: String) : BackupUiEvent()
}

class BackupViewModel(
    private val backupManager: BackupManager,
    private val repository: TicketRepository
) : ViewModel() {

    private val _event = MutableStateFlow<BackupUiEvent?>(null)
    val event: StateFlow<BackupUiEvent?> = _event

    fun clearEvent() { _event.value = null }

    fun exportBackup() {
        viewModelScope.launch {
            try {
                val file = backupManager.exportBackup()
                _event.value = BackupUiEvent.ExportSuccess(file)
            } catch (e: Exception) {
                _event.value = BackupUiEvent.Error(e.message ?: "Failed to export backup.")
            }
        }
    }

    fun restoreBackup(file: File) {
        viewModelScope.launch {
            val result = backupManager.restoreBackup(file)
            _event.value = if (result.success) {
                BackupUiEvent.RestoreDone(result.ticketsRestored)
            } else {
                BackupUiEvent.Error(result.error ?: "Could not read that backup file.")
            }
        }
    }

    fun resetUsedOnly() {
        viewModelScope.launch { repository.resetAllUsedStatus() }
    }

    fun resetEverything() {
        viewModelScope.launch { repository.clearEverything() }
    }
}
