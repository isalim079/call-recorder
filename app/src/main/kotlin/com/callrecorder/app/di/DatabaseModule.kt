package com.callrecorder.app.di

import android.content.Context
import androidx.room.Room
import com.callrecorder.core.database.AppDatabase
import com.callrecorder.core.database.dao.RecordingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room database and DAO instances.
 *
 * The database is a singleton — one instance for the entire app lifetime.
 * Room handles thread safety internally; always use suspend functions or Flow.
 *
 * Migration policy:
 * - No destructive migration — user data is never discarded.
 * - Add [Migration] objects to [AppDatabaseMigrations] as schema evolves.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME,
    )
        // Never allow destructive migration — user recordings must not be lost
        .fallbackToDestructiveMigrationOnDowngrade()
        // Add migration objects here as the schema evolves:
        // .addMigrations(AppDatabaseMigrations.MIGRATION_1_2)
        .build()

    @Provides
    @Singleton
    fun provideRecordingDao(database: AppDatabase): RecordingDao =
        database.recordingDao()
}
