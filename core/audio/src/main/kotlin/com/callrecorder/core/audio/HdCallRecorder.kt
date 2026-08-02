package com.callrecorder.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.callrecorder.core.audio.dsp.NoiseReductionPipeline
import com.callrecorder.core.audio.encode.AacMediaEncoder
import com.callrecorder.core.audio.effects.AndroidAudioEffects
import com.callrecorder.core.audio.vad.CallAnswerDetector
import com.callrecorder.core.domain.model.AudioQuality
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * HD call capture (PCM) pipeline.
 *
 * Prefer [MediaRecorder]-based capture for telephony; use this when PCM is available.
 *
 * Encode arms as soon as a short noise profile is learned (or max brief wait for
 * outgoing answer detect). Never blocks for long — empty call = silent file, not
 * "nothing saved".
 */
class HdCallRecorder {

    private val effects = AndroidAudioEffects()
    private var audioRecord: AudioRecord? = null
    private var encoder: AacMediaEncoder? = null
    private var pipeline: NoiseReductionPipeline? = null
    private var answerDetector: CallAnswerDetector? = null
    private var captureThread: Thread? = null

    private val recording = AtomicBoolean(false)
    private val stopRequested = AtomicBoolean(false)

    @Volatile private var recordStartElapsedMs = 0L
    @Volatile private var encodingArmed = false
    @Volatile private var actualSampleRate = 44_100

    val isRecording: Boolean get() = recording.get()

    @SuppressLint("MissingPermission")
    fun start(
        filePath: String,
        quality: AudioQuality,
        waitForAnswer: Boolean = false,
    ): Result<Unit> {
        if (!recording.compareAndSet(false, true)) {
            return Result.success(Unit)
        }
        stopRequested.set(false)
        encodingArmed = false
        recordStartElapsedMs = 0L

        val config = AudioCaptureConfig.from(quality, waitForAnswer)

        return try {
            val opened = openAudioRecord(preferredRates(config.sampleRate))
                ?: return failStart(IllegalStateException("No AudioRecord source available"))

            audioRecord = opened.record
            actualSampleRate = opened.sampleRate
            effects.attach(opened.record)

            pipeline = NoiseReductionPipeline(opened.sampleRate, config.fftSize).also { it.reset() }
            answerDetector = CallAnswerDetector().also { it.reset() }

            // Match encoder to the rate we actually got from the device
            encoder = AacMediaEncoder(filePath, opened.sampleRate, config.bitrate)

            opened.record.startRecording()
            if (opened.record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                return failStart(IllegalStateException("AudioRecord failed to enter RECORDING state"))
            }

            val frameSamples = (opened.sampleRate * FRAME_MS / 1000).coerceAtLeast(160)
            captureThread = thread(name = "HdCallCapture", isDaemon = true) {
                captureLoop(config, frameSamples, waitForAnswer)
            }

            Timber.i(
                "HdCallRecorder started waitForAnswer=$waitForAnswer rate=${opened.sampleRate} br=${config.bitrate}"
            )
            Result.success(Unit)
        } catch (e: SecurityException) {
            failStart(e)
        } catch (e: Exception) {
            failStart(e)
        }
    }

    fun stop(): Result<Long> {
        if (!recording.get()) {
            return Result.failure(IllegalStateException("No active recording"))
        }
        stopRequested.set(true)
        try {
            captureThread?.join(5_000)
        } catch (_: InterruptedException) {
        }
        captureThread = null

        val duration = try {
            encoder?.stop() ?: 0L
        } catch (e: Exception) {
            Timber.e(e, "Encoder stop failed")
            if (recordStartElapsedMs > 0L) System.currentTimeMillis() - recordStartElapsedMs else 0L
        }

        releaseInternal()
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
        releaseInternal()
        recording.set(false)
    }

