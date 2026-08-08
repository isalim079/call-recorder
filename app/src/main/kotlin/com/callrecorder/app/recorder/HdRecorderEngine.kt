package com.callrecorder.app.recorder

import android.content.Context
import com.callrecorder.core.audio.capture.VoiceCallCaptureEngine
import com.callrecorder.core.domain.model.AudioQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Production capture orchestration (OEM-style priority):
 *
 * 1. [VoiceCallCaptureEngine] — AudioRecord `VOICE_CALL` uplink+downlink when device allows,
 *    with live AGC for speech volume. Matches BCR / dialer model (no remote announcement).
 * 2. [MediaRecorderEngine] fallback — `VOICE_CALL` then recognition (still not mic-first).
 *
 * Critical: never prefer plain MIC over call stream when call stream initializes "quiet".
 */
class HdRecorderEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaRecorder: MediaRecorderEngine,
) : AudioRecorderEngine {

    private val voiceCall = VoiceCallCaptureEngine(context)
    @Volatile private var mode = Mode.NONE

    private enum class Mode { NONE, VOICE_CALL_PCM, MEDIA_RECORDER }

    override val isRecording: Boolean
        get() = when (mode) {
            Mode.VOICE_CALL_PCM -> voiceCall.isRecording
            Mode.MEDIA_RECORDER -> mediaRecorder.isRecording
            Mode.NONE -> false
        }

    override fun startRecording(
        filePath: String,
        quality: AudioQuality,
        waitForAnswer: Boolean,
    ): Result<Unit> {
        mode = Mode.NONE

        // 1) True call-audio path with AGC (preferred)
        val pcm = voiceCall.start(filePath, quality, applyLiveDenoise = true)
        if (pcm.isSuccess) {
            mode = Mode.VOICE_CALL_PCM
            Timber.i(
                "Recording via VoiceCallCaptureEngine source=${voiceCall.activeAudioSource} " +
                    "(VOICE_CALL=${android.media.MediaRecorder.AudioSource.VOICE_CALL})"
            )
            return pcm
        }
        Timber.w(pcm.exceptionOrNull(), "VoiceCall PCM path failed — MediaRecorder VOICE_CALL chain")

        // 2) MediaRecorder with VOICE_CALL first, no silent-reject on call sources
        val media = mediaRecorder.startRecording(filePath, quality, waitForAnswer)
        if (media.isSuccess) {
            mode = Mode.MEDIA_RECORDER
            Timber.i("Recording via MediaRecorder call chain")
            return media
        }

        Timber.e(media.exceptionOrNull(), "All capture paths failed")
        return media
    }

    override fun stopRecording(): Result<Long> {
        val result = when (mode) {
            Mode.VOICE_CALL_PCM -> voiceCall.stop().fold(
                onSuccess = { Result.success(it) },
                onFailure = { Result.failure(RecorderError.Unknown(it.message, it)) },
            )
            Mode.MEDIA_RECORDER -> mediaRecorder.stopRecording()
            Mode.NONE -> Result.failure(RecorderError.InvalidState("No active recording"))
        }
        mode = Mode.NONE
        return result
    }

    override fun releaseResources() {
        try {
            voiceCall.release()
        } catch (_: Exception) {
        }
        try {
            mediaRecorder.releaseResources()
        } catch (_: Exception) {
        }
        mode = Mode.NONE
    }
}
