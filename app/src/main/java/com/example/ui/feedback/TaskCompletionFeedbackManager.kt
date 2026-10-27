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
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Supported completion sound profiles for LifeOS.
 * Synthesized 100% offline via algorithmic PCM WAV generation.
 */
enum class FeedbackSoundType(val displayName: String, val filename: String) {
    TASK("Task Chime", "lifeos_chime_task.wav"),
    HABIT("Habit Bell", "lifeos_chime_habit.wav"),
    WATER("Water Droplet", "lifeos_chime_water.wav"),
    MILESTONE("Milestone Flourish", "lifeos_chime_milestone.wav"),
    LEVEL_UP("Level Up Fanfare", "lifeos_chime_levelup.wav"),
    ALL_DONE("All Tasks Complete", "lifeos_chime_alldone.wav"),
    BELL("Tibetan Bell", "lifeos_sound_bell.wav"),
    POP("Crisp Bubble Pop", "lifeos_sound_pop.wav"),
    RETRO("8-Bit Retro Ping", "lifeos_sound_retro.wav"),
    SUCCESS("Victory Marimba", "lifeos_sound_success.wav"),
    HARP("Celestial Harp", "lifeos_sound_harp.wav"),
    CUSTOM("Custom Sound", "lifeos_custom_sound.wav");

    companion object {
        fun fromString(value: String): FeedbackSoundType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: TASK
        }
    }
}

/**
 * Centralized, reusable task completion feedback system for LifeOS.
 * Provides:
 * 1. 6 unique algorithmic PCM WAV synthesized feedback sounds via SoundPool.
 * 2. Subtle, crisp haptic vibration via modern Vibrator APIs or View feedback.
 * 3. Silent mode & audio setting respect with zero network or external dependencies.
 */
class TaskCompletionFeedbackManager private constructor(private val context: Context) {