    private fun captureLoop(
        config: AudioCaptureConfig,
        frameSamples: Int,
        waitForAnswer: Boolean,
    ) {
        val record = audioRecord ?: return
        val shortBuf = ShortArray(frameSamples)
        val floatBuf = FloatArray(frameSamples)
        // Soft outgoing skip: never wait longer than a few seconds
        val maxWaitMs = if (waitForAnswer) FORCE_ARM_OUTGOING_MS else FORCE_ARM_INCOMING_MS
        val deadlineMs = System.currentTimeMillis() + maxWaitMs
        var frames = 0

        try {
            while (!stopRequested.get()) {
                val read = record.read(shortBuf, 0, frameSamples)
                if (read <= 0) {
                    if (read == AudioRecord.ERROR_INVALID_OPERATION || read == AudioRecord.ERROR_BAD_VALUE) {
                        Timber.e("AudioRecord read error: $read")
                        break
                    }
                    continue
                }
                frames++
                NoiseReductionPipeline.shortsToFloat(shortBuf, read, floatBuf)
                val frame = floatBuf.copyOf(read)
                val denoise = pipeline ?: continue
                val detector = answerDetector
                val aac = encoder

                if (!encodingArmed) {
                    denoise.learnFrame(frame.copyOf())

                    val speech = if (waitForAnswer) detector?.onFrame(frame) == true else false
                    val timedOut = System.currentTimeMillis() >= deadlineMs
                    val learnedEnough = denoise.noiseFramesLearned >= MIN_LEARN_FRAMES
                    // Always arm: speech, or enough learn frames (incoming), or short timeout
                    val ready = when {
                        timedOut -> true
                        !waitForAnswer && learnedEnough -> true
                        waitForAnswer && learnedEnough && speech -> true
                        else -> false
                    }

                    if (ready) {
                        denoise.lockNoiseProfile()
                        armEncoder(aac)
                        encodingArmed = true
                        Timber.i(
                            "Encode armed speech=$speech timeout=$timedOut wait=$waitForAnswer " +
                                "noiseFrames=${denoise.noiseFramesLearned}"
                        )
                    }
                    continue
                }

                val cleaned = denoise.process(frame.copyOf())
                val out = ShortArray(cleaned.size.coerceAtLeast(1))
                val written = NoiseReductionPipeline.floatToShorts(cleaned, out)
                aac?.encodePcm(out, written)
            }
        } catch (e: Exception) {
            Timber.e(e, "Capture loop crashed")
        } finally {
            try {
                record.stop()
            } catch (_: Exception) {
            }
        }
    }

    private fun armEncoder(aac: AacMediaEncoder?) {
        if (aac == null) return
        try {
            aac.start()
            recordStartElapsedMs = System.currentTimeMillis()
        } catch (e: Exception) {
            Timber.e(e, "Failed to arm AAC encoder")
        }
    }

    private data class OpenedRecord(val record: AudioRecord, val sampleRate: Int)

    @SuppressLint("MissingPermission")
    private fun openAudioRecord(sampleRates: IntArray): OpenedRecord? {
        val sources = intArrayOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_CALL,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.DEFAULT,
        )
        for (rate in sampleRates) {
            for (source in sources) {
                try {
                    val minBuf = AudioRecord.getMinBufferSize(
                        rate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                    )
                    if (minBuf <= 0) continue
                    val bufferSize = minBuf * 4
                    @Suppress("DEPRECATION")
                    val rec = AudioRecord(
                        source,
                        rate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                    )
                    if (rec.state == AudioRecord.STATE_INITIALIZED) {
                        Timber.d("AudioRecord opened source=$source rate=$rate buf=$bufferSize")
                        return OpenedRecord(rec, rate)
                    }
                    rec.release()
                } catch (e: Exception) {
                    Timber.w(e, "AudioRecord source $source @ $rate failed")
                }
            }
        }
        return null
    }

    private fun preferredRates(preferred: Int): IntArray {
        val common = intArrayOf(preferred, 44_100, 48_000, 16_000, 8_000)
        return common.distinct().toIntArray()
    }

    private fun failStart(e: Exception): Result<Unit> {
        Timber.e(e, "HdCallRecorder start failed")
        releaseInternal()
        recording.set(false)
        return Result.failure(e)
    }

    private fun releaseInternal() {
        effects.release()
        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null
        encoder = null
        pipeline?.reset()
        pipeline = null
        answerDetector = null
        encodingArmed = false
        recordStartElapsedMs = 0L
    }

    companion object {
        private const val FRAME_MS = 20
        private const val MIN_LEARN_FRAMES = 10
        /** Force encode so short calls are never lost if VAD never hears speech. */
        private const val FORCE_ARM_OUTGOING_MS = 4_000L
        private const val FORCE_ARM_INCOMING_MS = 300L
    }
}
