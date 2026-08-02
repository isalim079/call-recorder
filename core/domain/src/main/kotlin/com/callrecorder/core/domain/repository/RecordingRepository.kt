package com.callrecorder.core.domain.repository

import com.callrecorder.core.domain.model.Recording
import com.callrecorder.core.domain.model.RecordingFilter
import com.callrecorder.core.domain.model.RecordingSortOrder
import com.callrecorder.core.domain.model.RecordingStatistics
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for the recordings domain.
 *
 * This interface lives in the domain layer — it is the contract that the
 * data layer ([RecordingRepositoryImpl]) must fulfill. ViewModels and
 * use cases depend only on this interface, never on the implementation.
 *
 * All list queries return [Flow] for real-time database observation.
 * Single-shot writes are suspend functions.
 */
interface RecordingRepository {

    // ── Queries ────────────────────────────────────────────────────────────

    /**
     * Observe all non-deleted recordings, with optional filter and sort.
     *
     * Emits a new list whenever the underlying database changes.
     */
    fun getRecordings(
        filter: RecordingFilter = RecordingFilter(),
        sortOrder: RecordingSortOrder = RecordingSortOrder.NEWEST_FIRST,
    ): Flow<List<Recording>>

    /**
     * Observe a single recording by its [id].
     * Emits null if the recording is not found.
     */
    fun getRecordingById(id: Long): Flow<Recording?>

    /**
     * Observe only recordings marked as favorite.
     */
    fun getFavoriteRecordings(): Flow<List<Recording>>

    /**
     * Observe recordings made today (from midnight to now).
     */
    fun getTodayRecordings(): Flow<List<Recording>>

    /**
     * Observe recordings made in the current calendar week.
     */
    fun getThisWeekRecordings(): Flow<List<Recording>>

    /**
     * Observe recordings made in the current calendar month.
     */
    fun getThisMonthRecordings(): Flow<List<Recording>>

    /**
     * Search recordings across phone number, contact name, and notes.
     *
     * @param query Free-text search query.
     * @return Flow of matching [Recording] objects.
     */
    fun searchRecordings(query: String): Flow<List<Recording>>

    /**
     * Compute aggregated statistics for all recordings.
     */
    fun getStatistics(): Flow<RecordingStatistics>

    /**
     * Compute statistics for a specific time window.
     *
     * @param fromTimestamp Start of the window (epoch millis, inclusive).
     * @param toTimestamp   End of the window (epoch millis, inclusive).
     */
    fun getStatisticsForPeriod(
        fromTimestamp: Long,
        toTimestamp: Long,
    ): Flow<RecordingStatistics>

    /**
     * Get total storage consumed by all recordings in bytes.
     */
    fun getTotalStorageBytes(): Flow<Long>

    /**
     * Get recordings sorted by file size descending (for Storage screen).
     */
    fun getLargestRecordings(limit: Int = 10): Flow<List<Recording>>

    /**
     * Get the oldest recordings (for auto-delete / storage cleanup).
     */
    fun getOldestRecordings(limit: Int = 10): Flow<List<Recording>>

    // ── Mutations ──────────────────────────────────────────────────────────

    /**
     * Insert a new recording into the database.
     *
     * @return The new recording's auto-generated [Recording.id].
     */
    suspend fun insertRecording(recording: Recording): Long

    /**
     * Update an existing recording (rename, notes, favorite toggle, etc.).
     */
    suspend fun updateRecording(recording: Recording)

    /**
     * Soft-delete a recording (marks [Recording.isDeleted] = true).
     * The file on disk is NOT deleted — use [purgeRecording] for that.
     */
    suspend fun softDeleteRecording(id: Long)

    /**
     * Permanently deletes the database record AND the file on disk.
     *
     * @throws IllegalStateException if the file cannot be deleted.
     */
    suspend fun purgeRecording(id: Long)

    /**
     * Permanently purge a list of recordings by their IDs.
     */
    suspend fun purgeRecordings(ids: List<Long>)

    /**
     * Restore a soft-deleted recording (marks [Recording.isDeleted] = false).
     */
    suspend fun restoreRecording(id: Long)

    /**
     * Toggle the favorite status of a recording.
     */
    suspend fun toggleFavorite(id: Long)

    /**
     * Set notes for a recording.
     */
    suspend fun updateNotes(id: Long, notes: String)

    /**
     * Rename a recording (updates [Recording.contactName] display label).
     */
    suspend fun renameRecording(id: Long, newName: String)

    /**
     * Delete all recordings older than [olderThanTimestamp] (epoch millis).
     * Used by the auto-delete WorkManager task.
     */
    suspend fun autoDeleteOlderThan(olderThanTimestamp: Long)
}
