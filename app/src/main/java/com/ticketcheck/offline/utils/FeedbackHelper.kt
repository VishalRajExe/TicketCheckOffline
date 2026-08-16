package com.ticketcheck.offline.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Sound + haptic feedback for scan results. Sounds are synthesized at
 * runtime by [SoundEffects] (AudioTrack PCM) and haptics use the plain
 * system Vibrator - no bundled audio assets, no network, works in
 * airplane mode.
 */
class FeedbackHelper(context: Context) {

    private val appContext = context.applicationContext

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun playValid(soundOn: Boolean, vibrationOn: Boolean) {
        if (soundOn) SoundEffects.play(AppSound.SUCCESS)
        if (vibrationOn) vibrate(longArrayOf(0, 60, 50, 40))
    }

    fun playAlreadyUsed(soundOn: Boolean, vibrationOn: Boolean) {
        if (soundOn) SoundEffects.play(AppSound.WARN)
        if (vibrationOn) vibrate(longArrayOf(0, 90, 70, 90))
    }

    fun playInvalid(soundOn: Boolean, vibrationOn: Boolean) {
        if (soundOn) SoundEffects.play(AppSound.ERROR)
        if (vibrationOn) vibrate(longArrayOf(0, 140, 80, 140))
    }

    private fun vibrate(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (_: Exception) {
        }
    }
}
