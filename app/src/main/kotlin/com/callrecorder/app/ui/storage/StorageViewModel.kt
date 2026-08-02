package com.callrecorder.app.ui.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.core.data.storage.StorageManager
import com.callrecorder.core.domain.model.Recording
import com.callrecorder.core.domain.usecase.DeleteRecordingUseCase
import com.callrecorder.core.domain.usecase.GetStorageInfoUseCase
import com.callrecorder.core.domain.usecase.RenameRecordingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageUiState(
    val totalRecordingBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val largestRecordings: List<Recording> = emptyList(),
    val oldestRecordings: List<Recording> = emptyList(),
    val isLoading: Boolean = true,
    // Rename dialog state
    val showRenameDialog: Boolean = false,
    val renameTargetId: Long? = null,
    val renameInitialName: String = "",
)

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val getStorageInfoUseCase: GetStorageInfoUseCase,
    private val deleteRecordingUseCase: DeleteRecordingUseCase,
    private val renameRecordingUseCase: RenameRecordingUseCase,
    private val storageManager: StorageManager,
) : ViewModel() {

    private val _renameDialog = MutableStateFlow<Pair<Long, String>?>(null)

    val uiState: StateFlow<StorageUiState> = combine(
        getStorageInfoUseCase.totalRecordingBytes(),
        getStorageInfoUseCase.largestRecordings(10),
        getStorageInfoUseCase.oldestRecordings(10),
        _renameDialog,
    ) { totalBytes, largest, oldest, renameDialog ->
        StorageUiState(
            totalRecordingBytes = totalBytes,
            availableBytes      = storageManager.getAvailableBytes(),
            largestRecordings   = largest,
            oldestRecordings    = oldest,
            isLoading           = false,
            showRenameDialog    = renameDialog != null,
            renameTargetId      = renameDialog?.first,
            renameInitialName   = renameDialog?.second ?: "",
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = StorageUiState(),
    )

    fun deleteRecording(id: Long) {
        viewModelScope.launch { deleteRecordingUseCase(id) }
    }

    fun cleanOldRecordings(ids: List<Long>) {
        viewModelScope.launch { deleteRecordingUseCase.deleteAll(ids) }
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
