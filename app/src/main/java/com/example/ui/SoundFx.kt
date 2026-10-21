package com.example.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.GameSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural low-latency sound synthesizer for arcade zombie combat.
 * Generates custom PCM sound effects in memory without requiring external audio assets.
 */
object SoundFx {
    private const val SAMPLE_RATE = 22050
    private var isInitialized = false

    private var trackRifle: AudioTrack? = null
    private var trackSmg: AudioTrack? = null
    private var trackShotgun: AudioTrack? = null
    private var trackSniper: AudioTrack? = null
    private var trackHit: AudioTrack? = null
    private var trackDeath: AudioTrack? = null
    private var trackHurt: AudioTrack? = null
    private var trackReload: AudioTrack? = null
    private var trackCombo: AudioTrack? = null
    private var trackBossRoar: AudioTrack? = null
    private var trackBossSlam: AudioTrack? = null

    private val audioScope = CoroutineScope(Dispatchers.Default)

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        audioScope.launch {
            try {
                trackRifle = createTrack(generateGunshot(durationMs = 90, baseFreq = 160f, noiseWeight = 0.7f))
                trackSmg = createTrack(generateGunshot(durationMs = 60, baseFreq = 220f, noiseWeight = 0.8f))
                trackShotgun = createTrack(generateGunshot(durationMs = 150, baseFreq = 90f, noiseWeight = 0.9f))
                trackSniper = createTrack(generateGunshot(durationMs = 180, baseFreq = 120f, noiseWeight = 0.6f))
                trackHit = createTrack(generateHit(durationMs = 50, freq = 120f))
                trackDeath = createTrack(generateGroan(durationMs = 220))
                trackHurt = createTrack(generateHurt(durationMs = 140))
                trackReload = createTrack(generateReload(durationMs = 120))
                trackCombo = createTrack(generateChime(durationMs = 200))
                trackBossRoar = createTrack(generateGroan(durationMs = 450, baseFreq = 65f))
                trackBossSlam = createTrack(generateGunshot(durationMs = 240, baseFreq = 55f, noiseWeight = 0.85f))
            } catch (_: Exception) {
                // Silently fallback if device audio system is unavailable
            }
        }
    }

    private fun play(context: Context, track: AudioTrack?) {
        if (track == null) return
        if (!GameSettings.isSoundEnabled(context)) return

        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.reloadStaticData()
            track.play()
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    fun playGunshot(context: Context, weaponType: String) {
        when (weaponType.lowercase()) {
            "submachine gun", "smg" -> play(context, trackSmg)
            "combat shotgun", "shotgun" -> play(context, trackShotgun)
            "heavy sniper rifle", "sniper rifle", "sniper" -> play(context, trackSniper)
            else -> play(context, trackRifle)
        }
    }

    fun playZombieHit(context: Context) = play(context, trackHit)
    fun playZombieDeath(context: Context) = play(context, trackDeath)
    fun playPlayerHurt(context: Context) = play(context, trackHurt)
    fun playReload(context: Context) = play(context, trackReload)
    fun playCombo(context: Context) = play(context, trackCombo)
    fun playBossRoar(context: Context) = play(context, trackBossRoar)
    fun playBossSlam(context: Context) = play(context, trackBossSlam)

    private fun createTrack(samples: ShortArray): AudioTrack? {
        return try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track
        } catch (_: Exception) {
            null
        }
    }

    private fun generateGunshot(durationMs: Int, baseFreq: Float, noiseWeight: Float): ShortArray {
        val count = (SAMPLE_RATE * (durationMs / 1000f)).toInt().coerceAtLeast(100)
        val buffer = ShortArray(count)
        val rnd = Random(42)

        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = i.toFloat() / count
            val envelope = exp(-progress * 12.0).toFloat()

            val tone = sin(2.0 * PI * (baseFreq * (1.0 - progress * 0.7)) * t).toFloat()
            val noise = (rnd.nextFloat() * 2f - 1f)

            val mixed = (tone * (1f - noiseWeight) + noise * noiseWeight) * envelope
            buffer[i] = (mixed.coerceIn(-1f, 1f) * 28000).toInt().toShort()
        }
        return buffer
    }

    private fun generateHit(durationMs: Int, freq: Float): ShortArray {
        val count = (SAMPLE_RATE * (durationMs / 1000f)).toInt().coerceAtLeast(100)
        val buffer = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = i.toFloat() / count
            val envelope = exp(-progress * 18.0).toFloat()
            val sample = sin(2.0 * PI * (freq * (1.0 - progress * 0.5)) * t).toFloat() * envelope
            buffer[i] = (sample.coerceIn(-1f, 1f) * 26000).toInt().toShort()
        }
        return buffer
    }

    private fun generateGroan(durationMs: Int, baseFreq: Float = 95f): ShortArray {
        val count = (SAMPLE_RATE * (durationMs / 1000f)).toInt().coerceAtLeast(100)
        val buffer = ShortArray(count)
        val rnd = Random(13)
        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = i.toFloat() / count
            val envelope = sin(progress * PI.toFloat()).coerceAtLeast(0f) * exp(-progress * 2.5).toFloat()
            val pitch = baseFreq * (1.0f - progress * 0.4f)
            val tone = sin(2.0 * PI * pitch * t).toFloat()
            val grit = (rnd.nextFloat() * 2f - 1f) * 0.25f
            buffer[i] = ((tone + grit) * envelope * 22000).toInt().toShort()
        }
        return buffer
    }

    private fun generateHurt(durationMs: Int): ShortArray {
        val count = (SAMPLE_RATE * (durationMs / 1000f)).toInt().coerceAtLeast(100)
        val buffer = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = i.toFloat() / count
            val envelope = exp(-progress * 8.0).toFloat()
            val sample = sin(2.0 * PI * 80.0 * t).toFloat() * envelope
            buffer[i] = (sample.coerceIn(-1f, 1f) * 30000).toInt().toShort()
        }
        return buffer
    }

    private fun generateReload(durationMs: Int): ShortArray {
        val count = (SAMPLE_RATE * (durationMs / 1000f)).toInt().coerceAtLeast(100)
        val buffer = ShortArray(count)
        val rnd = Random(7)
        for (i in 0 until count) {
            val progress = i.toFloat() / count
            // Double click at 20% and 70%
            val click1 = exp(-((progress - 0.2) * 50.0).let { it * it }).toFloat()
            val click2 = exp(-((progress - 0.7) * 50.0).let { it * it }).toFloat()
            val noise = (rnd.nextFloat() * 2f - 1f) * (click1 + click2)
            buffer[i] = (noise.coerceIn(-1f, 1f) * 24000).toInt().toShort()
        }
        return buffer
    }

    private fun generateChime(durationMs: Int): ShortArray {
        val count = (SAMPLE_RATE * (durationMs / 1000f)).toInt().coerceAtLeast(100)
        val buffer = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / SAMPLE_RATE
            val progress = i.toFloat() / count
            val envelope = exp(-progress * 6.0).toFloat()
            val freq = if (progress < 0.45f) 523.25f else 783.99f // C5 to G5 fanfare
            val sample = sin(2.0 * PI * freq * t).toFloat() * envelope
            buffer[i] = (sample.coerceIn(-1f, 1f) * 20000).toInt().toShort()
        }
        return buffer
    }
}
