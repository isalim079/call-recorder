package com.callrecorder.core.domain.usecase

import com.callrecorder.core.domain.repository.RecordingRepository
import javax.inject.Inject

/**
 * Use case: Toggle the favorite status of a recording.
 *
 * Idempotent — calling this twice on the same recording returns it to
 * its original state.
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val repository: RecordingRepository,
) {
    suspend operator fun invoke(id: Long) {
        repository.toggleFavorite(id)
    }
}
