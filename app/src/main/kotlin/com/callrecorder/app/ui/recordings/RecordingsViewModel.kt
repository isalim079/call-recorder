package com.callrecorder.app.ui.recordings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.core.domain.model.CallType
import com.callrecorder.core.domain.model.RecordingFilter
import com.callrecorder.core.domain.model.RecordingSortOrder
import com.callrecorder.core.domain.usecase.DeleteRecordingUseCase
import com.callrecorder.core.domain.usecase.GetRecordingsUseCase
import com.callrecorder.core.domain.usecase.RenameRecordingUseCase
import com.callrecorder.core.domain.usecase.ToggleFavoriteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordingsViewModel @Inject constructor(
    private val getRecordingsUseCase: GetRecordingsUseCase,
    private val deleteRecordingUseCase: DeleteRecordingUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val renameRecordingUseCase: RenameRecordingUseCase,
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(RecordingSortOrder.NEWEST_FIRST)
    private val _filterCallType = MutableStateFlow<CallType?>(null)
    private val _filterFavoritesOnly = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _showDeleteConfirm = MutableStateFlow(false)
    private val _showRenameDialog = MutableStateFlow<Pair<Long, String>?>(null)

    private val recordings = combine(
        _sortOrder, _filterCallType, _filterFavoritesOnly, _searchQuery
    ) { sort, callType, favOnly, query ->
        RecordingFilter(
            callType      = callType,
            favoritesOnly = favOnly,
            query         = query,
        ) to sort
    }.flatMapLatest { (filter, sort) ->
        getRecordingsUseCase(filter, sort)
    }

    val uiState = combine(
        recordings,
        _sortOrder,
        _filterCallType,
        _filterFavoritesOnly,
        _searchQuery,
        _selectedIds,
        _showDeleteConfirm,
        _showRenameDialog,
    ) { arr ->
        @Suppress("UNCHECKED_CAST")
        val recs = arr[0] as List<*>
        val sort = arr[1] as RecordingSortOrder
        val callType = arr[2] as CallType?
        val favOnly = arr[3] as Boolean
        val query = arr[4] as String
        val selected = arr[5] as Set<Long>
        val showDelete = arr[6] as Boolean
        val renameDialog = arr[7] as Pair<Long, String>?

        @Suppress("UNCHECKED_CAST")
        RecordingsUiState(
            isLoading           = false,
            recordings          = recs as List<com.callrecorder.core.domain.model.Recording>,
            sortOrder           = sort,
            filterCallType      = callType,
            filterFavoritesOnly = favOnly,
            searchQuery         = query,
            selectedIds         = selected,
            isInSelectionMode   = selected.isNotEmpty(),
            showDeleteConfirm   = showDelete,
            showRenameDialog    = renameDialog != null,
            renameTargetId      = renameDialog?.first,
            renameInitialName   = renameDialog?.second ?: "",
        )
    }.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordingsUiState(),
    )

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortOrder(sort: RecordingSortOrder) { _sortOrder.value = sort }
    fun setFilterCallType(type: CallType?) { _filterCallType.value = type }
    fun setFavoritesOnly(favOnly: Boolean) { _filterFavoritesOnly.value = favOnly }

    fun toggleSelection(id: Long) {
        _selectedIds.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun clearSelection() { _selectedIds.value = emptySet() }

    fun requestDeleteSelected() { _showDeleteConfirm.value = true }
    fun cancelDelete() { _showDeleteConfirm.value = false }

    fun deleteSelected() {
        val ids = _selectedIds.value.toList()
        viewModelScope.launch {
            deleteRecordingUseCase.deleteAll(ids)
            _selectedIds.value = emptySet()
            _showDeleteConfirm.value = false
        }
    }

    fun deleteRecording(id: Long) {
        viewModelScope.launch { deleteRecordingUseCase(id) }
    }

    fun toggleFavorite(id: Long) {
        viewModelScope.launch { toggleFavoriteUseCase(id) }
    }

    fun showRenameDialog(id: Long, currentName: String) {
        _showRenameDialog.value = id to currentName
    }

    fun dismissRenameDialog() { _showRenameDialog.value = null }

    fun renameRecording(newName: String) {
        val id = _showRenameDialog.value?.first ?: return
        viewModelScope.launch {
            runCatching { renameRecordingUseCase(id, newName) }
            _showRenameDialog.value = null
        }
    }
}
