package com.callrecorder.core.domain.model

/**
 * Aggregated statistics for a period of time.
 *
 * Used by the Statistics screen and the Home dashboard.
 *
 * @param totalRecordings   Total number of non-deleted recordings in this period.
 * @param incomingCount     Count of [CallType.INCOMING] recordings.
 * @param outgoingCount     Count of [CallType.OUTGOING] recordings.
 * @param totalDurationMs   Sum of all recording durations in milliseconds.
 * @param avgDurationMs     Average recording duration in milliseconds.
 * @param longestDurationMs Duration of the longest individual recording.
 * @param totalSizeBytes    Sum of all recording file sizes in bytes.
 * @param favoriteCount     Count of recordings marked as favorite.
 */
data class RecordingStatistics(
    val totalRecordings: Int = 0,
    val incomingCount: Int = 0,
    val outgoingCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val avgDurationMs: Long = 0L,
    val longestDurationMs: Long = 0L,
    val totalSizeBytes: Long = 0L,
    val favoriteCount: Int = 0,
)
