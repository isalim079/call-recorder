package com.callrecorder.core.audio.capture

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import com.callrecorder.core.audio.dsp.CallAgc
import com.callrecorder.core.audio.dsp.NoiseReductionPipeline
import com.callrecorder.core.audio.dsp.ProductionSpeechChain
import com.callrecorder.core.audio.encode.AacMediaEncoder
import com.callrecorder.core.domain.model.AudioQuality
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Production call capture engine — same model used by serious call recorders:
 *
 * 1. Prefer [MediaRecorder.AudioSource.VOICE_CALL] — uplink+downlink mix from the
 *    telephony HAL (what Google/OEM dialers and BCR capture). No remote announcement.
 * 2. Fall through to VOICE_COMMUNICATION / VOICE_RECOGNITION / MIC when OEM blocks
 *    VOICE_CALL for unprivileged apps.
 * 3. [AudioRecord] PCM path → AGC → optional denoise → HD AAC encode.
 *
 * Live DSP (unlike post-only enhance) so levels stay speech-loud during record.
 */
class VoiceCallCaptureEngine(
    private val context: Context,
) {
    private val recording = AtomicBoolean(false)
    private val stopRequested = AtomicBoolean(false)

    private var audioRecord: AudioRecord? = null
    private var encoder: AacMediaEncoder? = null
    private var captureThread: Thread? = null
    private var previousAudioMode = AudioManager.MODE_NORMAL
    private var sampleRateUsed = 44_100
    private var sourceUsed = -1

    val isRecording: Boolean get() = recording.get()
    val activeAudioSource: Int get() = sourceUsed

    @SuppressLint("MissingPermission")
    fun start(filePath: String, quality: AudioQuality, applyLiveDenoise: Boolean): Result<Unit> {
        if (!recording.compareAndSet(false, true)) return Result.success(Unit)
        stopRequested.set(false)

        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        previousAudioMode = am.mode
        // Put audio policy into call path — critical for VOICE_CALL routing on many OEMs
        try {
            am.mode = AudioManager.MODE_IN_CALL
        } catch (e: Exception) {
            Timber.w(e, "MODE_IN_CALL failed")
        }

        val rates = preferRates(quality.sampleRate)
        val sources = callSources()

        var opened: Opened? = null
        for (rate in rates) {
            for (source in sources) {
                opened = openRecord(source, rate)
                if (opened != null) {
                    Timber.i("VoiceCallCapture opened source=$source (${sourceName(source)}) rate=$rate")
                    break
                }
            }
            if (opened != null) break
        }

        if (opened == null) {
            restoreAudioMode(am)
            recording.set(false)
            return Result.failure(IllegalStateException("No telephony-capable AudioRecord source"))
        }

        audioRecord = opened.record
        sampleRateUsed = opened.sampleRate
        sourceUsed = opened.source

        val bitrate = quality.bitrate.coerceIn(128_000, 320_000)
        val enc = AacMediaEncoder(filePath, opened.sampleRate, bitrate)
        try {
            enc.start()
        } catch (e: Exception) {
            safeReleaseRecord()
            restoreAudioMode(am)
            recording.set(false)
            return Result.failure(e)
        }
        encoder = enc

        try {
            opened.record.startRecording()
        } catch (e: Exception) {
            enc.release()
            safeReleaseRecord()
            restoreAudioMode(am)
            recording.set(false)
            return Result.failure(e)
        }

        captureThread = thread(name = "VoiceCallCapture", isDaemon = true) {
            runCapture(opened.sampleRate, applyLiveDenoise)
        }
        return Result.success(Unit)
    }

    fun stop(): Result<Long> {
        if (!recording.get()) return Result.failure(IllegalStateException("Not recording"))
        stopRequested.set(true)
        try {
            captureThread?.join(6_000)
        } catch (_: InterruptedException) {
        }
        captureThread = null

        val duration = try {
            encoder?.stop() ?: 0L
        } catch (e: Exception) {
            Timber.e(e, "Encoder stop")
            0L
        }

        safeReleaseRecord()
        encoder = null
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        restoreAudioMode(am)
        recording.set(false)
        return Result.success(duration.coerceAtLeast(0L))
    }

    fun release() {
        stopRequested.set(true)
        try {
            captureThread?.join(2_000)
        } catch (_: Exception) {
        }
        try {
            encoder?.release()
        } catch (_: Exception) {
        }
        safeReleaseRecord()
        encoder = null
        recording.set(false)
    }

    private fun runCapture(sampleRate: Int, applyLiveDenoise: Boolean) {
        val record = audioRecord ?: return
        val frame = (sampleRate * 20 / 1000).coerceAtLeast(320)
        val shortBuf = ShortArray(frame)
        val floatBuf = FloatArray(frame)
        val outShort = ShortArray(frame * 2)
        val agc = CallAgc(targetRms = 0.20f, maxGain = 14f)
        val denoise = if (applyLiveDenoise) {
            ProductionSpeechChain(sampleRate).also { it.reset() }
        } else null

        try {
            while (!stopRequested.get()) {
                val n = record.read(shortBuf, 0, frame)
                if (n <= 0) {
                    if (n == AudioRecord.ERROR_INVALID_OPERATION || n == AudioRecord.ERROR_BAD_VALUE) break
                    continue
                }
                NoiseReductionPipeline.shortsToFloat(shortBuf, n, floatBuf)
                val pcm = floatBuf.copyOf(n)

                // 1) Level speech first (fix user's "not loud enough")
                agc.process(pcm)

                // 2) Optional light denoise (does not replace post enhance)
                val cleaned = denoise?.process(pcm) ?: pcm

                val written = NoiseReductionPipeline.floatToShorts(cleaned, outShort)
                encoder?.encodePcm(outShort, written)
            }
            denoise?.flush()?.let { tail ->
                if (tail.isNotEmpty()) {
                    val written = NoiseReductionPipeline.floatToShorts(tail, outShort)
                    encoder?.encodePcm(outShort, written)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "VoiceCallCapture loop")
        } finally {
            try {
                record.stop()
            } catch (_: Exception) {
            }
        }
    }

    private data class Opened(val record: AudioRecord, val sampleRate: Int, val source: Int)

    @SuppressLint("MissingPermission")
    private fun openRecord(source: Int, sampleRate: Int): Opened? {
        return try {
            val min = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (min <= 0) return null
            val buf = min * 4
            @Suppress("DEPRECATION")
            val rec = AudioRecord(
                source,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                buf,
            )
            if (rec.state != AudioRecord.STATE_INITIALIZED) {
                rec.release()
                return null
            }
            // Warm check: don't reject VOICE_CALL for initial silence (HAL often quiet until ACTIVE)
            Opened(rec, sampleRate, source)
        } catch (e: Exception) {
            Timber.w(e, "openRecord source=$source rate=$sampleRate failed")
            null
        }
    }

    private fun callSources(): IntArray {
        // Official call mix first. Never bury VOICE_CALL behind MIC.
        val list = mutableListOf(
            MediaRecorder.AudioSource.VOICE_CALL,          // uplink+downlink (privileged / some OEMs)
        )
        // OEM / Android extras
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // VOICE_PERFORMANCE not for calls
        }
        list += MediaRecorder.AudioSource.VOICE_COMMUNICATION // VoIP path / Ha/lo duplex
        list += MediaRecorder.AudioSource.VOICE_RECOGNITION   // accessibility elevated mic
        list += MediaRecorder.AudioSource.MIC
        list += MediaRecorder.AudioSource.CAMCORDER
        list += MediaRecorder.AudioSource.DEFAULT
        return list.toIntArray()
    }

    private fun preferRates(preferred: Int): IntArray {
        // Telephony HAL often supplies 8/16 kHz natively; also try 48/44.1 for HD encode path
        return intArrayOf(preferred, 48_000, 44_100, 16_000, 8_000).distinct().toIntArray()
    }

    private fun sourceName(source: Int): String = when (source) {
        MediaRecorder.AudioSource.VOICE_CALL -> "VOICE_CALL"
        MediaRecorder.AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
        MediaRecorder.AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
        MediaRecorder.AudioSource.MIC -> "MIC"
        else -> "SRC_$source"
    }

    private fun safeReleaseRecord() {
        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null
        sourceUsed = -1
    }

    private fun restoreAudioMode(am: AudioManager) {
        try {
            am.mode = previousAudioMode
        } catch (_: Exception) {
            try {
                am.mode = AudioManager.MODE_NORMAL
            } catch (_: Exception) {
            }
        }
    }
}
