package com.ticketcheck.offline.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ticketcheck.offline.TicketCheckApp
import com.ticketcheck.offline.ui.screens.backup.BackupViewModel
import com.ticketcheck.offline.ui.screens.history.ScanHistoryViewModel
import com.ticketcheck.offline.ui.screens.home.HomeViewModel
import com.ticketcheck.offline.ui.screens.manage.ManageTicketsViewModel
import com.ticketcheck.offline.ui.screens.qrgen.QrGenerateViewModel
import com.ticketcheck.offline.ui.screens.scanner.ScannerViewModel
import com.ticketcheck.offline.ui.screens.ticketdetail.TicketDetailViewModel
import com.ticketcheck.offline.ui.screens.ticketlist.TicketListViewModel

/** Simple factory so every ViewModel gets its dependencies from the Application singletons. */
class ViewModelFactory(private val app: TicketCheckApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            HomeViewModel::class.java -> HomeViewModel(app.repository)
            ScannerViewModel::class.java -> ScannerViewModel(app.repository, app.settings, app.feedbackHelper)
            ManageTicketsViewModel::class.java -> ManageTicketsViewModel(app.repository)
            TicketListViewModel::class.java -> TicketListViewModel(app.repository)
            TicketDetailViewModel::class.java -> TicketDetailViewModel(app.repository)
            QrGenerateViewModel::class.java -> QrGenerateViewModel(app.repository)
            ScanHistoryViewModel::class.java -> ScanHistoryViewModel(app.repository)
            BackupViewModel::class.java -> BackupViewModel(app.backupManager, app.repository)
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}
