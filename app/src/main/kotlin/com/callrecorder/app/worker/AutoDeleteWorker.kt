package com.callrecorder.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.callrecorder.core.data.preferences.SettingsRepository
import com.callrecorder.core.domain.repository.RecordingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class AutoDeleteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val recordingRepository: RecordingRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("AutoDeleteWorker: starting cleanup job")
        return try {
            val autoDeleteDays = settingsRepository.getAutoDeleteDays().first()
            if (autoDeleteDays > 0) {
                val olderThanMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(autoDeleteDays.toLong())
                Timber.d("AutoDeleteWorker: deleting recordings older than $autoDeleteDays days (before $olderThanMs)")
                recordingRepository.autoDeleteOlderThan(olderThanMs)
            } else {
                Timber.d("AutoDeleteWorker: auto-delete is disabled (0 days)")
            }
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "AutoDeleteWorker: failed to complete cleanup")
            Result.retry()
        }
    }
}
