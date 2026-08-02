package com.callrecorder.core.data.repository

import com.callrecorder.core.database.dao.RecordingDao
import com.callrecorder.core.database.dao.StatisticsProjection
import com.callrecorder.core.database.mapper.toDomain
import com.callrecorder.core.database.mapper.toEntity
import com.callrecorder.core.domain.model.Recording
import com.callrecorder.core.domain.model.RecordingFilter
import com.callrecorder.core.domain.model.RecordingSortOrder
import com.callrecorder.core.domain.model.RecordingStatistics
import com.callrecorder.core.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production implementation of [RecordingRepository].
 *
 * Responsibilities:
 * - Translate between domain models and database entities via [RecordingMapper].
 * - Apply in-memory filter and sort on top of Room Flow queries.
 * - Delete .m4a files from disk when purging recordings.
 *
 * Filtering is performed in-memory (not in SQL) to keep queries simple and to
 * support complex multi-field AND logic without dynamic query building.
 * For very large datasets (10k+ recordings), migrate filtering to SQL for performance.
 *
 * @param dao           Room DAO for database access.
 */
@Singleton
class RecordingRepositoryImpl @Inject constructor(
    private val dao: RecordingDao,
) : RecordingRepository {

    // ── Queries ────────────────────────────────────────────────────────────

    override fun getRecordings(
        filter: RecordingFilter,
        sortOrder: RecordingSortOrder,
    ): Flow<List<Recording>> =
        dao.getAllActive().map { entities ->
            entities.toDomain()
                .applyFilter(filter)
                .applySort(sortOrder)
        }

    override fun getRecordingById(id: Long): Flow<Recording?> =
        dao.getById(id).map { it?.toDomain() }

    override fun getFavoriteRecordings(): Flow<List<Recording>> =
        dao.getFavorites().map { it.toDomain() }

    override fun getTodayRecordings(): Flow<List<Recording>> {
        val startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return dao.getFromTimestamp(startOfDay).map { it.toDomain() }
    }

    override fun getThisWeekRecordings(): Flow<List<Recording>> {
        val weekStart = LocalDate.now()
            .with(java.time.DayOfWeek.MONDAY)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return dao.getFromTimestamp(weekStart).map { it.toDomain() }
    }

    override fun getThisMonthRecordings(): Flow<List<Recording>> {
        val monthStart = LocalDate.now()
            .withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return dao.getFromTimestamp(monthStart).map { it.toDomain() }
    }

    override fun searchRecordings(query: String): Flow<List<Recording>> =
        dao.search(query).map { it.toDomain() }

    override fun getStatistics(): Flow<RecordingStatistics> =
        dao.getStatistics().map { it.toStatistics() }

    override fun getStatisticsForPeriod(
        fromTimestamp: Long,
        toTimestamp: Long,
    ): Flow<RecordingStatistics> =
        dao.getStatisticsForPeriod(fromTimestamp, toTimestamp).map { it.toStatistics() }

    override fun getTotalStorageBytes(): Flow<Long> =
        dao.getTotalSizeBytes().map { it ?: 0L }

    override fun getLargestRecordings(limit: Int): Flow<List<Recording>> =
        dao.getLargest(limit).map { it.toDomain() }

    override fun getOldestRecordings(limit: Int): Flow<List<Recording>> =
        dao.getOldest(limit).map { it.toDomain() }

    // ── Mutations ──────────────────────────────────────────────────────────

    override suspend fun insertRecording(recording: Recording): Long =
        dao.insert(recording.toEntity())

    override suspend fun updateRecording(recording: Recording) =
        dao.update(recording.toEntity())

    override suspend fun softDeleteRecording(id: Long) =
        dao.softDelete(id)

    override suspend fun purgeRecording(id: Long) {
        val filePath = dao.getFilePath(id)
        dao.hardDelete(id)
        filePath?.let { deleteFileFromDisk(it) }
    }

    override suspend fun purgeRecordings(ids: List<Long>) {
        // Collect file paths before deleting DB records
        val paths = ids.mapNotNull { dao.getFilePath(it) }
        dao.hardDeleteAll(ids)
        paths.forEach { deleteFileFromDisk(it) }
    }

    override suspend fun restoreRecording(id: Long) =
        dao.restore(id)

    override suspend fun toggleFavorite(id: Long) =
        dao.toggleFavorite(id)

    override suspend fun updateNotes(id: Long, notes: String) =
        dao.updateNotes(id, notes)

    override suspend fun renameRecording(id: Long, newName: String) =
        dao.rename(id, newName)

    override suspend fun autoDeleteOlderThan(olderThanTimestamp: Long) {
        val rows = dao.getOlderThan(olderThanTimestamp)
        rows.forEach { deleteFileFromDisk(it.file_path) }
        dao.deleteOlderThan(olderThanTimestamp)
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private fun deleteFileFromDisk(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                val deleted = file.delete()
                if (!deleted) {
                    Timber.w("Failed to delete file: $filePath")
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException when deleting: $filePath")
        }
    }

    // ── In-memory filter + sort ────────────────────────────────────────────

    private fun List<Recording>.applyFilter(filter: RecordingFilter): List<Recording> {
        if (filter.isEmpty) return this
        return filter { recording ->
            (filter.callType == null || recording.callType == filter.callType) &&
            (!filter.favoritesOnly || recording.isFavorite) &&
            (filter.query.isBlank() ||
                recording.phoneNumber.contains(filter.query, ignoreCase = true) ||
                recording.contactName?.contains(filter.query, ignoreCase = true) == true ||
                recording.notes.contains(filter.query, ignoreCase = true)) &&
            (filter.fromDate == null ||
                Instant.ofEpochMilli(recording.timestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate() >= filter.fromDate) &&
            (filter.toDate == null ||
                Instant.ofEpochMilli(recording.timestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate() <= filter.toDate) &&
            (filter.minDurationMs == null || recording.durationMs >= filter.minDurationMs!!) &&
            (filter.maxDurationMs == null || recording.durationMs <= filter.maxDurationMs!!)
        }
    }

    private fun List<Recording>.applySort(sortOrder: RecordingSortOrder): List<Recording> =
        when (sortOrder) {
            RecordingSortOrder.NEWEST_FIRST   -> sortedByDescending { it.timestamp }
            RecordingSortOrder.OLDEST_FIRST   -> sortedBy { it.timestamp }
            RecordingSortOrder.LONGEST_FIRST  -> sortedByDescending { it.durationMs }
            RecordingSortOrder.SHORTEST_FIRST -> sortedBy { it.durationMs }
            RecordingSortOrder.BY_NAME        -> sortedBy { it.displayName.lowercase() }
            RecordingSortOrder.LARGEST_FIRST  -> sortedByDescending { it.fileSizeBytes }
        }

    private fun StatisticsProjection.toStatistics() = RecordingStatistics(
        totalRecordings   = count,
        incomingCount     = incomingCount,
        outgoingCount     = outgoingCount,
        totalDurationMs   = totalDuration ?: 0L,
        avgDurationMs     = avgDuration?.toLong() ?: 0L,
        longestDurationMs = maxDuration ?: 0L,
        totalSizeBytes    = totalSize ?: 0L,
        favoriteCount     = favoriteCount,
    )
}
