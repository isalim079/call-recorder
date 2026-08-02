package com.callrecorder.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.callrecorder.core.database.entity.RecordingEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the `recordings` table.
 *
 * All list queries return [Flow] for real-time observation.
 * All write operations are suspend functions for safe coroutine dispatch.
 *
 * Search uses SQLite's LIKE operator with `%query%` pattern.
 * For production scale (10k+ recordings), consider FTS5 — see comments below.
 */
@Dao
interface RecordingDao {

    // ── Insert / Update ────────────────────────────────────────────────────

    /**
     * Insert a new recording. Returns the new row ID.
     * [OnConflictStrategy.ABORT] prevents duplicate inserts.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: RecordingEntity): Long

    /**
     * Update an existing recording (all fields).
     */
    @Update
    suspend fun update(entity: RecordingEntity)

    // ── Active recordings (not soft-deleted) ───────────────────────────────

    /**
     * Observe all non-deleted recordings, newest first.
     * Used as the base query — sort/filter applied in the repository layer.
     */
    @Query("""
        SELECT * FROM recordings
        WHERE is_deleted = 0
        ORDER BY timestamp DESC
    """)
    fun getAllActive(): Flow<List<RecordingEntity>>

    /**
     * Observe a single recording by ID (deleted or not).
     */
    @Query("SELECT * FROM recordings WHERE id = :id")
    fun getById(id: Long): Flow<RecordingEntity?>

    /**
     * Observe all favorite recordings, newest first.
     */
    @Query("""
        SELECT * FROM recordings
        WHERE is_deleted = 0 AND is_favorite = 1
        ORDER BY timestamp DESC
    """)
    fun getFavorites(): Flow<List<RecordingEntity>>

    /**
     * Observe recordings where [timestamp] >= [fromTimestamp],
     * ordered newest first. Used for Today / This Week / This Month queries.
     */
    @Query("""
        SELECT * FROM recordings
        WHERE is_deleted = 0 AND timestamp >= :fromTimestamp
        ORDER BY timestamp DESC
    """)
    fun getFromTimestamp(fromTimestamp: Long): Flow<List<RecordingEntity>>

    /**
     * Observe recordings within a time range [fromTimestamp, toTimestamp].
     */
    @Query("""
        SELECT * FROM recordings
        WHERE is_deleted = 0
          AND timestamp >= :fromTimestamp
          AND timestamp <= :toTimestamp
        ORDER BY timestamp DESC
    """)
    fun getForPeriod(fromTimestamp: Long, toTimestamp: Long): Flow<List<RecordingEntity>>

    /**
     * Search across phone_number, contact_name, and notes.
     *
     * NOTE: For very large datasets (10k+ rows), replace with FTS5:
     *   CREATE VIRTUAL TABLE recordings_fts USING fts5(phone_number, contact_name, notes, content=recordings)
     *   @Query("SELECT * FROM recordings_fts WHERE recordings_fts MATCH :query")
     */
    @Query("""
        SELECT * FROM recordings
        WHERE is_deleted = 0
          AND (
            phone_number LIKE '%' || :query || '%'
            OR contact_name LIKE '%' || :query || '%'
            OR notes LIKE '%' || :query || '%'
          )
        ORDER BY timestamp DESC
    """)
    fun search(query: String): Flow<List<RecordingEntity>>

    // ── Statistics ──────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM recordings WHERE is_deleted = 0")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recordings WHERE is_deleted = 0 AND call_type = 'INCOMING'")
    fun getIncomingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM recordings WHERE is_deleted = 0 AND call_type = 'OUTGOING'")
    fun getOutgoingCount(): Flow<Int>

    @Query("SELECT SUM(duration_ms) FROM recordings WHERE is_deleted = 0")
    fun getTotalDurationMs(): Flow<Long?>

    @Query("SELECT AVG(duration_ms) FROM recordings WHERE is_deleted = 0")
    fun getAvgDurationMs(): Flow<Double?>

    @Query("SELECT MAX(duration_ms) FROM recordings WHERE is_deleted = 0")
    fun getLongestDurationMs(): Flow<Long?>

    @Query("SELECT SUM(file_size_bytes) FROM recordings WHERE is_deleted = 0")
    fun getTotalSizeBytes(): Flow<Long?>