    private var soundPool: SoundPool? = null
    private val soundIds = ConcurrentHashMap<FeedbackSoundType, Int>()
    private val loadedSoundIds = ConcurrentHashMap.newKeySet<Int>()
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
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    loadedSoundIds.add(sampleId)
                }
            }

            // Synthesize and load all 6 offline audio profiles
            FeedbackSoundType.entries.forEach { type ->
                val file = getOrCreateWavFile(context, type)
                if (file.exists()) {
                    val sId = soundPool?.load(file.absolutePath, 1) ?: -1
                    if (sId != -1) {
                        soundIds[type] = sId
                    }
                }
            }
        } catch (_: Exception) {
            // Audio initialization failed gracefully; UI operations must never crash
        }
    }

    /**
     * Triggers completion feedback (sound and haptics) if enabled.
     * Prevents duplicate executions from rapid successive taps.
     */
    fun triggerCompletionFeedback(
        soundEnabled: Boolean = true,
        vibrationEnabled: Boolean = true,
        view: View? = null,
        soundType: FeedbackSoundType = FeedbackSoundType.TASK
    ) {
        val now = System.currentTimeMillis()
        if (now - lastTriggerTimestamp < 150L) {
            return // Debounce rapid taps
        }
        lastTriggerTimestamp = now

        if (soundEnabled) {
            playSound(soundType)
        }

        if (vibrationEnabled) {
            playCompletionHaptic(view)
        }
    }

    fun playSound(type: FeedbackSoundType = FeedbackSoundType.TASK) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null && audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
                return
            }

            val soundId = soundIds[type] ?: return
            if (loadedSoundIds.contains(soundId)) {
                soundPool?.play(soundId, 0.85f, 0.85f, 1, 0, 1.0f)
            }
        } catch (_: Exception) {
            // Audio playback error handled gracefully
        }
    }

    fun playCompletionSound() {
        playSound(FeedbackSoundType.TASK)
    }

    fun importCustomAudio(sourceUri: android.net.Uri): Boolean {
        return try {
            val destFile = File(context.filesDir, FeedbackSoundType.CUSTOM.filename)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            val sId = soundPool?.load(destFile.absolutePath, 1) ?: -1
            if (sId != -1) {
                soundIds[FeedbackSoundType.CUSTOM] = sId
            }
            true
        } catch (_: Exception) {
            false
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
            loadedSoundIds.clear()
            soundIds.clear()
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

        private fun getOrCreateWavFile(context: Context, type: FeedbackSoundType): File {
            val file = File(context.filesDir, type.filename)
            if (file.exists() && file.length() > 0) {
                return file
            }

            try {
                val sampleRate = 44100
                val durationSec = when (type) {
                    FeedbackSoundType.TASK -> 0.180
                    FeedbackSoundType.HABIT -> 0.220
                    FeedbackSoundType.WATER -> 0.130
                    FeedbackSoundType.MILESTONE -> 0.280
                    FeedbackSoundType.LEVEL_UP -> 0.340
                    FeedbackSoundType.ALL_DONE -> 0.400
                    FeedbackSoundType.BELL -> 0.300
                    FeedbackSoundType.POP -> 0.080
                    FeedbackSoundType.RETRO -> 0.200
                    FeedbackSoundType.SUCCESS -> 0.250
                    FeedbackSoundType.HARP -> 0.300
                    FeedbackSoundType.CUSTOM -> 0.180
                }
                val totalSamples = (sampleRate * durationSec).toInt()
                val pcmData = ShortArray(totalSamples)

                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val sampleValue: Int = when (type) {
                        FeedbackSoundType.TASK -> {
                            val attack = if (t < 0.005) (t / 0.005) * (t / 0.005) else 1.0
                            val decay = exp(-t / 0.045)
                            val env = attack * decay
                            val tone = sin(2.0 * PI * 880.0 * t) * 0.65 +
                                    sin(2.0 * PI * 1320.0 * t) * 0.25 +
                                    sin(2.0 * PI * 1760.0 * t) * 0.10
                            (tone * env * 26000.0).toInt()
                        }
                        FeedbackSoundType.HABIT -> {
                            // Two-note warm bell: C5 (523Hz) then E5 (659Hz)
                            val note1 = if (t < 0.110) {
                                val env1 = (if (t < 0.005) t / 0.005 else 1.0) * exp(-t / 0.050)
                                sin(2.0 * PI * 523.25 * t) * env1
                            } else 0.0
                            val t2 = t - 0.080
                            val note2 = if (t >= 0.080) {
                                val env2 = (if (t2 < 0.005) t2 / 0.005 else 1.0) * exp(-t2 / 0.065)
                                (sin(2.0 * PI * 659.25 * t) * 0.75 + sin(2.0 * PI * 1318.5 * t) * 0.25) * env2
                            } else 0.0
                            ((note1 + note2) * 26000.0).toInt()
                        }
                        FeedbackSoundType.WATER -> {
                            // Pitch-swept droplet 600Hz -> 1350Hz with sharp bubble decay
                            val freq = 600.0 + (750.0 * (t / durationSec))
                            val env = (if (t < 0.004) t / 0.004 else 1.0) * exp(-t / 0.032)
                            (sin(2.0 * PI * freq * t) * env * 27000.0).toInt()
                        }
                        FeedbackSoundType.MILESTONE -> {
                            // 4-note arpeggio C5, E5, G5, C6
                            val segLen = 0.065
                            val seg = (t / segLen).toInt().coerceIn(0, 3)
                            val tLocal = t - (seg * segLen)
                            val f = when (seg) {
                                0 -> 523.25
                                1 -> 659.25
                                2 -> 783.99
                                else -> 1046.50
                            }
                            val env = (if (tLocal < 0.005) tLocal / 0.005 else 1.0) * exp(-tLocal / (if (seg == 3) 0.075 else 0.045))
                            (sin(2.0 * PI * f * t) * env * 26000.0).toInt()
                        }
                        FeedbackSoundType.LEVEL_UP -> {
                            // 5-note fanfare C5, D5, E5, G5, C6
                            val segLen = 0.055
                            val seg = (t / segLen).toInt().coerceIn(0, 4)
                            val tLocal = t - (seg * segLen)
                            val f = when (seg) {
                                0 -> 523.25
                                1 -> 587.33
                                2 -> 659.25
                                3 -> 783.99
                                else -> 1046.50
                            }
                            val env = (if (tLocal < 0.004) tLocal / 0.004 else 1.0) * exp(-tLocal / (if (seg == 4) 0.090 else 0.040))
                            val harm = if (seg == 4) sin(2.0 * PI * f * 2.0 * t) * 0.25 else 0.0
                            ((sin(2.0 * PI * f * t) * 0.85 + harm) * env * 26000.0).toInt()
                        }
                        FeedbackSoundType.ALL_DONE -> {
                            // Grand celebratory chord: C5 + G5 + C6 + E6
                            val env = (if (t < 0.008) t / 0.008 else 1.0) * exp(-t / 0.110)
                            val chord = sin(2.0 * PI * 523.25 * t) * 0.35 +
                                    sin(2.0 * PI * 783.99 * t) * 0.25 +
                                    sin(2.0 * PI * 1046.50 * t) * 0.25 +
                                    sin(2.0 * PI * 1318.51 * t) * 0.15
                            (chord * env * 27000.0).toInt()
                        }
                        FeedbackSoundType.BELL -> {
                            val attack = if (t < 0.005) t / 0.005 else 1.0
                            val decay = exp(-t / 0.120)
                            val tone = sin(2.0 * PI * 1046.50 * t) * 0.60 + sin(2.0 * PI * 2093.00 * t) * 0.25 + sin(2.0 * PI * 3139.50 * t) * 0.15
                            ((tone * attack * decay) * 26000.0).toInt()
                        }
                        FeedbackSoundType.POP -> {
                            val freq = 1200.0 - (800.0 * (t / durationSec))
                            val env = (if (t < 0.003) t / 0.003 else 1.0) * exp(-t / 0.020)
                            (sin(2.0 * PI * freq * t) * env * 28000.0).toInt()
                        }
                        FeedbackSoundType.RETRO -> {
                            val seg = (t / 0.040).toInt().coerceIn(0, 3)
                            val f = when (seg) { 0 -> 659.25; 1 -> 783.99; 2 -> 987.77; else -> 1318.51 }
                            val env = exp(-(t - (seg * 0.040)) / 0.030)
                            val tone = if (sin(2.0 * PI * f * t) > 0.0) 0.8 else -0.8
                            (tone * env * 22000.0).toInt()
                        }
                        FeedbackSoundType.SUCCESS -> {
                            val env = (if (t < 0.004) t / 0.004 else 1.0) * exp(-t / 0.070)
                            val tone = sin(2.0 * PI * 698.46 * t) * 0.40 + sin(2.0 * PI * 880.00 * t) * 0.35 + sin(2.0 * PI * 1046.50 * t) * 0.25
                            (tone * env * 26000.0).toInt()
                        }
                        FeedbackSoundType.HARP -> {
                            val seg = (t / 0.045).toInt().coerceIn(0, 4)
                            val f = when (seg) { 0 -> 523.25; 1 -> 659.25; 2 -> 783.99; 3 -> 1046.50; else -> 1318.51 }
                            val env = exp(-(t - (seg * 0.045)) / 0.060)
                            val tone = sin(2.0 * PI * f * t) * 0.7 + sin(2.0 * PI * f * 2.0 * t) * 0.3
                            (tone * env * 25000.0).toInt()
                        }
                        FeedbackSoundType.CUSTOM -> {
                            // Default fallback tone if user has not yet imported audio file
                            val attack = if (t < 0.005) t / 0.005 else 1.0
                            val decay = exp(-t / 0.050)
                            val tone = sin(2.0 * PI * 987.77 * t) * 0.70 + sin(2.0 * PI * 1479.98 * t) * 0.30
                            ((tone * attack * decay) * 26000.0).toInt()
                        }
                    }
                    pcmData[i] = sampleValue.coerceIn(-32768, 32767).toShort()
                }

                val subChunk2Size = totalSamples * 2
                val chunkSize = 36 + subChunk2Size

                FileOutputStream(file).use { fos ->
                    val header = ByteBuffer.allocate(44).apply {
                        order(ByteOrder.LITTLE_ENDIAN)
                        put("RIFF".toByteArray())
                        putInt(chunkSize)
                        put("WAVE".toByteArray())
                        put("fmt ".toByteArray())
                        putInt(16)
                        putShort(1) // PCM
                        putShort(1) // Mono
                        putInt(sampleRate)
                        putInt(sampleRate * 2)
                        putShort(2)
                        putShort(16)
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
