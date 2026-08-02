package com.callrecorder.core.domain.usecase

import com.callrecorder.core.domain.model.Recording
import com.callrecorder.core.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case: Observe a single recording by its ID.
 *
 * Emits null if the recording is not found or has been purged.
 * The Player screen subscribes to this to keep its UI in sync.
 */
class GetRecordingByIdUseCase @Inject constructor(
    private val repository: RecordingRepository,
) {
    operator fun invoke(id: Long): Flow<Recording?> =
        repository.getRecordingById(id)
}
