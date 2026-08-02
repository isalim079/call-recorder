package com.callrecorder.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.core.data.storage.StorageManager
import com.callrecorder.core.domain.usecase.GetRecordingsUseCase
import com.callrecorder.core.domain.usecase.GetStatisticsUseCase
import com.callrecorder.core.domain.usecase.GetStorageInfoUseCase
import com.callrecorder.core.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getRecordingsUseCase: GetRecordingsUseCase,
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val getStorageInfoUseCase: GetStorageInfoUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val storageManager: StorageManager,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        getStatisticsUseCase.today(),
        getStatisticsUseCase.thisWeek(),
        getStatisticsUseCase.thisMonth(),
        getStatisticsUseCase.allTime(),
        getRecordingsUseCase(),
        getRecordingsUseCase(com.callrecorder.core.domain.model.RecordingFilter(favoritesOnly = true)),
        getStorageInfoUseCase.totalRecordingBytes(),
    ) { array ->
        val todayStats = array[0] as com.callrecorder.core.domain.model.RecordingStatistics
        val weekStats = array[1] as com.callrecorder.core.domain.model.RecordingStatistics
        val monthStats = array[2] as com.callrecorder.core.domain.model.RecordingStatistics
        val allTimeStats = array[3] as com.callrecorder.core.domain.model.RecordingStatistics
        @Suppress("UNCHECKED_CAST")
        val allRecordings = array[4] as List<com.callrecorder.core.domain.model.Recording>
        @Suppress("UNCHECKED_CAST")
        val favorites = array[5] as List<com.callrecorder.core.domain.model.Recording>
        val totalBytes = array[6] as Long

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
        )
    }.stateIn(
        scope            = viewModelScope,
        started          = SharingStarted.WhileSubscribed(5_000),
        initialValue     = HomeUiState(),
    )

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { toggleFavoriteUseCase(id) }
    }
}
