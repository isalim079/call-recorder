package com.callrecorder.core.domain.model

import java.time.LocalDate

/**
 * Filter criteria applied to the recordings list.
 *
 * All fields are optional — null/false means "no filter on this dimension".
 * Multiple non-null fields are combined with AND logic.
 *
 * @param callType      Filter by call direction.
 * @param favoritesOnly Show only starred recordings.
 * @param fromDate      Include only recordings on or after this date.
 * @param toDate        Include only recordings on or before this date.
 * @param minDurationMs Exclude recordings shorter than this (milliseconds).
 * @param maxDurationMs Exclude recordings longer than this (milliseconds).
 * @param query         Free-text search across phone number, contact name, and notes.
 */
data class RecordingFilter(
    val callType: CallType? = null,
    val favoritesOnly: Boolean = false,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val minDurationMs: Long? = null,
    val maxDurationMs: Long? = null,
    val query: String = "",
) {
    /** Returns true if no filter constraints are set. */
    val isEmpty: Boolean
        get() = callType == null &&
            !favoritesOnly &&
            fromDate == null &&
            toDate == null &&
            minDurationMs == null &&
            maxDurationMs == null &&
            query.isBlank()
}
