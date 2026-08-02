package com.callrecorder.core.domain.usecase

import com.callrecorder.core.domain.repository.RecordingRepository
import javax.inject.Inject

/**
 * Use case: Permanently delete a recording and its file on disk.
 *
 * This is a hard delete — the recording is removed from the database
 * and the .m4a file is deleted from storage.
 *
 * For a soft-delete (move to trash), use the repository directly.
 *
 * @throws IllegalStateException if the file cannot be deleted.
 */
class DeleteRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository,
) {
    /**
     * Delete a single recording by ID.
     */
    suspend operator fun invoke(id: Long) {
        repository.purgeRecording(id)
    }

    /**
     * Delete multiple recordings in a single batch operation.
     * More efficient than calling [invoke] in a loop.
     */
    suspend fun deleteAll(ids: List<Long>) {
        repository.purgeRecordings(ids)
    }
}
