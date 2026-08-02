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
     * Low quality — smallest file size, suitable for voice-only recordings.
     * ~0.5 MB per minute.
     */
    LOW(
        label      = "Low",
        bitrate    = 64_000,
        sampleRate = 44_100,
    ),

    /**
     * Medium quality — good balance between quality and storage.
     * ~1 MB per minute.
     */
    MEDIUM(
        label      = "Medium",
        bitrate    = 128_000,
        sampleRate = 44_100,
    ),

    /**
     * High quality — best audio fidelity, larger files.
     * ~2 MB per minute.
     */
    HIGH(
        label      = "High",
        bitrate    = 256_000,
        sampleRate = 44_100,
    ),
}
