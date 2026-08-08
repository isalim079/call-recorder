package com.callrecorder.core.data.calllog

import android.content.Context
import android.provider.CallLog
import com.callrecorder.core.data.contacts.ContactsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallLogRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactsRepository: ContactsRepository,
) {
    fun getRecent(limit: Int = 80): List<CallLogEntry> {
        return try {
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
            )
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC",
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numIdx = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIdx = c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val typeIdx = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIdx = c.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durIdx = c.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                buildList {
                    while (c.moveToNext() && size < limit) {
                        val number = c.getString(numIdx).orEmpty()
                        val cached = c.getString(nameIdx)
                        val name = cached?.takeIf { it.isNotBlank() }
                            ?: contactsRepository.getContactName(number)
                        add(
                            CallLogEntry(
                                id = c.getLong(idIdx),
                                number = number,
                                name = name,
                                type = c.getInt(typeIdx),
                                dateMs = c.getLong(dateIdx),
                                durationSec = c.getLong(durIdx),
                                photoUri = contactsRepository.getContactPhotoUri(number),
                            )
                        )
                    }
                }
            } ?: emptyList()
        } catch (e: SecurityException) {
            Timber.w("READ_CALL_LOG denied")
            emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Call log query failed")
            emptyList()
        }
    }
}
