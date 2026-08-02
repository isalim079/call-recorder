package com.callrecorder.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.callrecorder.app.worker.AutoDeleteWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application class — entry point for Hilt dependency injection
 * and global initialization (logging, WorkManager custom initialization, etc.).
 *
 * Annotated with [HiltAndroidApp] to trigger Hilt's code generation.
 * Timber is planted here so all modules can log consistently.
 */
@HiltAndroidApp
class CallRecorderApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        initLogging()
        scheduleAutoDeleteWorker()
    }

    /**
     * Plant Timber's [DebugTree] only in debug builds.
     * Production builds are silent — no crash reporting since the app is fully offline.
     */
    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    /**
     * Enqueue unique daily periodic work to automatically delete recordings
     * older than the user's defined retention threshold.
     */
    private fun scheduleAutoDeleteWorker() {
        val workRequest = PeriodicWorkRequestBuilder<AutoDeleteWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "AutoDeleteWork",
            ExistingPeriodicWorkPolicy.KEEP, // Maintain schedule across app launches
            workRequest
        )
    }
}
