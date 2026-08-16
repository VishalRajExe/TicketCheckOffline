package com.ticketcheck.offline

import android.app.Application
import com.ticketcheck.offline.data.database.AppDatabase
import com.ticketcheck.offline.data.repository.TicketRepository
import com.ticketcheck.offline.utils.BackupManager
import com.ticketcheck.offline.utils.FeedbackHelper
import com.ticketcheck.offline.utils.SettingsStore
import com.ticketcheck.offline.utils.SoundEffects

/**
 * Application-wide singletons. Everything constructed here is local:
 * a Room database file on disk, SharedPreferences, and small helper
 * classes. There is no Firebase, no Supabase, no HTTP client anywhere
 * in this project.
 */
class TicketCheckApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var repository: TicketRepository
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var backupManager: BackupManager
        private set
    lateinit var feedbackHelper: FeedbackHelper
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = TicketRepository(database.ticketDao(), database.eventDao(), database.scanHistoryDao())
        settings = SettingsStore(this)
        SoundEffects.enabled = settings.soundEnabled
        backupManager = BackupManager(this, repository)
        feedbackHelper = FeedbackHelper(this)
    }
}