    @Query("SELECT COUNT(*) FROM recordings WHERE is_deleted = 0 AND is_favorite = 1")
    fun getFavoriteCount(): Flow<Int>

    /** Statistics for a period — returns raw aggregate values. */
    @Query("""
        SELECT
            COUNT(*) as count,
            SUM(duration_ms) as totalDuration,
            AVG(duration_ms) as avgDuration,
            MAX(duration_ms) as maxDuration,
            SUM(file_size_bytes) as totalSize,
            SUM(CASE WHEN call_type = 'INCOMING' THEN 1 ELSE 0 END) as incomingCount,
            SUM(CASE WHEN call_type = 'OUTGOING' THEN 1 ELSE 0 END) as outgoingCount,
            SUM(CASE WHEN is_favorite = 1 THEN 1 ELSE 0 END) as favoriteCount
        FROM recordings
        WHERE is_deleted = 0
          AND timestamp >= :fromTimestamp
          AND timestamp <= :toTimestamp
    """)
    fun getStatisticsForPeriod(fromTimestamp: Long, toTimestamp: Long): Flow<StatisticsProjection>

    /** All-time statistics projection. */
    @Query("""
        SELECT
            COUNT(*) as count,
            SUM(duration_ms) as totalDuration,
            AVG(duration_ms) as avgDuration,
            MAX(duration_ms) as maxDuration,
            SUM(file_size_bytes) as totalSize,
            SUM(CASE WHEN call_type = 'INCOMING' THEN 1 ELSE 0 END) as incomingCount,
            SUM(CASE WHEN call_type = 'OUTGOING' THEN 1 ELSE 0 END) as outgoingCount,
            SUM(CASE WHEN is_favorite = 1 THEN 1 ELSE 0 END) as favoriteCount
        FROM recordings
        WHERE is_deleted = 0
    """)
    fun getStatistics(): Flow<StatisticsProjection>

    // ── Storage queries ─────────────────────────────────────────────────────

    @Query("""
        SELECT * FROM recordings
        WHERE is_deleted = 0
        ORDER BY file_size_bytes DESC
        LIMIT :limit
    """)
    fun getLargest(limit: Int): Flow<List<RecordingEntity>>

    @Query("""
        SELECT * FROM recordings
        WHERE is_deleted = 0
        ORDER BY timestamp ASC
        LIMIT :limit
    """)
    fun getOldest(limit: Int): Flow<List<RecordingEntity>>

    // ── Soft delete / restore ───────────────────────────────────────────────

    @Query("UPDATE recordings SET is_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)

    @Query("UPDATE recordings SET is_deleted = 0 WHERE id = :id")
    suspend fun restore(id: Long)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun hardDelete(id: Long)

    @Query("DELETE FROM recordings WHERE id IN (:ids)")
    suspend fun hardDeleteAll(ids: List<Long>)

    // ── Mutations ───────────────────────────────────────────────────────────

    @Query("UPDATE recordings SET is_favorite = NOT is_favorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("UPDATE recordings SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: Long, notes: String)

    @Query("UPDATE recordings SET contact_name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    // ── Auto-delete ─────────────────────────────────────────────────────────

    /**
     * Returns IDs and file paths of recordings older than [olderThanTimestamp].
     * Used by the auto-delete worker to delete files before removing DB records.
     */
    @Query("""
        SELECT id, file_path FROM recordings
        WHERE is_deleted = 0 AND timestamp < :olderThanTimestamp
    """)
    suspend fun getOlderThan(olderThanTimestamp: Long): List<IdFilePath>

    @Query("DELETE FROM recordings WHERE timestamp < :olderThanTimestamp AND is_deleted = 0")
    suspend fun deleteOlderThan(olderThanTimestamp: Long)

    // ── File path lookup (for purge) ────────────────────────────────────────

    @Query("SELECT file_path FROM recordings WHERE id = :id")
    suspend fun getFilePath(id: Long): String?
}

/** Lightweight projection for auto-delete file cleanup. */
data class IdFilePath(val id: Long, val file_path: String)

/** Aggregated statistics projection returned by statistics queries. */
data class StatisticsProjection(
    val count: Int = 0,
    val totalDuration: Long? = null,
    val avgDuration: Double? = null,
    val maxDuration: Long? = null,
    val totalSize: Long? = null,
    val incomingCount: Int = 0,
    val outgoingCount: Int = 0,
    val favoriteCount: Int = 0,
)
