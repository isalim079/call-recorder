package com.callrecorder.core.audio

import com.callrecorder.core.domain.model.AudioQuality

/**
 * Capture / encode parameters derived from [AudioQuality].
 */
data class AudioCaptureConfig(
    val sampleRate: Int,
    val bitrate: Int,
    val channelCount: Int = 1,
    val fftSize: Int = 512,
    /** Soft skip of ringback via VAD (outgoing). Still force-arms after short wait. */
    val waitForAnswer: Boolean = false,
    /** Max wait before force-recording (never block whole call). */
    val answerTimeoutMs: Long = 4_000L,
) {
    companion object {
        fun from(quality: AudioQuality, waitForAnswer: Boolean = false): AudioCaptureConfig =
            AudioCaptureConfig(
                sampleRate      = quality.sampleRate,
                bitrate         = quality.bitrate,
                waitForAnswer   = waitForAnswer,
                answerTimeoutMs = if (waitForAnswer) 4_000L else 300L,
            )
    }
}
