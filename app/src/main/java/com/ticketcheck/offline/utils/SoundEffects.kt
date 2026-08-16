package com.ticketcheck.offline.utils

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Every sound the app can make. All are synthesized at runtime. */
enum class AppSound {
    SUCCESS,     // scan: ticket is valid  - rising two-note chime
    WARN,        // scan: already used     - polite double beep
    ERROR,       // scan: invalid          - low descending buzz
    CLICK,       // generic button press   - soft tick
    TOGGLE,      // switch flipped         - short up-sweep
    OPEN,        // screen opened          - airy whoosh
    ARPEGGIO,    // success confirmation   - 3-note major arpeggio
    DELETE       // destructive confirm    - descending pair
}

/**
 * Tiny offline sound engine. Every effect is synthesized from sine waves
 * into 16-bit PCM and played through AudioTrack in MODE_STATIC - there are
 * no bundled audio assets and no network fetches, so the app keeps its
 * "works in airplane mode" guarantee.
 */
object SoundEffects {

    @Volatile
    var enabled: Boolean = true

    private const val SAMPLE_RATE = 44100

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val cache = mutableMapOf<AppSound, ShortArray>()

    fun play(sound: AppSound) {
        if (!enabled) return
        executor.execute {
            val pcm = synchronized(cache) { cache.getOrPut(sound) { synthesize(sound) } }
            playPcm(pcm)
        }
    }

    // ── Playback ─────────────────────────────────────────────────────────

    private fun playPcm(samples: ShortArray) {
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()

            track.setVolume(0.9f)
            track.write(samples, 0, samples.size)
            track.play()

            val durationMs = samples.size * 1000L / SAMPLE_RATE + 80
            mainHandler.postDelayed({
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {
                }
            }, durationMs)
        } catch (_: Exception) {
            // Audio device quirks must never crash the scanning flow.
        }
    }

    // ── Synthesis ────────────────────────────────────────────────────────

    private fun synthesize(sound: AppSound): ShortArray = when (sound) {
        AppSound.SUCCESS -> buildSound(420) {
            note(freq = 659.25, startMs = 0, durMs = 170, amp = 0.42)
            note(freq = 880.0, startMs = 125, durMs = 240, amp = 0.48)
            note(freq = 1760.0, startMs = 150, durMs = 200, amp = 0.10) // shimmer
        }

        AppSound.WARN -> buildSound(430) {
            note(freq = 493.88, startMs = 0, durMs = 120, amp = 0.38)
            note(freq = 493.88, startMs = 185, durMs = 150, amp = 0.38)
        }

        AppSound.ERROR -> buildSound(320) {
            sweepNote(startFreq = 210.0, endFreq = 140.0, startMs = 0, durMs = 290, amp = 0.42)
        }

        AppSound.CLICK -> buildSound(20) {
            note(freq = 1900.0, startMs = 0, durMs = 14, amp = 0.16)
        }

        AppSound.TOGGLE -> buildSound(60) {
            sweepNote(startFreq = 800.0, endFreq = 1250.0, startMs = 0, durMs = 50, amp = 0.22)
        }

        AppSound.OPEN -> buildSound(150) { whoosh(durMs = 140, amp = 0.26) }

        AppSound.ARPEGGIO -> buildSound(430) {
            note(freq = 523.25, startMs = 0, durMs = 140, amp = 0.36)   // C5
            note(freq = 659.25, startMs = 90, durMs = 140, amp = 0.36)  // E5
            note(freq = 783.99, startMs = 180, durMs = 230, amp = 0.40) // G5
        }

        AppSound.DELETE -> buildSound(320) {
            note(freq = 392.0, startMs = 0, durMs = 140, amp = 0.34)
            sweepNote(startFreq = 311.13, endFreq = 261.63, startMs = 110, durMs = 200, amp = 0.34)
        }
    }

    /** Allocates the PCM buffer for [totalMs] and lets the DSL fill it. */
    private inline fun buildSound(totalMs: Int, block: SynthScope.() -> Unit): ShortArray {
        val total = totalMs * SAMPLE_RATE / 1000
        val samples = ShortArray(total)
        SynthScope(samples).block()
        return samples
    }

    private class SynthScope(val out: ShortArray) {
        private val total get() = out.size

        /** One sine note with a fast attack and exponential decay envelope. */
        fun note(freq: Double, startMs: Int, durMs: Int, amp: Double) {
            val start = startMs * SAMPLE_RATE / 1000
            val len = durMs * SAMPLE_RATE / 1000
            val attack = SAMPLE_RATE / 200 // 5 ms
            val tau = (durMs / 4.0) / 1000.0
            for (i in 0 until len) {
                val idx = start + i
                if (idx >= total) break
                val t = i.toDouble() / SAMPLE_RATE
                val env = when {
                    i < attack -> i.toDouble() / attack
                    else -> exp(-(t - attack.toDouble() / SAMPLE_RATE) / tau)
                }
                val value = sin(2.0 * PI * freq * t) * amp * env
                out[idx] = mix(out[idx], value)
            }
        }

        /** A pitch-sweeping buzzy note (sine + odd harmonics) for errors. */
        fun sweepNote(startFreq: Double, endFreq: Double, startMs: Int, durMs: Int, amp: Double) {
            val start = startMs * SAMPLE_RATE / 1000
            val len = durMs * SAMPLE_RATE / 1000
            val attack = SAMPLE_RATE / 200
            val tau = (durMs / 3.5) / 1000.0
            var phase = 0.0
            for (i in 0 until len) {
                val idx = start + i
                if (idx >= total) break
                val t = i.toDouble() / SAMPLE_RATE
                val k = i.toDouble() / len
                val f = startFreq + (endFreq - startFreq) * k
                phase += 2.0 * PI * f / SAMPLE_RATE
                val env = when {
                    i < attack -> i.toDouble() / attack
                    else -> exp(-(t - attack.toDouble() / SAMPLE_RATE) / tau)
                }
                val value = (sin(phase) + 0.35 * sin(3 * phase) + 0.12 * sin(5 * phase)) / 1.47 * amp * env
                out[idx] = mix(out[idx], value)
            }
        }

        /** Low-pass filtered noise burst with a bell envelope (a soft "whoosh"). */
        fun whoosh(durMs: Int, amp: Double) {
            val len = durMs * SAMPLE_RATE / 1000
            val attack = len / 3
            var lp = 0.0
            var randomSeed = 123456789L
            for (i in 0 until len) {
                if (i >= total) break
                randomSeed = randomSeed * 6364136223846793005L + 1442695040888963407L
                val white = ((randomSeed ushr 33).toDouble() / Double.MAX_VALUE) - 1.0
                lp += 0.12 * (white - lp) // one-pole low-pass
                val k = i.toDouble() / len
                val env = if (i < attack) k * 3 else (1.0 - k).coerceAtLeast(0.0)
                out[i] = mix(out[i], lp * amp * env * (1.0 - 0.5 * k))
            }
        }

        private fun mix(current: Short, value: Double): Short =
            (current + (value * Short.MAX_VALUE)).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }
}
