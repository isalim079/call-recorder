package com.callrecorder.core.domain.usecase

import com.callrecorder.core.domain.model.RecordingStatistics
import com.callrecorder.core.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Use case: Retrieve aggregated statistics for different time windows.
 *
 * Provides statistics for:
 * - All time
 * - Today
 * - This week
 * - This month
 * - A custom date range
 *
 * All timestamps are computed relative to the device's local timezone.
 */
class GetStatisticsUseCase @Inject constructor(
    private val repository: RecordingRepository,
) {
    /** Statistics for all recordings, all time. */
    fun allTime(): Flow<RecordingStatistics> =
        repository.getStatistics()

    /** Statistics for today (midnight to now). */
    fun today(): Flow<RecordingStatistics> {
        val todayStart = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return repository.getStatisticsForPeriod(todayStart, Long.MAX_VALUE)
    }

    /** Statistics for the current ISO calendar week (Monday to now). */
    fun thisWeek(): Flow<RecordingStatistics> {
        val weekStart = LocalDate.now()
            .with(java.time.DayOfWeek.MONDAY)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return repository.getStatisticsForPeriod(weekStart, Long.MAX_VALUE)
    }

    /** Statistics for the current calendar month (1st to now). */
    fun thisMonth(): Flow<RecordingStatistics> {
        val monthStart = LocalDate.now()
            .withDayOfMonth(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        return repository.getStatisticsForPeriod(monthStart, Long.MAX_VALUE)
    }

    /**
     * Statistics for a custom date range.
     *
     * @param from Start date (inclusive).
     * @param to   End date (inclusive, extended to end of day).
     */
    fun forPeriod(from: LocalDate, to: LocalDate): Flow<RecordingStatistics> {
        val fromMs = from.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val toMs   = to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
        return repository.getStatisticsForPeriod(fromMs, toMs)
    }
}
