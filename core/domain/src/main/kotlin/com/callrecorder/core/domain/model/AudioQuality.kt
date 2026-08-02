package com.callrecorder.core.domain.model

/**
 * Recording audio quality levels.
 *
 * Maps to specific [MediaRecorder] encoding parameters in [AudioQualityConfig].
 *
 * @param label        Human-readable description shown in Settings.
 * @param bitrate      Audio encoding bitrate in bits per second.
 * @param sampleRate   Audio sample rate in Hz.
 */
enum class AudioQuality(
    val label: String,
    val bitrate: Int,
    val sampleRate: Int,
) {
    /**
     * Low quality — smallest file size, voice bandwidth.
     * ~0.5 MB per minute.
     */
    LOW(
        label      = "Low",
        bitrate    = 64_000,
        sampleRate = 16_000,
    ),

    /**
     * Medium quality — clear speech, moderate storage.
     * ~1 MB per minute.
     */
    MEDIUM(
        label      = "Medium",
        bitrate    = 128_000,
        sampleRate = 44_100,
    ),

    /**
     * High / HD — high-bitrate AAC for clean post-denoise speech.
     * ~2 MB per minute at 48 kHz mono.
     */
    HIGH(
        label      = "High (HD)",
        bitrate    = 256_000,
        sampleRate = 48_000,
    ),
}
