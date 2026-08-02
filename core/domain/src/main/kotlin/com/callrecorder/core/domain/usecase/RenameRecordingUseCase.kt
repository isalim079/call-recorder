package com.callrecorder.core.domain.usecase

import com.callrecorder.core.domain.repository.RecordingRepository
import javax.inject.Inject

/**
 * Use case: Rename a recording.
 *
 * Updates the display label stored in the database.
 * Does NOT rename the file on disk — the file name is fixed at recording time.
 *
 * @throws IllegalArgumentException if [newName] is blank.
 */
class RenameRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository,
) {
    /**
     * @param id      ID of the recording to rename.
     * @param newName New display name. Must not be blank.
     */
    suspend operator fun invoke(id: Long, newName: String) {
        require(newName.isNotBlank()) { "Recording name must not be blank." }
        repository.renameRecording(id, newName.trim())
    }
}
