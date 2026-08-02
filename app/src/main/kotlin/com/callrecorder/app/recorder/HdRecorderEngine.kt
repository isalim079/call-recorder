package com.callrecorder.app.recorder

import com.callrecorder.core.audio.HdCallRecorder
import com.callrecorder.core.domain.model.AudioQuality
import timber.log.Timber
import javax.inject.Inject

/**
 * Primary recording engine: HD PCM path with denoise + answer detection.
 * Falls back to classic [MediaRecorderEngine] if AudioRecord cannot start.
 */
class HdRecorderEngine @Inject constructor(
    private val fallback: MediaRecorderEngine,
) : AudioRecorderEngine {

    private val hd = HdCallRecorder()
    @Volatile private var usingFallback = false

    override val isRecording: Boolean
        get() = if (usingFallback) fallback.isRecording else hd.isRecording

    override fun startRecording(
        filePath: String,
        quality: AudioQuality,
        waitForAnswer: Boolean,
    ): Result<Unit> {
        usingFallback = false
        val hdResult = hd.start(filePath, quality, waitForAnswer = waitForAnswer)
        if (hdResult.isSuccess) {
            Timber.i("Recording via HdCallRecorder (denoise+VAD)")
            return hdResult
        }

        Timber.w(hdResult.exceptionOrNull(), "HD pipeline failed — falling back to MediaRecorder")
        usingFallback = true
        return fallback.startRecording(filePath, quality, waitForAnswer)
            .onFailure { Timber.e(it, "MediaRecorder fallback also failed") }
    }

    override fun stopRecording(): Result<Long> {
        return if (usingFallback) {
            fallback.stopRecording()
        } else {
            hd.stop().fold(
                onSuccess = { Result.success(it) },
                onFailure = {
                    Timber.e(it, "HdCallRecorder stop failed")
                    Result.failure(RecorderError.Unknown(it.message, it))
                },
            )
        }.also { usingFallback = false }
    }

    override fun releaseResources() {
        try {
            hd.release()
        } catch (_: Exception) {
        }
        try {
            fallback.releaseResources()
        } catch (_: Exception) {
        }
        usingFallback = false
    }
}
