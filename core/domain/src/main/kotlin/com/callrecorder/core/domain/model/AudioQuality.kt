package com.callrecorder.core.domain.model

/**
 * Recording audio quality levels for capture + post-enhance AAC encode.
 *
 * Default for new installs: [HIGH] (HD).
 */
enum class AudioQuality(
    val label: String,
    val bitrate: Int,
    val sampleRate: Int,
) {
    LOW(
        label      = "Low",
        bitrate    = 96_000,
        sampleRate = 44_100,
    ),

    MEDIUM(
        label      = "Medium",
        bitrate    = 160_000,
        sampleRate = 44_100,
    ),

    /**
     * HD — default. High-bitrate AAC LC mono after denoise pipeline.
     */
    HIGH(
        label      = "High (HD)",
        bitrate    = 256_000,
        sampleRate = 44_100,
    ),
}
