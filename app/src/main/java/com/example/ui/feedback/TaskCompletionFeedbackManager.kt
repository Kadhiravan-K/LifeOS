package com.example.ui.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Centralized, reusable task completion feedback system for LifeOS.
 * Provides:
 * 1. Short, pleasant completion chime (180ms synthesized harmonic bell chime via SoundPool).
 * 2. Subtle, crisp haptic vibration via modern Vibrator APIs or View feedback.
 * 3. Silent mode & audio setting respect with zero network or external dependencies.
 */
class TaskCompletionFeedbackManager private constructor(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var chimeSoundId: Int = -1
    private var isSoundLoaded: Boolean = false
    private var lastTriggerTimestamp: Long = 0L

    init {
        initializeAudio()
    }

    private fun initializeAudio() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0 && sampleId == chimeSoundId) {
                    isSoundLoaded = true
                }
            }

            // Ensure our pristine, original harmonic completion chime WAV file exists locally
            val chimeFile = getOrCreateChimeWavFile(context)
            if (chimeFile.exists()) {
                chimeSoundId = soundPool?.load(chimeFile.absolutePath, 1) ?: -1
            }
        } catch (_: Exception) {
            // Audio initialization failed gracefully; UI operations must not crash
        }
    }

    /**
     * Triggers completion feedback (sound and haptics) if enabled.
     * Prevents duplicate executions from rapid successive taps.
     */
    fun triggerCompletionFeedback(
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true,
        view: View? = null
    ) {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTimestamp < 150L) {
            return // Debounce rapid taps
        }
        lastTriggerTimestamp = now

        if (soundEnabled) {
            playCompletionSound()
        }

        if (vibrationEnabled) {
            playCompletionHaptic(view)
        }
    }

    fun playCompletionSound() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            // Respect silent and vibrate-only device modes
            if (audioManager != null && audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                return
            }

            if (isSoundLoaded && chimeSoundId != -1) {
                soundPool?.play(chimeSoundId, 0.75f, 0.75f, 1, 0, 1.0f)
            }
        } catch (_: Exception) {
            // Audio playback error handled gracefully
        }
    }

    fun playCompletionHaptic(view: View? = null) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                if (vibrator?.hasVibrator() == true) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                    return
                }
            }

            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(20L)
                }
                return
            }
        } catch (_: Exception) {
            // Fallback to view-based haptic
        }

        try {
            view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {
            // Device does not support haptics
        }
    }

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            isSoundLoaded = false
        } catch (_: Exception) {
        }
    }

    companion object {
        @Volatile
        private var instance: TaskCompletionFeedbackManager? = null

        fun getInstance(context: Context): TaskCompletionFeedbackManager {
            return instance ?: synchronized(this) {
                instance ?: TaskCompletionFeedbackManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Synthesizes an original, pleasant 180ms dual-bell harmonic chime WAV file.
         * Frequencies: 880 Hz (A5), 1320 Hz (E6), 1760 Hz (A6) with smooth quadratic attack and exponential decay.
         * 100% offline, original, zero external assets or permissions needed.
         */
        private fun getOrCreateChimeWavFile(context: Context): File {
            val file = File(context.filesDir, "lifeos_completion_chime.wav")
            if (file.exists() && file.length() > 0) {
                return file
            }

            try {
                val sampleRate = 44100
                val durationSec = 0.180 // 180 milliseconds
                val totalSamples = (sampleRate * durationSec).toInt()

                val pcmData = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    // Smooth 5ms quadratic rise attack
                    val attack = if (t < 0.005) (t / 0.005) * (t / 0.005) else 1.0
                    // Exponential gentle bell release decay
                    val decay = exp(-t / 0.045)
                    val envelope = attack * decay

                    // Harmonic frequencies: 880Hz (A5), 1320Hz (E6, fifth), 1760Hz (A6, octave)
                    val tone1 = sin(2.0 * PI * 880.0 * t) * 0.65
                    val tone2 = sin(2.0 * PI * 1320.0 * t) * 0.25
                    val tone3 = sin(2.0 * PI * 1760.0 * t) * 0.10
                    val sampleValue = ((tone1 + tone2 + tone3) * envelope * 26000.0).toInt().coerceIn(-32768, 32767)
                    pcmData[i] = sampleValue.toShort()
                }

                val subChunk2Size = totalSamples * 2 // 16-bit mono = 2 bytes per sample
                val chunkSize = 36 + subChunk2Size

                FileOutputStream(file).use { fos ->
                    val header = ByteBuffer.allocate(44).apply {
                        order(ByteOrder.LITTLE_ENDIAN)
                        put("RIFF".toByteArray())
                        putInt(chunkSize)
                        put("WAVE".toByteArray())
                        put("fmt ".toByteArray())
                        putInt(16) // Subchunk1Size for PCM
                        putShort(1) // AudioFormat 1 = PCM
                        putShort(1) // NumChannels 1 = Mono
                        putInt(sampleRate)
                        putInt(sampleRate * 2) // ByteRate
                        putShort(2) // BlockAlign
                        putShort(16) // BitsPerSample
                        put("data".toByteArray())
                        putInt(subChunk2Size)
                    }.array()

                    fos.write(header)

                    val buffer = ByteBuffer.allocate(subChunk2Size).apply {
                        order(ByteOrder.LITTLE_ENDIAN)
                        for (s in pcmData) {
                            putShort(s)
                        }
                    }.array()

                    fos.write(buffer)
                }
            } catch (_: Exception) {
            }

            return file
        }
    }
}
