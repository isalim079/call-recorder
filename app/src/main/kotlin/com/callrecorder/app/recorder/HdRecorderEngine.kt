package com.callrecorder.app.recorder

import com.callrecorder.core.audio.HdCallRecorder
import com.callrecorder.core.domain.model.AudioQuality
import timber.log.Timber
import javax.inject.Inject

/**
 * Recording engine used by [com.callrecorder.app.service.CallRecorderService].
 *
 * Android often blocks or zeros third-party [android.media.AudioRecord] during
 * telephony, while [MediaRecorder] + VOICE_RECOGNITION still works. So:
 * 1. **Primary:** proven [MediaRecorderEngine] (always records during OFFHOOK)
 * 2. **Optional HD attempt:** only if MediaRecorder fails
 *
 * Denoise / answer-detect in [HdCallRecorder] stay available as secondary path
 * when the device actually delivers PCM during a call.
 */
class HdRecorderEngine @Inject constructor(
    private val mediaRecorder: MediaRecorderEngine,
) : AudioRecorderEngine {

    private val hd = HdCallRecorder()
    @Volatile private var usingHd = false

    override val isRecording: Boolean
        get() = if (usingHd) hd.isRecording else mediaRecorder.isRecording

    override fun startRecording(
        filePath: String,
        quality: AudioQuality,
        waitForAnswer: Boolean,
    ): Result<Unit> {
        usingHd = false

        // Always prefer MediaRecorder — that is what worked before the HD rewrite.
        val mediaResult = mediaRecorder.startRecording(filePath, quality, waitForAnswer)
        if (mediaResult.isSuccess) {
            Timber.i("Recording via MediaRecorder (reliable call path)")
            return mediaResult
        }

        Timber.w(mediaResult.exceptionOrNull(), "MediaRecorder failed — trying HD PCM path")
        usingHd = true
        // Force encode soon even on outgoing (do not sit silent for whole call)
        return hd.start(filePath, quality, waitForAnswer = false)
            .onFailure { Timber.e(it, "HD PCM path also failed") }
            .onSuccess { Timber.i("Recording via HdCallRecorder (PCM path)") }
    }

    override fun stopRecording(): Result<Long> {
        return if (usingHd) {
            hd.stop().fold(
                onSuccess = { Result.success(it) },
                onFailure = {
                    Result.failure(RecorderError.Unknown(it.message, it))
                },
            )
        } else {
            mediaRecorder.stopRecording()
        }.also { usingHd = false }
    }

    override fun releaseResources() {
        try {
            hd.release()
        } catch (_: Exception) {
        }
        try {
            mediaRecorder.releaseResources()
        } catch (_: Exception) {
        }
        usingHd = false
    }
}
