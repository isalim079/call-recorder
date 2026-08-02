package com.callrecorder.app.di

import com.callrecorder.core.data.repository.RecordingRepositoryImpl
import com.callrecorder.core.domain.repository.RecordingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding domain interfaces to data-layer implementations.
 *
 * Using [@Binds] (not [@Provides]) for interface binding — this is more
 * efficient since Hilt avoids generating an unnecessary wrapper.
 *
 * This is the only place in the codebase where [RecordingRepositoryImpl]
 * is referenced directly. All other code uses [RecordingRepository].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(
        impl: RecordingRepositoryImpl,
    ): RecordingRepository
}
