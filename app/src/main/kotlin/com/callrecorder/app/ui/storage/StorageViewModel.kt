package com.callrecorder.app.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.core.data.storage.StorageManager
import com.callrecorder.core.domain.model.Recording
import com.callrecorder.core.domain.usecase.DeleteRecordingUseCase
import com.callrecorder.core.domain.usecase.GetStorageInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageUiState(
    val totalRecordingBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val largestRecordings: List<Recording> = emptyList(),
    val oldestRecordings: List<Recording> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val getStorageInfoUseCase: GetStorageInfoUseCase,
    private val deleteRecordingUseCase: DeleteRecordingUseCase,
    private val storageManager: StorageManager,
) : ViewModel() {

    val uiState: StateFlow<StorageUiState> = combine(
        getStorageInfoUseCase.totalRecordingBytes(),
        getStorageInfoUseCase.largestRecordings(10),
        getStorageInfoUseCase.oldestRecordings(10),
    ) { totalBytes, largest, oldest ->
        StorageUiState(
            totalRecordingBytes = totalBytes,
            availableBytes = storageManager.getAvailableBytes(),
            largestRecordings = largest,
            oldestRecordings = oldest,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StorageUiState(),
    )

    fun deleteRecording(id: Long) {
        viewModelScope.launch {
            deleteRecordingUseCase(id)
        }
    }

    fun cleanOldRecordings(ids: List<Long>) {
        viewModelScope.launch {
            deleteRecordingUseCase.deleteAll(ids)
        }
    }
}
