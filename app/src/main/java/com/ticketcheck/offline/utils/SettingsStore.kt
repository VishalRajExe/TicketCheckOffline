package com.ticketcheck.offline.utils

import android.content.Context

/**
 * Tiny local-only settings wrapper around SharedPreferences.
 * No network, nothing leaves the device.
 */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("ticketcheck_settings", Context.MODE_PRIVATE)

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration_enabled", true)
        set(value) = prefs.edit().putBoolean("vibration_enabled", value).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", true)
        set(value) = prefs.edit().putBoolean("dark_theme", value).apply()

    var resultAutoReturnMillis: Long
        get() = prefs.getLong("auto_return_ms", 1500L)
        set(value) = prefs.edit().putLong("auto_return_ms", value).apply()
}
