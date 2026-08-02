package com.callrecorder.app.recorder

import com.callrecorder.core.domain.model.AudioQuality

/**
 * Contract for the audio recording engine.
 *
 * Implementations:
 * - [HdRecorderEngine] — AudioRecord + denoise module + answer detect (primary)
 * - [MediaRecorderEngine] — classic MediaRecorder (fallback)
 *
 * All methods are synchronous. Call them from a background thread or coroutine.
 */
interface AudioRecorderEngine {
    /** True if a recording is currently in progress. */
    val isRecording: Boolean

    /**
     * Start recording to [filePath] with the given [quality].
     *
     * @param waitForAnswer When true (typical for outgoing), codec only starts after
     * speech is detected — skips ringback. Incoming answered calls should pass false.
     * @return [Result.success] on success, [Result.failure] with [RecorderError] on failure.
     */
    fun startRecording(
        filePath: String,
        quality: AudioQuality,
        waitForAnswer: Boolean = false,
    ): Result<Unit>

    /**
     * Stop the current recording and finalize the file.
     *
     * @return [Result.success] with the duration in milliseconds, or [Result.failure].
     */
    fun stopRecording(): Result<Long>

    /**
     * Release all MediaRecorder resources immediately.
     * Safe to call even if no recording is active.
     */
    fun releaseResources()
}
