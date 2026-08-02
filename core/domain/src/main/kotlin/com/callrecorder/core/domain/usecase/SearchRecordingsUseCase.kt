package com.callrecorder.core.domain.usecase

import com.callrecorder.core.domain.model.Recording
import com.callrecorder.core.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case: Full-text search across recordings.
 *
 * Searches the following fields:
 * - [Recording.phoneNumber]
 * - [Recording.contactName]
 * - [Recording.notes]
 *
 * Returns an empty list for blank queries (no results when nothing is typed).
 * Results are ordered by relevance (most recent match first).
 *
 * @see RecordingRepository.searchRecordings
 */
class SearchRecordingsUseCase @Inject constructor(
    private val repository: RecordingRepository,
) {
    /**
     * @param query Search term. Whitespace-trimmed before passing to the repository.
     * @return Flow of matching recordings, or empty list if [query] is blank.
     */
    operator fun invoke(query: String): Flow<List<Recording>> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            // Return empty list immediately — don't show all recordings in search mode
            return repository.searchRecordings("").map { emptyList() }
        }
        return repository.searchRecordings(trimmed)
    }
}
