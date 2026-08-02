package com.callrecorder.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.core.data.storage.StorageManager
import com.callrecorder.core.domain.usecase.DeleteRecordingUseCase
import com.callrecorder.core.domain.usecase.GetRecordingsUseCase
import com.callrecorder.core.domain.usecase.GetStatisticsUseCase
import com.callrecorder.core.domain.usecase.GetStorageInfoUseCase
import com.callrecorder.core.domain.usecase.RenameRecordingUseCase
import com.callrecorder.core.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRecordingsUseCase: GetRecordingsUseCase,
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val getStorageInfoUseCase: GetStorageInfoUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val deleteRecordingUseCase: DeleteRecordingUseCase,
    private val renameRecordingUseCase: RenameRecordingUseCase,
    private val storageManager: StorageManager,
) : ViewModel() {

    /** Holds the rename dialog state: (targetId, initialName) or null when hidden. */
    private val _renameDialog = MutableStateFlow<Pair<Long, String>?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        getStatisticsUseCase.today(),
        getStatisticsUseCase.thisWeek(),
        getStatisticsUseCase.thisMonth(),
        getStatisticsUseCase.allTime(),
        getRecordingsUseCase(),
        getRecordingsUseCase(com.callrecorder.core.domain.model.RecordingFilter(favoritesOnly = true)),
        getStorageInfoUseCase.totalRecordingBytes(),
        _renameDialog,
    ) { array ->
        val todayStats   = array[0] as com.callrecorder.core.domain.model.RecordingStatistics
        val weekStats    = array[1] as com.callrecorder.core.domain.model.RecordingStatistics
        val monthStats   = array[2] as com.callrecorder.core.domain.model.RecordingStatistics
        val allTimeStats = array[3] as com.callrecorder.core.domain.model.RecordingStatistics
        @Suppress("UNCHECKED_CAST")
        val allRecordings = array[4] as List<com.callrecorder.core.domain.model.Recording>
        @Suppress("UNCHECKED_CAST")
        val favorites    = array[5] as List<com.callrecorder.core.domain.model.Recording>
        val totalBytes   = array[6] as Long
        val renameDialog = array[7] as Pair<Long, String>?

        HomeUiState(
            isLoading             = false,
            todayStats            = todayStats,
            weekStats             = weekStats,
            monthStats            = monthStats,
            allTimeStats          = allTimeStats,
            recentRecordings      = allRecordings.take(5),
            favoriteRecordings    = favorites.take(5),
            totalStorageBytes     = totalBytes,
            availableStorageBytes = storageManager.getAvailableBytes(),
            showRenameDialog      = renameDialog != null,
            renameTargetId        = renameDialog?.first,
            renameInitialName     = renameDialog?.second ?: "",
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { toggleFavoriteUseCase(id) }
    }

    fun deleteRecording(id: Long) {
        viewModelScope.launch { deleteRecordingUseCase(id) }
    }

    fun showRenameDialog(id: Long, currentName: String) {
        _renameDialog.update { id to currentName }
    }

    fun dismissRenameDialog() {
        _renameDialog.value = null
    }

    fun renameRecording(newName: String) {
        val id = _renameDialog.value?.first ?: return
        viewModelScope.launch {
            runCatching { renameRecordingUseCase(id, newName) }
            _renameDialog.value = null
        }
    }
}
