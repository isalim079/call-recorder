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
    val audioQuality: String = "MEDIUM",
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

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.getTheme(),
        settingsRepository.getDynamicColors(),
        settingsRepository.getAudioQuality(),
        settingsRepository.getRecordUnknown(),
        settingsRepository.getRecordEveryone(),
        settingsRepository.getAutoDeleteDays(),
        settingsRepository.getAppLockEnabled(),
        settingsRepository.getNotificationEnabled(),
    ) { arr ->
        SettingsUiState(
            theme               = arr[0] as String,
            dynamicColors       = arr[1] as Boolean,
            audioQuality        = arr[2] as String,
            recordUnknown       = arr[3] as Boolean,
            recordEveryone      = arr[4] as Boolean,
            autoDeleteDays      = arr[5] as Int,
            appLockEnabled      = arr[6] as Boolean,
            notificationEnabled = arr[7] as Boolean,
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setTheme(theme: String)            = viewModelScope.launch { settingsRepository.setTheme(theme) }
    fun setDynamicColors(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColors(enabled) }
    fun setAudioQuality(q: String)         = viewModelScope.launch { settingsRepository.setAudioQuality(q) }
    fun setRecordUnknown(v: Boolean)       = viewModelScope.launch { settingsRepository.setRecordUnknown(v) }
    fun setRecordEveryone(v: Boolean)      = viewModelScope.launch { settingsRepository.setRecordEveryone(v) }
    fun setAutoDeleteDays(days: Int)       = viewModelScope.launch { settingsRepository.setAutoDeleteDays(days) }
    fun setAppLock(enabled: Boolean)       = viewModelScope.launch { settingsRepository.setAppLockEnabled(enabled) }
    fun setNotification(enabled: Boolean)  = viewModelScope.launch { settingsRepository.setNotificationEnabled(enabled) }
}
