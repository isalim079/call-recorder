package com.callrecorder.app.ui.home

import com.callrecorder.core.domain.model.Recording
import com.callrecorder.core.domain.model.RecordingStatistics

data class HomeUiState(
    val isLoading: Boolean = true,
    val todayStats: RecordingStatistics = RecordingStatistics(),
    val weekStats: RecordingStatistics = RecordingStatistics(),
    val monthStats: RecordingStatistics = RecordingStatistics(),
    val allTimeStats: RecordingStatistics = RecordingStatistics(),
    val recentRecordings: List<Recording> = emptyList(),
    val favoriteRecordings: List<Recording> = emptyList(),
    val totalStorageBytes: Long = 0L,
    val availableStorageBytes: Long = 0L,
)
