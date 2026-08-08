package com.callrecorder.app.recorder

import android.content.Context
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import com.callrecorder.core.domain.model.AudioQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * MediaRecorder fallback for telephony capture.
 *
 * Rules (learned from BCR / AOSP dialer behaviour):
 * - Prefer [AudioSource.VOICE_CALL] — mixed call audio, no remote announcement
 * - Never reject VOICE_CALL solely because the first ~300ms are silent (HAL warm-up)
 * - Only reject pure MIC/DEFAULT if they are silent (useless files)
 */
@Suppress("DEPRECATION")
class MediaRecorderEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioRecorderEngine {

    private var mediaRecorder: MediaRecorder? = null
    private var recordingStartMs: Long = 0L
    private var previousMode: Int = AudioManager.MODE_NORMAL

    override val isRecording: Boolean
        get() = mediaRecorder != null

    override fun startRecording(
        filePath: String,
        quality: AudioQuality,
        waitForAnswer: Boolean,
    ): Result<Unit> {
        if (isRecording) {
            Timber.w("startRecording called while already recording — ignoring")
            return Result.success(Unit)
        }

        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        previousMode = am.mode
        try {
            am.mode = AudioManager.MODE_IN_CALL
        } catch (e: Exception) {
            Timber.w(e, "MODE_IN_CALL failed")
        }

        val result = tryWithSource(
            filePath = filePath,
            quality  = quality,
            sources  = listOf(
                MediaRecorder.AudioSource.VOICE_CALL,
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.CAMCORDER,
                MediaRecorder.AudioSource.DEFAULT,
            ),
        )
        if (result.isFailure) {
            restoreMode(am)
        }
        return result
    }

    override fun stopRecording(): Result<Long> {
        val recorder = mediaRecorder
            ?: return Result.failure(RecorderError.InvalidState("No active recording"))

        return try {
            recorder.stop()
            val durationMs = System.currentTimeMillis() - recordingStartMs
            Timber.d("Recording stopped. Duration: ${durationMs}ms")
            Result.success(durationMs)
        } catch (e: RuntimeException) {
            Timber.e(e, "Failed to stop MediaRecorder")
            Result.failure(RecorderError.Unknown(e.message, e))
        } finally {
            releaseResources()
        }
    }

    override fun releaseResources() {
        try {
            mediaRecorder?.apply {
                reset()
                release()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error releasing MediaRecorder")
        } finally {
            mediaRecorder = null
            recordingStartMs = 0L
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            restoreMode(am)
        }
    }

    private fun tryWithSource(
        filePath: String,
        quality: AudioQuality,
        sources: List<Int>,
    ): Result<Unit> {
        truncateFile(filePath)

        for ((index, source) in sources.withIndex()) {
            val isLast = index == sources.lastIndex
            // Never amplitude-kill telephony call mix — silent warm-up is normal
            val requireAmp = !isLast && !isTelephonyCallSource(source)
            val result = attemptStartWithSource(filePath, quality, source, requireAmp)
            if (result.isSuccess) {
                Timber.d("MediaRecorder started source=$source requireAmp=$requireAmp")
                return result
            }
            Timber.w("Audio source $source failed, trying next...")
            truncateFile(filePath)
        }
        releaseResources()
        return Result.failure(RecorderError.MicrophoneBusy)
    }

    private fun isTelephonyCallSource(source: Int): Boolean =
        source == MediaRecorder.AudioSource.VOICE_CALL ||
            source == MediaRecorder.AudioSource.VOICE_COMMUNICATION ||
            source == MediaRecorder.AudioSource.VOICE_RECOGNITION

    private fun attemptStartWithSource(
        filePath: String,
        quality: AudioQuality,
        audioSource: Int,
        requireAmplitude: Boolean,
    ): Result<Unit> {
        var recorder: MediaRecorder? = null
        return try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            val sampleRate = safeSampleRate(quality.sampleRate)
            // HD floor for call capture
            val bitrate = quality.bitrate.coerceIn(128_000, 320_000)

            recorder.apply {
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(bitrate)
                setAudioSamplingRate(sampleRate)
                setAudioChannels(1)
                setOutputFile(filePath)
                prepare()
                start()
            }

            if (requireAmplitude && !hasAudioSignal(recorder)) {
                Timber.w("Mic-like source $audioSource silent — skip")
                safeStopRelease(recorder)
                return Result.failure(RecorderError.Unknown("Silent mic source $audioSource"))
            }

            mediaRecorder = recorder
            recordingStartMs = System.currentTimeMillis()
            Result.success(Unit)
        } catch (e: SecurityException) {
            Timber.w(e, "SecurityException source=$audioSource")
            safeStopRelease(recorder)
            Result.failure(RecorderError.PermissionDenied)
        } catch (e: Exception) {
            Timber.w(e, "Failed source=$audioSource")
            safeStopRelease(recorder)
            Result.failure(RecorderError.Unknown(e.message, e))
        }
    }

    private fun hasAudioSignal(recorder: MediaRecorder): Boolean {
        return try {
            recorder.maxAmplitude
            var peak = 0
            repeat(6) {
                Thread.sleep(40)
                peak = maxOf(peak, recorder.maxAmplitude)
                if (peak >= 120) return true
            }
            peak >= 120
        } catch (e: Exception) {
            true
        }
    }

    private fun safeSampleRate(requested: Int): Int {
        val supported = intArrayOf(8_000, 16_000, 22_050, 32_000, 44_100, 48_000)
        return if (requested in supported) requested else 44_100
    }

    private fun safeStopRelease(recorder: MediaRecorder?) {
        if (recorder == null) return
        try { recorder.stop() } catch (_: Exception) {}
        try { recorder.reset() } catch (_: Exception) {}
        try { recorder.release() } catch (_: Exception) {}
    }

    private fun truncateFile(path: String) {
        try {
            File(path).takeIf { it.exists() }?.delete()
        } catch (_: Exception) {
        }
    }

    private fun restoreMode(am: AudioManager) {
        try {
            am.mode = previousMode
        } catch (_: Exception) {
            try { am.mode = AudioManager.MODE_NORMAL } catch (_: Exception) {}
        }
    }
}
