package com.callrecorder.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ── DataStore instance (extension on Context) ──────────────────────────────
private val Context.dataStore: DataStore<Preferences>
    by preferencesDataStore(name = "call_recorder_settings")

/**
 * Persists user preferences via Jetpack DataStore (typed key-value storage).
 *
 * All reads return [Flow] for real-time updates to the Settings UI.
 * All writes are suspend functions dispatching on the DataStore dispatcher.
 *
 * Preference keys are private and accessed only through named functions
 * to prevent typo bugs and maintain a clear public API.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore: DataStore<Preferences> = context.dataStore

    // ── Keys ───────────────────────────────────────────────────────────────
    private object Keys {
        val THEME                = stringPreferencesKey("theme")
        val DYNAMIC_COLORS       = booleanPreferencesKey("dynamic_colors")
        val AUDIO_QUALITY        = stringPreferencesKey("audio_quality")
        val RECORDING_FOLDER     = stringPreferencesKey("recording_folder")
        val RECORD_UNKNOWN       = booleanPreferencesKey("record_unknown")
        val RECORD_EVERYONE      = booleanPreferencesKey("record_everyone")
        val AUTO_DELETE_DAYS     = intPreferencesKey("auto_delete_days")
        val APP_LOCK_ENABLED     = booleanPreferencesKey("app_lock_enabled")
        val APP_LOCK_BIOMETRIC   = booleanPreferencesKey("app_lock_biometric")
        val PIN_HASH             = stringPreferencesKey("pin_hash")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val BACKUP_FOLDER        = stringPreferencesKey("backup_folder")
        val RESTORE_FOLDER       = stringPreferencesKey("restore_folder")
        val RECORDING_ACTIVE     = booleanPreferencesKey("recording_active")
    }

    // ── Defaults ───────────────────────────────────────────────────────────
    object Defaults {
        const val THEME          = "SYSTEM"
        const val DYNAMIC_COLORS = true
        const val AUDIO_QUALITY  = "MEDIUM"
        const val RECORD_UNKNOWN = true
        const val RECORD_EVERYONE = true
        const val AUTO_DELETE_DAYS = 0      // 0 = never
        const val APP_LOCK_ENABLED = false
        const val APP_LOCK_BIOMETRIC = false
        const val NOTIFICATION_ENABLED = true
    }

    // ── Reads ──────────────────────────────────────────────────────────────

    fun getTheme(): Flow<String> =
        dataStore.data.map { it[Keys.THEME] ?: Defaults.THEME }

    fun getDynamicColors(): Flow<Boolean> =
        dataStore.data.map { it[Keys.DYNAMIC_COLORS] ?: Defaults.DYNAMIC_COLORS }

    fun getAudioQuality(): Flow<String> =
        dataStore.data.map { it[Keys.AUDIO_QUALITY] ?: Defaults.AUDIO_QUALITY }

    fun getRecordingFolder(): Flow<String?> =
        dataStore.data.map { it[Keys.RECORDING_FOLDER] }

    fun getRecordUnknown(): Flow<Boolean> =
        dataStore.data.map { it[Keys.RECORD_UNKNOWN] ?: Defaults.RECORD_UNKNOWN }

    fun getRecordEveryone(): Flow<Boolean> =
        dataStore.data.map { it[Keys.RECORD_EVERYONE] ?: Defaults.RECORD_EVERYONE }

    fun getAutoDeleteDays(): Flow<Int> =
        dataStore.data.map { it[Keys.AUTO_DELETE_DAYS] ?: Defaults.AUTO_DELETE_DAYS }

    fun getAppLockEnabled(): Flow<Boolean> =
        dataStore.data.map { it[Keys.APP_LOCK_ENABLED] ?: Defaults.APP_LOCK_ENABLED }

    fun getAppLockBiometric(): Flow<Boolean> =
        dataStore.data.map { it[Keys.APP_LOCK_BIOMETRIC] ?: Defaults.APP_LOCK_BIOMETRIC }

    fun getPinHash(): Flow<String?> =
        dataStore.data.map { it[Keys.PIN_HASH] }

    fun getNotificationEnabled(): Flow<Boolean> =
        dataStore.data.map { it[Keys.NOTIFICATION_ENABLED] ?: Defaults.NOTIFICATION_ENABLED }

    // ── Writes ─────────────────────────────────────────────────────────────

    suspend fun setTheme(theme: String) =
        dataStore.edit { it[Keys.THEME] = theme }

    suspend fun setDynamicColors(enabled: Boolean) =
        dataStore.edit { it[Keys.DYNAMIC_COLORS] = enabled }

    suspend fun setAudioQuality(quality: String) =
        dataStore.edit { it[Keys.AUDIO_QUALITY] = quality }

    suspend fun setRecordingFolder(path: String?) =
        dataStore.edit {
            if (path != null) it[Keys.RECORDING_FOLDER] = path
            else it.remove(Keys.RECORDING_FOLDER)
        }

    suspend fun setRecordUnknown(enabled: Boolean) =
        dataStore.edit { it[Keys.RECORD_UNKNOWN] = enabled }

    suspend fun setRecordEveryone(enabled: Boolean) =
        dataStore.edit { it[Keys.RECORD_EVERYONE] = enabled }

    suspend fun setAutoDeleteDays(days: Int) =
        dataStore.edit { it[Keys.AUTO_DELETE_DAYS] = days }

    suspend fun setAppLockEnabled(enabled: Boolean) =
        dataStore.edit { it[Keys.APP_LOCK_ENABLED] = enabled }

    suspend fun setAppLockBiometric(enabled: Boolean) =
        dataStore.edit { it[Keys.APP_LOCK_BIOMETRIC] = enabled }

    /**
     * Store a BCrypt hash of the user's PIN.
     * NEVER store the raw PIN.
     */
    suspend fun setPinHash(hash: String?) =
        dataStore.edit {
            if (hash != null) it[Keys.PIN_HASH] = hash
            else it.remove(Keys.PIN_HASH)
        }

    suspend fun setNotificationEnabled(enabled: Boolean) =
        dataStore.edit { it[Keys.NOTIFICATION_ENABLED] = enabled }
}
