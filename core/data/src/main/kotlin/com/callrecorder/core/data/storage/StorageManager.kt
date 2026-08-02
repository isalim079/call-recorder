package com.callrecorder.core.data.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.time.Month
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the local file system storage for call recordings.
 *
 * Folder structure:
 * ```
 * <app-files-dir>/recordings/
 *   2026/
 *     January/
 *     February/
 *     ...
 *     December/
 * ```
 *
 * File naming convention:
 * `YYYY-MM-DD_HH-mm-ss_<PHONE_NUMBER>.m4a`
 *
 * Uses the app's internal files directory (`context.filesDir`) by default.
 * Users can optionally choose an external directory in Settings (future feature).
 *
 * Design notes:
 * - Directory creation is done lazily on first access.
 * - File names sanitize the phone number to remove special characters.
 * - No internet required — all paths are local.
 */
@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val RECORDINGS_DIR = "callRecorder"
        private val FILE_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    }

    /**
     * Generate a new .m4a file path for a recording.
     *
     * Creates the directory structure if it doesn't exist.
     *
     * @param phoneNumber Raw phone number string. Special characters are sanitized.
     * @param timestamp   Unix epoch millis when the call started.
     * @return Absolute path to the new .m4a file (does NOT create the file itself).
     */
    fun createRecordingFilePath(phoneNumber: String, timestamp: Long): String {
        val dir = getRecordingDirectory(timestamp)
        ensureDirectory(dir)

        val sanitizedNumber = sanitizePhoneNumber(phoneNumber)
        val datePrefix = FILE_DATE_FORMAT.format(Date(timestamp))
        val fileName = "${datePrefix}_${sanitizedNumber}.m4a"

        return File(dir, fileName).absolutePath
    }

    /**
     * Get the base recordings directory.
     *
     * Uses external app-specific storage (`getExternalFilesDir("callRecorder")`).
     * This path resolves to: /sdcard/Android/data/com.callrecorder.app/files/callRecorder
     * - No `WRITE_EXTERNAL_STORAGE` permission required on Android 10+.
     * - Files survive as long as the app is installed.
     * - Visible to users via a file manager.
     * Falls back to internal storage if external is not available.
     */
    fun getBaseRecordingsDir(): File {
        val externalDir = context.getExternalFilesDir(RECORDINGS_DIR)
        return externalDir ?: File(context.filesDir, RECORDINGS_DIR)
    }

    /**
     * Get total bytes used by all recordings.
     */
    fun getTotalUsedBytes(): Long =
        getBaseRecordingsDir().walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }

    /**
     * Get available space in bytes on the partition where recordings are stored.
     */
    fun getAvailableBytes(): Long =
        getBaseRecordingsDir().usableSpace

    /**
     * Returns true if available storage is below [thresholdBytes].
     * Default threshold: 50 MB.
     */
    fun isStorageLow(thresholdBytes: Long = 50L * 1024 * 1024): Boolean =
        getAvailableBytes() < thresholdBytes

    // ── Private helpers ────────────────────────────────────────────────────

    private fun getRecordingDirectory(timestamp: Long): File {
        val date = Date(timestamp)
        val year = SimpleDateFormat("yyyy", Locale.US).format(date)
        val month = Month.of(
            SimpleDateFormat("MM", Locale.US).format(date).toInt()
        ).getDisplayName(java.time.format.TextStyle.FULL, Locale.US)

        return File(getBaseRecordingsDir(), "$year/$month")
    }

    private fun ensureDirectory(dir: File) {
        if (!dir.exists()) {
            val created = dir.mkdirs()
            if (!created) {
                Timber.w("Failed to create directory: ${dir.absolutePath}")
            }
        }
    }

    /**
     * Replace characters that are illegal in file names with underscores.
     * Preserves digits and the '+' sign (international format).
     */
    private fun sanitizePhoneNumber(phoneNumber: String): String {
        if (phoneNumber.isBlank()) return "unknown"
        return phoneNumber
            .filter { it.isDigit() || it == '+' }
            .take(20)   // Cap length to avoid excessively long file names
            .ifBlank { "unknown" }
    }
}
