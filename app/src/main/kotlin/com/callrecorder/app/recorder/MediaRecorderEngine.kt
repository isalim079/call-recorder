package com.callrecorder.app.recorder

import android.media.MediaRecorder
import android.os.Build
import com.callrecorder.core.domain.model.AudioQuality
import timber.log.Timber
import javax.inject.Inject

/**
 * Production implementation of [AudioRecorderEngine] using Android [MediaRecorder].
 *
 * Audio source strategy (OEM compatibility):
 * 1. Try [MediaRecorder.AudioSource.VOICE_CALL] — records both sides of the call.
 *    Works on Samsung, some Xiaomi/MIUI, and rooted devices.
 * 2. Fall back to [MediaRecorder.AudioSource.VOICE_COMMUNICATION] — records
 *    the microphone optimised for VoIP (typically only the local side).
 * 3. Last resort: [MediaRecorder.AudioSource.MIC] — unprocessed microphone.
 *
 * Output format: MPEG_4 container (.m4a)
 * Audio codec: AAC
 *
 * This is a stateful, non-thread-safe object. Use it from a single thread/coroutine.
 */
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Suppress("DEPRECATION")
class MediaRecorderEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioRecorderEngine {

    private var mediaRecorder: MediaRecorder? = null
    private var recordingStartMs: Long = 0L

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
        // MediaRecorder cannot delay until remote answer; waitForAnswer is ignored here.
        if (waitForAnswer) {
            Timber.d("MediaRecorder fallback cannot skip ringback (no PCM access)")
        }

        return tryWithSource(
            filePath  = filePath,
            quality   = quality,
            sources   = listOf(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                MediaRecorder.AudioSource.VOICE_CALL,
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                MediaRecorder.AudioSource.MIC,
                MediaRecorder.AudioSource.CAMCORDER,
                MediaRecorder.AudioSource.DEFAULT
            )
        )
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
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun createMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)  // API 31+
        } else {
            MediaRecorder()
        }

    private fun tryWithSource(
        filePath: String,
        quality: AudioQuality,
        sources: List<Int>,
    ): Result<Unit> {
        for (source in sources) {
            val result = attemptStartWithSource(filePath, quality, source)
            if (result.isSuccess) {
                Timber.d("Recording started with audio source: $source")
                return result
            }
            Timber.w("Audio source $source failed, trying next...")
        }
        releaseResources()
        return Result.failure(RecorderError.MicrophoneBusy)
    }

    private fun attemptStartWithSource(
        filePath: String,
        quality: AudioQuality,
        audioSource: Int,
    ): Result<Unit> {
        return try {
            @Suppress("DEPRECATION")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // The constructor accepting Context is preferred on API 31+
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(quality.bitrate)
                setAudioSamplingRate(quality.sampleRate)
                setAudioChannels(1)  // Mono — adequate for voice calls
                setOutputFile(filePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartMs = System.currentTimeMillis()
            Result.success(Unit)
        } catch (e: SecurityException) {
            Timber.w(e, "SecurityException for audio source $audioSource")
            mediaRecorder?.let { it.reset(); it.release() }
            mediaRecorder = null
            Result.failure(RecorderError.PermissionDenied)
        } catch (e: Exception) {
            Timber.w(e, "Failed to start recording with source $audioSource")
            try { mediaRecorder?.let { it.reset(); it.release() } } catch (_: Exception) {}
            mediaRecorder = null
            Result.failure(RecorderError.Unknown(e.message, e))
        }
    }
}
