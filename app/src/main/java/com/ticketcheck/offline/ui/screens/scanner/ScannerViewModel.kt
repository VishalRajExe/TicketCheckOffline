package com.ticketcheck.offline.ui.screens.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticketcheck.offline.data.repository.TicketRepository
import com.ticketcheck.offline.domain.models.ScanOutcome
import com.ticketcheck.offline.utils.FeedbackHelper
import com.ticketcheck.offline.utils.SettingsStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

sealed class ScannerUiState {
    data object Scanning : ScannerUiState()
    data class Result(val outcome: ScanOutcome) : ScannerUiState()
}

class ScannerViewModel(
    private val repository: TicketRepository,
    private val settings: SettingsStore,
    private val feedback: FeedbackHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Scanning)
    val uiState: StateFlow<ScannerUiState> = _uiState

    // Guards against the analyzer firing again for the same physical scan
    // before the result screen has even had a chance to appear.
    private val isHandlingScan = AtomicBoolean(false)

    fun onQrDetected(rawValue: String) {
        if (!isHandlingScan.compareAndSet(false, true)) return
        viewModelScope.launch {
            val outcome = repository.processScan(rawValue)
            when (outcome) {
                is ScanOutcome.Valid -> feedback.playValid(settings.soundEnabled, settings.vibrationEnabled)
                is ScanOutcome.AlreadyUsed -> feedback.playAlreadyUsed(settings.soundEnabled, settings.vibrationEnabled)
                is ScanOutcome.Invalid -> feedback.playInvalid(settings.soundEnabled, settings.vibrationEnabled)
            }
            _uiState.value = ScannerUiState.Result(outcome)
            delay(settings.resultAutoReturnMillis)
            resumeScanning()
        }
    }

    fun resumeScanning() {
        _uiState.value = ScannerUiState.Scanning
        isHandlingScan.set(false)
    }
}
