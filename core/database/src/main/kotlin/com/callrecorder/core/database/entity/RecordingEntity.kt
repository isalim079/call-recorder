package com.callrecorder.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a persisted call recording.
 *
 * Maps 1:1 to the `recordings` table.
 * Converted to/from [com.callrecorder.core.domain.model.Recording] via [RecordingMapper].
 *
 * Design decisions:
 * - [phoneNumber] and [timestamp] are indexed for fast search/filter queries.
 * - [isDeleted] enables soft-delete (trash) without permanent data loss.
 * - Contact info ([contactName], [contactPhotoUri]) is cached at recording time;
 *   it is not refreshed live from the Contacts database to avoid unnecessary queries.
 * - [callType] stored as a String for readability in SQLite inspection tools.
 * - [quality] stored as a String for forward compatibility with new quality levels.
 */
@Entity(
    tableName = "recordings",
    indices = [
        Index(value = ["phone_number"]),
        Index(value = ["timestamp"]),
        Index(value = ["is_favorite"]),
        Index(value = ["is_deleted"]),
    ]
)
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** Raw phone number as reported by TelephonyManager. */
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    /** Resolved contact display name at recording time; null if unknown. */
    @ColumnInfo(name = "contact_name")
    val contactName: String?,

    /** Content URI string for the contact photo; null if unavailable. */
    @ColumnInfo(name = "contact_photo_uri")
    val contactPhotoUri: String?,

    /** "INCOMING", "OUTGOING", or "UNKNOWN" — stored as a string. */
    @ColumnInfo(name = "call_type")
    val callType: String,

    /** Duration of the recording in milliseconds. */
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,

    /** Unix epoch milliseconds when the call started. */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    /** Absolute path to the .m4a file on device storage. */
    @ColumnInfo(name = "file_path")
    val filePath: String,

    /** File size in bytes. */
    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,

    /** "LOW", "MEDIUM", or "HIGH" — stored as a string. */
    @ColumnInfo(name = "quality")
    val quality: String,

    /** True if the user has starred this recording. */
    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    val isFavorite: Boolean = false,

    /** True if soft-deleted (moved to trash). False = active recording. */
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,

    /** Optional user-written note. Empty string if no notes. */
    @ColumnInfo(name = "notes", defaultValue = "")
    val notes: String = "",
)
