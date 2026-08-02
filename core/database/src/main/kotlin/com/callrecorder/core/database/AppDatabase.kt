package com.callrecorder.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.callrecorder.core.database.dao.RecordingDao
import com.callrecorder.core.database.entity.RecordingEntity

/**
 * Room database for the Call Recorder application.
 *
 * Contains a single table: `recordings`.
 *
 * Version history:
 * - Version 1: Initial schema.
 *
 * Migration policy:
 * - All schema migrations are additive (new columns have DEFAULT values).
 * - Destructive migrations are NOT used — user data must never be lost.
 * - Migration objects are defined in [AppDatabaseMigrations].
 *
 * Singleton instantiation is handled by Hilt in [DatabaseModule].
 */
@Database(
    entities = [RecordingEntity::class],
    version  = 1,
    exportSchema = true,   // Enables schema export for migration testing
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recordingDao(): RecordingDao

    companion object {
        const val DATABASE_NAME = "call_recorder.db"
    }
}
