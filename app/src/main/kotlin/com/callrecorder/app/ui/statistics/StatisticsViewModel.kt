package com.callrecorder.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.core.domain.model.RecordingStatistics
import com.callrecorder.core.domain.usecase.GetStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatisticsUiState(
    val today: RecordingStatistics = RecordingStatistics(),
    val week: RecordingStatistics = RecordingStatistics(),
    val month: RecordingStatistics = RecordingStatistics(),
    val allTime: RecordingStatistics = RecordingStatistics(),
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getStatisticsUseCase: GetStatisticsUseCase,
) : ViewModel() {

    val uiState: StateFlow<StatisticsUiState> = combine(
        getStatisticsUseCase.today(),
        getStatisticsUseCase.thisWeek(),
        getStatisticsUseCase.thisMonth(),
        getStatisticsUseCase.allTime(),
    ) { today, week, month, allTime ->
        StatisticsUiState(today = today, week = week, month = month, allTime = allTime)
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState(),
    )
}
