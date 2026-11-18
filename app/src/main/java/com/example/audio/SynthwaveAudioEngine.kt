package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural real-time 124 BPM Synthwave PCM synthesizer & SFX engine.
 * Works 100% offline on Android 7.0+ (API 24+) without external audio files.
 */
class SynthwaveAudioEngine {

    enum class SfxType {
        NONE,
        ORB,
        NEAR_MISS,
        CRASH,
        CLEAR
    }

    @Volatile
    var musicEnabled: Boolean = true

    @Volatile
    var sfxEnabled: Boolean = true

    @Volatile
    var isGameplayActive: Boolean = false

    @Volatile
    var isTurning: Boolean = false

    @Volatile
    var intensity: Float = 0.25f

    private val isRunning = AtomicBoolean(false)
    private val pendingSfx = AtomicReference(SfxType.NONE)
    private val sfxTriggerCounter = AtomicInteger(0)
    private var audioThread: Thread? = null

    fun triggerSfx(type: SfxType) {
        if (!sfxEnabled) return
        pendingSfx.set(type)
        sfxTriggerCounter.incrementAndGet()
    }

    fun start() {
        if (isRunning.getAndSet(true)) return
        audioThread = Thread({
            runAudioLoop()
        }, "GhostLine-SynthAudio").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            audioThread?.join(350)
        } catch (_: InterruptedException) {
        }
        audioThread = null
    }

    private fun runAudioLoop() {
        val sampleRate = 22050
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) {
            isRunning.set(false)
            return
        }

        val bufferSamples = 512
        val byteBufferSize = maxOf(minBuf, bufferSamples * 4)

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(byteBufferSize)
                .build()
        } catch (e: Exception) {
            Log.w("SynthAudio", "AudioTrack unavailable: ${e.message}")
            isRunning.set(false)
            return
        }

        try {
            track.play()
        } catch (e: Exception) {
            Log.w("SynthAudio", "AudioTrack play failed: ${e.message}")
            track.release()
            isRunning.set(false)
            return
        }

        val pcm = ShortArray(bufferSamples)
        val rng = Random(42)

        // 124 BPM -> 16th note duration = 60 / 124 / 4 = 0.120967s
        val samplesPerStep = (sampleRate * 60.0 / 124.0 / 4.0).toInt()
        var sampleIndex = 0L

        // A minor / Outrun chord progression: Am (A2), F (F2), G (G2), Em (E2)
        val bassRootsHz = doubleArrayOf(
            110.00, 110.00, 110.00, 110.00, 110.00, 110.00, 110.00, 110.00,
            87.31,  87.31,  87.31,  87.31,  87.31,  87.31,  87.31,  87.31,
            98.00,  98.00,  98.00,  98.00,  98.00,  98.00,  98.00,  98.00,
            82.41,  82.41,  82.41,  82.41,  82.41,  82.41,  82.41,  82.41
        )
        // Shimmering synthwave arp notes (Hz)
        val arpPatternHz = doubleArrayOf(
            440.00, 523.25, 659.25, 880.00, 659.25, 523.25, 440.00, 659.25,
            349.23, 440.00, 523.25, 698.46, 523.25, 440.00, 349.23, 523.25,
            392.00, 493.88, 587.33, 783.99, 587.33, 493.88, 392.00, 587.33,
            329.63, 392.00, 493.88, 659.25, 493.88, 392.00, 329.63, 493.88
        )

        var bassPhase = 0.0
        var arpPhase = 0.0
        var padPhase = 0.0
        var turnHumPhase = 0.0
        var lpState = 0.0

        var activeSfx = SfxType.NONE
        var sfxAgeSamples = 0
        var sfxPhase = 0.0
        var lastSfxCounter = sfxTriggerCounter.get()

        while (isRunning.get()) {
            val currentCounter = sfxTriggerCounter.get()
            if (currentCounter != lastSfxCounter) {
                lastSfxCounter = currentCounter
                val next = pendingSfx.getAndSet(SfxType.NONE)
                if (next != SfxType.NONE) {
                    activeSfx = next
                    sfxAgeSamples = 0
                    sfxPhase = 0.0
                }
            }

            val playMusic = musicEnabled
            val playSfx = sfxEnabled
            val gameplay = isGameplayActive
            val turning = isTurning
            val energy = intensity.coerceIn(0f, 1f)

            for (i in 0 until bufferSamples) {
                var mix = 0.0

                if (playMusic) {
                    val step = ((sampleIndex / samplesPerStep) % 32).toInt()
                    val stepPos = (sampleIndex % samplesPerStep).toDouble() / samplesPerStep.toDouble()
                    val step16 = step % 16

                    // 1. Driving 16th-note Synthwave Bass
                    val bassHz = bassRootsHz[step]
                    bassPhase = (bassPhase + bassHz / sampleRate) % 1.0
                    val rawSaw = (bassPhase * 2.0 - 1.0)
                    val bassEnv = (1.0 - stepPos * 0.65).coerceAtLeast(0.18)
                    val cutoff = if (gameplay) (0.16 + energy * 0.22 + if (turning) 0.12 else 0.0) else 0.12
                    lpState += cutoff * (rawSaw - lpState)
                    mix += lpState * bassEnv * (if (gameplay) 0.34 else 0.20)

                    // 2. Warm analog fifth pad
                    val padHz = bassHz * 1.5
                    padPhase = (padPhase + padHz / sampleRate) % 1.0
                    val padWave = sin(padPhase * 2.0 * PI)
                    mix += padWave * 0.08

                    // 3. Punchy Retro Drums (Kick on 0,4,8,12 | Gated snare on 4,12 | Hat on offbeats)
                    if (step % 4 == 0 && stepPos < 0.85) {
                        val kickEnv = (1.0 - stepPos * 1.15).coerceAtLeast(0.0)
                        val kickFreq = 48.0 + 115.0 * kickEnv * kickEnv
                        val kickWave = sin(2.0 * PI * kickFreq * (stepPos * samplesPerStep / sampleRate))
                        mix += kickWave * kickEnv * (if (gameplay) 0.38 else 0.20)
                    }
                    if ((step16 == 4 || step16 == 12) && gameplay && stepPos < 0.75) {
                        val snareEnv = (1.0 - stepPos * 1.3).coerceAtLeast(0.0)
                        val noise = rng.nextDouble(-1.0, 1.0)
                        mix += noise * snareEnv * 0.16
                    }
                    if (step % 2 == 1 && gameplay && stepPos < 0.35) {
                        val hatEnv = (1.0 - stepPos * 2.8).coerceAtLeast(0.0)
                        mix += rng.nextDouble(-1.0, 1.0) * hatEnv * 0.055
                    }

                    // 4. Lead Arpeggiator during active run
                    if (gameplay) {
                        val arpHz = arpPatternHz[step]
                        arpPhase = (arpPhase + arpHz / sampleRate) % 1.0
                        val arpSquare = if (arpPhase < 0.35) 1.0 else -1.0
                        val arpEnv = (1.0 - stepPos * 0.9).coerceAtLeast(0.0)
                        mix += arpSquare * arpEnv * (0.07 + 0.06 * energy)
                    }
                }

                // 5. Subtle turning synth resonance when player holds screen
                if (playSfx && gameplay && turning) {
                    val humHz = 220.0 + energy * 140.0
                    turnHumPhase = (turnHumPhase + humHz / sampleRate) % 1.0
                    mix += sin(turnHumPhase * 2.0 * PI) * 0.055
                }

                // 6. One-shot SFX Synthesis
                if (playSfx && activeSfx != SfxType.NONE) {
                    val t = sfxAgeSamples.toDouble() / sampleRate.toDouble()
                    when (activeSfx) {
                        SfxType.ORB -> {
                            if (t < 0.18) {
                                val f = if (t < 0.07) 880.0 else 1318.5
                                sfxPhase = (sfxPhase + f / sampleRate) % 1.0
                                val env = (1.0 - t / 0.18)
                                mix += sin(sfxPhase * 2.0 * PI) * env * 0.32
                            } else {
                                activeSfx = SfxType.NONE
                            }
                        }
                        SfxType.NEAR_MISS -> {
                            if (t < 0.20) {
                                val f = 1046.5 + t * 1400.0
                                sfxPhase = (sfxPhase + f / sampleRate) % 1.0
                                val env = (1.0 - t / 0.20)
                                mix += (if (sfxPhase < 0.5) 1.0 else -1.0) * env * 0.20
                            } else {
                                activeSfx = SfxType.NONE
                            }
                        }
                        SfxType.CRASH -> {
                            if (t < 0.42) {
                                val env = (1.0 - t / 0.42)
                                val sweepHz = (240.0 * (1.0 - t / 0.42)).coerceAtLeast(36.0)
                                sfxPhase = (sfxPhase + sweepHz / sampleRate) % 1.0
                                val sub = sin(sfxPhase * 2.0 * PI)
                                val noise = rng.nextDouble(-1.0, 1.0)
                                mix += (sub * 0.55 + noise * 0.45) * env * 0.52
                            } else {
                                activeSfx = SfxType.NONE
                            }
                        }
                        SfxType.CLEAR -> {
                            if (t < 0.55) {
                                val f = when {
                                    t < 0.12 -> 523.25
                                    t < 0.24 -> 659.25
                                    t < 0.36 -> 783.99
                                    else -> 1046.50
                                }
                                sfxPhase = (sfxPhase + f / sampleRate) % 1.0
                                val env = (1.0 - t / 0.55)
                                mix += sin(sfxPhase * 2.0 * PI) * env * 0.34
                            } else {
                                activeSfx = SfxType.NONE
                            }
                        }
                        SfxType.NONE -> Unit
                    }
                    sfxAgeSamples++
                }

                val clamped = mix.coerceIn(-0.92, 0.92)
                pcm[i] = (clamped * 32767.0).toInt().toShort()
                sampleIndex++
            }

            if (isRunning.get()) {
                val written = track.write(pcm, 0, bufferSamples)
                if (written < 0) break
            }
        }

        try {
            track.stop()
        } catch (_: Exception) {
        }
        track.release()
    }
}
