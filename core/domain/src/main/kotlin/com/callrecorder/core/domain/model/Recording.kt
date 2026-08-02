package com.callrecorder.core.domain.model

/**
 * Core domain model representing a single recorded call.
 *
 * This is an immutable value object — all state changes create a new copy.
 * Mapped to/from [RecordingEntity] in the database layer.
 *
 * @param id           Unique identifier (Room auto-generated primary key).
 * @param phoneNumber  Raw phone number string as reported by TelephonyManager.
 * @param contactName  Resolved contact display name, or null if unknown.
 * @param contactPhotoUri Content URI for the contact photo, or null.
 * @param callType     Direction of the call ([CallType.INCOMING] / [CallType.OUTGOING]).
 * @param durationMs   Recording duration in milliseconds.
 * @param timestamp    Unix epoch millis when the recording started.
 * @param filePath     Absolute path to the .m4a file on device storage.
 * @param fileSizeBytes Size of the .m4a file in bytes.
 * @param quality      Audio quality setting used during recording.
 * @param isFavorite   Whether the user has starred this recording.
 * @param isDeleted    Soft-delete flag — true means moved to trash (not yet purged).
 * @param notes        Optional user-written notes about this recording.
 */
data class Recording(
    val id: Long = 0L,
    val phoneNumber: String,
    val contactName: String?,
    val contactPhotoUri: String?,
    val callType: CallType,
    val durationMs: Long,
    val timestamp: Long,
    val filePath: String,
    val fileSizeBytes: Long,
    val quality: AudioQuality,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val notes: String = "",
) {
    /**
     * The display name for this recording — contact name if available,
     * otherwise the raw phone number, or "Unknown Caller" if both are absent.
     */
    val displayName: String
        get() = when {
            !contactName.isNullOrBlank() -> contactName
            phoneNumber.isNotBlank()     -> phoneNumber
            else                          -> "Unknown Caller"
        }

    /**
     * Duration formatted as mm:ss or h:mm:ss.
     */
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1_000
            val hours   = totalSeconds / 3_600
            val minutes = (totalSeconds % 3_600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
            }
        }

    /**
     * File size formatted as a human-readable string (e.g. "1.2 MB").
     */
    val formattedFileSize: String
        get() {
            val kb = fileSizeBytes / 1_024.0
            val mb = kb / 1_024.0
            return when {
                mb >= 1.0 -> "%.1f MB".format(mb)
                kb >= 1.0 -> "%.0f KB".format(kb)
                else      -> "$fileSizeBytes B"
            }
        }
}
