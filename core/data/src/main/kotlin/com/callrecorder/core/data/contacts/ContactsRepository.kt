package com.callrecorder.core.data.contacts

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves contact display names and photo URIs from a phone number.
 *
 * Uses [ContactsContract] to query the system contacts database.
 * Results are NOT cached — the caller should cache results in the database
 * (stored in [RecordingEntity.contactName] / [RecordingEntity.contactPhotoUri]).
 *
 * Requires: [android.permission.READ_CONTACTS] (runtime permission).
 * Returns null gracefully if permission is denied or contact is not found.
 */
@Singleton
class ContactsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Look up a contact's display name for the given [phoneNumber].
     *
     * @return Contact display name, or null if not found.
     */
    fun getContactName(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        return try {
            queryContactName(phoneNumber)
        } catch (e: SecurityException) {
            Timber.w("READ_CONTACTS permission denied — cannot resolve contact name.")
            null
        } catch (e: Exception) {
            Timber.e(e, "Error resolving contact name for $phoneNumber")
            null
        }
    }

    /**
     * Look up a contact photo content URI for the given [phoneNumber].
     *
     * @return URI string (e.g. "content://com.android.contacts/contacts/42/photo"),
     *         or null if the contact has no photo.
     */
    fun getContactPhotoUri(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        return try {
            queryContactPhotoUri(phoneNumber)
        } catch (e: SecurityException) {
            Timber.w("READ_CONTACTS permission denied — cannot resolve contact photo.")
            null
        } catch (e: Exception) {
            Timber.e(e, "Error resolving contact photo for $phoneNumber")
            null
        }
    }

    /**
     * Convenience: resolve both name and photo in one query.
     *
     * @return Pair of (displayName, photoUri), either may be null.
     */
    fun resolveContact(phoneNumber: String): Pair<String?, String?> =
        getContactName(phoneNumber) to getContactPhotoUri(phoneNumber)

    // ── Private ─────────────────────────────────────────────────────────────

    private fun queryContactName(phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

    private fun queryContactPhotoUri(phoneNumber: String): String? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.PHOTO_URI,
            ),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val photoUri = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI)
                )
                return photoUri
            }
        }
        return null
    }
}
