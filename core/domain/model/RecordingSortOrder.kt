package com.callrecorder.core.domain.model

/**
 * Defines the sort order for recordings list queries.
 *
 * The default is [NEWEST_FIRST] — most recent recordings shown at the top.
 */
enum class RecordingSortOrder {
    /** Most recently recorded first (default). */
    NEWEST_FIRST,
    /** Oldest recording first. */
    OLDEST_FIRST,
    /** Longest duration first. */
    LONGEST_FIRST,
    /** Shortest duration first. */
    SHORTEST_FIRST,
    /** Alphabetical by display name (contact name or phone number). */
    BY_NAME,
    /** Largest file size first. */
    LARGEST_FIRST,
}
