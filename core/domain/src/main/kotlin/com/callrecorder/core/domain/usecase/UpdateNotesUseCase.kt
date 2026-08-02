package com.callrecorder.core.domain.usecase

import com.callrecorder.core.domain.repository.RecordingRepository
import javax.inject.Inject

/**
 * Use case: Update the notes field of a recording.
 *
 * Notes are optional free-text annotations the user can attach to any recording.
 * They are searchable via [SearchRecordingsUseCase].
 */
class UpdateNotesUseCase @Inject constructor(
    private val repository: RecordingRepository,
) {
    suspend operator fun invoke(id: Long, notes: String) {
        repository.updateNotes(id, notes)
    }
}
