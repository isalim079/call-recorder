package com.callrecorder.app.di

import android.content.Context
import androidx.work.WorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing WorkManager and other app-level infrastructure.
 *
 * WorkManager is used for:
 * - Scheduled auto-delete of old recordings
 * - Storage cleanup tasks
 *
 * Workers are injected via [@HiltWorker] / [HiltWorkerFactory].
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)
}
