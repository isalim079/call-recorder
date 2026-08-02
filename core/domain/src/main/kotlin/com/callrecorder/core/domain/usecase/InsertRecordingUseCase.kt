package com.callrecorder.core.domain.usecase

import com.callrecorder.core.domain.repository.RecordingRepository
import javax.inject.Inject

/**
 * Use case: Insert a newly completed recording into the database.
 *
 * Called by [CallRecorderService] at the end of a successful recording.
 *
 * @return The auto-generated database ID for the new recording.
 */
class InsertRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository,
) {
    suspend operator fun invoke(recording: com.callrecorder.core.domain.model.Recording): Long =
        repository.insertRecording(recording)
}
