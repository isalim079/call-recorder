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
    /** Skip ringback until speech detected (outgoing dialing). */
    val waitForAnswer: Boolean = true,
    /** Give up waiting and record anyway after this many ms. */
    val answerTimeoutMs: Long = 90_000L,
) {
    companion object {
        fun from(quality: AudioQuality, waitForAnswer: Boolean = true): AudioCaptureConfig =
            AudioCaptureConfig(
                sampleRate    = quality.sampleRate,
                bitrate       = quality.bitrate,
                waitForAnswer = waitForAnswer,
            )
    }
}
