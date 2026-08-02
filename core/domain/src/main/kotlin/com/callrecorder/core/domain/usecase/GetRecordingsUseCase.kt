package com.callrecorder.core.domain.usecase

import com.callrecorder.core.domain.model.Recording
import com.callrecorder.core.domain.model.RecordingFilter
import com.callrecorder.core.domain.model.RecordingSortOrder
import com.callrecorder.core.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case: Observe the full list of recordings with optional filter and sort.
 *
 * Returns a [Flow] that emits a new list whenever the database changes.
 * The ViewModel subscribes to this flow and exposes it to the UI.
 *
 * Usage:
 * ```kotlin
 * val recordings = getRecordingsUseCase(
 *     filter = RecordingFilter(favoritesOnly = true),
 *     sortOrder = RecordingSortOrder.NEWEST_FIRST,
 * )
 * ```
 */
class GetRecordingsUseCase @Inject constructor(
    private val repository: RecordingRepository,
) {
    /**
     * @param filter    Optional filter criteria (default: no filter).
     * @param sortOrder Sort direction (default: newest first).
     * @return Flow of the matching [Recording] list.
     */
    operator fun invoke(
        filter: RecordingFilter = RecordingFilter(),
        sortOrder: RecordingSortOrder = RecordingSortOrder.NEWEST_FIRST,
    ): Flow<List<Recording>> = repository.getRecordings(filter, sortOrder)
}
