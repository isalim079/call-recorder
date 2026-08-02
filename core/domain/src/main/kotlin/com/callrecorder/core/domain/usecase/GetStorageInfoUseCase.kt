package com.callrecorder.core.domain.usecase

import com.callrecorder.core.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case: Get total and free storage information for the Storage screen.
 *
 * Returns total bytes consumed by recordings as a Flow.
 * Available device storage is queried from [StorageManager] in the data layer.
 */
class GetStorageInfoUseCase @Inject constructor(
    private val repository: RecordingRepository,
) {
    /** Total bytes used by all recordings. */
    fun totalRecordingBytes(): Flow<Long> =
        repository.getTotalStorageBytes()

    /** The [limit] largest recordings by file size. */
    fun largestRecordings(limit: Int = 10) =
        repository.getLargestRecordings(limit)

    /** The [limit] oldest recordings by timestamp. */
    fun oldestRecordings(limit: Int = 10) =
        repository.getOldestRecordings(limit)
}
