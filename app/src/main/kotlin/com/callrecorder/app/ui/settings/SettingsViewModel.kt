package com.callrecorder.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.core.data.preferences.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: String = "SYSTEM",
    val dynamicColors: Boolean = true,
    val audioQuality: String = "HIGH",
    val noiseCancellation: Boolean = true,
    val recordUnknown: Boolean = true,
    val recordEveryone: Boolean = true,
    val autoDeleteDays: Int = 0,
    val appLockEnabled: Boolean = false,
    val notificationEnabled: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // Nested combine — kotlinx only has typed overloads through 5 flows.
    private val recordingPrefs = combine(
        settingsRepository.getAudioQuality(),
        settingsRepository.getNoiseCancellation(),
        settingsRepository.getRecordUnknown(),
        settingsRepository.getRecordEveryone(),
        settingsRepository.getAutoDeleteDays(),
    ) { quality, noise, unknown, everyone, days ->
        RecordingPrefs(quality, noise, unknown, everyone, days)
    }

    private val otherPrefs = combine(
        settingsRepository.getTheme(),
        settingsRepository.getDynamicColors(),
        settingsRepository.getAppLockEnabled(),
        settingsRepository.getNotificationEnabled(),
    ) { theme, dynamic, lock, notif ->
        OtherPrefs(theme, dynamic, lock, notif)
    }

    val uiState: StateFlow<SettingsUiState> = combine(recordingPrefs, otherPrefs) { rec, other ->
        SettingsUiState(
            theme               = other.theme,
            dynamicColors       = other.dynamicColors,
            audioQuality        = rec.audioQuality,
            noiseCancellation   = rec.noiseCancellation,
            recordUnknown       = rec.recordUnknown,
            recordEveryone      = rec.recordEveryone,
            autoDeleteDays      = rec.autoDeleteDays,
            appLockEnabled      = other.appLockEnabled,
            notificationEnabled = other.notificationEnabled,
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setTheme(theme: String) =
        viewModelScope.launch { settingsRepository.setTheme(theme) }

    fun setDynamicColors(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setDynamicColors(enabled) }

    fun setAudioQuality(q: String) =
        viewModelScope.launch { settingsRepository.setAudioQuality(q) }

    fun setNoiseCancellation(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setNoiseCancellation(enabled) }

    fun setRecordUnknown(v: Boolean) =
        viewModelScope.launch { settingsRepository.setRecordUnknown(v) }

    fun setRecordEveryone(v: Boolean) =
        viewModelScope.launch { settingsRepository.setRecordEveryone(v) }

    fun setAutoDeleteDays(days: Int) =
        viewModelScope.launch { settingsRepository.setAutoDeleteDays(days) }

    fun setAppLock(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setAppLockEnabled(enabled) }

    fun setNotification(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setNotificationEnabled(enabled) }

    private data class RecordingPrefs(
        val audioQuality: String,
        val noiseCancellation: Boolean,
        val recordUnknown: Boolean,
        val recordEveryone: Boolean,
        val autoDeleteDays: Int,
    )

    private data class OtherPrefs(
        val theme: String,
        val dynamicColors: Boolean,
        val appLockEnabled: Boolean,
        val notificationEnabled: Boolean,
    )
}
